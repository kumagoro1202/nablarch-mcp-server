package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 検索品質指標の計算ロジックテスト。
 *
 * <p>RRFスコアの計算精度、重複マージの正確性、
 * topK切り捨ての精密さを数学的に検証する。</p>
 *
 * <p>テスト対象: {@link HybridSearchService#rrfMerge(List, List, int, int)}</p>
 */
@ExtendWith(MockitoExtension.class)
class SearchQualityTest {

    @Mock
    private BM25SearchService bm25SearchService;

    @Mock
    private VectorSearchService vectorSearchService;

    private HybridSearchService service;

    @BeforeEach
    void setUp() {
        service = new HybridSearchService(bm25SearchService, vectorSearchService);
    }

    /**
     * テスト用の検索結果を生成するヘルパー。
     */
    private static SearchResult createSearchResult(String id, String content, double score) {
        return new SearchResult(id, content, score,
                Map.of("source", "nablarch-document"), null);
    }

    @Nested
    @DisplayName("RRFスコア計算精度検証")
    class RrfScorePrecisionTests {

        @Test
        @DisplayName("k=60で既知の入力に対し、期待スコアを小数6桁精度で検証")
        void rrfScorePrecisionWithKnownInputs() {
            int k = 60;

            // BM25: doc-A=rank1, doc-B=rank2, doc-C=rank3
            List<SearchResult> bm25 = List.of(
                    createSearchResult("doc-A", "ドキュメントA", 0.95),
                    createSearchResult("doc-B", "ドキュメントB", 0.85),
                    createSearchResult("doc-C", "ドキュメントC", 0.75)
            );

            // Vector: doc-B=rank1, doc-A=rank2, doc-D=rank3
            List<SearchResult> vector = List.of(
                    createSearchResult("doc-B", "ドキュメントB", 0.92),
                    createSearchResult("doc-A", "ドキュメントA", 0.88),
                    createSearchResult("doc-D", "ドキュメントD", 0.80)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // doc-A: BM25 rank=1 → 1/(60+1) + Vector rank=2 → 1/(60+2)
            double expectedA = 1.0 / (k + 1) + 1.0 / (k + 2);
            // doc-B: BM25 rank=2 → 1/(60+2) + Vector rank=1 → 1/(60+1)
            double expectedB = 1.0 / (k + 2) + 1.0 / (k + 1);
            // doc-C: BM25 rank=3 → 1/(60+3) のみ
            double expectedC = 1.0 / (k + 3);
            // doc-D: Vector rank=3 → 1/(60+3) のみ
            double expectedD = 1.0 / (k + 3);

            // doc-AとBは同じスコア（対称的なランク）
            assertEquals(expectedA, expectedB, 1e-10, "doc-AとBは同じRRFスコアであるべき");

            // 結果からスコアを取得して検証
            double scoreA = findScore(results, "doc-A");
            double scoreB = findScore(results, "doc-B");
            double scoreC = findScore(results, "doc-C");
            double scoreD = findScore(results, "doc-D");

            assertEquals(expectedA, scoreA, 1e-6, "doc-AのRRFスコア");
            assertEquals(expectedB, scoreB, 1e-6, "doc-BのRRFスコア");
            assertEquals(expectedC, scoreC, 1e-6, "doc-CのRRFスコア");
            assertEquals(expectedD, scoreD, 1e-6, "doc-DのRRFスコア");
        }

        @Test
        @DisplayName("非対称ランクでのRRFスコア計算精度")
        void asymmetricRankPrecision() {
            int k = 60;

            // BM25: A=rank1, B=rank2
            List<SearchResult> bm25 = List.of(
                    createSearchResult("doc-A", "ドキュメントA", 0.95),
                    createSearchResult("doc-B", "ドキュメントB", 0.85)
            );

            // Vector: A=rank1, B=rank3（間にXが入る）
            List<SearchResult> vector = List.of(
                    createSearchResult("doc-A", "ドキュメントA", 0.92),
                    createSearchResult("doc-X", "ドキュメントX", 0.88),
                    createSearchResult("doc-B", "ドキュメントB", 0.80)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // doc-A: 1/(60+1) + 1/(60+1) = 2/(60+1)
            double expectedA = 2.0 / (k + 1);
            // doc-B: 1/(60+2) + 1/(60+3)
            double expectedB = 1.0 / (k + 2) + 1.0 / (k + 3);

            double scoreA = findScore(results, "doc-A");
            double scoreB = findScore(results, "doc-B");

            assertEquals(expectedA, scoreA, 1e-6, "doc-AのRRFスコア");
            assertEquals(expectedB, scoreB, 1e-6, "doc-BのRRFスコア");

            // AがBより高いスコア
            assertTrue(scoreA > scoreB, "doc-Aはdoc-Bより高スコアであるべき");
        }

        @Test
        @DisplayName("単一ソースのRRFスコア = 1/(k+rank)")
        void singleSourceRrfScore() {
            int k = 60;

            List<SearchResult> bm25 = List.of(
                    createSearchResult("doc-only", "単一ソースの文書", 0.90)
            );
            List<SearchResult> vector = List.of();

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // 片方のみ: 1/(60+1)
            double expected = 1.0 / (k + 1);
            assertEquals(expected, results.get(0).score(), 1e-6);
        }
    }

    @Nested
    @DisplayName("重複マージ精度")
    class DuplicateMergePrecisionTests {

        @Test
        @DisplayName("同一IDの結果がRRFで正しく合算される")
        void duplicateIdsCorrectlyMerged() {
            int k = 60;

            List<SearchResult> bm25 = List.of(
                    createSearchResult("dup-1", "重複テスト文書。ハンドラキュー設定。", 0.90)
            );
            List<SearchResult> vector = List.of(
                    createSearchResult("dup-1", "重複テスト文書。ハンドラキュー設定。", 0.88)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // 重複は1件に統合される
            assertEquals(1, results.size());
            assertEquals("dup-1", results.get(0).id());

            // スコアは両方のRRFスコアの合算
            double expectedScore = 1.0 / (k + 1) + 1.0 / (k + 1);
            assertEquals(expectedScore, results.get(0).score(), 1e-6);
        }

        @Test
        @DisplayName("重複IDのメタデータは最初に出現したものが保持される")
        void duplicatePreservesFirstMetadata() {
            int k = 60;

            List<SearchResult> bm25 = List.of(
                    new SearchResult("dup-meta", "テスト", 0.90,
                            Map.of("source", "bm25-source"), "https://bm25.example.com")
            );
            List<SearchResult> vector = List.of(
                    new SearchResult("dup-meta", "テスト", 0.88,
                            Map.of("source", "vector-source"), "https://vector.example.com")
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // BM25結果が先に処理されるので、そのメタデータが保持される
            assertEquals("bm25-source", results.get(0).metadata().get("source"));
            assertEquals("https://bm25.example.com", results.get(0).sourceUrl());
        }
    }

    @Nested
    @DisplayName("topK切り捨て精度")
    class TopKTruncationTests {

        @Test
        @DisplayName("結果50件 → topK=5 で正確に5件返却")
        void truncateTo5FromMany() {
            int k = 60;

            // BM25で25件生成
            List<SearchResult> bm25 = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                bm25.add(createSearchResult("bm25-" + i, "BM25文書" + i, 0.95 - i * 0.01));
            }

            // Vectorで25件生成（一部重複あり）
            List<SearchResult> vector = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                vector.add(createSearchResult("vec-" + i, "Vector文書" + i, 0.93 - i * 0.01));
            }

            List<SearchResult> results = service.rrfMerge(bm25, vector, 5, k);

            assertEquals(5, results.size(), "topK=5で正確に5件返却されるべき");

            // スコア降順であることを確認
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                        "結果はスコア降順であるべき");
            }
        }

        @Test
        @DisplayName("結果がtopKより少ない場合は全件返却")
        void fewerResultsThanTopK() {
            int k = 60;

            List<SearchResult> bm25 = List.of(
                    createSearchResult("only-1", "唯一の文書", 0.90)
            );
            List<SearchResult> vector = List.of(
                    createSearchResult("only-2", "もう一つの文書", 0.85)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            assertEquals(2, results.size(), "全2件が返却されるべき");
        }

        @Test
        @DisplayName("topK=1 で最高スコアの1件のみ返却")
        void topKOne() {
            int k = 60;

            List<SearchResult> bm25 = List.of(
                    createSearchResult("top-A", "トップ文書", 0.95),
                    createSearchResult("top-B", "2番手", 0.85)
            );
            List<SearchResult> vector = List.of(
                    createSearchResult("top-A", "トップ文書", 0.90),
                    createSearchResult("top-C", "3番手", 0.80)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 1, k);

            assertEquals(1, results.size());
            // top-Aは両方に出現するので最高スコア
            assertEquals("top-A", results.get(0).id());
        }
    }

    /**
     * 結果リストから指定IDのスコアを取得する。
     */
    private double findScore(List<SearchResult> results, String id) {
        return results.stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ID " + id + " が結果に見つからない"))
                .score();
    }
}

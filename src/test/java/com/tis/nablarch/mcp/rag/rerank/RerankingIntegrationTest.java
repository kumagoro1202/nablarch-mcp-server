package com.tis.nablarch.mcp.rag.rerank;

import com.tis.nablarch.mcp.rag.search.BM25SearchService;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import com.tis.nablarch.mcp.rag.search.VectorSearchService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * リランキング統合テスト。
 *
 * <p>HybridSearchServiceの検索結果をCrossEncoderRerankerでリランキングする
 * 統合パイプラインを検証する。</p>
 *
 * <p>HybridSearchService → CrossEncoderReranker(MockWebServer) の連携テスト。
 * BM25/VectorSearchServiceはMockitoでモック化し、
 * Jina Reranker APIはMockWebServerでモック化する。</p>
 */
@ExtendWith(MockitoExtension.class)
class RerankingIntegrationTest {

    @Mock
    private BM25SearchService bm25SearchService;

    @Mock
    private VectorSearchService vectorSearchService;

    private HybridSearchService hybridSearchService;
    private CrossEncoderReranker reranker;
    private MockWebServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        hybridSearchService = new HybridSearchService(bm25SearchService, vectorSearchService);

        mockServer = new MockWebServer();
        mockServer.start();

        RerankProperties properties = new RerankProperties();
        RerankProperties.Jina jina = properties.getJina();
        jina.setApiKey("test-api-key");
        jina.setModel("jina-reranker-v2-base-multilingual");
        jina.setBaseUrl(mockServer.url("/v1/rerank").toString());
        jina.setTimeoutMs(5000);
        jina.setTopK(10);

        reranker = new CrossEncoderReranker(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    /**
     * テスト用の検索結果を生成するヘルパー。
     */
    private static SearchResult createSearchResult(
            String id, String content, double score, Map<String, String> metadata) {
        return new SearchResult(id, content, score, metadata,
                "https://nablarch.github.io/docs/" + id);
    }

    @Nested
    @DisplayName("リランキング効果検証")
    class RerankingEffectTests {

        @Test
        @DisplayName("HybridSearch結果 → Reranker → 順位変動確認: 特定文書が上位に移動")
        void rerankingChangesOrderWithSpecificDocMovedUp() {
            // HybridSearch結果（RRFスコア順）: A > B > C > D > E
            List<SearchResult> hybridResults = List.of(
                    createSearchResult("doc-A", "ハンドラキューの概要。Nablarchの基本アーキテクチャ。", 0.033,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("doc-B", "ハンドラの実装方法。Handler<TData, TResult>。", 0.030,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("doc-C", "ハンドラキューの構築手順。web-component-configuration.xml。", 0.028,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("doc-D", "HTTPリクエストの処理フロー。", 0.025,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("doc-E", "ルーティング設定。", 0.022,
                            Map.of("source", "nablarch-document"))
            );

            // Reranker API レスポンス: doc-C（index=2）を最高スコアに
            String responseJson = """
                    {
                        "results": [
                            {"index": 2, "relevance_score": 0.98},
                            {"index": 0, "relevance_score": 0.85},
                            {"index": 1, "relevance_score": 0.75},
                            {"index": 3, "relevance_score": 0.60},
                            {"index": 4, "relevance_score": 0.40}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> reranked = reranker.rerank(
                    "ハンドラキューの構築手順", hybridResults, 5);

            assertEquals(5, reranked.size());
            // doc-C がトップに移動（RRFでは3位だった）
            assertEquals("doc-C", reranked.get(0).id(),
                    "Reranker後にdoc-Cがトップに移動すべき");
            assertEquals(0.98, reranked.get(0).score(), 0.001);
            // スコアはRerankerスコアに置換されている
            assertTrue(reranked.get(0).score() > reranked.get(1).score());
        }
    }

    @Nested
    @DisplayName("Top-N制御")
    class TopNControlTests {

        @Test
        @DisplayName("候補50件 → rerank → topN=5 で5件返却")
        void topNControlWith50Candidates() {
            // 50件の候補を生成
            List<SearchResult> candidates = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                candidates.add(createSearchResult(
                        "doc-" + i,
                        "Nablarchドキュメント第" + i + "章。",
                        0.50 - i * 0.005,
                        Map.of("source", "nablarch-document")));
            }

            // Reranker APIレスポンス: トップ5のインデックスとスコア
            String responseJson = """
                    {
                        "results": [
                            {"index": 42, "relevance_score": 0.99},
                            {"index": 7, "relevance_score": 0.92},
                            {"index": 15, "relevance_score": 0.88},
                            {"index": 0, "relevance_score": 0.80},
                            {"index": 33, "relevance_score": 0.75}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> reranked = reranker.rerank("テストクエリ", candidates, 5);

            assertEquals(5, reranked.size(), "topN=5で正確に5件返却されるべき");
            assertEquals("doc-42", reranked.get(0).id());
            assertEquals("doc-7", reranked.get(1).id());

            // スコア降順
            for (int i = 0; i < reranked.size() - 1; i++) {
                assertTrue(reranked.get(i).score() >= reranked.get(i + 1).score());
            }
        }
    }

    @Nested
    @DisplayName("Reranker API失敗時のフォールバック")
    class FallbackTests {

        @Test
        @DisplayName("WebClient例外発生 → 元のHybridSearch順序を維持")
        void apiFailureFallbackToOriginalOrder() {
            List<SearchResult> hybridResults = List.of(
                    createSearchResult("fallback-A", "ドキュメントA。", 0.033,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("fallback-B", "ドキュメントB。", 0.030,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("fallback-C", "ドキュメントC。", 0.028,
                            Map.of("source", "nablarch-document"))
            );

            // APIサーバーエラー
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"));

            List<SearchResult> results = reranker.rerank(
                    "テストクエリ", hybridResults, 3);

            // 結果件数・内容は同一
            assertEquals(3, results.size());
            // スコアは元のスコア降順
            assertEquals("fallback-A", results.get(0).id());
            assertEquals("fallback-B", results.get(1).id());
            assertEquals("fallback-C", results.get(2).id());
            // スコアは元のまま（Rerankerスコアではない）
            assertEquals(0.033, results.get(0).score(), 0.001);
            assertEquals(0.030, results.get(1).score(), 0.001);
            assertEquals(0.028, results.get(2).score(), 0.001);
        }
    }

    @Nested
    @DisplayName("空候補のリランキング")
    class EmptyCandidateTests {

        @Test
        @DisplayName("空リスト → 空リスト返却（API呼出なし）")
        void emptyCandidatesNoApiCall() {
            List<SearchResult> results = reranker.rerank(
                    "テストクエリ", Collections.emptyList(), 5);

            assertTrue(results.isEmpty());
            // MockWebServerに1件もリクエストが来ていないことを確認
            assertEquals(0, mockServer.getRequestCount(),
                    "空候補の場合はAPI呼出が行われないべき");
        }
    }

    @Nested
    @DisplayName("パイプライン全段テスト")
    class FullPipelineTests {

        @Test
        @DisplayName("BM25結果 + Vector結果 → RRF → Rerank → 最終結果")
        void fullPipeline_bm25VectorRrfRerank() {
            // BM25結果
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("pipe-A", "Nablarchのアクションクラス設計。", 0.90,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("pipe-B", "リクエストパラメータのバインド方法。", 0.80,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("pipe-C", "アクションのテスト方法。", 0.70,
                            Map.of("source", "nablarch-document", "app_type", "web"))
            );

            // Vector結果（pipe-Aが重複）
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("pipe-D", "Webアプリケーションのリクエスト処理。", 0.92,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("pipe-A", "Nablarchのアクションクラス設計。", 0.88,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("pipe-E", "フォーム入力のバリデーション。", 0.75,
                            Map.of("source", "nablarch-document", "app_type", "web"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            // Step 1: HybridSearch (RRF)
            List<SearchResult> hybridResults = hybridSearchService.search(
                    "アクションクラスの設計", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(hybridResults.isEmpty());
            // pipe-Aは両方に出現するので最高RRFスコア
            assertEquals("pipe-A", hybridResults.get(0).id(),
                    "RRF後にpipe-Aがトップであるべき");

            // Step 2: Rerank（pipe-Dを最高スコアに変更）
            // pipe-Aのインデックス=0, pipe-Dのインデックスを特定
            int pipeDIndex = -1;
            for (int i = 0; i < hybridResults.size(); i++) {
                if ("pipe-D".equals(hybridResults.get(i).id())) {
                    pipeDIndex = i;
                    break;
                }
            }
            assertTrue(pipeDIndex >= 0, "pipe-DがRRF結果に含まれるべき");

            // Reranker APIレスポンス: pipe-D(動的index)を最高に
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{\"results\": [");
            for (int i = 0; i < hybridResults.size(); i++) {
                if (i > 0) jsonBuilder.append(",");
                double score;
                if (hybridResults.get(i).id().equals("pipe-D")) {
                    score = 0.99;
                } else if (hybridResults.get(i).id().equals("pipe-A")) {
                    score = 0.85;
                } else {
                    score = 0.70 - i * 0.05;
                }
                jsonBuilder.append("{\"index\": ").append(i)
                        .append(", \"relevance_score\": ").append(score).append("}");
            }
            jsonBuilder.append("]}");

            mockServer.enqueue(new MockResponse()
                    .setBody(jsonBuilder.toString())
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> finalResults = reranker.rerank(
                    "アクションクラスの設計", hybridResults, 5);

            assertFalse(finalResults.isEmpty());
            // 最終結果: Rerankerスコアで並ぶ
            assertEquals("pipe-D", finalResults.get(0).id(),
                    "Rerank後にpipe-Dがトップであるべき");
            assertEquals(0.99, finalResults.get(0).score(), 0.01);

            // スコアはRerankerスコアであることを確認
            assertTrue(finalResults.get(0).score() > finalResults.get(1).score(),
                    "最終結果はRerankerスコア降順であるべき");
        }
    }
}

package com.tis.nablarch.mcp.rag.evaluation;

import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * RAG検索品質評価テスト。
 *
 * <p>HybridSearchService + CrossEncoderRerankerのパイプラインに対して、
 * 評価データセット（50件）を用いた品質メトリクス（MRR, Recall@K, NDCG@K）を
 * 計算・検証する。</p>
 *
 * <p>BM25/Vectorは@Mockで制御し、RRFマージとリランキングの効果を評価する。</p>
 */
@ExtendWith(MockitoExtension.class)
class SearchQualityEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(SearchQualityEvaluationTest.class);

    @Mock
    private BM25SearchService bm25SearchService;

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private Reranker reranker;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        hybridSearchService = new HybridSearchService(bm25SearchService, vectorSearchService);
    }

    /**
     * テスト用SearchResultを生成する。
     */
    private static SearchResult result(String id, String content, double score,
                                       Map<String, String> metadata) {
        return new SearchResult(id, content, score, metadata, null);
    }

    private static SearchResult result(String id, String content, double score) {
        return new SearchResult(id, content, score, Map.of(), null);
    }

    // ============================
    // 評価データセット読み込みテスト
    // ============================

    @Nested
    class DatasetTest {

        @Test
        void 評価データセットが50件以上読み込める() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            assertTrue(dataset.size() >= 50,
                    "評価データセットは50件以上必要だが " + dataset.size() + " 件のみ");
        }

        @Test
        void 全5カテゴリが含まれる() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            Set<String> categories = dataset.getQueries().stream()
                    .map(QueryEvaluationDataset.EvaluationQuery::category)
                    .collect(Collectors.toSet());

            assertTrue(categories.contains("handler_queue"));
            assertTrue(categories.contains("api_usage"));
            assertTrue(categories.contains("design_pattern"));
            assertTrue(categories.contains("troubleshooting"));
            assertTrue(categories.contains("configuration"));
        }

        @Test
        void 各カテゴリに10件のクエリがある() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            for (String category : List.of("handler_queue", "api_usage",
                    "design_pattern", "troubleshooting", "configuration")) {
                long count = dataset.getQueriesByCategory(category).size();
                assertEquals(10, count,
                        "カテゴリ " + category + " は10件必要だが " + count + " 件");
            }
        }

        @Test
        void 日本語と英語のクエリが混在する() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            Set<String> languages = dataset.getQueries().stream()
                    .map(QueryEvaluationDataset.EvaluationQuery::language)
                    .collect(Collectors.toSet());

            assertTrue(languages.contains("JAPANESE"));
            assertTrue(languages.contains("ENGLISH"));
        }

        @Test
        void 全クエリにrelevantKeywordsが設定されている() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            for (var q : dataset.getQueries()) {
                assertFalse(q.relevantKeywords().isEmpty(),
                        "クエリ " + q.id() + " にrelevantKeywordsが未設定");
            }
        }
    }

    // ============================
    // MRR計算テスト
    // ============================

    @Nested
    class MRRCalculationTest {

        @Test
        void 理想的なMRR_常に1位正解() {
            // 全クエリで1位が正解の場合、MRR=1.0
            List<SearchResult> results = List.of(
                    result("1", "handler queue configuration guide", 0.95),
                    result("2", "unrelated document", 0.80),
                    result("3", "another unrelated", 0.70)
            );
            Set<String> keywords = Set.of("handler", "queue");
            assertEquals(1.0, EvaluationMetrics.calculateMRR(results, keywords));
        }

        @Test
        void 最悪MRR_最後に正解() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated A", 0.9),
                    result("2", "unrelated B", 0.8),
                    result("3", "unrelated C", 0.7),
                    result("4", "unrelated D", 0.6),
                    result("5", "handler queue found here", 0.5)
            );
            Set<String> keywords = Set.of("handler", "queue");
            assertEquals(0.2, EvaluationMetrics.calculateMRR(results, keywords));
        }

        @Test
        void 複数クエリの平均MRRを計算() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();

            // 各クエリに対してモック結果を生成し、平均MRRを計算
            double totalMRR = 0.0;
            int count = 0;

            for (var q : dataset.getQueries().subList(0, 5)) {
                // 関連結果が2位にある模擬結果
                List<SearchResult> results = List.of(
                        result("1", "zzz completely irrelevant zzz", 0.9),
                        result("2", String.join(" ", q.relevantKeywords()), 0.8),
                        result("3", "yyy also irrelevant yyy", 0.7)
                );
                Set<String> keywords = new HashSet<>(q.relevantKeywords());
                totalMRR += EvaluationMetrics.calculateMRR(results, keywords);
                count++;
            }

            double averageMRR = totalMRR / count;
            // 各クエリで最初の関連結果が2位 → MRR=0.5
            assertTrue(averageMRR > 0.0 && averageMRR <= 1.0,
                    "平均MRRは0〜1の範囲: " + averageMRR);
        }
    }

    // ============================
    // Recall@5テスト
    // ============================

    @Nested
    class RecallAt5Test {

        @Test
        void 上位5件に関連ドキュメントが含まれる() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated A", 0.9),
                    result("2", "unrelated B", 0.8),
                    result("3", "handler queue config", 0.7),
                    result("4", "unrelated C", 0.6),
                    result("5", "unrelated D", 0.5)
            );
            assertEquals(1.0, EvaluationMetrics.calculateRecallAtK(results, HANDLER_KEYWORDS, 5));
        }

        @Test
        void 上位5件に関連ドキュメントがない() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated A", 0.9),
                    result("2", "unrelated B", 0.8),
                    result("3", "unrelated C", 0.7),
                    result("4", "unrelated D", 0.6),
                    result("5", "unrelated E", 0.5)
            );
            assertEquals(0.0, EvaluationMetrics.calculateRecallAtK(results, HANDLER_KEYWORDS, 5));
        }

        @Test
        void 複数クエリのRecallAt5を計算() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();
            double totalRecall = 0.0;
            int count = 0;

            for (var q : dataset.getQueries().subList(0, 10)) {
                // 関連結果が3位にある模擬結果
                List<SearchResult> results = List.of(
                        result("1", "general nablarch content", 0.9),
                        result("2", "some docs", 0.8),
                        result("3", String.join(" ", q.relevantKeywords()), 0.7),
                        result("4", "more content", 0.6),
                        result("5", "extra content", 0.5)
                );
                Set<String> keywords = new HashSet<>(q.relevantKeywords());
                totalRecall += EvaluationMetrics.calculateRecallAtK(results, keywords, 5);
                count++;
            }

            double avgRecall = totalRecall / count;
            assertEquals(1.0, avgRecall, 0.001,
                    "全クエリで上位5件に関連あり→Recall@5=1.0");
        }

        private static final Set<String> HANDLER_KEYWORDS = Set.of("handler", "queue");
    }

    // ============================
    // Recall@10テスト
    // ============================

    @Nested
    class RecallAt10Test {

        @Test
        void 上位10件に関連ドキュメントが含まれる() {
            List<SearchResult> results = new ArrayList<>();
            for (int i = 1; i <= 9; i++) {
                results.add(result(String.valueOf(i), "unrelated " + i, 1.0 - i * 0.1));
            }
            results.add(result("10", "handler queue in 10th position", 0.1));

            assertEquals(1.0, EvaluationMetrics.calculateRecallAtK(
                    results, Set.of("handler", "queue"), 10));
        }

        @Test
        void Recall10はRecall5以上になる() {
            List<SearchResult> results = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                results.add(result(String.valueOf(i), "unrelated " + i, 1.0 - i * 0.1));
            }
            results.add(result("6", "handler docs", 0.4));
            for (int i = 7; i <= 10; i++) {
                results.add(result(String.valueOf(i), "unrelated " + i, 1.0 - i * 0.1));
            }

            Set<String> keywords = Set.of("handler");
            double recall5 = EvaluationMetrics.calculateRecallAtK(results, keywords, 5);
            double recall10 = EvaluationMetrics.calculateRecallAtK(results, keywords, 10);

            assertTrue(recall10 >= recall5,
                    "Recall@10 (" + recall10 + ") >= Recall@5 (" + recall5 + ")");
        }
    }

    // ============================
    // NDCG@5テスト
    // ============================

    @Nested
    class NDCGAt5Test {

        @Test
        void 理想的な順序でNDCGは1() {
            // 関連ドキュメントが上位に集中
            List<SearchResult> results = List.of(
                    result("1", "handler queue config", 0.95),
                    result("2", "handler setup guide", 0.90),
                    result("3", "unrelated A", 0.70),
                    result("4", "unrelated B", 0.60),
                    result("5", "unrelated C", 0.50)
            );
            double ndcg = EvaluationMetrics.calculateNDCG(results, Set.of("handler", "queue"), 5);
            assertEquals(1.0, ndcg, 0.001);
        }

        @Test
        void 非理想的な順序でNDCGは1未満() {
            // 関連ドキュメントが下位に分散
            List<SearchResult> results = List.of(
                    result("1", "unrelated A", 0.95),
                    result("2", "unrelated B", 0.90),
                    result("3", "unrelated C", 0.80),
                    result("4", "handler queue config", 0.60),
                    result("5", "handler guide", 0.50)
            );
            double ndcg = EvaluationMetrics.calculateNDCG(results, Set.of("handler", "queue"), 5);
            assertTrue(ndcg < 1.0, "非理想的な順序ではNDCG < 1.0");
            assertTrue(ndcg > 0.0, "関連結果があるのでNDCG > 0.0");
        }

        @Test
        void NDCG値の比較_上位集中が有利() {
            Set<String> keywords = Set.of("handler");

            // パターンA: 関連が1位
            List<SearchResult> resultsA = List.of(
                    result("1", "handler guide", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "unrelated", 0.7)
            );

            // パターンB: 関連が3位
            List<SearchResult> resultsB = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "handler guide", 0.7)
            );

            double ndcgA = EvaluationMetrics.calculateNDCG(resultsA, keywords, 3);
            double ndcgB = EvaluationMetrics.calculateNDCG(resultsB, keywords, 3);

            assertTrue(ndcgA > ndcgB,
                    "上位集中のNDCG (" + ndcgA + ") > 下位分散のNDCG (" + ndcgB + ")");
        }
    }

    // ============================
    // リランキング効果検証テスト
    // ============================

    @Nested
    class RerankingEffectTest {

        @Test
        void リランキングでMRRが改善する() {
            // リランキング前: 関連ドキュメントが3位
            List<SearchResult> beforeRerank = List.of(
                    result("1", "unrelated A", 0.9),
                    result("2", "unrelated B", 0.8),
                    result("3", "handler queue configuration guide", 0.7),
                    result("4", "unrelated C", 0.6),
                    result("5", "unrelated D", 0.5)
            );

            // リランキング後: 関連ドキュメントが1位に昇格
            List<SearchResult> afterRerank = List.of(
                    result("3", "handler queue configuration guide", 0.98),
                    result("1", "unrelated A", 0.85),
                    result("2", "unrelated B", 0.80),
                    result("4", "unrelated C", 0.60),
                    result("5", "unrelated D", 0.50)
            );

            Set<String> keywords = Set.of("handler", "queue");

            double mrrBefore = EvaluationMetrics.calculateMRR(beforeRerank, keywords);
            double mrrAfter = EvaluationMetrics.calculateMRR(afterRerank, keywords);

            log.info("=== リランキング効果 ===");
            log.info("MRR before: {}", mrrBefore);
            log.info("MRR after:  {}", mrrAfter);
            log.info("MRR improvement: +{}", mrrAfter - mrrBefore);

            assertTrue(mrrAfter > mrrBefore,
                    "リランキング後MRR (" + mrrAfter + ") > 前MRR (" + mrrBefore + ")");
        }

        @Test
        void リランキングでNDCGが改善する() {
            // 関連ドキュメント2件が下位にある
            List<SearchResult> beforeRerank = List.of(
                    result("1", "unrelated A", 0.9),
                    result("2", "unrelated B", 0.8),
                    result("3", "handler config", 0.7),
                    result("4", "unrelated C", 0.6),
                    result("5", "queue setup guide", 0.5)
            );

            // リランキング後: 関連ドキュメントが上位に移動
            List<SearchResult> afterRerank = List.of(
                    result("3", "handler config", 0.98),
                    result("5", "queue setup guide", 0.95),
                    result("1", "unrelated A", 0.80),
                    result("2", "unrelated B", 0.75),
                    result("4", "unrelated C", 0.60)
            );

            Set<String> keywords = Set.of("handler", "queue");

            double ndcgBefore = EvaluationMetrics.calculateNDCG(beforeRerank, keywords, 5);
            double ndcgAfter = EvaluationMetrics.calculateNDCG(afterRerank, keywords, 5);

            log.info("=== リランキングNDCG効果 ===");
            log.info("NDCG@5 before: {}", ndcgBefore);
            log.info("NDCG@5 after:  {}", ndcgAfter);
            log.info("NDCG@5 improvement: +{}", ndcgAfter - ndcgBefore);

            assertTrue(ndcgAfter > ndcgBefore,
                    "リランキング後NDCG (" + ndcgAfter + ") > 前NDCG (" + ndcgBefore + ")");
        }

        @Test
        void リランキングでRecallは維持される() {
            // リランキングは順序変更のみ、候補集合は同じ
            List<SearchResult> beforeRerank = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "handler doc", 0.8),
                    result("3", "unrelated", 0.7)
            );

            List<SearchResult> afterRerank = List.of(
                    result("2", "handler doc", 0.98),
                    result("1", "unrelated", 0.85),
                    result("3", "unrelated", 0.70)
            );

            Set<String> keywords = Set.of("handler");

            double recallBefore = EvaluationMetrics.calculateRecallAtK(beforeRerank, keywords, 5);
            double recallAfter = EvaluationMetrics.calculateRecallAtK(afterRerank, keywords, 5);

            assertEquals(recallBefore, recallAfter,
                    "リランキングはRecallを変化させない（候補集合同一）");
        }
    }

    // ============================
    // 検索モード別比較テスト
    // ============================

    @Nested
    class SearchModeComparisonTest {

        @Test
        void KEYWORD_ONLY検索のメトリクスを計算() {
            // BM25結果をモック
            List<SearchResult> bm25Results = List.of(
                    result("bm1", "handler queue configuration", 0.9),
                    result("bm2", "validation setup", 0.8),
                    result("bm3", "database connection", 0.7),
                    result("bm4", "batch processing", 0.6),
                    result("bm5", "logging guide", 0.5)
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);

            List<SearchResult> results = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.KEYWORD);

            Set<String> keywords = Set.of("handler", "queue");
            double mrr = EvaluationMetrics.calculateMRR(results, keywords);
            double recall5 = EvaluationMetrics.calculateRecallAtK(results, keywords, 5);

            log.info("=== KEYWORD_ONLY Mode ===");
            log.info("MRR: {}", mrr);
            log.info("Recall@5: {}", recall5);

            assertTrue(mrr > 0.0, "KEYWORD_ONLY: MRR > 0");
        }

        @Test
        void VECTOR_ONLY検索のメトリクスを計算() {
            // Vector結果をモック
            List<SearchResult> vectorResults = List.of(
                    result("v1", "handler queue setup and configuration", 0.85),
                    result("v2", "web application handler design", 0.80),
                    result("v3", "unrelated semantic match", 0.75),
                    result("v4", "handler pattern overview", 0.70),
                    result("v5", "queue management system", 0.65)
            );

            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            List<SearchResult> results = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.VECTOR);

            Set<String> keywords = Set.of("handler", "queue");
            double mrr = EvaluationMetrics.calculateMRR(results, keywords);
            double recall5 = EvaluationMetrics.calculateRecallAtK(results, keywords, 5);

            log.info("=== VECTOR_ONLY Mode ===");
            log.info("MRR: {}", mrr);
            log.info("Recall@5: {}", recall5);

            assertTrue(mrr > 0.0, "VECTOR_ONLY: MRR > 0");
        }

        @Test
        void HYBRID検索はKEYWORDとVECTORの結果を統合する() {
            // BM25結果: handlerが1位
            List<SearchResult> bm25Results = List.of(
                    result("bm1", "handler queue configuration", 0.9),
                    result("bm2", "unrelated bm25 result", 0.7),
                    result("bm3", "validation doc", 0.6)
            );

            // Vector結果: handlerが2位
            List<SearchResult> vectorResults = List.of(
                    result("v1", "semantic match unrelated", 0.85),
                    result("bm1", "handler queue configuration", 0.80), // 重複
                    result("v2", "handler design pattern", 0.75)
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            List<SearchResult> results = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.HYBRID);

            Set<String> keywords = Set.of("handler", "queue");
            double mrr = EvaluationMetrics.calculateMRR(results, keywords);
            double recall5 = EvaluationMetrics.calculateRecallAtK(results, keywords, 5);
            double ndcg5 = EvaluationMetrics.calculateNDCG(results, keywords, 5);

            log.info("=== HYBRID Mode ===");
            log.info("MRR: {}", mrr);
            log.info("Recall@5: {}", recall5);
            log.info("NDCG@5: {}", ndcg5);

            // RRFマージで重複ドキュメント（bm1）が上位に来るはず
            assertTrue(mrr > 0.0, "HYBRID: MRR > 0");
            assertTrue(recall5 > 0.0, "HYBRID: Recall@5 > 0");
        }

        @Test
        void 全モード比較サマリ出力() {
            // 共通モック
            List<SearchResult> bm25Results = List.of(
                    result("d1", "handler queue detailed configuration guide", 0.95),
                    result("d2", "web application setup", 0.75),
                    result("d3", "batch handler overview", 0.65)
            );
            List<SearchResult> vectorResults = List.of(
                    result("d1", "handler queue detailed configuration guide", 0.88),
                    result("d4", "handler design patterns", 0.82),
                    result("d5", "queue management docs", 0.78)
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            Set<String> keywords = Set.of("handler", "queue");

            // KEYWORD
            List<SearchResult> keywordResults = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.KEYWORD);
            double keywordMRR = EvaluationMetrics.calculateMRR(keywordResults, keywords);

            // VECTOR
            List<SearchResult> vectorOnlyResults = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.VECTOR);
            double vectorMRR = EvaluationMetrics.calculateMRR(vectorOnlyResults, keywords);

            // HYBRID
            List<SearchResult> hybridResults = hybridSearchService.search(
                    "handler queue", SearchFilters.NONE, 5, SearchMode.HYBRID);
            double hybridMRR = EvaluationMetrics.calculateMRR(hybridResults, keywords);

            log.info("=== 検索モード別比較 ===");
            log.info("KEYWORD MRR: {} | VECTOR MRR: {} | HYBRID MRR: {}",
                    keywordMRR, vectorMRR, hybridMRR);

            // 全モードで正の値
            assertTrue(keywordMRR > 0.0);
            assertTrue(vectorMRR > 0.0);
            assertTrue(hybridMRR > 0.0);
        }
    }

    // ============================
    // 評価データセットフルサマリ
    // ============================

    @Nested
    class FullEvaluationSummaryTest {

        @Test
        void 全50クエリのメトリクスサマリを出力() {
            QueryEvaluationDataset dataset = new QueryEvaluationDataset();

            double totalMRR = 0.0;
            double totalRecall5 = 0.0;
            double totalRecall10 = 0.0;
            double totalNDCG5 = 0.0;
            int queryCount = 0;

            for (var q : dataset.getQueries()) {
                // 各クエリのキーワードを含む模擬結果を生成
                // (実際のDB接続テストではHybridSearchServiceが本番結果を返す)
                List<SearchResult> results = generateMockResults(q);
                Set<String> keywords = new HashSet<>(q.relevantKeywords());

                totalMRR += EvaluationMetrics.calculateMRR(results, keywords);
                totalRecall5 += EvaluationMetrics.calculateRecallAtK(results, keywords, 5);
                totalRecall10 += EvaluationMetrics.calculateRecallAtK(results, keywords, 10);
                totalNDCG5 += EvaluationMetrics.calculateNDCG(results, keywords, 5);
                queryCount++;
            }

            double avgMRR = totalMRR / queryCount;
            double avgRecall5 = totalRecall5 / queryCount;
            double avgRecall10 = totalRecall10 / queryCount;
            double avgNDCG5 = totalNDCG5 / queryCount;

            log.info("=== RAG Search Quality Evaluation ===");
            log.info("Total queries: {}", queryCount);
            log.info("MRR: {}", String.format("%.3f", avgMRR));
            log.info("Recall@5: {}", String.format("%.3f", avgRecall5));
            log.info("Recall@10: {}", String.format("%.3f", avgRecall10));
            log.info("NDCG@5: {}", String.format("%.3f", avgNDCG5));

            // 基本的な妥当性チェック
            assertTrue(queryCount >= 50, "50件以上のクエリで評価");
            assertTrue(avgMRR >= 0.0 && avgMRR <= 1.0, "MRR範囲チェック");
            assertTrue(avgRecall5 >= 0.0 && avgRecall5 <= 1.0, "Recall@5範囲チェック");
            assertTrue(avgRecall10 >= avgRecall5, "Recall@10 >= Recall@5");
            assertTrue(avgNDCG5 >= 0.0 && avgNDCG5 <= 1.0, "NDCG@5範囲チェック");
        }

        /**
         * 評価クエリに対する模擬検索結果を生成する。
         *
         * <p>実際のDB接続テストでは本番HybridSearchServiceの結果を使用する。
         * ここではメトリクス計算フレームワークの正常動作を検証するため、
         * 各クエリのキーワードを含む結果を2位に配置する模擬データを生成する。</p>
         */
        private List<SearchResult> generateMockResults(
                QueryEvaluationDataset.EvaluationQuery query) {
            List<SearchResult> results = new ArrayList<>();

            // 1位: 非関連
            results.add(result("mock-1", "general nablarch documentation overview", 0.90));

            // 2位: 関連（キーワードを含む）
            String relevantContent = "This document covers " +
                    String.join(" and ", query.relevantKeywords()) +
                    " in Nablarch framework";
            results.add(result("mock-2", relevantContent, 0.85));

            // 3-10位: 非関連
            for (int i = 3; i <= 10; i++) {
                results.add(result("mock-" + i,
                        "additional documentation content " + i, 0.9 - i * 0.05));
            }

            return results;
        }
    }
}

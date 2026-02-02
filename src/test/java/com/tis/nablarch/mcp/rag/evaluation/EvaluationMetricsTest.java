package com.tis.nablarch.mcp.rag.evaluation;

import com.tis.nablarch.mcp.rag.search.SearchResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EvaluationMetrics}のテスト。
 *
 * <p>MRR、Recall@K、NDCG@Kの各メトリクス計算の正確性と
 * エッジケースを検証する。</p>
 */
class EvaluationMetricsTest {

    private static final Set<String> KEYWORDS = Set.of("handler", "queue");

    /**
     * テスト用SearchResultを生成する。
     */
    private static SearchResult result(String id, String content, double score) {
        return new SearchResult(id, content, score, Map.of(), null);
    }

    // ============================
    // MRRテスト
    // ============================

    @Nested
    class MRRTest {

        @Test
        void 最初の結果が関連の場合MRRは1() {
            List<SearchResult> results = List.of(
                    result("1", "handler queue configuration", 0.9),
                    result("2", "unrelated content", 0.8)
            );
            assertEquals(1.0, EvaluationMetrics.calculateMRR(results, KEYWORDS));
        }

        @Test
        void 二番目の結果が関連の場合MRRは0_5() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated content", 0.9),
                    result("2", "handler queue configuration", 0.8)
            );
            assertEquals(0.5, EvaluationMetrics.calculateMRR(results, KEYWORDS));
        }

        @Test
        void 三番目の結果が関連の場合MRRは約0_333() {
            List<SearchResult> results = List.of(
                    result("1", "no match", 0.9),
                    result("2", "still no match", 0.8),
                    result("3", "handler configuration", 0.7)
            );
            assertEquals(1.0 / 3.0, EvaluationMetrics.calculateMRR(results, KEYWORDS), 0.001);
        }

        @Test
        void 関連結果がない場合MRRは0() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "also unrelated", 0.8)
            );
            assertEquals(0.0, EvaluationMetrics.calculateMRR(results, KEYWORDS));
        }

        @Test
        void 空リストの場合MRRは0() {
            assertEquals(0.0, EvaluationMetrics.calculateMRR(List.of(), KEYWORDS));
        }

        @Test
        void nullリストの場合MRRは0() {
            assertEquals(0.0, EvaluationMetrics.calculateMRR(null, KEYWORDS));
        }

        @Test
        void nullキーワードの場合MRRは0() {
            List<SearchResult> results = List.of(result("1", "handler", 0.9));
            assertEquals(0.0, EvaluationMetrics.calculateMRR(results, null));
        }

        @Test
        void 空キーワードの場合MRRは0() {
            List<SearchResult> results = List.of(result("1", "handler", 0.9));
            assertEquals(0.0, EvaluationMetrics.calculateMRR(results, Set.of()));
        }
    }

    // ============================
    // Recall@Kテスト
    // ============================

    @Nested
    class RecallAtKTest {

        @Test
        void 上位5件に関連結果がある場合Recallは1() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "handler queue", 0.7),
                    result("4", "unrelated", 0.6),
                    result("5", "unrelated", 0.5)
            );
            assertEquals(1.0, EvaluationMetrics.calculateRecallAtK(results, KEYWORDS, 5));
        }

        @Test
        void 上位5件に関連結果がない場合Recallは0() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "unrelated", 0.7),
                    result("4", "unrelated", 0.6),
                    result("5", "unrelated", 0.5),
                    result("6", "handler queue", 0.4) // K=5の外
            );
            assertEquals(0.0, EvaluationMetrics.calculateRecallAtK(results, KEYWORDS, 5));
        }

        @Test
        void K10で関連結果がある場合Recallは1() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "unrelated", 0.7),
                    result("4", "unrelated", 0.6),
                    result("5", "unrelated", 0.5),
                    result("6", "unrelated", 0.4),
                    result("7", "unrelated", 0.3),
                    result("8", "handler found here", 0.2)
            );
            assertEquals(1.0, EvaluationMetrics.calculateRecallAtK(results, KEYWORDS, 10));
        }

        @Test
        void 空リストの場合Recallは0() {
            assertEquals(0.0, EvaluationMetrics.calculateRecallAtK(List.of(), KEYWORDS, 5));
        }

        @Test
        void K0以下の場合Recallは0() {
            List<SearchResult> results = List.of(result("1", "handler", 0.9));
            assertEquals(0.0, EvaluationMetrics.calculateRecallAtK(results, KEYWORDS, 0));
        }

        @Test
        void 結果数がKより少ない場合は結果数で計算() {
            List<SearchResult> results = List.of(
                    result("1", "handler queue", 0.9),
                    result("2", "unrelated", 0.8)
            );
            assertEquals(1.0, EvaluationMetrics.calculateRecallAtK(results, KEYWORDS, 10));
        }
    }

    // ============================
    // countRelevantAtKテスト
    // ============================

    @Nested
    class CountRelevantAtKTest {

        @Test
        void 上位5件中の関連数を正しくカウントする() {
            List<SearchResult> results = List.of(
                    result("1", "handler config", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "queue setup", 0.7),
                    result("4", "handler queue doc", 0.6),
                    result("5", "unrelated", 0.5)
            );
            assertEquals(3, EvaluationMetrics.countRelevantAtK(results, KEYWORDS, 5));
        }

        @Test
        void 関連結果がない場合は0() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9)
            );
            assertEquals(0, EvaluationMetrics.countRelevantAtK(results, KEYWORDS, 5));
        }
    }

    // ============================
    // NDCG@Kテスト
    // ============================

    @Nested
    class NDCGTest {

        @Test
        void 全て関連の場合NDCGは1() {
            List<SearchResult> results = List.of(
                    result("1", "handler config", 0.9),
                    result("2", "queue setup", 0.8),
                    result("3", "handler queue doc", 0.7)
            );
            assertEquals(1.0, EvaluationMetrics.calculateNDCG(results, KEYWORDS, 3), 0.001);
        }

        @Test
        void 理想的な順序でNDCGは1() {
            // 2件関連、1件非関連。関連が上位に来ている理想的な順序
            List<SearchResult> results = List.of(
                    result("1", "handler config", 0.9),
                    result("2", "queue setup", 0.8),
                    result("3", "unrelated content", 0.7)
            );
            assertEquals(1.0, EvaluationMetrics.calculateNDCG(results, KEYWORDS, 3), 0.001);
        }

        @Test
        void 非理想的な順序でNDCGは1未満() {
            // 関連が下位に来ている非理想的な順序
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "unrelated", 0.8),
                    result("3", "handler config", 0.7)
            );
            double ndcg = EvaluationMetrics.calculateNDCG(results, KEYWORDS, 3);
            assertTrue(ndcg > 0.0);
            assertTrue(ndcg < 1.0);
        }

        @Test
        void 全て非関連の場合NDCGは0() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "also unrelated", 0.8)
            );
            assertEquals(0.0, EvaluationMetrics.calculateNDCG(results, KEYWORDS, 5));
        }

        @Test
        void 空リストの場合NDCGは0() {
            assertEquals(0.0, EvaluationMetrics.calculateNDCG(List.of(), KEYWORDS, 5));
        }

        @Test
        void NDCG値は0から1の範囲() {
            List<SearchResult> results = List.of(
                    result("1", "unrelated", 0.9),
                    result("2", "handler config", 0.8),
                    result("3", "unrelated", 0.7),
                    result("4", "queue doc", 0.6),
                    result("5", "unrelated", 0.5)
            );
            double ndcg = EvaluationMetrics.calculateNDCG(results, KEYWORDS, 5);
            assertTrue(ndcg >= 0.0 && ndcg <= 1.0,
                    "NDCG should be in [0,1] but was: " + ndcg);
        }
    }

    // ============================
    // isRelevantテスト
    // ============================

    @Nested
    class IsRelevantTest {

        @Test
        void キーワードを含む場合はtrue() {
            SearchResult r = result("1", "This is about handler queue", 0.9);
            assertTrue(EvaluationMetrics.isRelevant(r, KEYWORDS));
        }

        @Test
        void キーワードを含まない場合はfalse() {
            SearchResult r = result("1", "completely unrelated content", 0.9);
            assertFalse(EvaluationMetrics.isRelevant(r, KEYWORDS));
        }

        @Test
        void 大文字小文字を無視して判定する() {
            SearchResult r = result("1", "HANDLER QUEUE CONFIG", 0.9);
            assertTrue(EvaluationMetrics.isRelevant(r, KEYWORDS));
        }

        @Test
        void nullの結果はfalse() {
            assertFalse(EvaluationMetrics.isRelevant(null, KEYWORDS));
        }
    }

    // ============================
    // log2テスト
    // ============================

    @Nested
    class Log2Test {

        @Test
        void log2の計算が正しい() {
            assertEquals(0.0, EvaluationMetrics.log2(1), 0.001);
            assertEquals(1.0, EvaluationMetrics.log2(2), 0.001);
            assertEquals(2.0, EvaluationMetrics.log2(4), 0.001);
            assertEquals(3.0, EvaluationMetrics.log2(8), 0.001);
        }
    }
}

package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link HybridSearchService} のユニットテスト。
 *
 * <p>BM25SearchServiceとVectorSearchServiceをモック化し、
 * モード切替、RRF統合、グレースフルデグレードを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private BM25SearchService bm25SearchService;

    @Mock
    private VectorSearchService vectorSearchService;

    private HybridSearchService service;

    @BeforeEach
    void setUp() {
        service = new HybridSearchService(bm25SearchService, vectorSearchService);
    }

    @Nested
    @DisplayName("KEYWORDモード")
    class KeywordModeTests {

        @Test
        @DisplayName("BM25SearchServiceのみが呼ばれる")
        void keywordModeCallsOnlyBm25() {
            List<SearchResult> expected = List.of(
                    new SearchResult("1", "内容", 0.9, Map.of(), null)
            );
            when(bm25SearchService.search("テスト", SearchFilters.NONE, 10))
                    .thenReturn(expected);

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.KEYWORD);

            assertEquals(expected, results);
            verify(bm25SearchService).search("テスト", SearchFilters.NONE, 10);
            verifyNoInteractions(vectorSearchService);
        }
    }

    @Nested
    @DisplayName("VECTORモード")
    class VectorModeTests {

        @Test
        @DisplayName("VectorSearchServiceのみが呼ばれる")
        void vectorModeCallsOnlyVector() {
            List<SearchResult> expected = List.of(
                    new SearchResult("1", "内容", 0.85, Map.of(), null)
            );
            when(vectorSearchService.search("テスト", SearchFilters.NONE, 10))
                    .thenReturn(expected);

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.VECTOR);

            assertEquals(expected, results);
            verify(vectorSearchService).search("テスト", SearchFilters.NONE, 10);
            verifyNoInteractions(bm25SearchService);
        }
    }

    @Nested
    @DisplayName("HYBRIDモード")
    class HybridModeTests {

        @Test
        @DisplayName("両検索が実行されRRFで統合される")
        void hybridModeExecutesBothAndMerges() {
            List<SearchResult> bm25Results = List.of(
                    new SearchResult("A", "docA", 0.9, Map.of(), null),
                    new SearchResult("B", "docB", 0.7, Map.of(), null)
            );
            List<SearchResult> vectorResults = List.of(
                    new SearchResult("B", "docB", 0.95, Map.of(), null),
                    new SearchResult("A", "docA", 0.80, Map.of(), null)
            );

            when(bm25SearchService.search(eq("テスト"), eq(SearchFilters.NONE),
                    eq(HybridSearchService.CANDIDATE_K)))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(eq("テスト"), eq(SearchFilters.NONE),
                    eq(HybridSearchService.CANDIDATE_K)))
                    .thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            // 両方の検索が呼ばれたことを確認
            verify(bm25SearchService).search(eq("テスト"), eq(SearchFilters.NONE),
                    eq(HybridSearchService.CANDIDATE_K));
            verify(vectorSearchService).search(eq("テスト"), eq(SearchFilters.NONE),
                    eq(HybridSearchService.CANDIDATE_K));
        }

        @Test
        @DisplayName("modeがnullの場合HYBRIDとして実行される")
        void nullModeDefaultsToHybrid() {
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, null);

            assertTrue(results.isEmpty());
            // 両方の検索が呼ばれている（HYBRID動作）
            verify(bm25SearchService).search(anyString(), any(), anyInt());
            verify(vectorSearchService).search(anyString(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("グレースフルデグレード")
    class GracefulDegradationTests {

        @Test
        @DisplayName("BM25失敗時はベクトル結果のみ返す")
        void fallsBackToVectorWhenBm25Fails() {
            List<SearchResult> vectorResults = List.of(
                    new SearchResult("1", "内容", 0.85, Map.of(), null)
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("BM25 failure"));
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals("1", results.get(0).id());
        }

        @Test
        @DisplayName("ベクトル検索失敗時はBM25結果のみ返す")
        void fallsBackToBm25WhenVectorFails() {
            List<SearchResult> bm25Results = List.of(
                    new SearchResult("1", "内容", 0.75, Map.of(), null)
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Vector failure"));

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals("1", results.get(0).id());
        }

        @Test
        @DisplayName("両方失敗時は空リストを返す")
        void returnsEmptyWhenBothFail() {
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("BM25 failure"));
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Vector failure"));

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("両方空結果の場合は空リストを返す")
        void returnsEmptyWhenBothEmpty() {
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search(
                    "テスト", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("RRFスコア計算")
    class RrfTests {

        @Test
        @DisplayName("両方に出現する結果のRRFスコアが正しい")
        void rrfScoreCalculationForOverlappingResults() {
            List<SearchResult> bm25 = List.of(
                    new SearchResult("A", "docA", 0.9, Map.of(), null),  // rank=1
                    new SearchResult("B", "docB", 0.7, Map.of(), null)   // rank=2
            );
            List<SearchResult> vector = List.of(
                    new SearchResult("B", "docB", 0.95, Map.of(), null), // rank=1
                    new SearchResult("A", "docA", 0.80, Map.of(), null)  // rank=2
            );

            int k = 60;
            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            assertEquals(2, results.size());

            // doc_A: 1/(60+1) + 1/(60+2) = 0.01639 + 0.01613 = 0.03252
            // doc_B: 1/(60+2) + 1/(60+1) = 0.01613 + 0.01639 = 0.03252
            // 両方同じスコア（対称的なランク）
            double expectedScore = 1.0 / (k + 1) + 1.0 / (k + 2);
            assertEquals(expectedScore, results.get(0).score(), 0.0001);
        }

        @Test
        @DisplayName("片方にのみ出現する結果のRRFスコアが正しい")
        void rrfScoreForNonOverlappingResults() {
            List<SearchResult> bm25 = List.of(
                    new SearchResult("A", "docA", 0.9, Map.of(), null)   // rank=1
            );
            List<SearchResult> vector = List.of(
                    new SearchResult("B", "docB", 0.95, Map.of(), null)  // rank=1
            );

            int k = 60;
            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            assertEquals(2, results.size());

            // 両方 1/(60+1) = 0.01639（片方のランキングのみ）
            double expectedScore = 1.0 / (k + 1);
            assertEquals(expectedScore, results.get(0).score(), 0.0001);
            assertEquals(expectedScore, results.get(1).score(), 0.0001);
        }

        @Test
        @DisplayName("両方に出現する結果が片方のみの結果より高スコアになる")
        void overlappingResultsRankedHigher() {
            List<SearchResult> bm25 = List.of(
                    new SearchResult("A", "docA", 0.9, Map.of(), null),  // rank=1
                    new SearchResult("C", "docC", 0.7, Map.of(), null)   // rank=2
            );
            List<SearchResult> vector = List.of(
                    new SearchResult("A", "docA", 0.85, Map.of(), null), // rank=1
                    new SearchResult("B", "docB", 0.80, Map.of(), null)  // rank=2
            );

            int k = 60;
            List<SearchResult> results = service.rrfMerge(bm25, vector, 10, k);

            // Aは両方に出現するので最高RRFスコア
            assertEquals("A", results.get(0).id());
        }

        @Test
        @DisplayName("topK制限がRRF結果に適用される")
        void rrfRespectsTopK() {
            List<SearchResult> bm25 = List.of(
                    new SearchResult("A", "docA", 0.9, Map.of(), null),
                    new SearchResult("B", "docB", 0.8, Map.of(), null),
                    new SearchResult("C", "docC", 0.7, Map.of(), null)
            );
            List<SearchResult> vector = List.of(
                    new SearchResult("D", "docD", 0.95, Map.of(), null),
                    new SearchResult("E", "docE", 0.85, Map.of(), null)
            );

            List<SearchResult> results = service.rrfMerge(bm25, vector, 2, 60);

            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("バリデーション")
    class ValidationTests {

        @Test
        @DisplayName("queryがnullの場合はIllegalArgumentException")
        void throwsOnNullQuery() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search(null, SearchFilters.NONE, 10, SearchMode.HYBRID));
        }

        @Test
        @DisplayName("queryが空白の場合はIllegalArgumentException")
        void throwsOnBlankQuery() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search("  ", SearchFilters.NONE, 10, SearchMode.HYBRID));
        }

        @Test
        @DisplayName("topKが0以下の場合はIllegalArgumentException")
        void throwsOnInvalidTopK() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search("テスト", SearchFilters.NONE, 0, SearchMode.HYBRID));
        }
    }
}

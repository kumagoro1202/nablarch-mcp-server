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
 * ハイブリッド検索統合テスト。
 *
 * <p>BM25SearchServiceとVectorSearchServiceをモックし、
 * HybridSearchServiceのRRF統合パイプライン全体をNablarchドメインの
 * 具体的なクエリ・結果で検証する。</p>
 *
 * <p>テスト対象: {@link HybridSearchService} のパイプラインフロー</p>
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchIntegrationTest {

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
    private static SearchResult createSearchResult(
            String id, String content, double score, Map<String, String> metadata) {
        return new SearchResult(id, content, score, metadata, "https://nablarch.github.io/docs/" + id);
    }

    @Nested
    @DisplayName("HYBRIDモード統合フロー")
    class HybridIntegrationFlowTests {

        @Test
        @DisplayName("BM25 5件 + Vector 5件（重複2件）→ RRF統合: 重複結果が非重複より高スコア")
        void rrfMergeWithOverlap_overlappingResultsRankedHigher() {
            // BM25結果5件（doc-A, doc-B が重複候補）
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("doc-A", "ハンドラキューの設定方法について説明する。", 0.92,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("doc-B", "REST APIのバリデーション設定。", 0.85,
                            Map.of("source", "nablarch-document", "app_type", "rest")),
                    createSearchResult("doc-C", "データベースアクセスの設定方法。", 0.78,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("doc-D", "メッセージングの設定。", 0.72,
                            Map.of("source", "nablarch-document", "app_type", "messaging")),
                    createSearchResult("doc-E", "バッチ処理の概要。", 0.65,
                            Map.of("source", "nablarch-document", "app_type", "batch"))
            );

            // Vector結果5件（doc-A, doc-B が重複、doc-F, doc-G, doc-H は新規）
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("doc-B", "REST APIのバリデーション設定。", 0.95,
                            Map.of("source", "nablarch-document", "app_type", "rest")),
                    createSearchResult("doc-A", "ハンドラキューの設定方法について説明する。", 0.88,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("doc-F", "ログ出力の設定。", 0.82,
                            Map.of("source", "nablarch-document", "app_type", "common")),
                    createSearchResult("doc-G", "国際化対応。", 0.75,
                            Map.of("source", "nablarch-document", "app_type", "web")),
                    createSearchResult("doc-H", "セキュリティ設定。", 0.68,
                            Map.of("source", "nablarch-document", "app_type", "web"))
            );

            when(bm25SearchService.search(anyString(), any(), eq(HybridSearchService.CANDIDATE_K)))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), eq(HybridSearchService.CANDIDATE_K)))
                    .thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "ハンドラキューの設定", SearchFilters.NONE, 8, SearchMode.HYBRID);

            // 結果は8件（重複2件を統合して8ユニーク）
            assertEquals(8, results.size());

            // 重複結果（doc-A, doc-B）が上位にいることを確認
            String topId1 = results.get(0).id();
            String topId2 = results.get(1).id();
            assertTrue(
                    (topId1.equals("doc-A") || topId1.equals("doc-B"))
                            && (topId2.equals("doc-A") || topId2.equals("doc-B")),
                    "重複結果(doc-A, doc-B)がトップ2にいるべき。実際: " + topId1 + ", " + topId2);

            // 重複結果のスコアが非重複より高いことを確認
            double overlapMinScore = Math.min(results.get(0).score(), results.get(1).score());
            double nonOverlapMaxScore = results.get(2).score();
            assertTrue(overlapMinScore > nonOverlapMaxScore,
                    "重複結果の最低スコア(" + overlapMinScore + ")が非重複の最高スコア("
                            + nonOverlapMaxScore + ")より高いべき");

            // スコア降順を確認
            for (int i = 0; i < results.size() - 1; i++) {
                assertTrue(results.get(i).score() >= results.get(i + 1).score(),
                        "index " + i + " のスコア(" + results.get(i).score()
                                + ")が index " + (i + 1) + " のスコア("
                                + results.get(i + 1).score() + ")以上であるべき");
            }
        }
    }

    @Nested
    @DisplayName("Nablarchドメインテストクエリセット")
    class NablarchDomainQueryTests {

        @Test
        @DisplayName("ハンドラキューの設定方法 → handler関連ドキュメント上位")
        void handlerQueueQuery() {
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("handler-1", "ハンドラキューの構築方法。InboundHandlerとOutboundHandler。", 0.95,
                            Map.of("source", "nablarch-document", "source_type", "documentation")),
                    createSearchResult("handler-2", "ハンドラの実装方法。Handler<TData, TResult>インターフェース。", 0.80,
                            Map.of("source", "nablarch-document", "source_type", "documentation"))
            );
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("handler-1", "ハンドラキューの構築方法。InboundHandlerとOutboundHandler。", 0.90,
                            Map.of("source", "nablarch-document", "source_type", "documentation")),
                    createSearchResult("handler-3", "Nablarchのリクエスト処理パイプライン。", 0.85,
                            Map.of("source", "nablarch-document", "source_type", "documentation"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt())).thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt())).thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "ハンドラキューの設定方法", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            // handler-1 は両方に出現するのでトップ
            assertEquals("handler-1", results.get(0).id());
            assertTrue(results.get(0).content().contains("ハンドラキュー"));
        }

        @Test
        @DisplayName("REST APIのバリデーション → JAX-RS+validation関連上位")
        void restApiValidationQuery() {
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("rest-val-1", "JAX-RSアクションでのBeanValidation設定。@Valid注釈の使用方法。", 0.90,
                            Map.of("source", "nablarch-document", "app_type", "rest")),
                    createSearchResult("rest-val-2", "バリデーションエラーメッセージのカスタマイズ。", 0.75,
                            Map.of("source", "nablarch-document", "app_type", "rest"))
            );
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("rest-val-1", "JAX-RSアクションでのBeanValidation設定。@Valid注釈の使用方法。", 0.92,
                            Map.of("source", "nablarch-document", "app_type", "rest")),
                    createSearchResult("rest-val-3", "Nablarchにおけるバリデーションアーキテクチャ。", 0.80,
                            Map.of("source", "nablarch-document", "app_type", "common"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt())).thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt())).thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "REST APIのバリデーション", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals("rest-val-1", results.get(0).id());
            assertTrue(results.get(0).content().contains("BeanValidation"));
        }

        @Test
        @DisplayName("バッチ処理のエラーハンドリング → batch+error関連上位")
        void batchErrorHandlingQuery() {
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("batch-err-1", "バッチアクションにおけるトランザクション管理とエラーハンドリング。", 0.88,
                            Map.of("source", "nablarch-document", "app_type", "batch")),
                    createSearchResult("batch-err-2", "リトライ可能例外とリトライ不可例外の分類。", 0.72,
                            Map.of("source", "nablarch-document", "app_type", "batch"))
            );
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("batch-err-1", "バッチアクションにおけるトランザクション管理とエラーハンドリング。", 0.91,
                            Map.of("source", "nablarch-document", "app_type", "batch")),
                    createSearchResult("batch-err-3", "Nablarchバッチの障害復旧パターン。", 0.78,
                            Map.of("source", "nablarch-document", "app_type", "batch"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt())).thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt())).thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "バッチ処理のエラーハンドリング", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals("batch-err-1", results.get(0).id());
            assertTrue(results.get(0).content().contains("エラーハンドリング"));
        }
    }

    @Nested
    @DisplayName("フィルタ適用統合テスト")
    class FilterIntegrationTests {

        @Test
        @DisplayName("appType=web フィルタがBM25/Vector両方に渡される")
        void filterPassedToBothServices() {
            SearchFilters webFilter = new SearchFilters("web", null, null, null, null);

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", webFilter, 10, SearchMode.HYBRID);

            // BM25とVectorの両方にフィルタが渡されている
            verify(bm25SearchService).search(
                    eq("テスト"), eq(webFilter), eq(HybridSearchService.CANDIDATE_K));
            verify(vectorSearchService).search(
                    eq("テスト"), eq(webFilter), eq(HybridSearchService.CANDIDATE_K));
        }
    }

    @Nested
    @DisplayName("グレースフルデグレード統合テスト")
    class GracefulDegradationIntegrationTests {

        @Test
        @DisplayName("BM25タイムアウト → Vector結果のみ返却")
        void bm25TimeoutReturnsVectorOnly() {
            List<SearchResult> vectorResults = List.of(
                    createSearchResult("vec-1", "Nablarchのルーティング設定。", 0.90,
                            Map.of("source", "nablarch-document")),
                    createSearchResult("vec-2", "WebアプリケーションのURL設計。", 0.85,
                            Map.of("source", "nablarch-document"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("BM25 query timeout"));
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);

            List<SearchResult> results = service.search(
                    "ルーティング設定", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals(2, results.size());
            assertEquals("vec-1", results.get(0).id());
            assertEquals("vec-2", results.get(1).id());
        }

        @Test
        @DisplayName("Vector例外 → BM25結果のみ返却")
        void vectorExceptionReturnsBm25Only() {
            List<SearchResult> bm25Results = List.of(
                    createSearchResult("bm-1", "nablarch-fw-webモジュールの概要。", 0.82,
                            Map.of("source", "nablarch-document", "module", "nablarch-fw-web")),
                    createSearchResult("bm-2", "HTTPリクエスト処理の流れ。", 0.75,
                            Map.of("source", "nablarch-document"))
            );

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Embedding API connection refused"));

            List<SearchResult> results = service.search(
                    "Webモジュール", SearchFilters.NONE, 5, SearchMode.HYBRID);

            assertFalse(results.isEmpty());
            assertEquals(2, results.size());
            assertEquals("bm-1", results.get(0).id());
        }
    }

    @Nested
    @DisplayName("空結果統合テスト")
    class EmptyResultIntegrationTests {

        @Test
        @DisplayName("BM25/Vector共に空 → 空リスト")
        void bothEmptyReturnsEmpty() {
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search(
                    "存在しないキーワード", SearchFilters.NONE, 10, SearchMode.HYBRID);

            assertTrue(results.isEmpty());
        }
    }
}

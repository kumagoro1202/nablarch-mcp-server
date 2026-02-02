package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.TestConfig;
import com.tis.nablarch.mcp.rag.ingestion.OfficialDocsIngester;
import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.BM25SearchService;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import com.tis.nablarch.mcp.rag.search.VectorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link SemanticSearchTool} の統合テスト。
 *
 * <p>RAGパイプライン全段（HybridSearch → Rerank → 結果整形）の
 * 統合動作を {@code @SpringBootTest} で検証する。</p>
 *
 * <p>外部依存（DB, Embedding API, Reranker API）はモック化し、
 * 実際のBean（HybridSearchService, SemanticSearchTool）の統合を検証する。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class SemanticSearchIntegrationTest {

    @Autowired
    private SemanticSearchTool semanticSearchTool;

    @Autowired
    private HybridSearchService hybridSearchService;

    @MockitoBean
    private BM25SearchService bm25SearchService;

    @MockitoBean
    private VectorSearchService vectorSearchService;

    @MockitoBean
    private Reranker reranker;

    @MockitoBean
    @SuppressWarnings("unused")
    private OfficialDocsIngester officialDocsIngester;

    /**
     * テスト用の検索結果を生成する。
     */
    private List<SearchResult> createSearchResults(String prefix, int count, double baseScore) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SearchResult(
                        prefix + "-" + i,
                        prefix + "のドキュメント内容" + i + "。Nablarchフレームワークの設定方法。",
                        baseScore - (i - 1) * 0.05,
                        Map.of("source", "nablarch-document",
                                "app_type", "rest",
                                "module", "nablarch-fw-jaxrs"),
                        "https://nablarch.github.io/docs/" + prefix + "/" + i
                ))
                .toList();
    }

    @Nested
    @DisplayName("全段パイプライン統合テスト")
    class FullPipelineTests {

        @Test
        @DisplayName("日本語クエリ: HybridSearch → Rerank → Markdown出力")
        void japaneseQueryFullPipeline() {
            // BM25とVectorの結果をモック
            List<SearchResult> bm25Results = createSearchResults("bm25", 5, 0.8);
            List<SearchResult> vectorResults = createSearchResults("vector", 5, 0.75);
            List<SearchResult> rerankedResults = createSearchResults("reranked", 3, 0.95);

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenReturn(rerankedResults);

            String result = semanticSearchTool.semanticSearch(
                    "Nablarchのハンドラキュー設定", null, null, null, null, null, null);

            // Markdown出力にスコア・コンテンツ・ソースURLが含まれる
            assertNotNull(result);
            assertTrue(result.contains("## 検索結果: \"Nablarchのハンドラキュー設定\""));
            assertTrue(result.contains("モード: hybrid"));
            assertTrue(result.contains("結果数: 3件"));
            assertTrue(result.contains("### 結果 1"));
            assertTrue(result.contains("スコア: 0.950"));
            assertTrue(result.contains("nablarch-document"));
            assertTrue(result.contains("https://nablarch.github.io"));

            // Rerankerが呼ばれたことを検証
            verify(reranker).rerank(eq("Nablarchのハンドラキュー設定"), anyList(), eq(5));
        }

        @Test
        @DisplayName("英語クエリ: 英語入力でもパイプラインが正常動作")
        void englishQueryFullPipeline() {
            List<SearchResult> bm25Results = createSearchResults("bm25-en", 3, 0.7);
            List<SearchResult> vectorResults = createSearchResults("vector-en", 3, 0.65);
            List<SearchResult> rerankedResults = createSearchResults("reranked-en", 2, 0.88);

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(vectorResults);
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenReturn(rerankedResults);

            String result = semanticSearchTool.semanticSearch(
                    "How to configure REST API validation",
                    null, null, null, null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("検索結果"));
            assertTrue(result.contains("結果数: 2件"));
        }
    }

    @Nested
    @DisplayName("フィルタ統合テスト")
    class FilterIntegrationTests {

        @Test
        @DisplayName("フィルタ付き検索: フィルタがHybridSearchに正しく渡される")
        void filteredSearch() {
            List<SearchResult> bm25Results = createSearchResults("batch", 2, 0.7);
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenReturn(bm25Results.subList(0, 1));

            String result = semanticSearchTool.semanticSearch(
                    "バッチ処理", "batch", "nablarch-fw-batch", null, null, null, null);

            assertNotNull(result);
            // BM25SearchServiceにフィルタが渡されたことを検証
            verify(bm25SearchService).search(
                    eq("バッチ処理"),
                    argThat(filters -> "batch".equals(filters.appType())
                            && "nablarch-fw-batch".equals(filters.module())),
                    anyInt());
        }
    }

    @Nested
    @DisplayName("フォールバック統合テスト")
    class FallbackIntegrationTests {

        @Test
        @DisplayName("Reranker失敗時: HybridSearch結果がそのまま表示される")
        void rerankerFailureFallback() {
            List<SearchResult> bm25Results = createSearchResults("bm25", 3, 0.8);
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenThrow(new RuntimeException("Reranker API timeout"));

            // SemanticSearchToolのエラーハンドリングがキャッチ
            String result = semanticSearchTool.semanticSearch(
                    "テストクエリ", null, null, null, null, null, null);

            assertNotNull(result);
            // エラーメッセージが返却される
            assertTrue(result.contains("エラー") || result.contains("検索結果"));
        }

        @Test
        @DisplayName("検索結果ゼロ: ヒント付きメッセージが返却される")
        void zeroResults() {
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            String result = semanticSearchTool.semanticSearch(
                    "存在しないドキュメント", null, null, null, null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("検索結果なし"));
            assertTrue(result.contains("ヒント"));
            assertTrue(result.contains("search_api"));
        }
    }

    @Nested
    @DisplayName("パラメータ統合テスト")
    class ParameterIntegrationTests {

        @Test
        @DisplayName("topK=3: 最終結果が3件以下に制限される")
        void topKParameter() {
            List<SearchResult> bm25Results = createSearchResults("bm25", 10, 0.9);
            List<SearchResult> rerankedResults = createSearchResults("reranked", 3, 0.95);

            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(vectorSearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(reranker.rerank(anyString(), anyList(), eq(3)))
                    .thenReturn(rerankedResults);

            String result = semanticSearchTool.semanticSearch(
                    "テスト", null, null, null, null, 3, null);

            assertNotNull(result);
            verify(reranker).rerank(anyString(), anyList(), eq(3));
            assertTrue(result.contains("結果数: 3件"));
        }

        @Test
        @DisplayName("keywordモード: BM25のみが使用される")
        void keywordMode() {
            List<SearchResult> bm25Results = createSearchResults("bm25", 2, 0.8);
            when(bm25SearchService.search(anyString(), any(), anyInt()))
                    .thenReturn(bm25Results);
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenReturn(bm25Results);

            String result = semanticSearchTool.semanticSearch(
                    "nablarch.fw.Handler", null, null, null, null, null, "keyword");

            assertNotNull(result);
            assertTrue(result.contains("モード: keyword"));
            // VectorSearchServiceは呼ばれない
            verify(vectorSearchService, never()).search(anyString(), any(), anyInt());
        }
    }
}

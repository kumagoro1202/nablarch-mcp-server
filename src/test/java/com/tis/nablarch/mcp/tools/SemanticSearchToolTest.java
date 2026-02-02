package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link SemanticSearchTool} のユニットテスト。
 *
 * <p>HybridSearchServiceとRerankerをモック化し、
 * 検索パイプラインの統合動作を検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchToolTest {

    @Mock
    private HybridSearchService hybridSearchService;

    @Mock
    private Reranker reranker;

    private SemanticSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new SemanticSearchTool(hybridSearchService, reranker);
    }

    /**
     * テスト用の検索結果を生成する。
     */
    private List<SearchResult> createResults(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SearchResult(
                        "doc-" + i,
                        "Nablarchの" + i + "番目のドキュメント内容",
                        0.9 - (i - 1) * 0.1,
                        Map.of("source", "nablarch-document",
                                "app_type", "rest",
                                "module", "nablarch-fw-jaxrs"),
                        "https://nablarch.github.io/doc" + i
                ))
                .toList();
    }

    @Nested
    @DisplayName("正常系")
    class NormalTests {

        @Test
        @DisplayName("正常検索: HybridSearch → Rerank → Markdown出力")
        void normalSearchPipeline() {
            List<SearchResult> candidates = createResults(10);
            List<SearchResult> reranked = createResults(3);

            when(hybridSearchService.search(eq("ハンドラキュー"), any(), eq(50), eq(SearchMode.HYBRID)))
                    .thenReturn(candidates);
            when(reranker.rerank(eq("ハンドラキュー"), eq(candidates), eq(5)))
                    .thenReturn(reranked);

            String result = tool.semanticSearch(
                    "ハンドラキュー", null, null, null, null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("## 検索結果: \"ハンドラキュー\""));
            assertTrue(result.contains("モード: hybrid"));
            assertTrue(result.contains("結果数: 3件"));
            assertTrue(result.contains("### 結果 1"));
            assertTrue(result.contains("### 結果 2"));
            assertTrue(result.contains("### 結果 3"));
        }

        @Test
        @DisplayName("検索モードが正しくHybridSearchServiceに渡される")
        void searchModePassedCorrectly() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), eq(SearchMode.VECTOR)))
                    .thenReturn(Collections.emptyList());

            tool.semanticSearch("クエリ", null, null, null, null, null, "vector");

            verify(hybridSearchService).search(eq("クエリ"), any(), eq(50), eq(SearchMode.VECTOR));
        }

        @Test
        @DisplayName("keywordモードが正しく渡される")
        void keywordModePassedCorrectly() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), eq(SearchMode.KEYWORD)))
                    .thenReturn(Collections.emptyList());

            tool.semanticSearch("クエリ", null, null, null, null, null, "keyword");

            verify(hybridSearchService).search(eq("クエリ"), any(), eq(50), eq(SearchMode.KEYWORD));
        }

        @Test
        @DisplayName("topKがRerankerに正しく渡される")
        void topKPassedToReranker() {
            List<SearchResult> candidates = createResults(5);
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(candidates);
            when(reranker.rerank(anyString(), anyList(), eq(3)))
                    .thenReturn(createResults(3));

            tool.semanticSearch("クエリ", null, null, null, null, 3, null);

            verify(reranker).rerank(eq("クエリ"), eq(candidates), eq(3));
        }

        @Test
        @DisplayName("topKがnullの場合デフォルト値(5)が使われる")
        void nullTopKUsesDefault() {
            List<SearchResult> candidates = createResults(5);
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(candidates);
            when(reranker.rerank(anyString(), anyList(), eq(SemanticSearchTool.DEFAULT_TOP_K)))
                    .thenReturn(createResults(5));

            tool.semanticSearch("クエリ", null, null, null, null, null, null);

            verify(reranker).rerank(anyString(), anyList(), eq(SemanticSearchTool.DEFAULT_TOP_K));
        }
    }

    @Nested
    @DisplayName("フィルタ変換")
    class FilterTests {

        @Test
        @DisplayName("フィルタパラメータがSearchFiltersに正しく変換される")
        void filtersConvertedCorrectly() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            tool.semanticSearch("クエリ", "rest", "nablarch-fw-web", "github", "code", null, null);

            ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
            verify(hybridSearchService).search(anyString(), captor.capture(), anyInt(), any());

            SearchFilters filters = captor.getValue();
            assertEquals("rest", filters.appType());
            assertEquals("nablarch-fw-web", filters.module());
            assertEquals("github", filters.source());
            assertEquals("code", filters.sourceType());
            assertNull(filters.language());
        }

        @Test
        @DisplayName("空白フィルタはnullに変換される")
        void blankFiltersConvertedToNull() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            tool.semanticSearch("クエリ", "  ", "", null, "  ", null, null);

            ArgumentCaptor<SearchFilters> captor = ArgumentCaptor.forClass(SearchFilters.class);
            verify(hybridSearchService).search(anyString(), captor.capture(), anyInt(), any());

            SearchFilters filters = captor.getValue();
            assertNull(filters.appType());
            assertNull(filters.module());
            assertNull(filters.source());
            assertNull(filters.sourceType());
        }
    }

    @Nested
    @DisplayName("QueryAnalyzer未注入時")
    class QueryAnalyzerAbsentTests {

        @Test
        @DisplayName("QueryAnalyzerが未注入でも正常に動作する")
        void worksWithoutQueryAnalyzer() {
            // queryAnalyzerフィールドはデフォルトnull（@Autowired(required=false)）
            List<SearchResult> candidates = createResults(3);
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(candidates);
            when(reranker.rerank(anyString(), anyList(), anyInt()))
                    .thenReturn(createResults(2));

            String result = tool.semanticSearch("クエリ", null, null, null, null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("検索結果"));
        }
    }

    @Nested
    @DisplayName("エラーハンドリング")
    class ErrorTests {

        @Test
        @DisplayName("クエリがnullの場合エラーメッセージを返す")
        void nullQueryReturnsError() {
            String result = tool.semanticSearch(null, null, null, null, null, null, null);
            assertEquals("検索クエリを指定してください。", result);
            verifyNoInteractions(hybridSearchService);
            verifyNoInteractions(reranker);
        }

        @Test
        @DisplayName("クエリが空白の場合エラーメッセージを返す")
        void blankQueryReturnsError() {
            String result = tool.semanticSearch("   ", null, null, null, null, null, null);
            assertEquals("検索クエリを指定してください。", result);
        }

        @Test
        @DisplayName("HybridSearchService例外時はエラーメッセージを返す")
        void hybridSearchExceptionReturnsErrorMessage() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenThrow(new RuntimeException("DB接続失敗"));

            String result = tool.semanticSearch("クエリ", null, null, null, null, null, null);

            assertTrue(result.contains("検索中にエラーが発生しました"));
        }

        @Test
        @DisplayName("検索結果が0件の場合ヒントを表示する")
        void emptyResultsShowHints() {
            when(hybridSearchService.search(anyString(), any(), anyInt(), any()))
                    .thenReturn(Collections.emptyList());

            String result = tool.semanticSearch("存在しないクエリ", null, null, null, null, null, null);

            assertTrue(result.contains("検索結果なし"));
            assertTrue(result.contains("ヒント"));
            assertTrue(result.contains("search_api"));
        }
    }

    @Nested
    @DisplayName("Markdown出力形式")
    class MarkdownOutputTests {

        @Test
        @DisplayName("結果にスコア・メタデータ・URLが含まれる")
        void resultContainsScoreMetadataUrl() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "テスト内容", 0.952,
                            Map.of("source", "nablarch-document",
                                    "app_type", "rest",
                                    "module", "nablarch-fw-jaxrs"),
                            "https://nablarch.github.io/doc1")
            );

            String markdown = tool.formatResults(
                    "テストクエリ", SearchMode.HYBRID, results, 100);

            assertTrue(markdown.contains("スコア: 0.952"));
            assertTrue(markdown.contains("nablarch-document"));
            assertTrue(markdown.contains("rest"));
            assertTrue(markdown.contains("nablarch-fw-jaxrs"));
            assertTrue(markdown.contains("https://nablarch.github.io/doc1"));
            assertTrue(markdown.contains("テスト内容"));
            assertTrue(markdown.contains("検索時間: 100ms"));
        }

        @Test
        @DisplayName("メタデータがnullの場合もエラーにならない")
        void nullMetadataHandled() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "テスト", 0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertNotNull(markdown);
            assertTrue(markdown.contains("スコア: 0.900"));
        }

        @Test
        @DisplayName("複数結果が連番で表示される")
        void multipleResultsNumbered() {
            List<SearchResult> results = createResults(3);

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("### 結果 1"));
            assertTrue(markdown.contains("### 結果 2"));
            assertTrue(markdown.contains("### 結果 3"));
        }
    }
}

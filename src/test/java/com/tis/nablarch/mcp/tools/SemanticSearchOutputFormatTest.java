package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
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

/**
 * {@link SemanticSearchTool} の出力Markdown形式検証テスト。
 *
 * <p>SemanticSearchToolの{@code formatResults}メソッドが生成する
 * Markdown文字列の構造・メタデータ表示・特殊文字処理を検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchOutputFormatTest {

    @Mock
    private HybridSearchService hybridSearchService;

    @Mock
    private Reranker reranker;

    private SemanticSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new SemanticSearchTool(hybridSearchService, reranker);
    }

    @Nested
    @DisplayName("Markdown構造")
    class MarkdownStructureTests {

        @Test
        @DisplayName("ヘッダー: クエリ名・モード・結果数・検索時間を含む")
        void headerContainsQueryModeCountTime() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "テスト内容", 0.95,
                            Map.of("source", "nablarch-document"),
                            "https://example.com/1")
            );

            String markdown = tool.formatResults("テストクエリ", SearchMode.HYBRID, results, 150);

            assertTrue(markdown.contains("## 検索結果: \"テストクエリ\""),
                    "H2ヘッダーにクエリ名が含まれること");
            assertTrue(markdown.contains("モード: hybrid"),
                    "検索モードが含まれること");
            assertTrue(markdown.contains("結果数: 1件"),
                    "結果数が含まれること");
            assertTrue(markdown.contains("検索時間: 150ms"),
                    "検索時間が含まれること");
        }

        @Test
        @DisplayName("結果ブロック: 連番・スコア・区切り線を含む")
        void resultBlockContainsNumberScoreSeparator() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "1番目の結果", 0.952, null, null),
                    new SearchResult("id-2", "2番目の結果", 0.841, null, null),
                    new SearchResult("id-3", "3番目の結果", 0.730, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 100);

            assertTrue(markdown.contains("### 結果 1 (スコア: 0.952)"),
                    "結果1の連番とスコアが含まれること");
            assertTrue(markdown.contains("### 結果 2 (スコア: 0.841)"),
                    "結果2の連番とスコアが含まれること");
            assertTrue(markdown.contains("### 結果 3 (スコア: 0.730)"),
                    "結果3の連番とスコアが含まれること");
            assertTrue(markdown.contains("---"),
                    "区切り線が含まれること");
        }

        @Test
        @DisplayName("コンテンツ: 各結果のテキスト内容が含まれる")
        void resultContentIncluded() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1",
                            "Nablarchのハンドラキューは、リクエスト処理のパイプラインを構成する。",
                            0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("Nablarchのハンドラキューは、リクエスト処理のパイプラインを構成する。"),
                    "ドキュメント内容がそのまま含まれること");
        }

        @Test
        @DisplayName("ソースURL: URLが表示される")
        void sourceUrlDisplayed() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9, null,
                            "https://nablarch.github.io/docs/latest/handler_queue.html")
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("**URL**: https://nablarch.github.io/docs/latest/handler_queue.html"),
                    "ソースURLが **URL**: プレフィクスで表示されること");
        }

        @Test
        @DisplayName("ソースURL未設定: URLブロックが省略される")
        void nullSourceUrlOmitted() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertFalse(markdown.contains("**URL**:"),
                    "ソースURLがnullの場合URLブロックは省略されること");
        }
    }

    @Nested
    @DisplayName("メタデータ表示")
    class MetadataDisplayTests {

        @Test
        @DisplayName("全メタデータ: source, app_type, module が表示される")
        void fullMetadataDisplayed() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9,
                            Map.of("source", "nablarch-document",
                                    "app_type", "rest",
                                    "module", "nablarch-fw-jaxrs"),
                            null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("**ソース**: nablarch-document"),
                    "sourceが表示されること");
            assertTrue(markdown.contains("rest"),
                    "app_typeが表示されること");
            assertTrue(markdown.contains("nablarch-fw-jaxrs"),
                    "moduleが表示されること");
        }

        @Test
        @DisplayName("部分メタデータ: sourceのみでもエラーにならない")
        void partialMetadata() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9,
                            Map.of("source", "github"),
                            null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("**ソース**: github"),
                    "sourceのみの場合も正しく表示されること");
        }

        @Test
        @DisplayName("メタデータなし: ソース行が省略される")
        void nullMetadata() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "テスト内容", 0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertNotNull(markdown);
            assertFalse(markdown.contains("**ソース**:"),
                    "メタデータがnullの場合ソース行は省略されること");
            assertTrue(markdown.contains("テスト内容"),
                    "コンテンツは表示されること");
        }

        @Test
        @DisplayName("空メタデータ: ソース行が省略される")
        void emptyMetadata() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "テスト内容", 0.9,
                            Collections.emptyMap(), null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertFalse(markdown.contains("**ソース**:"),
                    "空Mapの場合ソース行は省略されること");
        }
    }

    @Nested
    @DisplayName("特殊文字処理")
    class SpecialCharacterTests {

        @Test
        @DisplayName("Markdownパイプ文字: コンテンツ中の | がそのまま表示される")
        void pipeCharacterInContent() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1",
                            "| ハンドラ名 | 説明 |\n|---|---|\n| ThreadContextHandler | スレッドコンテキスト設定 |",
                            0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("ThreadContextHandler"),
                    "パイプ文字を含むテーブルコンテンツが表示されること");
        }

        @Test
        @DisplayName("アスタリスク文字: コンテンツ中の * がそのまま表示される")
        void asteriskInContent() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1",
                            "**重要**: `nablarch.fw.*` パッケージを使用する。",
                            0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("nablarch.fw.*"),
                    "アスタリスク文字がそのまま含まれること");
        }

        @Test
        @DisplayName("ブラケット文字: コンテンツ中の [] がそのまま表示される")
        void bracketInContent() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1",
                            "設定例: [handler-queue] セクション参照。@see [HttpResponse](link)",
                            0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("[handler-queue]"),
                    "ブラケット文字がそのまま含まれること");
        }

        @Test
        @DisplayName("日本語クエリ: ヘッダーに日本語クエリが正しく表示される")
        void japaneseQueryInHeader() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9, null, null)
            );

            String markdown = tool.formatResults(
                    "RESTfulウェブサービスの認証ハンドラキュー構成",
                    SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("## 検索結果: \"RESTfulウェブサービスの認証ハンドラキュー構成\""),
                    "日本語クエリがヘッダーに正しく表示されること");
        }
    }

    @Nested
    @DisplayName("エッジケース")
    class EdgeCaseTests {

        @Test
        @DisplayName("空結果: ヒント付きメッセージが返却される")
        void emptyResultsShowHints() {
            String markdown = tool.formatResults(
                    "存在しないキーワード", SearchMode.HYBRID, Collections.emptyList(), 50);

            assertTrue(markdown.contains("検索結果なし"),
                    "検索結果なしメッセージが含まれること");
            assertTrue(markdown.contains("ヒント"),
                    "検索ヒントが含まれること");
            assertTrue(markdown.contains("search_api"),
                    "フォールバックツールの案内が含まれること");
        }

        @Test
        @DisplayName("vectorモード: モード表示がvectorになる")
        void vectorModeDisplay() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.VECTOR, results, 50);

            assertTrue(markdown.contains("モード: vector"),
                    "vectorモードが正しく表示されること");
        }

        @Test
        @DisplayName("keywordモード: モード表示がkeywordになる")
        void keywordModeDisplay() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.9, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.KEYWORD, results, 50);

            assertTrue(markdown.contains("モード: keyword"),
                    "keywordモードが正しく表示されること");
        }

        @Test
        @DisplayName("スコア精度: 小数点以下3桁で表示される")
        void scorePrecision() {
            List<SearchResult> results = List.of(
                    new SearchResult("id-1", "内容", 0.12345, null, null)
            );

            String markdown = tool.formatResults("クエリ", SearchMode.HYBRID, results, 50);

            assertTrue(markdown.contains("スコア: 0.123"),
                    "スコアが小数点以下3桁で表示されること");
        }
    }
}

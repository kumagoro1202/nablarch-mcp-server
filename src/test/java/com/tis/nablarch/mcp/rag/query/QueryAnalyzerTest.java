package com.tis.nablarch.mcp.rag.query;

import com.tis.nablarch.mcp.rag.search.SearchFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link QueryAnalyzer}のテスト。
 *
 * <p>言語検出、エンティティ抽出、同義語展開、フィルタ推定を網羅する。</p>
 */
class QueryAnalyzerTest {

    private QueryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new QueryAnalyzer();
    }

    // ============================
    // 言語検出テスト
    // ============================

    @Nested
    class DetectLanguageTest {

        @Test
        void 日本語クエリはJAPANESEと判定される() {
            QueryLanguage lang = analyzer.detectLanguage("バリデーションの設定方法を教えてください");
            assertEquals(QueryLanguage.JAPANESE, lang);
        }

        @Test
        void 英語クエリはENGLISHと判定される() {
            QueryLanguage lang = analyzer.detectLanguage("How to configure validation in Nablarch");
            assertEquals(QueryLanguage.ENGLISH, lang);
        }

        @Test
        void 日英混在クエリはMIXEDと判定される() {
            // 日本語と英語が混在するクエリ
            QueryLanguage lang = analyzer.detectLanguage("nablarch-fw-webのハンドラキュー設定");
            assertEquals(QueryLanguage.MIXED, lang);
        }

        @Test
        void 完全英語テクニカルクエリはENGLISHと判定される() {
            QueryLanguage lang = analyzer.detectLanguage("nablarch.fw.web.HttpRequestHandler configuration");
            assertEquals(QueryLanguage.ENGLISH, lang);
        }

        @Test
        void 全てひらがなはJAPANESEと判定される() {
            QueryLanguage lang = analyzer.detectLanguage("はんどらのせってい");
            assertEquals(QueryLanguage.JAPANESE, lang);
        }

        @Test
        void 全てカタカナはJAPANESEと判定される() {
            QueryLanguage lang = analyzer.detectLanguage("バリデーション");
            assertEquals(QueryLanguage.JAPANESE, lang);
        }
    }

    @Nested
    class CalculateJapaneseRatioTest {

        @Test
        void nullの場合は0を返す() {
            assertEquals(0.0, analyzer.calculateJapaneseRatio(null));
        }

        @Test
        void 空文字の場合は0を返す() {
            assertEquals(0.0, analyzer.calculateJapaneseRatio(""));
        }

        @Test
        void 空白のみの場合は0を返す() {
            assertEquals(0.0, analyzer.calculateJapaneseRatio("   "));
        }

        @Test
        void 全て日本語の場合は1を返す() {
            double ratio = analyzer.calculateJapaneseRatio("日本語テスト");
            assertEquals(1.0, ratio, 0.01);
        }

        @Test
        void 全て英語の場合は0を返す() {
            double ratio = analyzer.calculateJapaneseRatio("english only");
            assertEquals(0.0, ratio, 0.01);
        }
    }

    // ============================
    // エンティティ抽出テスト
    // ============================

    @Nested
    class ExtractEntitiesTest {

        @Test
        void FQCNを抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "nablarch.fw.web.HttpRequestHandler の使い方");
            assertTrue(entities.contains("nablarch.fw.web.HttpRequestHandler"));
        }

        @Test
        void jpCoTisのFQCNを抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "jp.co.tis.nablarch.tool.Toolクラスについて");
            assertTrue(entities.contains("jp.co.tis.nablarch.tool.Tool"));
        }

        @Test
        void ハンドラ名を抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "ThreadContextHandler と HttpCharacterEncodingHandler について");
            assertTrue(entities.contains("ThreadContextHandler"));
            assertTrue(entities.contains("HttpCharacterEncodingHandler"));
        }

        @Test
        void モジュール名を抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "nablarch-fw-web と nablarch-core-validation を使いたい");
            assertTrue(entities.contains("nablarch-fw-web"));
            assertTrue(entities.contains("nablarch-core-validation"));
        }

        @Test
        void 設定ファイルパターンを抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "web-component-configuration.xml の書き方");
            assertTrue(entities.stream().anyMatch(e ->
                    e.contains("web-component-configuration") || e.contains(".xml")));
        }

        @Test
        void 複数種類のエンティティを一度に抽出できる() {
            List<String> entities = analyzer.extractEntities(
                    "nablarch-fw-web の ThreadContextHandler と nablarch.fw.web.HttpRequest について");
            assertTrue(entities.size() >= 3);
            assertTrue(entities.contains("nablarch-fw-web"));
            assertTrue(entities.contains("ThreadContextHandler"));
            assertTrue(entities.contains("nablarch.fw.web.HttpRequest"));
        }

        @Test
        void エンティティがない場合は空リストを返す() {
            List<String> entities = analyzer.extractEntities("普通のテキスト");
            assertTrue(entities.isEmpty());
        }

        @Test
        void 重複エンティティは除去される() {
            List<String> entities = analyzer.extractEntities(
                    "ThreadContextHandler を使う。ThreadContextHandler の設定");
            long count = entities.stream()
                    .filter(e -> e.equals("ThreadContextHandler"))
                    .count();
            assertEquals(1, count);
        }
    }

    // ============================
    // 同義語展開テスト
    // ============================

    @Nested
    class ExpandQueryTest {

        @Test
        void 同義語が展開される() {
            String expanded = analyzer.expandQuery("バリデーション");
            assertTrue(expanded.contains("バリデーション"));
            assertTrue(expanded.contains("validation"));
        }

        @Test
        void 英語から日本語へ展開される() {
            String expanded = analyzer.expandQuery("Handler");
            assertTrue(expanded.contains("Handler"));
            assertTrue(expanded.contains("ハンドラ"));
        }

        @Test
        void 同義語がない場合は元のクエリを返す() {
            String expanded = analyzer.expandQuery("特に同義語のないテキスト");
            assertEquals("特に同義語のないテキスト", expanded);
        }

        @Test
        void カスタム同義語マップで展開できる() {
            Map<String, List<String>> custom = Map.of(
                    "foo", List.of("bar", "baz")
            );
            QueryAnalyzer customAnalyzer = new QueryAnalyzer(new NablarchSynonymMap(custom));
            String expanded = customAnalyzer.expandQuery("foo");
            assertTrue(expanded.contains("foo"));
            assertTrue(expanded.contains("bar"));
            assertTrue(expanded.contains("baz"));
        }

        @Test
        void 大文字小文字を無視して展開される() {
            String expanded = analyzer.expandQuery("rest");
            assertTrue(expanded.contains("rest"));
            // REST の同義語が展開されるはず
            assertTrue(expanded.contains("JAX-RS") || expanded.contains("RESTful")
                    || expanded.contains("nablarch-fw-jaxrs"));
        }
    }

    // ============================
    // フィルタ推定テスト
    // ============================

    @Nested
    class SuggestFiltersTest {

        @Test
        void モジュール名からフィルタを推定できる() {
            List<String> entities = List.of("nablarch-fw-web");
            SearchFilters filters = analyzer.suggestFilters("nablarch-fw-web について", entities);
            assertNotNull(filters);
            assertEquals("nablarch-fw-web", filters.module());
        }

        @Test
        void webキーワードからアプリタイプを推定できる() {
            SearchFilters filters = analyzer.suggestFilters("web application の設定", List.of());
            assertNotNull(filters);
            assertEquals("web", filters.appType());
        }

        @Test
        void batchキーワードからアプリタイプを推定できる() {
            SearchFilters filters = analyzer.suggestFilters("batch processing の実装", List.of());
            assertNotNull(filters);
            assertEquals("batch", filters.appType());
        }

        @Test
        void restキーワードからアプリタイプを推定できる() {
            SearchFilters filters = analyzer.suggestFilters("rest api の構築方法", List.of());
            assertNotNull(filters);
            assertEquals("rest", filters.appType());
        }

        @Test
        void 日本語バッチキーワードからアプリタイプを推定できる() {
            SearchFilters filters = analyzer.suggestFilters("バッチ処理の実装方法", List.of());
            assertNotNull(filters);
            assertEquals("batch", filters.appType());
        }

        @Test
        void フィルタが推定できない場合はNONEを返す() {
            SearchFilters filters = analyzer.suggestFilters("一般的な質問", List.of());
            assertEquals(SearchFilters.NONE, filters);
        }
    }

    // ============================
    // analyze統合テスト
    // ============================

    @Nested
    class AnalyzeTest {

        @Test
        void 日本語クエリの総合解析() {
            AnalyzedQuery result = analyzer.analyze("nablarch-fw-web のハンドラキュー設定方法");
            assertNotNull(result);
            assertEquals("nablarch-fw-web のハンドラキュー設定方法", result.originalQuery());
            assertNotNull(result.expandedQuery());
            assertNotNull(result.language());
            assertNotNull(result.entities());
            assertTrue(result.entities().contains("nablarch-fw-web"));
            assertNotNull(result.suggestedFilters());
        }

        @Test
        void 英語クエリの総合解析() {
            AnalyzedQuery result = analyzer.analyze(
                    "How to use nablarch.fw.web.HttpRequestHandler");
            assertEquals(QueryLanguage.ENGLISH, result.language());
            assertTrue(result.entities().contains("nablarch.fw.web.HttpRequestHandler"));
        }

        @Test
        void 複合クエリの総合解析() {
            AnalyzedQuery result = analyzer.analyze(
                    "ThreadContextHandler と nablarch-fw-web の設定について バリデーション");
            assertNotNull(result);
            assertTrue(result.entities().contains("ThreadContextHandler"));
            assertTrue(result.entities().contains("nablarch-fw-web"));
            // 同義語展開されている
            assertTrue(result.expandedQuery().length() >= result.originalQuery().length());
        }

        @Test
        void nullクエリは例外をスローする() {
            assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null));
        }

        @Test
        void 空文字クエリは例外をスローする() {
            assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(""));
        }

        @Test
        void 空白のみクエリは例外をスローする() {
            assertThrows(IllegalArgumentException.class, () -> analyzer.analyze("   "));
        }
    }
}

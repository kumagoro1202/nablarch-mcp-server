package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MetadataFilteringService} のユニットテスト。
 */
class MetadataFilteringServiceTest {

    private MetadataFilteringService service;

    /** テスト用の検索結果リスト */
    private List<SearchResult> testResults;

    @BeforeEach
    void setUp() {
        service = new MetadataFilteringService();

        testResults = List.of(
                new SearchResult("1", "Webハンドラキュー設定", 0.95,
                        Map.of("source", "nablarch-document", "source_type", "documentation",
                                "app_type", "web", "module", "nablarch-fw-web",
                                "language", "ja", "fqcn", "nablarch.fw.web.handler.HttpResponseHandler",
                                "nablarch_version", "6u3",
                                "updated_at", "2026-01-15T10:00:00Z"),
                        "https://nablarch.github.io/docs/web"),
                new SearchResult("2", "バッチハンドラキュー設定", 0.88,
                        Map.of("source", "nablarch-document", "source_type", "documentation",
                                "app_type", "batch", "module", "nablarch-fw-batch",
                                "language", "ja", "fqcn", "nablarch.fw.handler.MultiThreadExecutionHandler",
                                "nablarch_version", "6u3",
                                "updated_at", "2026-01-20T10:00:00Z"),
                        "https://nablarch.github.io/docs/batch"),
                new SearchResult("3", "UniversalDao解説", 0.82,
                        Map.of("source", "fintan", "source_type", "documentation",
                                "app_type", "common", "module", "nablarch-common-dao",
                                "language", "ja", "fqcn", "nablarch.common.dao.UniversalDao",
                                "nablarch_version", "5u23",
                                "updated_at", "2025-06-01T10:00:00Z"),
                        "https://fintan.jp/page/dao"),
                new SearchResult("4", "Handler interface source", 0.75,
                        Map.of("source", "github", "source_type", "code",
                                "app_type", "common", "module", "nablarch-core",
                                "language", "en", "fqcn", "nablarch.fw.Handler",
                                "nablarch_version", "6u3",
                                "updated_at", "2026-02-01T10:00:00Z"),
                        "https://github.com/nablarch/nablarch-core"),
                new SearchResult("5", "RESTハンドラ設定", 0.70,
                        Map.of("source", "nablarch-document", "source_type", "documentation",
                                "app_type", "rest", "module", "nablarch-fw-jaxrs",
                                "language", "ja", "fqcn", "nablarch.fw.jaxrs.JaxRsResponseHandler",
                                "nablarch_version", "6u3",
                                "updated_at", "2026-01-25T10:00:00Z"),
                        "https://nablarch.github.io/docs/rest")
        );
    }

    @Nested
    @DisplayName("filter: ポストフィルタリング")
    class FilterTests {

        @Test
        @DisplayName("appTypeフィルタで1件に絞り込まれる")
        void filterByAppType() {
            SearchFilters base = new SearchFilters("web", null, null, null, null);
            ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(1, result.size());
            assertEquals("1", result.get(0).id());
        }

        @Test
        @DisplayName("sourceフィルタで対象ソースに絞り込まれる")
        void filterBySource() {
            SearchFilters base = new SearchFilters(null, null, "nablarch-document", null, null);
            ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(r -> "nablarch-document".equals(r.metadata().get("source"))));
        }

        @Test
        @DisplayName("languageフィルタで言語絞り込み")
        void filterByLanguage() {
            SearchFilters base = new SearchFilters(null, null, null, null, "en");
            ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(1, result.size());
            assertEquals("4", result.get(0).id());
        }

        @Test
        @DisplayName("複合条件: source + appType")
        void filterByMultipleConditions() {
            SearchFilters base = new SearchFilters(null, null, "nablarch-document", "documentation", null);
            ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("バージョン前方一致: '6'は'6u3'にマッチ")
        void filterByVersionPrefix() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "6", null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(4, result.size());
            assertTrue(result.stream().allMatch(
                    r -> r.metadata().get("nablarch_version").startsWith("6")));
        }

        @Test
        @DisplayName("バージョン前方一致: '5'は'5u23'にマッチ")
        void filterByVersionPrefix5() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "5", null, null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(1, result.size());
            assertEquals("3", result.get(0).id());
        }

        @Test
        @DisplayName("FQCN前方一致: 'nablarch.fw.web'に2件マッチ")
        void filterByFqcnPrefix() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(
                    null, null, "nablarch.fw.web", null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(1, result.size());
            assertEquals("1", result.get(0).id());
        }

        @Test
        @DisplayName("FQCN前方一致: 'nablarch.fw'に3件マッチ")
        void filterByFqcnPrefixBroad() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(
                    null, null, "nablarch.fw", null, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(4, result.size());
        }

        @Test
        @DisplayName("日付範囲: 2026年1月の結果のみ取得")
        void filterByDateRange() {
            Instant from = Instant.parse("2026-01-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-31T23:59:59Z");
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, null, null, from, to);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(3, result.size());
            assertTrue(result.stream().noneMatch(r -> "3".equals(r.id()))); // 2025年
            assertTrue(result.stream().noneMatch(r -> "4".equals(r.id()))); // 2026年2月
        }

        @Test
        @DisplayName("日付範囲: dateFromのみ指定")
        void filterByDateFrom() {
            Instant from = Instant.parse("2026-01-20T00:00:00Z");
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, null, null, from, null);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("日付範囲: dateToのみ指定")
        void filterByDateTo() {
            Instant to = Instant.parse("2025-12-31T23:59:59Z");
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, null, null, null, to);

            List<SearchResult> result = service.filter(testResults, filters);

            assertEquals(1, result.size());
            assertEquals("3", result.get(0).id());
        }

        @Test
        @DisplayName("null条件は無視される（フィルタなし）")
        void nullFiltersIgnored() {
            List<SearchResult> result = service.filter(testResults, null);
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("NONE条件は全件返却")
        void noneFiltersReturnsAll() {
            List<SearchResult> result = service.filter(testResults, ExtendedSearchFilters.NONE);
            assertEquals(5, result.size());
        }

        @Test
        @DisplayName("入力がnullの場合は空リスト")
        void nullResultsReturnsEmpty() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "6", null, null, null);
            List<SearchResult> result = service.filter(null, filters);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("入力が空リストの場合は空リスト")
        void emptyResultsReturnsEmpty() {
            ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "6", null, null, null);
            List<SearchResult> result = service.filter(Collections.emptyList(), filters);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("metadataがnullの結果は条件指定時にフィルタされる")
        void nullMetadataFilteredOut() {
            List<SearchResult> resultsWithNull = List.of(
                    new SearchResult("1", "content", 0.9, null, null),
                    new SearchResult("2", "content", 0.8,
                            Map.of("source", "github"), null)
            );
            SearchFilters base = new SearchFilters(null, null, "github", null, null);
            ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);

            List<SearchResult> result = service.filter(resultsWithNull, filters);

            assertEquals(1, result.size());
            assertEquals("2", result.get(0).id());
        }
    }

    @Nested
    @DisplayName("computeFacets: ファセット集計")
    class FacetTests {

        @Test
        @DisplayName("正常系: 全ファセットキーが集計される")
        void computeFacetsNormal() {
            Map<String, Map<String, Long>> facets = service.computeFacets(testResults);

            // source
            Map<String, Long> sourceFacet = facets.get("source");
            assertNotNull(sourceFacet);
            assertEquals(3L, sourceFacet.get("nablarch-document"));
            assertEquals(1L, sourceFacet.get("fintan"));
            assertEquals(1L, sourceFacet.get("github"));

            // app_type
            Map<String, Long> appTypeFacet = facets.get("app_type");
            assertNotNull(appTypeFacet);
            assertEquals(1L, appTypeFacet.get("web"));
            assertEquals(1L, appTypeFacet.get("batch"));
            assertEquals(2L, appTypeFacet.get("common"));
            assertEquals(1L, appTypeFacet.get("rest"));

            // language
            Map<String, Long> languageFacet = facets.get("language");
            assertNotNull(languageFacet);
            assertEquals(4L, languageFacet.get("ja"));
            assertEquals(1L, languageFacet.get("en"));
        }

        @Test
        @DisplayName("正常系: source_typeファセット")
        void computeFacetsSourceType() {
            Map<String, Map<String, Long>> facets = service.computeFacets(testResults);

            Map<String, Long> sourceTypeFacet = facets.get("source_type");
            assertNotNull(sourceTypeFacet);
            assertEquals(4L, sourceTypeFacet.get("documentation"));
            assertEquals(1L, sourceTypeFacet.get("code"));
        }

        @Test
        @DisplayName("正常系: moduleファセット")
        void computeFacetsModule() {
            Map<String, Map<String, Long>> facets = service.computeFacets(testResults);

            Map<String, Long> moduleFacet = facets.get("module");
            assertNotNull(moduleFacet);
            assertEquals(5, moduleFacet.size());
        }

        @Test
        @DisplayName("空リストの場合は空マップ")
        void computeFacetsEmpty() {
            Map<String, Map<String, Long>> facets = service.computeFacets(Collections.emptyList());
            assertTrue(facets.isEmpty());
        }

        @Test
        @DisplayName("nullの場合は空マップ")
        void computeFacetsNull() {
            Map<String, Map<String, Long>> facets = service.computeFacets(null);
            assertTrue(facets.isEmpty());
        }

        @Test
        @DisplayName("metadataがnullの結果はスキップされる")
        void computeFacetsSkipsNullMetadata() {
            List<SearchResult> resultsWithNull = List.of(
                    new SearchResult("1", "content", 0.9, null, null),
                    new SearchResult("2", "content", 0.8,
                            Map.of("source", "github", "language", "en"), null)
            );

            Map<String, Map<String, Long>> facets = service.computeFacets(resultsWithNull);

            Map<String, Long> sourceFacet = facets.get("source");
            assertNotNull(sourceFacet);
            assertEquals(1L, sourceFacet.get("github"));
        }
    }
}

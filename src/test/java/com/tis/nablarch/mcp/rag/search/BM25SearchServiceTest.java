package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link BM25SearchService} のユニットテスト。
 *
 * <p>JdbcTemplateをモック化し、SQL構築ロジックとマージロジックを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class BM25SearchServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private BM25SearchService service;

    @BeforeEach
    void setUp() {
        service = new BM25SearchService(jdbcTemplate);
    }

    @Nested
    @DisplayName("search メソッド")
    class SearchTests {

        @Test
        @DisplayName("正常系: document_chunksとcode_chunksの結果がスコア降順でマージされる")
        void searchMergesResultsFromBothTables() {
            List<SearchResult> docResults = List.of(
                    new SearchResult("doc1", "ドキュメント内容1", 0.9, Map.of("source", "nablarch-document"), "https://example.com/1"),
                    new SearchResult("doc2", "ドキュメント内容2", 0.7, Map.of("source", "nablarch-document"), "https://example.com/2")
            );

            List<SearchResult> codeResults = List.of(
                    new SearchResult("code1", "コード内容1", 0.8, Map.of("source", "github"), "https://github.com/1"),
                    new SearchResult("code2", "コード内容2", 0.6, Map.of("source", "github"), "https://github.com/2")
            );

            when(jdbcTemplate.query(contains("document_chunks"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(docResults);

            when(jdbcTemplate.query(contains("code_chunks"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(codeResults);

            List<SearchResult> results = service.search("ハンドラキュー", SearchFilters.NONE, 10);

            assertEquals(4, results.size());
            assertEquals("doc1", results.get(0).id());
            assertEquals("code1", results.get(1).id());
            assertEquals("doc2", results.get(2).id());
            assertEquals("code2", results.get(3).id());
        }

        @Test
        @DisplayName("正常系: topKで結果数が制限される")
        void searchRespectsTopK() {
            List<SearchResult> docResults = List.of(
                    new SearchResult("doc1", "内容1", 0.9, Map.of(), null),
                    new SearchResult("doc2", "内容2", 0.7, Map.of(), null),
                    new SearchResult("doc3", "内容3", 0.5, Map.of(), null)
            );

            when(jdbcTemplate.query(contains("document_chunks"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(docResults);
            when(jdbcTemplate.query(contains("code_chunks"), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search("テスト", SearchFilters.NONE, 2);

            assertEquals(2, results.size());
            assertEquals("doc1", results.get(0).id());
            assertEquals("doc2", results.get(1).id());
        }

        @Test
        @DisplayName("正常系: 結果が0件の場合は空リストを返す")
        void searchReturnsEmptyListWhenNoResults() {
            when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search("存在しないキーワード", SearchFilters.NONE, 10);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("異常系: queryがnullの場合はIllegalArgumentException")
        void searchThrowsOnNullQuery() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search(null, SearchFilters.NONE, 10));
        }

        @Test
        @DisplayName("異常系: queryが空白の場合はIllegalArgumentException")
        void searchThrowsOnBlankQuery() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search("  ", SearchFilters.NONE, 10));
        }

        @Test
        @DisplayName("異常系: topKが0以下の場合はIllegalArgumentException")
        void searchThrowsOnInvalidTopK() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.search("テスト", SearchFilters.NONE, 0));
        }

        @Test
        @DisplayName("正常系: filtersがnullの場合はフィルタなしで検索")
        void searchWithNullFilters() {
            when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.search("テスト", null, 10));
        }

        @Test
        @DisplayName("正常系: SQLにILIKE条件が含まれる（pg_trgm対応）")
        @SuppressWarnings("unchecked")
        void searchUsesIlikeForTrgm() {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("バッチ処理", SearchFilters.NONE, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            for (String sql : sqls) {
                assertTrue(sql.contains("ILIKE"), "SQLにILIKE条件が含まれていない: " + sql);
                assertTrue(sql.contains("similarity"), "SQLにsimilarity関数が含まれていない: " + sql);
            }
        }
    }

    @Nested
    @DisplayName("メタデータフィルタリング")
    class FilterTests {

        @Test
        @DisplayName("app_typeフィルタがdocument_chunksのSQL WHERE句にのみ含まれる")
        @SuppressWarnings("unchecked")
        void appTypeFilterIncludedInDocSqlOnly() {
            SearchFilters filters = new SearchFilters("web", null, null, null, null);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", filters, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            String docSql = sqls.stream().filter(s -> s.contains("document_chunks")).findFirst().orElseThrow();
            String codeSql = sqls.stream().filter(s -> s.contains("code_chunks")).findFirst().orElseThrow();
            assertTrue(docSql.contains("app_type = :app_type"), "document_chunksにapp_typeフィルタが含まれていない");
            assertFalse(codeSql.contains("app_type = :app_type"), "code_chunksにapp_typeフィルタが含まれてはならない");
        }

        @Test
        @DisplayName("複数フィルタがdocument_chunksのSQL WHERE句に全て含まれる")
        @SuppressWarnings("unchecked")
        void multipleFiltersIncludedInDocSql() {
            SearchFilters filters = new SearchFilters("rest", "nablarch-fw-web", "github", "code", "ja");
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", filters, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            String docSql = sqls.stream().filter(s -> s.contains("document_chunks")).findFirst().orElseThrow();
            String codeSql = sqls.stream().filter(s -> s.contains("code_chunks")).findFirst().orElseThrow();

            assertAll(
                    () -> assertTrue(docSql.contains("app_type = :app_type")),
                    () -> assertTrue(docSql.contains("module = :module")),
                    () -> assertTrue(docSql.contains("source = :source")),
                    () -> assertTrue(docSql.contains("source_type = :source_type")),
                    () -> assertTrue(docSql.contains("language = :language"))
            );

            assertAll(
                    () -> assertFalse(codeSql.contains("app_type = :app_type")),
                    () -> assertTrue(codeSql.contains("module = :module")),
                    () -> assertFalse(codeSql.contains("source = :source")),
                    () -> assertFalse(codeSql.contains("source_type = :source_type")),
                    () -> assertTrue(codeSql.contains("language = :language"))
            );
        }

        @Test
        @DisplayName("フィルタなしの場合はフィルタ条件がSQLに含まれない")
        @SuppressWarnings("unchecked")
        void noFiltersExcludesFilterConditions() {
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", SearchFilters.NONE, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            for (String sql : sqls) {
                assertFalse(sql.contains("app_type = :app_type"), "app_typeフィルタが含まれてはならない");
                assertFalse(sql.contains("module = :module"), "moduleフィルタが含まれてはならない");
                assertFalse(sql.contains("source = :source"), "sourceフィルタが含まれてはならない");
            }
        }
    }

    @Nested
    @DisplayName("extractKeywords")
    class ExtractKeywordsTests {

        @Test
        @DisplayName("スペース区切りのトークンがキーワードリストとして返される")
        void tokensReturnedAsList() {
            List<String> result = service.extractKeywords("REST API 認証");
            assertEquals(List.of("REST", "API", "認証"), result);
        }

        @Test
        @DisplayName("特殊文字が除去される")
        void specialCharactersRemoved() {
            List<String> result = service.extractKeywords("nablarch.fw.Handler<T>");
            assertEquals(List.of("nablarch.fw.Handler"), result);
        }

        @Test
        @DisplayName("連続スペースが正しく処理される")
        void multipleSpacesHandled() {
            List<String> result = service.extractKeywords("REST   API   認証");
            assertEquals(List.of("REST", "API", "認証"), result);
        }

        @Test
        @DisplayName("単一トークンの場合は1要素のリスト")
        void singleTokenReturnedAsList() {
            List<String> result = service.extractKeywords("ハンドラキュー");
            assertEquals(List.of("ハンドラキュー"), result);
        }

        @Test
        @DisplayName("日本語キーワードが正しく抽出される")
        void japaneseKeywordsExtracted() {
            List<String> result = service.extractKeywords("バッチ処理 データベース アクセス");
            assertEquals(List.of("バッチ処理", "データベース", "アクセス"), result);
        }
    }
}

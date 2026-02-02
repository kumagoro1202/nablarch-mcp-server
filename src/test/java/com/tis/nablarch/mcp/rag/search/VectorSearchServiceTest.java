package com.tis.nablarch.mcp.rag.search;

import com.tis.nablarch.mcp.embedding.EmbeddingClient;
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
 * {@link VectorSearchService} のユニットテスト。
 *
 * <p>JdbcTemplateとEmbeddingClientをモック化し、
 * SQL構築ロジック、ベクトル変換、マージロジックを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private EmbeddingClient documentEmbeddingClient;

    @Mock
    private EmbeddingClient codeEmbeddingClient;

    private VectorSearchService service;

    @BeforeEach
    void setUp() {
        service = new VectorSearchService(
                jdbcTemplate, documentEmbeddingClient, codeEmbeddingClient);
    }

    @Nested
    @DisplayName("search メソッド")
    class SearchTests {

        @Test
        @DisplayName("正常系: 両テーブルの結果がスコア降順でマージされる")
        void searchMergesResultsFromBothTables() {
            float[] docVector = {0.1f, 0.2f, 0.3f};
            float[] codeVector = {0.4f, 0.5f, 0.6f};
            when(documentEmbeddingClient.embed("ハンドラキュー")).thenReturn(docVector);
            when(codeEmbeddingClient.embed("ハンドラキュー")).thenReturn(codeVector);

            List<SearchResult> docResults = List.of(
                    new SearchResult("1", "ドキュメント内容", 0.95,
                            Map.of("source", "nablarch-document"), "https://example.com/1"),
                    new SearchResult("2", "ドキュメント内容2", 0.80,
                            Map.of("source", "nablarch-document"), null)
            );
            List<SearchResult> codeResults = List.of(
                    new SearchResult("101", "コード内容", 0.90,
                            Map.of("repo", "nablarch-fw-web"), null),
                    new SearchResult("102", "コード内容2", 0.70,
                            Map.of("repo", "nablarch-core"), null)
            );

            when(jdbcTemplate.query(contains("document_chunks"),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(docResults);
            when(jdbcTemplate.query(contains("code_chunks"),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(codeResults);

            List<SearchResult> results = service.search("ハンドラキュー", SearchFilters.NONE, 10);

            assertEquals(4, results.size());
            assertEquals("1", results.get(0).id());     // 0.95
            assertEquals("101", results.get(1).id());    // 0.90
            assertEquals("2", results.get(2).id());      // 0.80
            assertEquals("102", results.get(3).id());    // 0.70

            // 各Embeddingクライアントが呼ばれたことを確認
            verify(documentEmbeddingClient).embed("ハンドラキュー");
            verify(codeEmbeddingClient).embed("ハンドラキュー");
        }

        @Test
        @DisplayName("正常系: topKで結果数が制限される")
        void searchRespectsTopK() {
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            List<SearchResult> docResults = List.of(
                    new SearchResult("1", "内容1", 0.9, Map.of(), null),
                    new SearchResult("2", "内容2", 0.8, Map.of(), null),
                    new SearchResult("3", "内容3", 0.7, Map.of(), null)
            );

            when(jdbcTemplate.query(contains("document_chunks"),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(docResults);
            when(jdbcTemplate.query(contains("code_chunks"),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            List<SearchResult> results = service.search("テスト", SearchFilters.NONE, 2);

            assertEquals(2, results.size());
            assertEquals("1", results.get(0).id());
            assertEquals("2", results.get(1).id());
        }

        @Test
        @DisplayName("正常系: 結果が0件の場合は空リストを返す")
        void searchReturnsEmptyListWhenNoResults() {
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            when(jdbcTemplate.query(anyString(),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
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
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            when(jdbcTemplate.query(anyString(),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> service.search("テスト", null, 10));
        }
    }

    @Nested
    @DisplayName("メタデータフィルタリング")
    class FilterTests {

        @Test
        @DisplayName("app_typeフィルタがdocument_chunksのSQL WHERE句に含まれる")
        @SuppressWarnings("unchecked")
        void appTypeFilterIncludedInDocSql() {
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            SearchFilters filters = new SearchFilters("web", null, null, null, null);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", filters, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            // document_chunksへのクエリにapp_typeフィルタが含まれる
            String docSql = sqls.stream()
                    .filter(s -> s.contains("document_chunks"))
                    .findFirst().orElseThrow();
            assertTrue(docSql.contains("app_type = :app_type"),
                    "app_typeフィルタがdocument_chunks SQLに含まれていない");
        }

        @Test
        @DisplayName("moduleフィルタが両テーブルのSQL WHERE句に含まれる")
        @SuppressWarnings("unchecked")
        void moduleFilterIncludedInBothTables() {
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            SearchFilters filters = new SearchFilters(null, "nablarch-fw-web", null, null, null);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", filters, 10);

            List<String> sqls = sqlCaptor.getAllValues();
            assertTrue(sqls.stream().allMatch(sql -> sql.contains("module = :module")),
                    "moduleフィルタが両テーブルSQLに含まれていない");
        }
    }

    @Nested
    @DisplayName("toVectorString")
    class ToVectorStringTests {

        @Test
        @DisplayName("float配列がpgvectorフォーマットに変換される")
        void convertsFloatArrayToVectorString() {
            float[] vector = {0.1f, 0.2f, 0.3f};
            String result = VectorSearchService.toVectorString(vector);
            assertEquals("[0.1,0.2,0.3]", result);
        }

        @Test
        @DisplayName("単一要素の配列が正しく変換される")
        void convertsSingleElementArray() {
            float[] vector = {0.5f};
            String result = VectorSearchService.toVectorString(vector);
            assertEquals("[0.5]", result);
        }

        @Test
        @DisplayName("空配列が正しく変換される")
        void convertsEmptyArray() {
            float[] vector = {};
            String result = VectorSearchService.toVectorString(vector);
            assertEquals("[]", result);
        }
    }

    @Nested
    @DisplayName("SQL構築")
    class SqlBuildTests {

        @Test
        @DisplayName("document_chunksのSQLにベクトル距離計算が含まれる")
        @SuppressWarnings("unchecked")
        void sqlContainsVectorDistanceCalculation() {
            when(documentEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
            when(codeEmbeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});

            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

            when(jdbcTemplate.query(sqlCaptor.capture(),
                    any(MapSqlParameterSource.class), any(RowMapper.class)))
                    .thenReturn(Collections.emptyList());

            service.search("テスト", SearchFilters.NONE, 10);

            String sql = sqlCaptor.getAllValues().get(0); // document_chunks
            assertTrue(sql.contains("1 - (embedding <=>"),
                    "コサイン類似度計算がSQLに含まれていない");
            assertTrue(sql.contains("CAST(:query_vec AS vector)"),
                    "ベクトルCASTがSQLに含まれていない");
            assertTrue(sql.contains("embedding IS NOT NULL"),
                    "embedding IS NOT NULLチェックがSQLに含まれていない");
        }
    }
}

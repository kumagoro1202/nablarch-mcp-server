package com.tis.nablarch.mcp.rag.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BM25キーワード検索サービス。
 *
 * <p>pg_trgm（trigram）のILIKE検索とsimilarity関数を使用した
 * キーワード検索を提供する。PostgreSQL標準のFTS（to_tsvector/to_tsquery）
 * は日本語テキストの空白区切りトークナイズに非対応のため、
 * pg_trgmベースのILIKE検索に置き換えた。</p>
 *
 * <p>検索対象テーブル:</p>
 * <ul>
 *   <li>{@code document_chunks} — ドキュメントチャンク</li>
 *   <li>{@code code_chunks} — コードチャンク</li>
 * </ul>
 *
 * @see SearchResult
 * @see SearchFilters
 */
@Service
public class BM25SearchService {

    private static final Logger log = LoggerFactory.getLogger(BM25SearchService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * コンストラクタ。
     *
     * @param jdbcTemplate Spring NamedParameterJdbcTemplate
     */
    public BM25SearchService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * キーワード検索を実行する。
     *
     * <p>document_chunksテーブルとcode_chunksテーブルの両方を検索し、
     * スコア降順で統合した結果を返す。</p>
     *
     * @param query 検索クエリ（自然言語またはキーワード）
     * @param filters メタデータフィルタ条件（nullの場合フィルタなし）
     * @param topK 返却する結果数（1以上）
     * @return 検索結果リスト（スコア降順）
     * @throws IllegalArgumentException queryがnullまたは空白の場合
     */
    public List<SearchResult> search(String query, SearchFilters filters, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("検索クエリはnullまたは空白であってはならない");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topKは1以上でなければならない");
        }

        SearchFilters effectiveFilters = (filters != null) ? filters : SearchFilters.NONE;
        List<String> keywords = extractKeywords(query);

        log.debug("BM25検索実行: query='{}', keywords={}, topK={}", query, keywords, topK);

        List<SearchResult> docResults = searchTable("document_chunks", query, keywords, effectiveFilters, topK);
        List<SearchResult> codeResults = searchTable("code_chunks", query, keywords, effectiveFilters, topK);

        return mergeAndSort(docResults, codeResults, topK);
    }

    /**
     * 指定テーブルに対してキーワード検索を実行する。
     *
     * <p>pg_trgm GINインデックスを活用したILIKE検索で日本語を含む全言語に対応する。
     * スコアはpg_trgm similarity関数で算出する。</p>
     *
     * @param tableName 検索対象テーブル名
     * @param originalQuery 元のクエリ文字列（similarity計算用）
     * @param keywords 抽出済みキーワードリスト
     * @param filters メタデータフィルタ条件
     * @param topK 返却する結果数
     * @return 検索結果リスト
     */
    private List<SearchResult> searchTable(
            String tableName, String originalQuery, List<String> keywords,
            SearchFilters filters, int topK) {

        boolean isDocTable = "document_chunks".equals(tableName);

        if (keywords.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, content, ");
        sql.append("similarity(content, :original_query) AS bm25_score, ");
        sql.append("module, language, ");
        if (isDocTable) {
            sql.append("source, source_type, app_type, url ");
        } else {
            sql.append("repo, chunk_type, file_path ");
        }
        sql.append("FROM ").append(tableName);

        // ILIKE条件：全キーワードをAND結合
        sql.append(" WHERE ");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            String paramName = "kw" + i;
            sql.append("content ILIKE :").append(paramName);
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("original_query", originalQuery);
        for (int i = 0; i < keywords.size(); i++) {
            params.addValue("kw" + i, "%" + escapeIlike(keywords.get(i)) + "%");
        }

        appendFilters(sql, params, filters, isDocTable);

        sql.append(" ORDER BY bm25_score DESC");
        sql.append(" LIMIT :top_k");
        params.addValue("top_k", topK);

        log.debug("BM25検索SQL ({}): {}", tableName, sql);

        return jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> mapRow(rs, rowNum, isDocTable));
    }

    /**
     * ユーザークエリからキーワードを抽出する。
     *
     * <p>スペースで区切られた各トークンからキーワードを抽出する。
     * 日本語テキスト（スペースなし）の場合はクエリ全体を1キーワードとして扱う。</p>
     *
     * @param query ユーザークエリ
     * @return キーワードリスト
     */
    List<String> extractKeywords(String query) {
        String[] tokens = query.trim().split("\\s+");
        List<String> keywords = new ArrayList<>();

        for (String token : tokens) {
            String sanitized = sanitizeToken(token);
            if (!sanitized.isEmpty()) {
                keywords.add(sanitized);
            }
        }

        return keywords;
    }

    /**
     * ILIKE用エスケープ。%と_と\をエスケープする。
     *
     * @param value エスケープ対象文字列
     * @return エスケープ済み文字列
     */
    private String escapeIlike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * トークンから特殊文字を除去する。
     *
     * @param token 入力トークン
     * @return サニタイズ済みトークン
     */
    private String sanitizeToken(String token) {
        String result = token.replaceAll("<[^>]*>", "");
        return result.replaceAll("[&|!():'\"\\\\]", "").trim();
    }

    /**
     * メタデータフィルタ条件をSQL WHERE句に追加する。
     *
     * @param sql SQL文字列ビルダー
     * @param params パラメータソース
     * @param filters フィルタ条件
     * @param isDocTable document_chunksテーブルの場合true
     */
    private void appendFilters(
            StringBuilder sql, MapSqlParameterSource params, SearchFilters filters,
            boolean isDocTable) {

        if (filters.module() != null) {
            sql.append(" AND module = :module");
            params.addValue("module", filters.module());
        }
        if (filters.language() != null) {
            sql.append(" AND language = :language");
            params.addValue("language", filters.language());
        }
        if (isDocTable) {
            if (filters.appType() != null) {
                sql.append(" AND app_type = :app_type");
                params.addValue("app_type", filters.appType());
            }
            if (filters.source() != null) {
                sql.append(" AND source = :source");
                params.addValue("source", filters.source());
            }
            if (filters.sourceType() != null) {
                sql.append(" AND source_type = :source_type");
                params.addValue("source_type", filters.sourceType());
            }
        }
    }

    /**
     * ResultSetからSearchResultにマッピングする。
     *
     * @param rs ResultSet
     * @param rowNum 行番号
     * @param isDocTable document_chunksテーブルの場合true
     * @return SearchResult
     * @throws SQLException SQL例外
     */
    private SearchResult mapRow(ResultSet rs, int rowNum, boolean isDocTable) throws SQLException {
        Map<String, String> metadata = new HashMap<>();
        putIfNotNull(metadata, "module", rs.getString("module"));
        putIfNotNull(metadata, "language", rs.getString("language"));

        String sourceUrl;
        if (isDocTable) {
            putIfNotNull(metadata, "source", rs.getString("source"));
            putIfNotNull(metadata, "source_type", rs.getString("source_type"));
            putIfNotNull(metadata, "app_type", rs.getString("app_type"));
            sourceUrl = rs.getString("url");
        } else {
            putIfNotNull(metadata, "repo", rs.getString("repo"));
            putIfNotNull(metadata, "chunk_type", rs.getString("chunk_type"));
            putIfNotNull(metadata, "file_path", rs.getString("file_path"));
            sourceUrl = rs.getString("file_path");
        }

        return new SearchResult(
                rs.getString("id"),
                rs.getString("content"),
                rs.getDouble("bm25_score"),
                metadata,
                sourceUrl
        );
    }

    /**
     * null以外の値をマップに追加する。
     *
     * @param map 追加先マップ
     * @param key キー
     * @param value 値（nullの場合は追加しない）
     */
    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    /**
     * 2つのテーブルの検索結果をスコア降順でマージする。
     *
     * @param results1 テーブル1の結果
     * @param results2 テーブル2の結果
     * @param topK 返却する結果数
     * @return マージ済み結果リスト
     */
    private List<SearchResult> mergeAndSort(
            List<SearchResult> results1, List<SearchResult> results2, int topK) {

        List<SearchResult> merged = new ArrayList<>(results1.size() + results2.size());
        merged.addAll(results1);
        merged.addAll(results2);

        return merged.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .toList();
    }
}

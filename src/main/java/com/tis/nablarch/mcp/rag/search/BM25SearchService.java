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
 * <p>PostgreSQL Full Text Search（to_tsvector / to_tsquery）を使用した
 * BM25ベースのキーワード検索を提供する。ts_rank_cd関数による
 * スコア計算とメタデータフィルタリングをサポートする。</p>
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

    /**
     * PostgreSQL FTSの辞書設定。日本語形態素解析を使用する。
     */
    private static final String FTS_CONFIG = "japanese";

    /**
     * ts_rank_cdの正規化フラグ。
     * 32 = rank / (rank + 1) で [0, 1) に正規化。
     */
    private static final int RANK_NORMALIZATION = 32;

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
     * BM25キーワード検索を実行する。
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
        String tsQuery = buildTsQuery(query);

        log.debug("BM25検索実行: query='{}', tsQuery='{}', topK={}", query, tsQuery, topK);

        List<SearchResult> docResults = searchTable("document_chunks", tsQuery, effectiveFilters, topK);
        List<SearchResult> codeResults = searchTable("code_chunks", tsQuery, effectiveFilters, topK);

        return mergeAndSort(docResults, codeResults, topK);
    }

    /**
     * 指定テーブルに対してBM25検索を実行する。
     *
     * @param tableName 検索対象テーブル名
     * @param tsQuery PostgreSQL tsquery文字列
     * @param filters メタデータフィルタ条件
     * @param topK 返却する結果数
     * @return 検索結果リスト
     */
    private List<SearchResult> searchTable(
            String tableName, String tsQuery, SearchFilters filters, int topK) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, content, ");
        sql.append("ts_rank_cd(");
        sql.append("to_tsvector('").append(FTS_CONFIG).append("', content), ");
        sql.append("to_tsquery('").append(FTS_CONFIG).append("', :ts_query), ");
        sql.append(RANK_NORMALIZATION);
        sql.append(") AS bm25_score, ");
        sql.append("metadata, source_url ");
        sql.append("FROM ").append(tableName).append(" ");
        sql.append("WHERE to_tsvector('").append(FTS_CONFIG).append("', content) ");
        sql.append("@@ to_tsquery('").append(FTS_CONFIG).append("', :ts_query)");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ts_query", tsQuery);

        appendFilters(sql, params, filters);

        sql.append(" ORDER BY bm25_score DESC");
        sql.append(" LIMIT :top_k");
        params.addValue("top_k", topK);

        log.debug("BM25検索SQL ({}): {}", tableName, sql);

        return jdbcTemplate.query(sql.toString(), params, this::mapRow);
    }

    /**
     * ユーザークエリをPostgreSQL tsquery文字列に変換する。
     *
     * <p>スペースで区切られた各トークンをAND演算子（&amp;）で結合する。
     * 空トークンと特殊文字は除外する。</p>
     *
     * @param query ユーザークエリ
     * @return tsquery文字列
     */
    String buildTsQuery(String query) {
        String[] tokens = query.trim().split("\\s+");
        List<String> validTokens = new ArrayList<>();

        for (String token : tokens) {
            String sanitized = sanitizeToken(token);
            if (!sanitized.isEmpty()) {
                validTokens.add(sanitized);
            }
        }

        if (validTokens.isEmpty()) {
            return query.trim();
        }

        return String.join(" & ", validTokens);
    }

    /**
     * トークンからtsquery特殊文字を除去する。
     *
     * @param token 入力トークン
     * @return サニタイズ済みトークン
     */
    private String sanitizeToken(String token) {
        // まずジェネリック型パラメータ（<T>等）を除去
        String result = token.replaceAll("<[^>]*>", "");
        // 次に特殊文字を除去（<と>は上で処理済みなので除外）
        return result.replaceAll("[&|!():'\"\\\\]", "").trim();
    }

    /**
     * メタデータフィルタ条件をSQL WHERE句に追加する。
     *
     * @param sql SQL文字列ビルダー
     * @param params パラメータソース
     * @param filters フィルタ条件
     */
    private void appendFilters(
            StringBuilder sql, MapSqlParameterSource params, SearchFilters filters) {

        if (filters.appType() != null) {
            sql.append(" AND metadata->>'app_type' = :app_type");
            params.addValue("app_type", filters.appType());
        }
        if (filters.module() != null) {
            sql.append(" AND metadata->>'module' = :module");
            params.addValue("module", filters.module());
        }
        if (filters.source() != null) {
            sql.append(" AND metadata->>'source' = :source");
            params.addValue("source", filters.source());
        }
        if (filters.sourceType() != null) {
            sql.append(" AND metadata->>'source_type' = :source_type");
            params.addValue("source_type", filters.sourceType());
        }
        if (filters.language() != null) {
            sql.append(" AND metadata->>'language' = :language");
            params.addValue("language", filters.language());
        }
    }

    /**
     * ResultSetからSearchResultにマッピングする。
     *
     * @param rs ResultSet
     * @param rowNum 行番号
     * @return SearchResult
     * @throws SQLException SQL例外
     */
    private SearchResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        String metadataJson = rs.getString("metadata");
        Map<String, String> metadata = parseMetadata(metadataJson);

        return new SearchResult(
                rs.getString("id"),
                rs.getString("content"),
                rs.getDouble("bm25_score"),
                metadata,
                rs.getString("source_url")
        );
    }

    /**
     * JSONB文字列をMap&lt;String, String&gt;に変換する。
     *
     * <p>簡易的なJSONパースを行う。本番環境ではJacksonのObjectMapperを
     * 使用することを推奨する。</p>
     *
     * @param json JSONB文字列
     * @return メタデータマップ
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("メタデータのパースに失敗: {}", e.getMessage());
            return Map.of();
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

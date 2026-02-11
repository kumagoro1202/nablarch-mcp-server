package com.tis.nablarch.mcp.rag.search;

import com.tis.nablarch.mcp.embedding.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * ベクトル類似度検索サービス。
 *
 * <p>pgvectorのコサイン類似度演算子（{@code <=>}）を使用した
 * セマンティック検索を提供する。クエリテキストをEmbeddingClient経由で
 * ベクトルに変換し、document_chunksとcode_chunksの両テーブルを検索する。</p>
 *
 * <p>Embeddingモデルの使い分け（provider設定により切替）:</p>
 * <ul>
 *   <li>{@code document_chunks} — local: BGE-M3 / api: Jina embeddings-v4</li>
 *   <li>{@code code_chunks} — local: CodeSage-small-v2 / api: Voyage-code-3</li>
 * </ul>
 *
 * @see SearchResult
 * @see SearchFilters
 * @see BM25SearchService
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EmbeddingClient documentEmbeddingClient;
    private final EmbeddingClient codeEmbeddingClient;

    /**
     * コンストラクタ。
     *
     * @param jdbcTemplate Spring NamedParameterJdbcTemplate
     * @param documentEmbeddingClient ドキュメント用Embeddingクライアント（local: BGE-M3 / api: Jina v4）
     * @param codeEmbeddingClient コード用Embeddingクライアント（local: CodeSage / api: Voyage-code-3）
     */
    public VectorSearchService(
            NamedParameterJdbcTemplate jdbcTemplate,
            @Qualifier("document") EmbeddingClient documentEmbeddingClient,
            @Qualifier("code") EmbeddingClient codeEmbeddingClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.documentEmbeddingClient = documentEmbeddingClient;
        this.codeEmbeddingClient = codeEmbeddingClient;
    }

    /**
     * ベクトル類似度検索を実行する。
     *
     * <p>クエリテキストをベクトルに変換し、document_chunksとcode_chunksの
     * 両テーブルに対してコサイン類似度検索を実行する。結果はスコア降順で
     * マージされ、topK件に制限される。</p>
     *
     * @param query 検索クエリ（自然言語テキスト）
     * @param filters メタデータフィルタ条件（nullの場合フィルタなし）
     * @param topK 返却する結果数（1以上）
     * @return 検索結果リスト（スコア降順）
     * @throws IllegalArgumentException queryがnullまたは空白、topKが1未満の場合
     */
    public List<SearchResult> search(String query, SearchFilters filters, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("検索クエリはnullまたは空白であってはならない");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topKは1以上でなければならない");
        }

        SearchFilters effectiveFilters = (filters != null) ? filters : SearchFilters.NONE;

        log.debug("ベクトル検索実行: query='{}', topK={}", query, topK);

        // ドキュメント用Embedding生成（Jina v4）
        float[] docEmbedding = documentEmbeddingClient.embed(query);
        String docVectorStr = toVectorString(docEmbedding);

        // コード用Embedding生成（Voyage-code-3）
        float[] codeEmbedding = codeEmbeddingClient.embed(query);
        String codeVectorStr = toVectorString(codeEmbedding);

        List<SearchResult> docResults = searchTable(
                "document_chunks", docVectorStr, effectiveFilters, topK);
        List<SearchResult> codeResults = searchTable(
                "code_chunks", codeVectorStr, effectiveFilters, topK);

        return mergeAndSort(docResults, codeResults, topK);
    }

    /**
     * 指定テーブルに対してベクトル類似度検索を実行する。
     *
     * @param tableName 検索対象テーブル名
     * @param vectorStr ベクトル文字列（"[0.1,0.2,...]"形式）
     * @param filters メタデータフィルタ条件
     * @param topK 返却する結果数
     * @return 検索結果リスト
     */
    private List<SearchResult> searchTable(
            String tableName, String vectorStr, SearchFilters filters, int topK) {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT id, content, ");
        sql.append("1 - (embedding <=> CAST(:query_vec AS vector)) AS vector_score");
        appendMetadataColumns(sql, tableName);
        sql.append(" FROM ").append(tableName);
        sql.append(" WHERE embedding IS NOT NULL");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("query_vec", vectorStr);

        appendFilters(sql, params, filters, tableName);

        sql.append(" ORDER BY embedding <=> CAST(:query_vec AS vector)");
        sql.append(" LIMIT :top_k");
        params.addValue("top_k", topK);

        log.debug("ベクトル検索SQL ({}): {}", tableName, sql);

        return jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> mapRow(rs, tableName));
    }

    /**
     * メタデータカラムをSELECT句に追加する。
     *
     * @param sql SQL文字列ビルダー
     * @param tableName テーブル名
     */
    private void appendMetadataColumns(StringBuilder sql, String tableName) {
        if ("document_chunks".equals(tableName)) {
            sql.append(", source, source_type, module, app_type, language, fqcn, url");
        } else {
            sql.append(", repo, chunk_type, module, language, fqcn, file_path");
        }
    }

    /**
     * メタデータフィルタ条件をSQL WHERE句に追加する。
     *
     * <p>テーブルごとにカラム名が異なるため、テーブル名に応じてフィルタを適用する。</p>
     *
     * @param sql SQL文字列ビルダー
     * @param params パラメータソース
     * @param filters フィルタ条件
     * @param tableName テーブル名
     */
    private void appendFilters(
            StringBuilder sql, MapSqlParameterSource params,
            SearchFilters filters, String tableName) {

        if (filters.appType() != null && "document_chunks".equals(tableName)) {
            sql.append(" AND app_type = :app_type");
            params.addValue("app_type", filters.appType());
        }
        if (filters.module() != null) {
            sql.append(" AND module = :module");
            params.addValue("module", filters.module());
        }
        if (filters.source() != null && "document_chunks".equals(tableName)) {
            sql.append(" AND source = :source");
            params.addValue("source", filters.source());
        }
        if (filters.sourceType() != null && "document_chunks".equals(tableName)) {
            sql.append(" AND source_type = :source_type");
            params.addValue("source_type", filters.sourceType());
        }
        if (filters.language() != null) {
            sql.append(" AND language = :language");
            params.addValue("language", filters.language());
        }
    }

    /**
     * ResultSetからSearchResultにマッピングする。
     *
     * @param rs ResultSet
     * @param tableName テーブル名（メタデータカラム判定用）
     * @return SearchResult
     * @throws SQLException SQL例外
     */
    private SearchResult mapRow(ResultSet rs, String tableName) throws SQLException {
        Map<String, String> metadata = new HashMap<>();

        if ("document_chunks".equals(tableName)) {
            putIfNotNull(metadata, "source", rs.getString("source"));
            putIfNotNull(metadata, "source_type", rs.getString("source_type"));
            putIfNotNull(metadata, "app_type", rs.getString("app_type"));
            putIfNotNull(metadata, "language", rs.getString("language"));
            putIfNotNull(metadata, "fqcn", rs.getString("fqcn"));
        } else {
            putIfNotNull(metadata, "repo", rs.getString("repo"));
            putIfNotNull(metadata, "chunk_type", rs.getString("chunk_type"));
            putIfNotNull(metadata, "language", rs.getString("language"));
            putIfNotNull(metadata, "fqcn", rs.getString("fqcn"));
            putIfNotNull(metadata, "file_path", rs.getString("file_path"));
        }
        putIfNotNull(metadata, "module", rs.getString("module"));
        metadata.put("table", tableName);

        String sourceUrl = "document_chunks".equals(tableName) ? rs.getString("url") : null;

        return new SearchResult(
                String.valueOf(rs.getLong("id")),
                rs.getString("content"),
                rs.getDouble("vector_score"),
                metadata,
                sourceUrl
        );
    }

    /**
     * 値がnullでない場合のみMapに追加する。
     */
    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * float配列をpgvectorのベクトル文字列形式に変換する。
     *
     * <p>例: {@code [0.1, 0.2, 0.3]} → {@code "[0.1,0.2,0.3]"}</p>
     *
     * @param vector float配列
     * @return ベクトル文字列
     */
    static String toVectorString(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            joiner.add(String.valueOf(v));
        }
        return joiner.toString();
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

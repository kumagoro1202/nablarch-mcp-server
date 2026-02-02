package com.tis.nablarch.mcp.db.repository;

import com.tis.nablarch.mcp.db.dto.ChunkSimilarityResult;
import com.tis.nablarch.mcp.db.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ドキュメントチャンクリポジトリ。
 *
 * <p>基本的なCRUD操作はSpring Data JPAの標準メソッドを使用し、
 * ベクトル類似度検索はpgvectorのネイティブクエリで実行する。</p>
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * ソース種別でドキュメントチャンクを検索する。
     *
     * @param source ソース種別
     * @return 該当するドキュメントチャンクのリスト
     */
    List<DocumentChunk> findBySource(String source);

    /**
     * モジュール名でドキュメントチャンクを検索する。
     *
     * @param module Mavenモジュール名
     * @return 該当するドキュメントチャンクのリスト
     */
    List<DocumentChunk> findByModule(String module);

    /**
     * アプリタイプでドキュメントチャンクを検索する。
     *
     * @param appType アプリタイプ
     * @return 該当するドキュメントチャンクのリスト
     */
    List<DocumentChunk> findByAppType(String appType);

    /**
     * 完全修飾クラス名でドキュメントチャンクを検索する。
     *
     * @param fqcn 完全修飾クラス名
     * @return 該当するドキュメントチャンクのリスト
     */
    List<DocumentChunk> findByFqcn(String fqcn);

    /**
     * エンベディングベクトルを更新する。
     *
     * <p>pgvectorのvector型にキャストするためネイティブクエリを使用する。</p>
     *
     * @param id チャンクID
     * @param embedding ベクトル文字列（例: "[0.1, 0.2, ...]"）
     */
    @Modifying
    @Query(value = "UPDATE document_chunks SET embedding = CAST(:embedding AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    /**
     * ベクトル類似度検索（コサイン類似度）を実行する。
     *
     * @param embedding クエリベクトル文字列
     * @param topK 取得件数上限
     * @return 類似度スコア付きの検索結果
     */
    @Query(value = "SELECT dc.id AS id, dc.content AS content, " +
            "1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity " +
            "FROM document_chunks dc " +
            "WHERE dc.embedding IS NOT NULL " +
            "ORDER BY dc.embedding <=> CAST(:embedding AS vector) " +
            "LIMIT :topK",
            nativeQuery = true)
    List<ChunkSimilarityResult> findSimilar(@Param("embedding") String embedding,
                                            @Param("topK") int topK);

    /**
     * アプリタイプでフィルタしたベクトル類似度検索を実行する。
     *
     * @param embedding クエリベクトル文字列
     * @param appType アプリタイプフィルタ
     * @param topK 取得件数上限
     * @return 類似度スコア付きの検索結果
     */
    @Query(value = "SELECT dc.id AS id, dc.content AS content, " +
            "1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity " +
            "FROM document_chunks dc " +
            "WHERE dc.embedding IS NOT NULL AND dc.app_type = :appType " +
            "ORDER BY dc.embedding <=> CAST(:embedding AS vector) " +
            "LIMIT :topK",
            nativeQuery = true)
    List<ChunkSimilarityResult> findSimilarByAppType(@Param("embedding") String embedding,
                                                     @Param("appType") String appType,
                                                     @Param("topK") int topK);

    /**
     * ハイブリッド検索（BM25 + ベクトル類似度）を実行する。
     *
     * <p>FTS（BM25）スコアとベクトル類似度スコアを重み付き結合する。
     * 重み: ベクトル 0.7 / FTS 0.3</p>
     *
     * @param embedding クエリベクトル文字列
     * @param keyword 全文検索キーワード
     * @param topK 取得件数上限
     * @return 統合スコア付きの検索結果
     */
    @Query(value = "WITH vector_results AS (" +
            "  SELECT id, content, 1 - (embedding <=> CAST(:embedding AS vector)) AS vector_score " +
            "  FROM document_chunks WHERE embedding IS NOT NULL " +
            "  ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT 50" +
            "), fts_results AS (" +
            "  SELECT id, content, ts_rank(to_tsvector('english', content), " +
            "    plainto_tsquery('english', :keyword)) AS fts_score " +
            "  FROM document_chunks " +
            "  WHERE to_tsvector('english', content) @@ plainto_tsquery('english', :keyword) " +
            "  LIMIT 50" +
            ") SELECT COALESCE(v.id, f.id) AS id, " +
            "  COALESCE(v.content, f.content) AS content, " +
            "  COALESCE(v.vector_score, 0) * 0.7 + COALESCE(f.fts_score, 0) * 0.3 AS similarity " +
            "FROM vector_results v FULL OUTER JOIN fts_results f ON v.id = f.id " +
            "ORDER BY similarity DESC LIMIT :topK",
            nativeQuery = true)
    List<ChunkSimilarityResult> findByHybridSearch(@Param("embedding") String embedding,
                                                   @Param("keyword") String keyword,
                                                   @Param("topK") int topK);
}

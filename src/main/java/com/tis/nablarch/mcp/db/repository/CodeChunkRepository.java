package com.tis.nablarch.mcp.db.repository;

import com.tis.nablarch.mcp.db.dto.ChunkSimilarityResult;
import com.tis.nablarch.mcp.db.entity.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * コードチャンクリポジトリ。
 *
 * <p>基本的なCRUD操作はSpring Data JPAの標準メソッドを使用し、
 * ベクトル類似度検索はpgvectorのネイティブクエリで実行する。</p>
 */
@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {

    /**
     * リポジトリ名でコードチャンクを検索する。
     *
     * @param repo リポジトリ名
     * @return 該当するコードチャンクのリスト
     */
    List<CodeChunk> findByRepo(String repo);

    /**
     * モジュール名でコードチャンクを検索する。
     *
     * @param module Mavenモジュール名
     * @return 該当するコードチャンクのリスト
     */
    List<CodeChunk> findByModule(String module);

    /**
     * チャンク種別でコードチャンクを検索する。
     *
     * @param chunkType チャンク種別
     * @return 該当するコードチャンクのリスト
     */
    List<CodeChunk> findByChunkType(String chunkType);

    /**
     * 完全修飾クラス名でコードチャンクを検索する。
     *
     * @param fqcn 完全修飾クラス名
     * @return 該当するコードチャンクのリスト
     */
    List<CodeChunk> findByFqcn(String fqcn);

    /**
     * エンベディングベクトルを更新する。
     *
     * @param id チャンクID
     * @param embedding ベクトル文字列（例: "[0.1, 0.2, ...]"）
     */
    @Modifying
    @Query(value = "UPDATE code_chunks SET embedding = CAST(:embedding AS vector) WHERE id = :id",
            nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    /**
     * ベクトル類似度検索（コサイン類似度）を実行する。
     *
     * @param embedding クエリベクトル文字列
     * @param topK 取得件数上限
     * @return 類似度スコア付きの検索結果
     */
    @Query(value = "SELECT cc.id AS id, cc.content AS content, " +
            "1 - (cc.embedding <=> CAST(:embedding AS vector)) AS similarity " +
            "FROM code_chunks cc " +
            "WHERE cc.embedding IS NOT NULL " +
            "ORDER BY cc.embedding <=> CAST(:embedding AS vector) " +
            "LIMIT :topK",
            nativeQuery = true)
    List<ChunkSimilarityResult> findSimilar(@Param("embedding") String embedding,
                                            @Param("topK") int topK);

    /**
     * リポジトリ名でフィルタしたベクトル類似度検索を実行する。
     *
     * @param embedding クエリベクトル文字列
     * @param repo リポジトリ名フィルタ
     * @param topK 取得件数上限
     * @return 類似度スコア付きの検索結果
     */
    @Query(value = "SELECT cc.id AS id, cc.content AS content, " +
            "1 - (cc.embedding <=> CAST(:embedding AS vector)) AS similarity " +
            "FROM code_chunks cc " +
            "WHERE cc.embedding IS NOT NULL AND cc.repo = :repo " +
            "ORDER BY cc.embedding <=> CAST(:embedding AS vector) " +
            "LIMIT :topK",
            nativeQuery = true)
    List<ChunkSimilarityResult> findSimilarByRepo(@Param("embedding") String embedding,
                                                  @Param("repo") String repo,
                                                  @Param("topK") int topK);

    /**
     * ハイブリッド検索（BM25 + ベクトル類似度）を実行する。
     *
     * @param embedding クエリベクトル文字列
     * @param keyword 全文検索キーワード
     * @param topK 取得件数上限
     * @return 統合スコア付きの検索結果
     */
    @Query(value = "WITH vector_results AS (" +
            "  SELECT id, content, 1 - (embedding <=> CAST(:embedding AS vector)) AS vector_score " +
            "  FROM code_chunks WHERE embedding IS NOT NULL " +
            "  ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT 50" +
            "), fts_results AS (" +
            "  SELECT id, content, ts_rank(to_tsvector('english', content), " +
            "    plainto_tsquery('english', :keyword)) AS fts_score " +
            "  FROM code_chunks " +
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

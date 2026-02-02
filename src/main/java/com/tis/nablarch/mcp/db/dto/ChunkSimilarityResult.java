package com.tis.nablarch.mcp.db.dto;

/**
 * ベクトル類似度検索結果のプロジェクションインターフェース。
 *
 * <p>ネイティブクエリの結果をSpring Data JPAのインターフェースプロジェクションで受け取る。</p>
 */
public interface ChunkSimilarityResult {

    /** チャンクID */
    Long getId();

    /** チャンクテキスト本文 */
    String getContent();

    /** コサイン類似度スコア（0.0〜1.0） */
    Double getSimilarity();
}

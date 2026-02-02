package com.tis.nablarch.mcp.embedding;

import java.util.List;

/**
 * Embeddingクライアントインターフェース。
 *
 * <p>テキストをベクトル（float配列）に変換するEmbedding APIの抽象化。
 * Jina v4（ドキュメント用）とVoyage-code-3（コード用）の共通インターフェースとして使用する。</p>
 */
public interface EmbeddingClient {

    /**
     * 単一テキストのEmbeddingベクトルを生成する。
     *
     * @param text 入力テキスト
     * @return Embeddingベクトル（float配列）
     */
    float[] embed(String text);

    /**
     * 複数テキストのEmbeddingベクトルを一括生成する。
     *
     * @param texts 入力テキストのリスト
     * @return Embeddingベクトルのリスト（入力と同じ順序）
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * このクライアントが使用するモデル名を返す。
     *
     * @return モデル名
     */
    String getModelName();

    /**
     * このクライアントが生成するベクトルの次元数を返す。
     *
     * @return 次元数
     */
    int getDimensions();
}

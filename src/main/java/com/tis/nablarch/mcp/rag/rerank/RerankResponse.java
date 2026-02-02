package com.tis.nablarch.mcp.rag.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jina Reranker API レスポンスDTO。
 *
 * <p>Jina Reranker APIからの応答をデシリアライズする。</p>
 *
 * @param results リランキング結果リスト（relevance_score降順）
 */
public record RerankResponse(
        List<Result> results
) {

    /**
     * リランキング結果1件。
     *
     * @param index 入力ドキュメントリスト内のインデックス
     * @param relevanceScore 関連度スコア（0.0〜1.0）
     */
    public record Result(
            int index,
            @JsonProperty("relevance_score") double relevanceScore
    ) {
    }
}

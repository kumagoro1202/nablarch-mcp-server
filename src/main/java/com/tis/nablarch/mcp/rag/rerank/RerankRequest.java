package com.tis.nablarch.mcp.rag.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jina Reranker API リクエストDTO。
 *
 * <p>POST https://api.jina.ai/v1/rerank に送信するリクエストボディ。</p>
 *
 * @param model モデル名（例: jina-reranker-v2-base-multilingual）
 * @param query 検索クエリ
 * @param documents 候補ドキュメントのテキストリスト
 * @param topN 返却する上位件数
 * @param returnDocuments ドキュメント本文を返却するか（false推奨）
 */
public record RerankRequest(
        String model,
        String query,
        List<String> documents,
        @JsonProperty("top_n") int topN,
        @JsonProperty("return_documents") boolean returnDocuments
) {
}

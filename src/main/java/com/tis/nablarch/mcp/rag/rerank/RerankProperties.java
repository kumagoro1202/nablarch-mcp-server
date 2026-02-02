package com.tis.nablarch.mcp.rag.rerank;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * リランキング設定プロパティ。
 *
 * <p>{@code nablarch.mcp.rerank} プレフィクスで設定値をバインドする。</p>
 */
@ConfigurationProperties(prefix = "nablarch.mcp.rerank")
public class RerankProperties {

    private final Jina jina = new Jina();

    /**
     * Jina Reranker設定を返す。
     *
     * @return Jina設定
     */
    public Jina getJina() {
        return jina;
    }

    /**
     * Jina Reranker API設定。
     */
    public static class Jina {

        private String apiKey = "";
        private String model = "jina-reranker-v2-base-multilingual";
        private String baseUrl = "https://api.jina.ai/v1/rerank";
        private int timeoutMs = 3000;
        private int topK = 10;

        /**
         * APIキーを返す。
         *
         * @return APIキー
         */
        public String getApiKey() {
            return apiKey;
        }

        /**
         * APIキーを設定する。
         *
         * @param apiKey APIキー
         */
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        /**
         * モデル名を返す。
         *
         * @return モデル名
         */
        public String getModel() {
            return model;
        }

        /**
         * モデル名を設定する。
         *
         * @param model モデル名
         */
        public void setModel(String model) {
            this.model = model;
        }

        /**
         * APIベースURLを返す。
         *
         * @return ベースURL
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * APIベースURLを設定する。
         *
         * @param baseUrl ベースURL
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * タイムアウト（ミリ秒）を返す。
         *
         * @return タイムアウト（ミリ秒）
         */
        public int getTimeoutMs() {
            return timeoutMs;
        }

        /**
         * タイムアウト（ミリ秒）を設定する。
         *
         * @param timeoutMs タイムアウト（ミリ秒）
         */
        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        /**
         * デフォルトのtopK値を返す。
         *
         * @return topK値
         */
        public int getTopK() {
            return topK;
        }

        /**
         * デフォルトのtopK値を設定する。
         *
         * @param topK topK値
         */
        public void setTopK(int topK) {
            this.topK = topK;
        }
    }
}

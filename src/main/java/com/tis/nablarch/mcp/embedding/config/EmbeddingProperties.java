package com.tis.nablarch.mcp.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding API設定プロパティ。
 *
 * <p>application.yamlの {@code nablarch.mcp.embedding} 配下の設定を保持する。</p>
 */
@Component
@ConfigurationProperties(prefix = "nablarch.mcp.embedding")
public class EmbeddingProperties {

    private ProviderConfig jina = new ProviderConfig();
    private ProviderConfig voyage = new ProviderConfig();

    public ProviderConfig getJina() {
        return jina;
    }

    public void setJina(ProviderConfig jina) {
        this.jina = jina;
    }

    public ProviderConfig getVoyage() {
        return voyage;
    }

    public void setVoyage(ProviderConfig voyage) {
        this.voyage = voyage;
    }

    /**
     * 各Embeddingプロバイダの設定。
     */
    public static class ProviderConfig {

        private String apiKey = "";
        private String model = "";
        private int dimensions = 1024;
        private String baseUrl = "";
        private int timeoutSeconds = 30;
        private int maxRetries = 3;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}

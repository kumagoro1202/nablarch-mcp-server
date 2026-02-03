package com.tis.nablarch.mcp.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding API設定プロパティ。
 *
 * <p>application.yamlの {@code nablarch.mcp.embedding} 配下の設定を保持する。
 * API（Jina/Voyage）とローカルONNXモデル（BGE-M3/CodeSage）の両方をサポートする。</p>
 */
@Component
@ConfigurationProperties(prefix = "nablarch.mcp.embedding")
public class EmbeddingProperties {

    /**
     * 使用するプロバイダの種別。
     * local: ローカルONNXモデル（推奨・無償）
     * api: 外部API（Jina/Voyage、従量課金）
     */
    private String provider = "local";

    private ProviderConfig jina = new ProviderConfig();
    private ProviderConfig voyage = new ProviderConfig();
    private LocalModelConfig local = new LocalModelConfig();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

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

    public LocalModelConfig getLocal() {
        return local;
    }

    public void setLocal(LocalModelConfig local) {
        this.local = local;
    }

    /**
     * 各Embeddingプロバイダ（API）の設定。
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

    /**
     * ローカルONNXモデルの設定。
     */
    public static class LocalModelConfig {

        private OnnxModelConfig document = new OnnxModelConfig();
        private OnnxModelConfig code = new OnnxModelConfig();

        public OnnxModelConfig getDocument() {
            return document;
        }

        public void setDocument(OnnxModelConfig document) {
            this.document = document;
        }

        public OnnxModelConfig getCode() {
            return code;
        }

        public void setCode(OnnxModelConfig code) {
            this.code = code;
        }
    }

    /**
     * 個別のONNXモデル設定。
     */
    public static class OnnxModelConfig {

        /**
         * モデル名（ログ出力用）。
         */
        private String modelName = "";

        /**
         * ONNXモデルファイルのパス。
         * 例: /opt/models/bge-m3/model.onnx
         */
        private String modelPath = "";

        /**
         * トークナイザーファイルのディレクトリパス。
         * 例: /opt/models/bge-m3/
         */
        private String tokenizerPath = "";

        /**
         * 出力ベクトルの次元数。
         */
        private int dimensions = 1024;

        /**
         * 最大トークン長。
         */
        private int maxTokens = 512;

        /**
         * バッチ推論サイズ。
         */
        private int batchSize = 32;

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getModelPath() {
            return modelPath;
        }

        public void setModelPath(String modelPath) {
            this.modelPath = modelPath;
        }

        public String getTokenizerPath() {
            return tokenizerPath;
        }

        public void setTokenizerPath(String tokenizerPath) {
            this.tokenizerPath = tokenizerPath;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }
}

package com.tis.nablarch.mcp.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Embedding APIレスポンスボディ。
 *
 * <p>Jina / Voyage共通のOpenAI互換Embedding APIレスポンス形式。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse {

    @JsonProperty("data")
    private List<EmbeddingData> data;

    @JsonProperty("model")
    private String model;

    @JsonProperty("usage")
    private Usage usage;

    public List<EmbeddingData> getData() {
        return data;
    }

    public void setData(List<EmbeddingData> data) {
        this.data = data;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * 個別のEmbeddingデータ。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddingData {

        @JsonProperty("embedding")
        private float[] embedding;

        @JsonProperty("index")
        private int index;

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    /**
     * API使用量情報。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;

        public int getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}

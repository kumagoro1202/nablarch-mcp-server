package com.tis.nablarch.mcp.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Embedding APIリクエストボディ。
 *
 * <p>Jina / Voyage共通のOpenAI互換Embedding APIリクエスト形式。</p>
 */
public class EmbeddingRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("input")
    private List<String> input;

    @JsonProperty("dimensions")
    private Integer dimensions;

    public EmbeddingRequest() {
    }

    public EmbeddingRequest(String model, List<String> input, Integer dimensions) {
        this.model = model;
        this.input = input;
        this.dimensions = dimensions;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }

    public Integer getDimensions() {
        return dimensions;
    }

    public void setDimensions(Integer dimensions) {
        this.dimensions = dimensions;
    }
}

package com.tis.nablarch.mcp.embedding;

/**
 * Embedding API呼び出し時の例外。
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.tis.nablarch.mcp.rag.query;

/**
 * クエリの言語を表す列挙型。
 *
 * <p>クエリ解析時に検出された言語に基づき、
 * FTS辞書の選択やEmbeddingモデルの選択に使用する。</p>
 */
public enum QueryLanguage {

    /**
     * 日本語クエリ。
     * 日本語文字の比率が70%以上の場合に判定される。
     */
    JAPANESE,

    /**
     * 英語クエリ。
     * 日本語文字の比率が10%未満の場合に判定される。
     */
    ENGLISH,

    /**
     * 日英混在クエリ。
     * 日本語文字の比率が10%以上70%未満の場合に判定される。
     */
    MIXED
}

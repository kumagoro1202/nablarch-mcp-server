package com.tis.nablarch.mcp.rag.search;

/**
 * 検索モードを表す列挙型。
 *
 * <p>ハイブリッド検索サービスにおいて、
 * 使用する検索アルゴリズムの組み合わせを指定する。</p>
 */
public enum SearchMode {

    /**
     * BM25キーワード検索とベクトルセマンティック検索の
     * ハイブリッド（デフォルト）。
     * Reciprocal Rank Fusionで結果を統合する。
     */
    HYBRID,

    /**
     * BM25キーワード検索のみ。
     * PostgreSQL Full Text Searchを使用する。
     * FQCN検索やexact match重視の場合に使用。
     */
    KEYWORD,

    /**
     * ベクトルセマンティック検索のみ。
     * pgvectorコサイン類似度を使用する。
     * 概念的な検索や同義語検索に使用。
     */
    VECTOR
}

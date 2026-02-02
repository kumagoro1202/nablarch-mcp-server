package com.tis.nablarch.mcp.rag.chunking;

/**
 * チャンキング対象のコンテンツタイプ。
 *
 * <p>各タイプに応じてチャンキング戦略とEmbeddingモデルが切り替わる。</p>
 */
public enum ContentType {

    /** 公式ドキュメント（HTML形式） */
    HTML,

    /** Fintan記事等（Markdown形式） */
    MARKDOWN,

    /** Javadocコメント */
    JAVADOC,

    /** Javaソースコード */
    JAVA,

    /** XML設定ファイル */
    XML,

    /** プレーンテキスト（フォールバック） */
    TEXT
}

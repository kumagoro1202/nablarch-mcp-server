package com.tis.nablarch.mcp.rag.parser;

import java.util.List;

/**
 * ドキュメントパーサーインターフェース。
 *
 * <p>各データソース形式（HTML, Markdown, Java, XML）に対応した
 * パーサーが本インターフェースを実装する。
 * 生のコンテンツ文字列を受け取り、構造化されたParsedDocumentのリストを返す。</p>
 */
public interface DocumentParser {

    /**
     * コンテンツをパースし、構造化されたドキュメントのリストを返す。
     *
     * <p>1つの入力コンテンツから複数のParsedDocumentが生成される場合がある。
     * 例えば、HTMLの各セクションやJavaの各メソッドがそれぞれ独立したParsedDocumentになる。</p>
     *
     * @param content パース対象の生コンテンツ
     * @param sourceUrl データソースのURL又はファイルパス
     * @return パース結果のリスト（空の場合もある）
     * @throws IllegalArgumentException contentがnull/空の場合
     */
    List<ParsedDocument> parse(String content, String sourceUrl);
}

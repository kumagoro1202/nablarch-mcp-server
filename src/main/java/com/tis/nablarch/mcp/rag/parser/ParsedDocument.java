package com.tis.nablarch.mcp.rag.parser;

import com.tis.nablarch.mcp.rag.chunking.ContentType;

import java.util.Map;

/**
 * パース結果ドキュメント。
 *
 * <p>各パーサーが生のコンテンツを構造化した結果を保持する。
 * 1つの入力ソースから複数のParsedDocumentが生成される場合がある
 * （例: HTMLの各セクション、Javaの各メソッド）。</p>
 *
 * @param content パース済みテキストコンテンツ
 * @param metadata メタデータ（source, title, fqcn等）
 * @param sourceUrl データソースのURL又はファイルパス
 * @param contentType コンテンツタイプ
 */
public record ParsedDocument(
        String content,
        Map<String, String> metadata,
        String sourceUrl,
        ContentType contentType
) {

    /**
     * ParsedDocumentを生成する。
     *
     * @param content パース済みテキストコンテンツ
     * @param metadata メタデータ
     * @param sourceUrl データソースのURL又はファイルパス
     * @param contentType コンテンツタイプ
     * @throws IllegalArgumentException content又はsourceUrlがnull/空の場合
     */
    public ParsedDocument {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrlは必須です");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentTypeは必須です");
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}

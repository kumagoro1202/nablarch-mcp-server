package com.tis.nablarch.mcp.rag.chunking;

import java.util.Map;

/**
 * チャンク結果DTO。
 *
 * <p>ChunkingServiceが生成するチャンクの情報を保持する。
 * 各チャンクはEmbedding対象のテキストコンテンツ、
 * フィルタリング用メタデータ、位置情報を含む。</p>
 *
 * @param content チャンクのテキストコンテンツ
 * @param metadata メタデータ（source, content_type, fqcn等）
 * @param chunkIndex チャンクのインデックス（0始まり）
 * @param totalChunks 元ドキュメントの総チャンク数
 * @param contentType コンテンツタイプ
 */
public record DocumentChunkDto(
        String content,
        Map<String, String> metadata,
        int chunkIndex,
        int totalChunks,
        ContentType contentType
) {

    /**
     * DocumentChunkDtoを生成する。
     *
     * @param content チャンクのテキストコンテンツ
     * @param metadata メタデータ
     * @param chunkIndex チャンクインデックス
     * @param totalChunks 総チャンク数
     * @param contentType コンテンツタイプ
     * @throws IllegalArgumentException contentがnull/空の場合
     */
    public DocumentChunkDto {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentTypeは必須です");
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}

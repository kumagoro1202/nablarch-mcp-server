package com.tis.nablarch.mcp.rag.search;

import java.util.Map;
import java.util.Objects;

/**
 * 検索結果を表すDTO。
 *
 * <p>BM25検索、ベクトル検索、ハイブリッド検索の各検索サービスが
 * 共通して返却する検索結果レコード。</p>
 *
 * @param id チャンクの一意識別子
 * @param content チャンクのテキスト内容
 * @param score 検索スコア（0.0〜1.0、高いほど関連度が高い）
 * @param metadata チャンクのメタデータ（app_type, module, source等）
 * @param sourceUrl 元ドキュメントのURL
 */
public record SearchResult(
        String id,
        String content,
        double score,
        Map<String, String> metadata,
        String sourceUrl
) {
    /**
     * コンパクトコンストラクタ。idとcontentの非null制約を検証する。
     */
    public SearchResult {
        Objects.requireNonNull(id, "idはnullであってはならない");
        Objects.requireNonNull(content, "contentはnullであってはならない");
        if (metadata != null) {
            metadata = Map.copyOf(metadata);
        }
    }
}

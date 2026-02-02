package com.tis.nablarch.mcp.rag.ingestion;

import java.util.List;

/**
 * ドキュメント取込結果DTO。
 *
 * <p>取込パイプラインの実行結果を保持する。
 * 処理件数、成功件数、エラー件数、個別エラーメッセージを含む。</p>
 *
 * @param processedCount 処理対象ドキュメント数
 * @param successCount 正常に取込が完了したドキュメント数
 * @param errorCount エラーが発生したドキュメント数
 * @param errors 個別エラーメッセージのリスト
 */
public record IngestionResult(
        int processedCount,
        int successCount,
        int errorCount,
        List<String> errors
) {

    /**
     * IngestionResultを生成する。
     *
     * @param processedCount 処理対象ドキュメント数
     * @param successCount 成功数
     * @param errorCount エラー数
     * @param errors エラーメッセージリスト
     */
    public IngestionResult {
        if (errors == null) {
            errors = List.of();
        }
    }

    /**
     * 全件成功の結果を生成する。
     *
     * @param count 処理件数（= 成功件数）
     * @return 全件成功のIngestionResult
     */
    public static IngestionResult allSuccess(int count) {
        return new IngestionResult(count, count, 0, List.of());
    }

    /**
     * 全件成功の結果を生成する。
     *
     * @param count 処理・成功件数
     * @return 全件成功の取込結果
     */
    public static IngestionResult success(int count) {
        return new IngestionResult(count, count, 0, List.of());
    }

    /**
     * 空結果（処理対象なし）を生成する。
     *
     * @return 空の取込結果
     */
    public static IngestionResult empty() {
        return new IngestionResult(0, 0, 0, List.of());
    }
}

package com.tis.nablarch.mcp.rag.ingestion;

import java.util.List;

/**
 * ドキュメント取り込み結果。
 *
 * <p>取り込みパイプラインの実行結果を集約する。
 * 処理件数、成功件数、エラー件数、エラー詳細を保持する。</p>
 *
 * @param processedCount 処理対象の総ドキュメント数
 * @param successCount 正常に取り込めたドキュメント数
 * @param errorCount エラーが発生したドキュメント数
 * @param errors エラー詳細のリスト
 */
public record IngestionResult(
        int processedCount,
        int successCount,
        int errorCount,
        List<IngestionError> errors
) {

    /**
     * IngestionResultを生成する。
     *
     * @param processedCount 処理対象の総ドキュメント数
     * @param successCount 正常に取り込めたドキュメント数
     * @param errorCount エラーが発生したドキュメント数
     * @param errors エラー詳細のリスト
     */
    public IngestionResult {
        if (errors == null) {
            errors = List.of();
        }
    }

    /**
     * 全件成功の結果を生成する。
     *
     * @param count 処理・成功件数
     * @return 全件成功の取り込み結果
     */
    public static IngestionResult success(int count) {
        return new IngestionResult(count, count, 0, List.of());
    }

    /**
     * 空結果（処理対象なし）を生成する。
     *
     * @return 空の取り込み結果
     */
    public static IngestionResult empty() {
        return new IngestionResult(0, 0, 0, List.of());
    }

    /**
     * 個別ドキュメントの取り込みエラー情報。
     *
     * @param url エラーが発生したドキュメントのURL
     * @param message エラーメッセージ
     * @param cause 原因例外（任意）
     */
    public record IngestionError(
            String url,
            String message,
            Throwable cause
    ) {
        /**
         * IngestionErrorを生成する。
         *
         * @param url エラーが発生したドキュメントのURL
         * @param message エラーメッセージ
         * @param cause 原因例外
         */
        public IngestionError {
            if (url == null || url.isBlank()) {
                url = "unknown";
            }
            if (message == null || message.isBlank()) {
                message = "unknown error";
            }
        }

        /**
         * 原因例外なしのエラーを生成する。
         *
         * @param url エラーが発生したドキュメントのURL
         * @param message エラーメッセージ
         */
        public IngestionError(String url, String message) {
            this(url, message, null);
        }
    }
}

package com.tis.nablarch.mcp.tools;

/**
 * MCPツール統一エラーレスポンスビルダー。
 *
 * <p>全ツールクラスで統一されたエラーレスポンスフォーマットを生成する。
 * エラーコード・メッセージ・詳細情報を含むMarkdown形式のレスポンスを構築する。</p>
 *
 * <p>使用例:</p>
 * <pre>{@code
 * // バリデーションエラー
 * return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
 *     .message("検索キーワードを指定してください")
 *     .build();
 *
 * // 詳細情報付きエラー
 * return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
 *     .message("不明なアプリケーションタイプ")
 *     .detail("指定値: " + appType)
 *     .hint("有効な値: web, rest, batch, messaging")
 *     .build();
 *
 * // 内部エラー（RuntimeExceptionとして送出）
 * throw ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_004)
 *     .message("検索中にエラーが発生しました")
 *     .hint("search_apiツールをお試しください")
 *     .toException();
 * }</pre>
 */
public final class ErrorResponseBuilder {

    private final ErrorCode errorCode;
    private String message;
    private String detail;
    private String hint;

    private ErrorResponseBuilder(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * 指定エラーコードでビルダーを生成する。
     *
     * @param errorCode エラーコード
     * @return ビルダーインスタンス
     */
    public static ErrorResponseBuilder of(ErrorCode errorCode) {
        return new ErrorResponseBuilder(errorCode);
    }

    /**
     * エラーメッセージを設定する。
     *
     * @param message エラーメッセージ
     * @return このビルダー
     */
    public ErrorResponseBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * 詳細情報を設定する。
     *
     * @param detail 詳細情報
     * @return このビルダー
     */
    public ErrorResponseBuilder detail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
     * ヒント（解決策の提案）を設定する。
     *
     * @param hint ヒント
     * @return このビルダー
     */
    public ErrorResponseBuilder hint(String hint) {
        this.hint = hint;
        return this;
    }

    /**
     * エラーレスポンス文字列を構築する。
     *
     * <p>フォーマット:</p>
     * <pre>
     * [MCP_TOOL_002] パラメータ不正: 検索キーワードを指定してください
     * 詳細: ...
     * ヒント: ...
     * </pre>
     *
     * @return フォーマット済みエラーレスポンス
     */
    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode.getCode()).append("] ");
        sb.append(errorCode.getDefaultMessage());

        if (message != null && !message.isEmpty()) {
            sb.append(": ").append(message);
        }

        if (detail != null && !detail.isEmpty()) {
            sb.append("\n詳細: ").append(detail);
        }

        if (hint != null && !hint.isEmpty()) {
            sb.append("\nヒント: ").append(hint);
        }

        return sb.toString();
    }

    /**
     * エラーレスポンスをRuntimeExceptionとして構築する。
     *
     * <p>Spring AI MCP SDKはToolメソッドからのRuntimeExceptionを
     * isError:trueのエラーレスポンスとして返す。
     * 外部サービスエラーや内部エラーにはこのメソッドを使用する。</p>
     *
     * @return 構築済みのRuntimeException
     */
    public RuntimeException toException() {
        return new RuntimeException(build());
    }

    /**
     * エラーレスポンスをRuntimeExceptionとして構築する（原因例外付き）。
     *
     * @param cause 原因例外
     * @return 構築済みのRuntimeException
     */
    public RuntimeException toException(Throwable cause) {
        return new RuntimeException(build(), cause);
    }
}

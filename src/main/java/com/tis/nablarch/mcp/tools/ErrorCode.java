package com.tis.nablarch.mcp.tools;

/**
 * MCPツールエラーコード体系。
 *
 * <p>全ツールで統一されたエラーコードを定義する。</p>
 *
 * <ul>
 *   <li>MCP_TOOL_001: 検索結果なし</li>
 *   <li>MCP_TOOL_002: パラメータ不正</li>
 *   <li>MCP_TOOL_003: 外部サービスエラー</li>
 *   <li>MCP_TOOL_004: 内部エラー</li>
 * </ul>
 */
public enum ErrorCode {

    /** 検索結果なし。 */
    MCP_TOOL_001("MCP_TOOL_001", "検索結果なし"),

    /** パラメータ不正。 */
    MCP_TOOL_002("MCP_TOOL_002", "パラメータ不正"),

    /** 外部サービスエラー。 */
    MCP_TOOL_003("MCP_TOOL_003", "外部サービスエラー"),

    /** 内部エラー。 */
    MCP_TOOL_004("MCP_TOOL_004", "内部エラー");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * エラーコード文字列を返す。
     *
     * @return エラーコード
     */
    public String getCode() {
        return code;
    }

    /**
     * デフォルトメッセージを返す。
     *
     * @return デフォルトメッセージ
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}

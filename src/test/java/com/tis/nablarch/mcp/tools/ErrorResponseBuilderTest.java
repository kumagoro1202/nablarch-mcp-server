package com.tis.nablarch.mcp.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ErrorResponseBuilder}のテスト。
 */
class ErrorResponseBuilderTest {

    @Test
    void メッセージのみのエラーレスポンスが正しいフォーマットで生成される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                .message("検索キーワードを指定してください")
                .build();

        assertTrue(result.startsWith("[MCP_TOOL_002]"));
        assertTrue(result.contains("パラメータ不正"));
        assertTrue(result.contains("検索キーワードを指定してください"));
    }

    @Test
    void 詳細とヒント付きのエラーレスポンスが正しいフォーマットで生成される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                .message("不明なアプリケーションタイプ: xyz")
                .detail("指定値: xyz")
                .hint("有効な値: web, rest, batch, messaging")
                .build();

        assertTrue(result.startsWith("[MCP_TOOL_002]"));
        assertTrue(result.contains("パラメータ不正"));
        assertTrue(result.contains("不明なアプリケーションタイプ: xyz"));
        assertTrue(result.contains("詳細: 指定値: xyz"));
        assertTrue(result.contains("ヒント: 有効な値: web, rest, batch, messaging"));
    }

    @Test
    void 内部エラーのレスポンスが正しいフォーマットで生成される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_004)
                .message("検索中にエラーが発生しました")
                .hint("search_apiツールをお試しください")
                .build();

        assertTrue(result.startsWith("[MCP_TOOL_004]"));
        assertTrue(result.contains("内部エラー"));
        assertTrue(result.contains("検索中にエラーが発生しました"));
        assertTrue(result.contains("ヒント: search_apiツールをお試しください"));
    }

    @Test
    void 検索結果なしのレスポンスが正しいフォーマットで生成される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_001)
                .message("該当するパターンが見つかりませんでした")
                .build();

        assertTrue(result.startsWith("[MCP_TOOL_001]"));
        assertTrue(result.contains("検索結果なし"));
    }

    @Test
    void 外部サービスエラーのレスポンスが正しいフォーマットで生成される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_003)
                .message("Embeddingサービスに接続できませんでした")
                .build();

        assertTrue(result.startsWith("[MCP_TOOL_003]"));
        assertTrue(result.contains("外部サービスエラー"));
    }

    @Test
    void toExceptionでRuntimeExceptionが生成される() {
        RuntimeException ex = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_004)
                .message("内部エラーが発生しました")
                .toException();

        assertNotNull(ex);
        assertTrue(ex.getMessage().startsWith("[MCP_TOOL_004]"));
        assertTrue(ex.getMessage().contains("内部エラーが発生しました"));
        assertNull(ex.getCause());
    }

    @Test
    void toExceptionで原因例外付きRuntimeExceptionが生成される() {
        IllegalArgumentException cause = new IllegalArgumentException("原因");
        RuntimeException ex = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_004)
                .message("内部エラーが発生しました")
                .toException(cause);

        assertNotNull(ex);
        assertTrue(ex.getMessage().startsWith("[MCP_TOOL_004]"));
        assertSame(cause, ex.getCause());
    }

    @Test
    void メッセージなしの場合はデフォルトメッセージのみ表示される() {
        String result = ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                .build();

        assertEquals("[MCP_TOOL_002] パラメータ不正", result);
    }

    @Test
    void 全エラーコードのgetCodeとgetDefaultMessageが正しい() {
        assertEquals("MCP_TOOL_001", ErrorCode.MCP_TOOL_001.getCode());
        assertEquals("検索結果なし", ErrorCode.MCP_TOOL_001.getDefaultMessage());

        assertEquals("MCP_TOOL_002", ErrorCode.MCP_TOOL_002.getCode());
        assertEquals("パラメータ不正", ErrorCode.MCP_TOOL_002.getDefaultMessage());

        assertEquals("MCP_TOOL_003", ErrorCode.MCP_TOOL_003.getCode());
        assertEquals("外部サービスエラー", ErrorCode.MCP_TOOL_003.getDefaultMessage());

        assertEquals("MCP_TOOL_004", ErrorCode.MCP_TOOL_004.getCode());
        assertEquals("内部エラー", ErrorCode.MCP_TOOL_004.getDefaultMessage());
    }
}

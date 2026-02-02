package com.tis.nablarch.mcp.rag.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IngestionResult のテスト。
 */
class IngestionResultTest {

    @Test
    @DisplayName("正常な取り込み結果を生成できる")
    void createNormalResult() {
        IngestionResult result = new IngestionResult(10, 8, 2, List.of(
                new IngestionResult.IngestionError("http://example.com/a", "parse error"),
                new IngestionResult.IngestionError("http://example.com/b", "timeout")
        ));

        assertEquals(10, result.processedCount());
        assertEquals(8, result.successCount());
        assertEquals(2, result.errorCount());
        assertEquals(2, result.errors().size());
    }

    @Test
    @DisplayName("success() で全件成功の結果を生成できる")
    void successFactory() {
        IngestionResult result = IngestionResult.success(15);

        assertEquals(15, result.processedCount());
        assertEquals(15, result.successCount());
        assertEquals(0, result.errorCount());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("empty() で空結果を生成できる")
    void emptyFactory() {
        IngestionResult result = IngestionResult.empty();

        assertEquals(0, result.processedCount());
        assertEquals(0, result.successCount());
        assertEquals(0, result.errorCount());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("errorsがnullの場合、空リストに正規化される")
    void nullErrorsNormalized() {
        IngestionResult result = new IngestionResult(5, 5, 0, null);

        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("IngestionError のURLがnull/空の場合、unknownに正規化される")
    void errorNullUrl() {
        IngestionResult.IngestionError error1 = new IngestionResult.IngestionError(null, "test");
        assertEquals("unknown", error1.url());

        IngestionResult.IngestionError error2 = new IngestionResult.IngestionError("", "test");
        assertEquals("unknown", error2.url());

        IngestionResult.IngestionError error3 = new IngestionResult.IngestionError("  ", "test");
        assertEquals("unknown", error3.url());
    }

    @Test
    @DisplayName("IngestionError のメッセージがnull/空の場合、unknown errorに正規化される")
    void errorNullMessage() {
        IngestionResult.IngestionError error1 = new IngestionResult.IngestionError("http://x", null);
        assertEquals("unknown error", error1.message());

        IngestionResult.IngestionError error2 = new IngestionResult.IngestionError("http://x", "");
        assertEquals("unknown error", error2.message());
    }

    @Test
    @DisplayName("IngestionError に原因例外を含められる")
    void errorWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        IngestionResult.IngestionError error = new IngestionResult.IngestionError(
                "http://example.com", "failed", cause);

        assertEquals("http://example.com", error.url());
        assertEquals("failed", error.message());
        assertSame(cause, error.cause());
    }

    @Test
    @DisplayName("IngestionError の原因例外なしコンストラクタ")
    void errorWithoutCause() {
        IngestionResult.IngestionError error = new IngestionResult.IngestionError(
                "http://example.com", "failed");

        assertNull(error.cause());
    }
}

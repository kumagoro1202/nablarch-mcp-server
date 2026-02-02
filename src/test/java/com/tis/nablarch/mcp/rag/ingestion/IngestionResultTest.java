package com.tis.nablarch.mcp.rag.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IngestionResult} のユニットテスト。
 */
class IngestionResultTest {

    @Test
    @DisplayName("正常に全フィールドが保持される")
    void allFieldsPreserved() {
        List<String> errors = List.of("error1", "error2");
        IngestionResult result = new IngestionResult(10, 8, 2, errors);

        assertAll(
                () -> assertEquals(10, result.processedCount()),
                () -> assertEquals(8, result.successCount()),
                () -> assertEquals(2, result.errorCount()),
                () -> assertEquals(2, result.errors().size())
        );
    }

    @Test
    @DisplayName("errorsがnullの場合は空リストに置換される")
    void nullErrorsReplacedWithEmptyList() {
        IngestionResult result = new IngestionResult(5, 5, 0, null);
        assertNotNull(result.errors());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("allSuccessファクトリメソッド")
    void allSuccessFactory() {
        IngestionResult result = IngestionResult.allSuccess(10);

        assertAll(
                () -> assertEquals(10, result.processedCount()),
                () -> assertEquals(10, result.successCount()),
                () -> assertEquals(0, result.errorCount()),
                () -> assertTrue(result.errors().isEmpty())
        );
    }

    @Test
    @DisplayName("successファクトリメソッド")
    void successFactory() {
        IngestionResult result = IngestionResult.success(15);

        assertEquals(15, result.processedCount());
        assertEquals(15, result.successCount());
        assertEquals(0, result.errorCount());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("emptyファクトリメソッド")
    void emptyFactory() {
        IngestionResult result = IngestionResult.empty();

        assertEquals(0, result.processedCount());
        assertEquals(0, result.successCount());
        assertEquals(0, result.errorCount());
        assertTrue(result.errors().isEmpty());
    }
}

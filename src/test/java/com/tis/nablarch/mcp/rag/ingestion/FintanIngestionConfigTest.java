package com.tis.nablarch.mcp.rag.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link FintanIngestionConfig} のユニットテスト。
 *
 * <p>設定値のデフォルト値とセッター/ゲッターの動作を検証する。</p>
 */
class FintanIngestionConfigTest {

    @Test
    @DisplayName("デフォルト値が正しく設定されている")
    void defaultValues() {
        FintanIngestionConfig config = new FintanIngestionConfig();

        assertAll(
                () -> assertEquals("https://fintan.jp/", config.getBaseUrl()),
                () -> assertEquals(List.of("Nablarch"), config.getSearchTags()),
                () -> assertEquals(10, config.getBatchSize()),
                () -> assertEquals(1000, config.getDelayMs()),
                () -> assertEquals(3, config.getMaxRetries()),
                () -> assertTrue(config.isEnabled())
        );
    }

    @Test
    @DisplayName("各プロパティのセッターが正しく動作する")
    void settersWork() {
        FintanIngestionConfig config = new FintanIngestionConfig();

        config.setBaseUrl("https://custom.fintan.jp/");
        config.setSearchTags(List.of("Nablarch", "Java"));
        config.setBatchSize(20);
        config.setDelayMs(2000);
        config.setMaxRetries(5);
        config.setEnabled(false);

        assertAll(
                () -> assertEquals("https://custom.fintan.jp/", config.getBaseUrl()),
                () -> assertEquals(List.of("Nablarch", "Java"), config.getSearchTags()),
                () -> assertEquals(20, config.getBatchSize()),
                () -> assertEquals(2000, config.getDelayMs()),
                () -> assertEquals(5, config.getMaxRetries()),
                () -> assertFalse(config.isEnabled())
        );
    }
}

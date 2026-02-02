package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GuideResourceProvider} のユニットテスト。
 */
class GuideResourceProviderTest {

    private GuideResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new GuideResourceProvider();
        provider.init();
    }

    /**
     * 全6トピックで正常にMarkdownが返却されることを検証する。
     *
     * @param topic ガイドトピック
     */
    @ParameterizedTest
    @ValueSource(strings = {"setup", "testing", "validation", "database", "handler-queue", "error-handling"})
    void getGuideMarkdown_validTopic_returnsMarkdown(String topic) {
        String md = provider.getGuideMarkdown(topic);
        assertNotNull(md);
        assertTrue(md.startsWith("# Nablarch "));
        assertTrue(md.contains("## Overview"));
    }

    /**
     * setupガイドにconfigテンプレート情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_setup_containsConfigTemplates() {
        String md = provider.getGuideMarkdown("setup");
        assertTrue(md.contains("Configuration Templates"));
        assertTrue(md.contains("web-xml"));
        assertTrue(md.contains("config-templates.yaml"));
    }

    /**
     * testingガイドにテストパターン情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_testing_containsTestPatterns() {
        String md = provider.getGuideMarkdown("testing");
        assertTrue(md.contains("Test Patterns"));
        assertTrue(md.contains("request-unit-test"));
        assertTrue(md.contains("api-patterns.yaml"));
    }

    /**
     * validationガイドにバリデーションパターン情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_validation_containsValidationPatterns() {
        String md = provider.getGuideMarkdown("validation");
        assertTrue(md.contains("form-validation"));
        assertTrue(md.contains("design-patterns.yaml"));
    }

    /**
     * databaseガイドにDAO関連パターンと設定情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_database_containsDaoPatterns() {
        String md = provider.getGuideMarkdown("database");
        assertTrue(md.contains("universal-dao"));
        assertTrue(md.contains("sql-file"));
        assertTrue(md.contains("db-connection"));
    }

    /**
     * handler-queueガイドにアプリタイプサマリと制約情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_handlerQueue_containsSummary() {
        String md = provider.getGuideMarkdown("handler-queue");
        assertTrue(md.contains("Application Types"));
        assertTrue(md.contains("web"));
        assertTrue(md.contains("rest"));
        assertTrue(md.contains("batch"));
        assertTrue(md.contains("Key Ordering Constraints"));
        assertTrue(md.contains("handler-catalog.yaml"));
    }

    /**
     * error-handlingガイドにエラーカタログ情報が含まれることを検証する。
     */
    @Test
    void getGuideMarkdown_errorHandling_containsErrors() {
        String md = provider.getGuideMarkdown("error-handling");
        assertTrue(md.contains("ERR-001"));
        assertTrue(md.contains("Severity"));
        assertTrue(md.contains("Solution"));
        assertTrue(md.contains("error-catalog.yaml"));
    }

    /**
     * 存在しないトピックでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getGuideMarkdown_unknownTopic_returnsErrorMessage() {
        String md = provider.getGuideMarkdown("unknown");
        assertNotNull(md);
        assertTrue(md.contains("Unknown guide topic: unknown"));
    }

    /**
     * nullトピックでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getGuideMarkdown_nullTopic_returnsErrorMessage() {
        String md = provider.getGuideMarkdown(null);
        assertNotNull(md);
        assertTrue(md.contains("Unknown Guide Topic"));
    }

    /**
     * 各ガイドにソース帰属情報が含まれることを検証する。
     *
     * @param topic ガイドトピック
     */
    @ParameterizedTest
    @ValueSource(strings = {"setup", "testing", "validation", "database", "handler-queue", "error-handling"})
    void getGuideMarkdown_validTopic_containsSourceAttribution(String topic) {
        String md = provider.getGuideMarkdown(topic);
        assertTrue(md.contains("Source:"));
    }
}

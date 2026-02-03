package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ConfigResourceProvider} のユニットテスト。
 *
 * <p>config/{name} リソースプロバイダのテスト。
 * XML設定テンプレートの一覧・詳細取得を検証する。</p>
 */
class ConfigResourceProviderTest {

    private ConfigResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new ConfigResourceProvider();
        provider.init();
    }

    // ==================== getTemplateList テスト ====================

    /**
     * テンプレート一覧がMarkdown形式で返却されることを検証する。
     */
    @Test
    void getTemplateList_returnsMarkdown() {
        String result = provider.getTemplateList();
        assertNotNull(result);
        assertTrue(result.startsWith("# Nablarch XML設定テンプレート一覧"));
    }

    /**
     * テンプレート一覧がテーブル形式であることを検証する。
     */
    @Test
    void getTemplateList_containsTable() {
        String result = provider.getTemplateList();
        assertTrue(result.contains("| # | テンプレート名 | カテゴリ | 説明 |"));
        assertTrue(result.contains("|---|--------------|---------|------|"));
    }

    /**
     * テンプレート一覧にソース帰属情報が含まれることを検証する。
     */
    @Test
    void getTemplateList_containsSourceAttribution() {
        String result = provider.getTemplateList();
        assertTrue(result.contains("Source: config-templates.yaml"));
    }

    /**
     * 代表的なテンプレートがリストに含まれることを検証する。
     */
    @Test
    void getTemplateList_containsWebXml() {
        String result = provider.getTemplateList();
        assertTrue(result.contains("web-xml"),
            "テンプレート一覧に web-xml が含まれること");
    }

    // ==================== getTemplate テスト ====================

    /**
     * 有効なテンプレート名でXML設定が取得できることを検証する。
     */
    @Test
    void getTemplate_validName_returnsXml() {
        String result = provider.getTemplate("web-xml");
        assertNotNull(result);
        assertTrue(result.contains("<!--"));
        assertTrue(result.contains("Nablarch Configuration Template: web-xml"));
        assertTrue(result.contains("Category:"));
    }

    /**
     * web-xmlテンプレートにXML設定内容が含まれることを検証する。
     */
    @Test
    void getTemplate_webXml_containsXmlContent() {
        String result = provider.getTemplate("web-xml");
        assertTrue(result.contains("<?xml version"));
        assertTrue(result.contains("<web-app"));
        assertTrue(result.contains("NablarchServletContextListener"));
    }

    /**
     * テンプレートにフィルタ設定が含まれることを検証する。
     */
    @Test
    void getTemplate_webXml_containsFilter() {
        String result = provider.getTemplate("web-xml");
        assertTrue(result.contains("<filter>"));
        assertTrue(result.contains("RepositoryBasedWebFrontController"));
    }

    /**
     * 存在しないテンプレートでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getTemplate_unknownName_returnsError() {
        String result = provider.getTemplate("unknown-template");
        assertNotNull(result);
        assertTrue(result.contains("Unknown config template: unknown-template"));
        assertTrue(result.contains("Valid templates:"));
    }

    /**
     * nullテンプレート名でエラーメッセージが返却されることを検証する。
     */
    @Test
    void getTemplate_nullName_returnsError() {
        String result = provider.getTemplate(null);
        assertNotNull(result);
        assertTrue(result.contains("Unknown config template: null") ||
                   result.contains("Unknown Config Template"));
    }

    // ==================== getValidTemplateNames テスト ====================

    /**
     * 有効なテンプレート名一覧が空でないことを検証する。
     */
    @Test
    void getValidTemplateNames_returnsNonEmpty() {
        Set<String> names = provider.getValidTemplateNames();
        assertNotNull(names);
        assertFalse(names.isEmpty(), "テンプレート名一覧は空でないこと");
    }

    /**
     * web-xmlテンプレートが含まれることを検証する。
     */
    @Test
    void getValidTemplateNames_containsWebXml() {
        Set<String> names = provider.getValidTemplateNames();
        assertTrue(names.contains("web-xml"),
            "テンプレート名一覧に web-xml が含まれること");
    }
}

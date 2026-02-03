package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AntipatternResourceProvider} のユニットテスト。
 *
 * <p>antipattern/{name} リソースプロバイダのテスト。
 * アンチパターンカタログの一覧・詳細取得を検証する。</p>
 */
class AntipatternResourceProviderTest {

    private AntipatternResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new AntipatternResourceProvider();
        provider.init();
    }

    // ==================== getAntipatternList テスト ====================

    /**
     * アンチパターン一覧がMarkdown形式で返却されることを検証する。
     */
    @Test
    void getAntipatternList_returnsMarkdown() {
        String result = provider.getAntipatternList();
        assertNotNull(result);
        assertTrue(result.startsWith("# Nablarch アンチパターンカタログ"));
    }

    /**
     * アンチパターン一覧がテーブル形式であることを検証する。
     */
    @Test
    void getAntipatternList_containsTable() {
        String result = provider.getAntipatternList();
        assertTrue(result.contains("| # | 名前 | カテゴリ | 重要度 | 説明 |"));
        assertTrue(result.contains("|---|------|---------|--------|------|"));
    }

    /**
     * アンチパターン一覧にソース帰属情報が含まれることを検証する。
     */
    @Test
    void getAntipatternList_containsSourceAttribution() {
        String result = provider.getAntipatternList();
        assertTrue(result.contains("Source: antipattern-catalog.yaml"));
    }

    /**
     * 代表的なアンチパターンがリストに含まれることを検証する。
     */
    @ParameterizedTest
    @ValueSource(strings = {"action-instance-field", "direct-sql-execution", "missing-transaction-handler"})
    void getAntipatternList_containsKnownAntipatterns(String antipatternName) {
        String result = provider.getAntipatternList();
        assertTrue(result.contains(antipatternName),
            "アンチパターン一覧に " + antipatternName + " が含まれること");
    }

    // ==================== getAntipatternDetail テスト ====================

    /**
     * 有効なアンチパターン名で詳細が取得できることを検証する。
     */
    @Test
    void getAntipatternDetail_validName_returnsDetail() {
        String result = provider.getAntipatternDetail("action-instance-field");
        assertNotNull(result);
        assertTrue(result.contains("**名前**: action-instance-field"));
        assertTrue(result.contains("**カテゴリ**: thread-safety"));
        assertTrue(result.contains("**重要度**: critical"));
    }

    /**
     * アンチパターン詳細に概要が含まれることを検証する。
     */
    @Test
    void getAntipatternDetail_containsDescription() {
        String result = provider.getAntipatternDetail("action-instance-field");
        assertTrue(result.contains("## 概要"));
    }

    /**
     * アンチパターン詳細に悪い例・良い例が含まれることを検証する。
     */
    @Test
    void getAntipatternDetail_containsExamples() {
        String result = provider.getAntipatternDetail("action-instance-field");
        assertTrue(result.contains("## 悪い例") || result.contains("## 良い例"),
            "アンチパターン詳細にコード例が含まれること");
    }

    /**
     * アンチパターン詳細にソース帰属情報が含まれることを検証する。
     */
    @Test
    void getAntipatternDetail_containsSourceAttribution() {
        String result = provider.getAntipatternDetail("action-instance-field");
        assertTrue(result.contains("Source: antipattern-catalog.yaml"));
    }

    /**
     * 全7種のアンチパターン詳細が取得できることを検証する。
     *
     * @param name アンチパターン名
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "action-instance-field",
        "direct-sql-execution",
        "missing-transaction-handler",
        "handler-order-violation",
        "form-entity-confusion",
        "session-overuse",
        "exception-swallowing"
    })
    void getAntipatternDetail_allAntipatterns_returnsDetail(String name) {
        String result = provider.getAntipatternDetail(name);
        assertNotNull(result);
        assertTrue(result.contains("**名前**: " + name));
    }

    /**
     * 存在しないアンチパターンでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getAntipatternDetail_unknownName_returnsError() {
        String result = provider.getAntipatternDetail("unknown-antipattern");
        assertNotNull(result);
        assertTrue(result.contains("Unknown antipattern: unknown-antipattern"));
        assertTrue(result.contains("Valid antipatterns:"));
    }

    /**
     * nullアンチパターン名でエラーメッセージが返却されることを検証する。
     */
    @Test
    void getAntipatternDetail_nullName_returnsError() {
        String result = provider.getAntipatternDetail(null);
        assertNotNull(result);
        assertTrue(result.contains("Unknown antipattern: null") ||
                   result.contains("Unknown Antipattern"));
    }

    // ==================== getValidAntipatternNames テスト ====================

    /**
     * 有効なアンチパターン名一覧が空でないことを検証する。
     */
    @Test
    void getValidAntipatternNames_returnsNonEmpty() {
        Set<String> names = provider.getValidAntipatternNames();
        assertNotNull(names);
        assertFalse(names.isEmpty(), "アンチパターン名一覧は空でないこと");
    }

    /**
     * 7種類のアンチパターンが含まれることを検証する。
     */
    @Test
    void getValidAntipatternNames_containsSevenAntipatterns() {
        Set<String> names = provider.getValidAntipatternNames();
        assertEquals(7, names.size(), "アンチパターンは7種類であること");
        assertTrue(names.contains("action-instance-field"));
        assertTrue(names.contains("direct-sql-execution"));
        assertTrue(names.contains("missing-transaction-handler"));
        assertTrue(names.contains("handler-order-violation"));
        assertTrue(names.contains("form-entity-confusion"));
        assertTrue(names.contains("session-overuse"));
        assertTrue(names.contains("exception-swallowing"));
    }
}

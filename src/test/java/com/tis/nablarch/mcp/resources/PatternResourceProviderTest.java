package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PatternResourceProvider} のユニットテスト。
 *
 * <p>pattern/{name} リソースプロバイダのテスト。
 * 設計パターンカタログの一覧・詳細取得を検証する。</p>
 */
class PatternResourceProviderTest {

    private PatternResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new PatternResourceProvider();
        provider.init();
    }

    // ==================== getPatternList テスト ====================

    /**
     * パターン一覧がMarkdown形式で返却されることを検証する。
     */
    @Test
    void getPatternList_returnsMarkdown() {
        String result = provider.getPatternList();
        assertNotNull(result);
        assertTrue(result.startsWith("# Nablarch デザインパターンカタログ"));
    }

    /**
     * パターン一覧がテーブル形式であることを検証する。
     */
    @Test
    void getPatternList_containsTable() {
        String result = provider.getPatternList();
        assertTrue(result.contains("| # | パターン名 | カテゴリ | 説明 |"));
        assertTrue(result.contains("|---|-----------|---------|------|"));
    }

    /**
     * パターン一覧にソース帰属情報が含まれることを検証する。
     */
    @Test
    void getPatternList_containsSourceAttribution() {
        String result = provider.getPatternList();
        assertTrue(result.contains("Source: design-patterns.yaml"));
    }

    /**
     * 代表的なパターンがリストに含まれることを検証する。
     */
    @ParameterizedTest
    @ValueSource(strings = {"handler-queue-pattern", "action-class-pattern"})
    void getPatternList_containsKnownPatterns(String patternName) {
        String result = provider.getPatternList();
        assertTrue(result.contains(patternName),
            "パターン一覧に " + patternName + " が含まれること");
    }

    // ==================== getPatternDetail テスト ====================

    /**
     * 有効なパターン名で詳細が取得できることを検証する。
     */
    @Test
    void getPatternDetail_validPattern_returnsDetail() {
        String result = provider.getPatternDetail("handler-queue-pattern");
        assertNotNull(result);
        assertTrue(result.contains("# handler-queue-pattern"));
        assertTrue(result.contains("**カテゴリ**"));
        assertTrue(result.contains("## 概要"));
    }

    /**
     * パターン詳細に問題・解決策が含まれることを検証する。
     */
    @Test
    void getPatternDetail_containsProblemSolution() {
        String result = provider.getPatternDetail("handler-queue-pattern");
        assertTrue(result.contains("## 問題") || result.contains("## 解決策"),
            "パターン詳細に問題または解決策が含まれること");
    }

    /**
     * パターン詳細にソース帰属情報が含まれることを検証する。
     */
    @Test
    void getPatternDetail_containsSourceAttribution() {
        String result = provider.getPatternDetail("handler-queue-pattern");
        assertTrue(result.contains("Source: design-patterns.yaml"));
    }

    /**
     * 存在しないパターンでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getPatternDetail_unknownPattern_returnsError() {
        String result = provider.getPatternDetail("unknown-pattern");
        assertNotNull(result);
        assertTrue(result.contains("Unknown pattern: unknown-pattern"));
        assertTrue(result.contains("Valid patterns:"));
    }

    /**
     * nullパターンでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getPatternDetail_nullPattern_returnsError() {
        String result = provider.getPatternDetail(null);
        assertNotNull(result);
        assertTrue(result.contains("Unknown pattern: null") ||
                   result.contains("Unknown Pattern"));
    }

    // ==================== getValidPatternNames テスト ====================

    /**
     * 有効なパターン名一覧が空でないことを検証する。
     */
    @Test
    void getValidPatternNames_returnsNonEmpty() {
        Set<String> names = provider.getValidPatternNames();
        assertNotNull(names);
        assertFalse(names.isEmpty(), "パターン名一覧は空でないこと");
    }

    /**
     * 代表的なパターン名が含まれることを検証する。
     */
    @Test
    void getValidPatternNames_containsKnownPatterns() {
        Set<String> names = provider.getValidPatternNames();
        assertTrue(names.contains("handler-queue-pattern"),
            "handler-queue-patternが含まれること");
    }
}

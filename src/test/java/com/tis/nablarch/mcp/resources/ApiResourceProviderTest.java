package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ApiResourceProvider} のユニットテスト。
 *
 * <p>api/{module}/{class} リソースプロバイダのテスト。
 * モジュール一覧、クラス一覧、クラス詳細の取得を検証する。</p>
 */
class ApiResourceProviderTest {

    private ApiResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new ApiResourceProvider();
        provider.init();
    }

    // ==================== getModuleList テスト ====================

    /**
     * モジュール一覧が正常に取得できることを検証する。
     */
    @Test
    void getModuleList_returnsValidJson() {
        String result = provider.getModuleList();
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"module_list\""));
        assertTrue(result.contains("\"modules\""));
        assertTrue(result.contains("\"total_modules\""));
    }

    /**
     * モジュール一覧にコアモジュールが含まれることを検証する。
     */
    @Test
    void getModuleList_containsCoreModules() {
        String result = provider.getModuleList();
        assertTrue(result.contains("\"module_key\""));
        assertTrue(result.contains("\"category\""));
        assertTrue(result.contains("\"uri\""));
        assertTrue(result.contains("nablarch://api/"));
    }

    // ==================== getClassList テスト ====================

    /**
     * 有効なモジュールキーでクラス一覧が取得できることを検証する。
     */
    @Test
    void getClassList_validModule_returnsClassList() {
        Set<String> validKeys = provider.getValidModuleKeys();
        assertFalse(validKeys.isEmpty(), "有効なモジュールキーが存在すること");

        String moduleKey = validKeys.iterator().next();
        String result = provider.getClassList(moduleKey);
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"class_list\""));
        assertTrue(result.contains("\"module_key\" : \"" + moduleKey + "\""));
    }

    /**
     * クラス一覧にFQCN情報が含まれることを検証する。
     */
    @Test
    void getClassList_containsFqcnInfo() {
        Set<String> validKeys = provider.getValidModuleKeys();
        if (validKeys.isEmpty()) return;

        String moduleKey = validKeys.iterator().next();
        String result = provider.getClassList(moduleKey);
        assertTrue(result.contains("\"classes\""));
        assertTrue(result.contains("\"total_classes\""));
    }

    /**
     * 存在しないモジュールでエラーが返却されることを検証する。
     */
    @Test
    void getClassList_unknownModule_returnsError() {
        String result = provider.getClassList("unknown-module");
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Unknown module: unknown-module"));
        assertTrue(result.contains("valid_modules"));
    }

    /**
     * nullモジュールでエラーが返却されることを検証する。
     */
    @Test
    void getClassList_nullModule_returnsError() {
        String result = provider.getClassList(null);
        assertNotNull(result);
        assertTrue(result.contains("error"));
    }

    // ==================== getClassDetail テスト ====================

    /**
     * 存在しないクラスでエラーが返却されることを検証する。
     */
    @Test
    void getClassDetail_unknownClass_returnsError() {
        Set<String> validKeys = provider.getValidModuleKeys();
        if (validKeys.isEmpty()) return;

        String moduleKey = validKeys.iterator().next();
        String result = provider.getClassDetail(moduleKey, "UnknownClassName");
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Class not found: UnknownClassName"));
    }

    /**
     * 存在しないモジュールでエラーが返却されることを検証する。
     */
    @Test
    void getClassDetail_unknownModule_returnsError() {
        String result = provider.getClassDetail("unknown-module", "SomeClass");
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Unknown module"));
    }

    // ==================== getValidModuleKeys テスト ====================

    /**
     * 有効なモジュールキー一覧が空でないことを検証する。
     */
    @Test
    void getValidModuleKeys_returnsNonEmpty() {
        Set<String> keys = provider.getValidModuleKeys();
        assertNotNull(keys);
        assertFalse(keys.isEmpty(), "モジュールキー一覧は空でないこと");
    }

    /**
     * モジュールキーがnablarch-プレフィックスなしの形式であることを検証する。
     */
    @Test
    void getValidModuleKeys_withoutPrefix() {
        Set<String> keys = provider.getValidModuleKeys();
        for (String key : keys) {
            assertFalse(key.startsWith("nablarch-"),
                "モジュールキーはnablarch-プレフィックスなしであること: " + key);
        }
    }
}

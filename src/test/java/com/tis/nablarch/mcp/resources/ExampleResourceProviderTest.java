package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ExampleResourceProvider} のユニットテスト。
 *
 * <p>example/{type} リソースプロバイダのテスト。
 * サンプルコードカタログの一覧・詳細取得を検証する。</p>
 */
class ExampleResourceProviderTest {

    private ExampleResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new ExampleResourceProvider();
        provider.init();
    }

    // ==================== getExampleList テスト ====================

    /**
     * サンプル一覧が正常にJSON形式で返却されることを検証する。
     */
    @Test
    void getExampleList_returnsValidJson() {
        String result = provider.getExampleList();
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"example_list\""));
        assertTrue(result.contains("\"examples\""));
        assertTrue(result.contains("\"total_examples\""));
    }

    /**
     * サンプル一覧に必要な情報が含まれることを検証する。
     */
    @Test
    void getExampleList_containsRequiredFields() {
        String result = provider.getExampleList();
        assertTrue(result.contains("\"type\""));
        assertTrue(result.contains("\"description\""));
        assertTrue(result.contains("\"app_type\""));
        assertTrue(result.contains("\"file_count\""));
    }

    /**
     * サンプル一覧に4種類のサンプルが含まれることを検証する。
     */
    @ParameterizedTest
    @ValueSource(strings = {"rest-api", "web-crud", "batch-db", "messaging-mom"})
    void getExampleList_containsKnownTypes(String exampleType) {
        String result = provider.getExampleList();
        assertTrue(result.contains("\"" + exampleType + "\""),
            "サンプル一覧に " + exampleType + " が含まれること");
    }

    // ==================== getExampleDetail テスト ====================

    /**
     * 有効なタイプで詳細が取得できることを検証する。
     */
    @Test
    void getExampleDetail_validType_returnsDetail() {
        String result = provider.getExampleDetail("rest-api");
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"example_detail\""));
        assertTrue(result.contains("\"example_type\" : \"rest-api\""));
    }

    /**
     * サンプル詳細にファイル情報が含まれることを検証する。
     */
    @Test
    void getExampleDetail_containsFiles() {
        String result = provider.getExampleDetail("rest-api");
        assertTrue(result.contains("\"files\""));
        assertTrue(result.contains("\"key_patterns\""));
        assertTrue(result.contains("\"reference_repo\""));
    }

    /**
     * 各サンプルタイプの詳細が取得できることを検証する。
     *
     * @param type サンプルタイプ
     */
    @ParameterizedTest
    @ValueSource(strings = {"rest-api", "web-crud", "batch-db", "messaging-mom"})
    void getExampleDetail_allTypes_returnsDetail(String type) {
        String result = provider.getExampleDetail(type);
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"example_detail\""));
        assertTrue(result.contains("\"example_type\" : \"" + type + "\""));
    }

    /**
     * 存在しないタイプでエラーが返却されることを検証する。
     */
    @Test
    void getExampleDetail_unknownType_returnsError() {
        String result = provider.getExampleDetail("unknown-type");
        assertNotNull(result);
        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("Unknown example type: unknown-type"));
        assertTrue(result.contains("\"valid_types\""));
    }

    /**
     * nullタイプでエラーが返却されることを検証する。
     */
    @Test
    void getExampleDetail_nullType_returnsError() {
        String result = provider.getExampleDetail(null);
        assertNotNull(result);
        assertTrue(result.contains("error"));
    }

    // ==================== getValidExampleTypes テスト ====================

    /**
     * 有効なサンプルタイプ一覧が空でないことを検証する。
     */
    @Test
    void getValidExampleTypes_returnsNonEmpty() {
        Set<String> types = provider.getValidExampleTypes();
        assertNotNull(types);
        assertFalse(types.isEmpty(), "サンプルタイプ一覧は空でないこと");
    }

    /**
     * 4種類のサンプルタイプが含まれることを検証する。
     */
    @Test
    void getValidExampleTypes_containsAllFourTypes() {
        Set<String> types = provider.getValidExampleTypes();
        assertEquals(4, types.size(), "サンプルタイプは4種類であること");
        assertTrue(types.contains("rest-api"));
        assertTrue(types.contains("web-crud"));
        assertTrue(types.contains("batch-db"));
        assertTrue(types.contains("messaging-mom"));
    }
}

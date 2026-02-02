package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HandlerResourceProvider} のユニットテスト。
 */
class HandlerResourceProviderTest {

    private HandlerResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new HandlerResourceProvider();
        provider.init();
    }

    /**
     * 全6アプリケーションタイプで正常にMarkdownが返却されることを検証する。
     *
     * @param appType アプリケーションタイプ
     */
    @ParameterizedTest
    @ValueSource(strings = {"web", "rest", "batch", "messaging", "http-messaging", "jakarta-batch"})
    void getHandlerMarkdown_validAppType_returnsMarkdown(String appType) {
        String md = provider.getHandlerMarkdown(appType);
        assertNotNull(md);
        assertTrue(md.startsWith("# Nablarch "));
        assertTrue(md.contains("## Handler Queue (in order)"));
        assertTrue(md.contains("## Ordering Constraints Summary"));
    }

    /**
     * WebアプリケーションのレスポンスにハンドラのFQCNが含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_containsFqcn() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("nablarch.fw.web.handler.HttpCharacterEncodingHandler"));
        assertTrue(md.contains("nablarch.fw.handler.GlobalErrorHandler"));
        assertTrue(md.contains("FQCN"));
    }

    /**
     * RESTアプリケーションのレスポンスにFQCNが含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_rest_containsFqcn() {
        String md = provider.getHandlerMarkdown("rest");
        assertTrue(md.contains("nablarch.fw.jaxrs.JaxRsResponseHandler"));
        assertTrue(md.contains("FQCN"));
    }

    /**
     * レスポンスに制約情報が含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_containsConstraints() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("Constraints"));
        assertTrue(md.contains("Must be before") || md.contains("Must be after"));
    }

    /**
     * レスポンスにRequired/Optionalの区別が含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_containsRequiredOptional() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("[Required]"));
        assertTrue(md.contains("[Optional]"));
    }

    /**
     * レスポンスにThreadの情報が含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_containsThread() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("Thread"));
        assertTrue(md.contains("main"));
    }

    /**
     * 制約サマリテーブルにGlobalErrorHandlerの制約が含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_constraintsSummaryContainsGlobalErrorHandler() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("| GlobalErrorHandler |"));
    }

    /**
     * 存在しないアプリケーションタイプでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getHandlerMarkdown_unknownType_returnsErrorMessage() {
        String md = provider.getHandlerMarkdown("unknown");
        assertNotNull(md);
        assertTrue(md.contains("Unknown application type: unknown"));
    }

    /**
     * nullアプリケーションタイプでエラーメッセージが返却されることを検証する。
     */
    @Test
    void getHandlerMarkdown_nullType_returnsErrorMessage() {
        String md = provider.getHandlerMarkdown(null);
        assertNotNull(md);
        assertTrue(md.contains("Unknown application type"));
    }

    /**
     * ソース帰属情報がレスポンスに含まれることを検証する。
     */
    @Test
    void getHandlerMarkdown_web_containsSourceAttribution() {
        String md = provider.getHandlerMarkdown("web");
        assertTrue(md.contains("handler-catalog.yaml"));
        assertTrue(md.contains("handler-constraints.yaml"));
    }
}

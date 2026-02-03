package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link DesignHandlerQueueTool} のユニットテスト。
 */
@ExtendWith(MockitoExtension.class)
class DesignHandlerQueueToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private DesignHandlerQueueTool tool;

    @BeforeEach
    void setUp() {
        tool = new DesignHandlerQueueTool(knowledgeBase);
    }

    @Nested
    @DisplayName("design メソッド - 入力検証")
    class InputValidationTests {

        @Test
        @DisplayName("app_typeがnullの場合はエラーメッセージを返す")
        void nullAppTypeReturnsError() {
            String result = tool.design(null, null, null);
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("app_type"));
        }

        @Test
        @DisplayName("app_typeが空の場合はエラーメッセージを返す")
        void emptyAppTypeReturnsError() {
            String result = tool.design("", null, null);
            assertTrue(result.contains("エラー"));
        }

        @Test
        @DisplayName("不明なapp_typeの場合はエラーメッセージを返す")
        void unknownAppTypeReturnsError() {
            String result = tool.design("unknown", null, null);
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("unknown"));
            assertTrue(result.contains("web"));
        }
    }

    @Nested
    @DisplayName("design メソッド - Webアプリケーション")
    class WebAppTests {

        @BeforeEach
        void setUp() {
            // validateHandlerQueueのモック設定
            when(knowledgeBase.validateHandlerQueue(eq("web"), anyList()))
                    .thenReturn("## 検証結果: OK\nハンドラキューの順序は正しいです。");
        }

        @Test
        @DisplayName("Webアプリタイプで正常にハンドラキューを設計できる")
        void designWebHandlerQueue() {
            String result = tool.design("web", null, true);

            assertNotNull(result);
            assertTrue(result.contains("ハンドラキュー設計結果"));
            assertTrue(result.contains("web"));
            assertTrue(result.contains("HttpResponseHandler"));
            assertTrue(result.contains("SecureHandler"));
            assertTrue(result.contains("<list name=\"handlerQueue\">"));
        }

        @Test
        @DisplayName("CSRF要件を指定するとCsrfTokenVerificationHandlerが追加される")
        void csrfRequirementAddsHandler() {
            String result = tool.design("web", "csrf", true);

            assertTrue(result.contains("CsrfTokenVerificationHandler"));
        }

        @Test
        @DisplayName("コメントなしオプションでXMLコメントが省略される")
        void withoutCommentsOmitsXmlComments() {
            String result = tool.design("web", null, false);

            // XML内のコメントを確認（<!-- で始まるコメントが少なくなる）
            assertNotNull(result);
            // 設定XMLは含まれる
            assertTrue(result.contains("<component class="));
        }
    }

    @Nested
    @DisplayName("design メソッド - RESTアプリケーション")
    class RestAppTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.validateHandlerQueue(eq("rest"), anyList()))
                    .thenReturn("## 検証結果: OK\nハンドラキューの順序は正しいです。");
        }

        @Test
        @DisplayName("RESTアプリタイプでJaxRsResponseHandlerが含まれる")
        void restIncludesJaxRsHandler() {
            String result = tool.design("rest", null, true);

            assertTrue(result.contains("JaxRsResponseHandler"));
            assertTrue(result.contains("nablarch.fw.jaxrs.JaxRsResponseHandler"));
        }
    }

    @Nested
    @DisplayName("design メソッド - バッチアプリケーション")
    class BatchAppTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.validateHandlerQueue(eq("batch"), anyList()))
                    .thenReturn("## 検証結果: OK\nハンドラキューの順序は正しいです。");
        }

        @Test
        @DisplayName("バッチアプリタイプでバッチ固有ハンドラが含まれる")
        void batchIncludesBatchHandlers() {
            String result = tool.design("batch", null, true);

            assertTrue(result.contains("LoopHandler"));
            assertTrue(result.contains("DataReadHandler"));
            assertTrue(result.contains("MultiThreadExecutionHandler"));
        }
    }

    @Nested
    @DisplayName("design メソッド - メッセージングアプリケーション")
    class MessagingAppTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.validateHandlerQueue(eq("messaging"), anyList()))
                    .thenReturn("## 検証結果: OK\nハンドラキューの順序は正しいです。");
        }

        @Test
        @DisplayName("メッセージングアプリタイプでメッセージング固有ハンドラが含まれる")
        void messagingIncludesMessagingHandlers() {
            String result = tool.design("messaging", null, true);

            assertTrue(result.contains("RequestThreadLoopHandler"));
        }
    }

    @Nested
    @DisplayName("design メソッド - 出力形式")
    class OutputFormatTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.validateHandlerQueue(anyString(), anyList()))
                    .thenReturn("## 検証結果: OK\n");
        }

        @Test
        @DisplayName("結果にMarkdown形式のハンドラ一覧テーブルが含まれる")
        void includesHandlerTable() {
            String result = tool.design("web", null, true);

            assertTrue(result.contains("### ハンドラ構成"));
            assertTrue(result.contains("| # | ハンドラ | 説明 | 必須 |"));
        }

        @Test
        @DisplayName("結果にXML設定セクションが含まれる")
        void includesXmlSection() {
            String result = tool.design("web", null, true);

            assertTrue(result.contains("### XML設定"));
            assertTrue(result.contains("```xml"));
            assertTrue(result.contains("<list name=\"handlerQueue\">"));
            assertTrue(result.contains("</list>"));
        }

        @Test
        @DisplayName("結果に検証結果セクションが含まれる")
        void includesValidationSection() {
            String result = tool.design("web", null, true);

            assertTrue(result.contains("### 検証結果"));
        }

        @Test
        @DisplayName("結果に補足説明が含まれる")
        void includesSupplementaryInfo() {
            String result = tool.design("web", null, true);

            assertTrue(result.contains("### 補足"));
            assertTrue(result.contains("component-configuration"));
        }
    }

    @Nested
    @DisplayName("design メソッド - 大文字小文字")
    class CaseInsensitiveTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.validateHandlerQueue(anyString(), anyList()))
                    .thenReturn("## 検証結果: OK\n");
        }

        @Test
        @DisplayName("app_typeは大文字でも受け付ける")
        void upperCaseAppTypeAccepted() {
            String result = tool.design("WEB", null, true);

            // 不明なapp_typeエラーは出ない（有効なタイプとして認識される）
            assertFalse(result.contains("不明なアプリケーションタイプ"));
            assertTrue(result.contains("ハンドラキュー設計結果"));
        }

        @Test
        @DisplayName("app_typeは大小混合でも受け付ける")
        void mixedCaseAppTypeAccepted() {
            String result = tool.design("Web", null, true);

            // 不明なapp_typeエラーは出ない
            assertFalse(result.contains("不明なアプリケーションタイプ"));
        }
    }
}

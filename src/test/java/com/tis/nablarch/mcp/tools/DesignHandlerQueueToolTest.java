package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

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

    /** テスト用のWebハンドラリストを生成する。 */
    private static List<HandlerEntry> webHandlers() {
        return List.of(
                handler("HttpCharacterEncodingHandler", "nablarch.fw.web.handler.HttpCharacterEncodingHandler",
                        "文字エンコーディング設定", 1, true),
                handler("GlobalErrorHandler", "nablarch.fw.handler.GlobalErrorHandler",
                        "グローバルエラー処理", 2, true),
                handler("HttpResponseHandler", "nablarch.fw.web.handler.HttpResponseHandler",
                        "HTTPレスポンス処理", 3, true),
                handler("SecureHandler", "nablarch.fw.web.handler.SecureHandler",
                        "セキュリティヘッダー付与", 4, true),
                handler("SessionStoreHandler", "nablarch.common.web.session.SessionStoreHandler",
                        "セッションストア管理", 5, true),
                handler("DbConnectionManagementHandler", "nablarch.common.handler.DbConnectionManagementHandler",
                        "DBコネクション管理", 6, true),
                handler("DispatchHandler", "nablarch.fw.handler.DispatchHandler",
                        "アクションディスパッチ", 7, true)
        );
    }

    /** テスト用のRESTハンドラリストを生成する。 */
    private static List<HandlerEntry> restHandlers() {
        return List.of(
                handler("GlobalErrorHandler", "nablarch.fw.handler.GlobalErrorHandler",
                        "グローバルエラー処理", 1, true),
                handler("HttpResponseHandler", "nablarch.fw.web.handler.HttpResponseHandler",
                        "HTTPレスポンス処理", 2, true),
                handler("SecureHandler", "nablarch.fw.web.handler.SecureHandler",
                        "セキュリティヘッダー付与", 3, true),
                handler("DbConnectionManagementHandler", "nablarch.common.handler.DbConnectionManagementHandler",
                        "DBコネクション管理", 4, true),
                handler("JaxRsResponseHandler", "nablarch.fw.jaxrs.JaxRsResponseHandler",
                        "JAX-RSレスポンス処理", 5, true),
                handler("DispatchHandler", "nablarch.fw.handler.DispatchHandler",
                        "アクションディスパッチ", 6, true)
        );
    }

    /** テスト用のバッチハンドラリストを生成する。 */
    private static List<HandlerEntry> batchHandlers() {
        return List.of(
                handler("GlobalErrorHandler", "nablarch.fw.handler.GlobalErrorHandler",
                        "グローバルエラー処理", 1, true),
                handler("DbConnectionManagementHandler", "nablarch.common.handler.DbConnectionManagementHandler",
                        "DBコネクション管理", 2, true),
                handler("LoopHandler", "nablarch.fw.handler.LoopHandler",
                        "ループ制御", 3, true),
                handler("DataReadHandler", "nablarch.fw.handler.DataReadHandler",
                        "データ読み込み", 4, true),
                handler("MultiThreadExecutionHandler", "nablarch.fw.handler.MultiThreadExecutionHandler",
                        "マルチスレッド実行", 5, true),
                handler("DispatchHandler", "nablarch.fw.handler.DispatchHandler",
                        "アクションディスパッチ", 6, true)
        );
    }

    /** テスト用のメッセージングハンドラリストを生成する。 */
    private static List<HandlerEntry> messagingHandlers() {
        return List.of(
                handler("GlobalErrorHandler", "nablarch.fw.handler.GlobalErrorHandler",
                        "グローバルエラー処理", 1, true),
                handler("DbConnectionManagementHandler", "nablarch.common.handler.DbConnectionManagementHandler",
                        "DBコネクション管理", 2, true),
                handler("RequestThreadLoopHandler", "nablarch.fw.messaging.handler.RequestThreadLoopHandler",
                        "リクエストスレッドループ", 3, true),
                handler("DispatchHandler", "nablarch.fw.handler.DispatchHandler",
                        "アクションディスパッチ", 4, true)
        );
    }

    private static HandlerEntry handler(String name, String fqcn, String desc, int order, boolean required) {
        HandlerEntry h = new HandlerEntry();
        h.name = name;
        h.fqcn = fqcn;
        h.description = desc;
        h.order = order;
        h.required = required;
        return h;
    }

    @Nested
    @DisplayName("design メソッド - 入力検証")
    class InputValidationTests {

        @Test
        @DisplayName("app_typeがnullの場合はエラーメッセージを返す")
        void nullAppTypeReturnsError() {
            when(knowledgeBase.getAvailableAppTypes()).thenReturn(Set.of("web", "rest", "batch", "messaging"));
            String result = tool.design(null, null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
            assertTrue(result.contains("app_type"));
        }

        @Test
        @DisplayName("app_typeが空の場合はエラーメッセージを返す")
        void emptyAppTypeReturnsError() {
            when(knowledgeBase.getAvailableAppTypes()).thenReturn(Set.of("web", "rest", "batch", "messaging"));
            String result = tool.design("", null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
        }

        @Test
        @DisplayName("不明なapp_typeの場合はエラーメッセージを返す")
        void unknownAppTypeReturnsError() {
            when(knowledgeBase.getHandlerEntries("unknown")).thenReturn(List.of());
            when(knowledgeBase.getAvailableAppTypes()).thenReturn(Set.of("web", "rest", "batch", "messaging"));
            String result = tool.design("unknown", null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
            assertTrue(result.contains("unknown"));
        }
    }

    @Nested
    @DisplayName("design メソッド - Webアプリケーション")
    class WebAppTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.getHandlerEntries("web")).thenReturn(webHandlers());
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

            assertNotNull(result);
            assertTrue(result.contains("<component class="));
        }
    }

    @Nested
    @DisplayName("design メソッド - RESTアプリケーション")
    class RestAppTests {

        @BeforeEach
        void setUp() {
            when(knowledgeBase.getHandlerEntries("rest")).thenReturn(restHandlers());
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
            when(knowledgeBase.getHandlerEntries("batch")).thenReturn(batchHandlers());
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
            when(knowledgeBase.getHandlerEntries("messaging")).thenReturn(messagingHandlers());
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
            when(knowledgeBase.getHandlerEntries("web")).thenReturn(webHandlers());
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
            when(knowledgeBase.getHandlerEntries("web")).thenReturn(webHandlers());
            when(knowledgeBase.validateHandlerQueue(anyString(), anyList()))
                    .thenReturn("## 検証結果: OK\n");
        }

        @Test
        @DisplayName("app_typeは大文字でも受け付ける")
        void upperCaseAppTypeAccepted() {
            String result = tool.design("WEB", null, true);

            assertFalse(result.contains("不明なアプリケーションタイプ"));
            assertTrue(result.contains("ハンドラキュー設計結果"));
        }

        @Test
        @DisplayName("app_typeは大小混合でも受け付ける")
        void mixedCaseAppTypeAccepted() {
            String result = tool.design("Web", null, true);

            assertFalse(result.contains("不明なアプリケーションタイプ"));
        }
    }
}

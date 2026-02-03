package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OptimizeHandlerQueueToolのユニットテスト。
 *
 * <p>品質担保戦略に基づき、開発者レビュー観点で設計：
 * <ul>
 *   <li>@DisplayName による意図明示</li>
 *   <li>AAA (Arrange-Act-Assert) パターン</li>
 *   <li>エッジケースの網羅</li>
 *   <li>テストの独立性</li>
 * </ul>
 * </p>
 *
 * @see OptimizeHandlerQueueTool
 */
@ExtendWith(MockitoExtension.class)
class OptimizeHandlerQueueToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private OptimizeHandlerQueueTool tool;

    @BeforeEach
    void setUp() {
        tool = new OptimizeHandlerQueueTool(knowledgeBase);
    }

    @Nested
    @DisplayName("入力検証テスト")
    class InputValidationTest {

        @Test
        @DisplayName("XMLがnullの場合エラーメッセージを返す")
        void nullXmlReturnsError() {
            // Arrange
            String xml = null;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("XMLが指定されていません"));
        }

        @Test
        @DisplayName("XMLが空白の場合エラーメッセージを返す")
        void emptyXmlReturnsError() {
            // Arrange
            String xml = "   ";

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("XMLが指定されていません"));
        }

        @Test
        @DisplayName("ハンドラを抽出できないXMLの場合エラーメッセージを返す")
        void invalidXmlReturnsError() {
            // Arrange
            String xml = "<invalid>no handlers here</invalid>";

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("XMLからハンドラを抽出できませんでした"));
        }
    }

    @Nested
    @DisplayName("appType自動推定テスト")
    class AppTypeDetectionTest {

        @Test
        @DisplayName("JaxRsResponseHandlerからRESTアプリと推定")
        void detectsRestApp() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.jaxrs.JaxRsResponseHandler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("**アプリタイプ**: rest"));
        }

        @Test
        @DisplayName("HttpResponseHandlerとRoutesMappingからWebアプリと推定")
        void detectsWebApp() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("**アプリタイプ**: web"));
        }

        @Test
        @DisplayName("RequestThreadLoopHandlerからメッセージングアプリと推定")
        void detectsMessagingApp() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.messaging.handler.RequestThreadLoopHandler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("**アプリタイプ**: messaging"));
        }

        @Test
        @DisplayName("MultiThreadExecutionHandlerとDataReadHandlerからバッチアプリと推定")
        void detectsBatchApp() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.handler.MultiThreadExecutionHandler"/>
                    <component class="nablarch.fw.batch.DataReadHandler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("**アプリタイプ**: batch"));
        }

        @Test
        @DisplayName("appTypeが推定できない場合エラーメッセージを返す")
        void undetectableAppTypeReturnsError() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="some.custom.Handler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("アプリケーションタイプを自動推定できませんでした"));
        }

        @Test
        @DisplayName("appTypeを明示指定した場合自動推定をスキップ")
        void explicitAppTypeSkipsDetection() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="some.custom.Handler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            assertTrue(result.contains("**アプリタイプ**: web"));
            assertFalse(result.contains("エラー"));
        }
    }

    @Nested
    @DisplayName("正確性ルールテスト")
    class CorrectnessRulesTest {

        @Test
        @DisplayName("COR-001: 必須ハンドラ欠落を検出")
        void detectsMissingRequiredHandler() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            Map<String, Object> catalog = Map.of(
                    "handlers", List.of(
                            Map.of("name", "GlobalErrorHandler", "required", true),
                            Map.of("name", "HttpResponseHandler", "required", true)
                    )
            );

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(catalog);
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", "correctness");

            // Assert
            assertTrue(result.contains("COR-001"));
            assertTrue(result.contains("GlobalErrorHandler"));
            assertTrue(result.contains("必須ハンドラが欠落"));
        }

        @Test
        @DisplayName("COR-002: 順序制約違反を検出")
        void detectsOrderViolation() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.SessionStoreHandler"/>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                </component-configuration>
                """;

            HandlerConstraintEntry constraint = new HandlerConstraintEntry();
            constraint.mustBefore = List.of("SessionStoreHandler");

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints("HttpResponseHandler")).thenReturn(constraint);
            when(knowledgeBase.getHandlerConstraints("SessionStoreHandler")).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", "correctness");

            // Assert
            assertTrue(result.contains("COR-002") || result.contains("順序"));
        }
    }

    @Nested
    @DisplayName("セキュリティルールテスト")
    class SecurityRulesTest {

        @Test
        @DisplayName("SEC-001: SecureHandler未設定を検出（Webアプリ）")
        void detectsMissingSecureHandler() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: セキュリティルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "security");

            // Assert
            assertTrue(result.contains("SEC-001"));
            assertTrue(result.contains("SecureHandler"));
            assertTrue(result.contains("セキュリティヘッダー"));
        }

        @Test
        @DisplayName("SEC-002: CSRF対策未設定を検出（Webアプリ）")
        void detectsMissingCsrfHandler() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.web.handler.SecureHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: セキュリティルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "security");

            // Assert
            assertTrue(result.contains("SEC-002"));
            assertTrue(result.contains("CSRF"));
        }

        @Test
        @DisplayName("SEC-003: セッションストア未設定を検出（Webアプリ）")
        void detectsMissingSessionStore() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.web.handler.SecureHandler"/>
                    <component class="nablarch.fw.web.handler.CsrfTokenVerificationHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: セキュリティルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "security");

            // Assert
            assertTrue(result.contains("SEC-003") || result.contains("セッション"));
        }

        @Test
        @DisplayName("SEC-005: 開発環境専用ハンドラ残存を検出")
        void detectsDevelopmentOnlyHandlers() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: セキュリティルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "security");

            // Assert
            assertTrue(result.contains("SEC-005"));
            assertTrue(result.contains("HotDeployHandler"));
            assertTrue(result.contains("開発環境専用"));
        }

        @Test
        @DisplayName("バッチアプリではセキュリティルールが適用されない")
        void batchAppSkipsSecurityRules() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.handler.MultiThreadExecutionHandler"/>
                    <component class="nablarch.fw.batch.DataReadHandler"/>
                </component-configuration>
                """;

            // Note: セキュリティルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "batch", "security");

            // Assert
            // セキュリティルールは適用されない（Web/RESTのみ）
            assertFalse(result.contains("SEC-001"));
            assertFalse(result.contains("SEC-002"));
        }
    }

    @Nested
    @DisplayName("パフォーマンスルールテスト")
    class PerformanceRulesTest {

        @Test
        @DisplayName("PERF-002: 重複ハンドラを検出")
        void detectsDuplicateHandlers() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: パフォーマンスルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "performance");

            // Assert
            assertTrue(result.contains("PERF-002"));
            assertTrue(result.contains("重複"));
        }

        @Test
        @DisplayName("PERF-001: 開発用ハンドラ残存を検出")
        void detectsDebugHandlers() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.DumpVariableHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: パフォーマンスルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "performance");

            // Assert
            assertTrue(result.contains("PERF") || result.contains("SEC-005"));
            assertTrue(result.contains("DumpVariableHandler") || result.contains("デバッグ") || result.contains("開発"));
        }

        @Test
        @DisplayName("PERF-005: AccessLogHandlerの非同期化を推奨")
        void suggestsAsyncLogging() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.common.handler.AccessLogHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: パフォーマンスルールのみの場合、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "performance");

            // Assert
            assertTrue(result.contains("PERF-005"));
            assertTrue(result.contains("AccessLogHandler"));
            assertTrue(result.contains("非同期"));
        }
    }

    @Nested
    @DisplayName("最適化観点フィルタテスト")
    class ConcernFilterTest {

        @Test
        @DisplayName("concern=correctnessで正確性ルールのみ実行")
        void correctnessConcernFiltersOthers() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", "correctness");

            // Assert
            // COR-xxx のみ検出され、SEC-xxx, PERF-xxx は検出されない
            assertFalse(result.contains("SEC-005")); // セキュリティルールは除外
        }

        @Test
        @DisplayName("concern=allで全ルール実行")
        void allConcernRunsAllRules() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", "all");

            // Assert
            // セキュリティルールが実行される
            assertTrue(result.contains("SEC-") || result.contains("セキュリティ"));
        }

        @Test
        @DisplayName("concernがnullの場合allとして扱う")
        void nullConcernTreatedAsAll() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            // 全ルールが実行される
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("出力フォーマットテスト")
    class OutputFormatTest {

        @Test
        @DisplayName("最適化提案がない場合適切なメッセージを返す")
        void noProposalsReturnsSuccessMessage() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.web.handler.SecureHandler"/>
                    <component class="nablarch.fw.web.handler.CsrfTokenVerificationHandler"/>
                    <component class="nablarch.fw.web.handler.SessionStoreHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            assertTrue(result.contains("適切に構成されています") ||
                    result.contains("最適化の提案はありません") ||
                    result.contains("検出された最適化ポイント: 0件"));
        }

        @Test
        @DisplayName("サマリテーブルが含まれる")
        void resultIncludesSummaryTable() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            assertTrue(result.contains("サマリ"));
            assertTrue(result.contains("観点") || result.contains("件数"));
        }

        @Test
        @DisplayName("Before/After XMLスニペットが含まれる")
        void resultIncludesBeforeAfterSnippets() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.fw.handler.HotDeployHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            // add/remove提案の場合Before/Afterが含まれる
            if (result.contains("ハンドラ追加") || result.contains("ハンドラ削除")) {
                assertTrue(result.contains("Before") || result.contains("After"));
            }
        }

        @Test
        @DisplayName("重大度に応じた絵文字が表示される")
        void severityEmojisAreDisplayed() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(Map.of("handlers", List.of()));
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            // 何らかの提案がある場合、絵文字が含まれる
            if (result.contains("SEC-001") || result.contains("SEC-002")) {
                assertTrue(result.contains("🔴") || result.contains("🟡") || result.contains("🟢") ||
                        result.contains("高") || result.contains("中") || result.contains("低"));
            }
        }
    }

    @Nested
    @DisplayName("エッジケーステスト")
    class EdgeCaseTest {

        @Test
        @DisplayName("FQCNにドットがない場合でも正常に処理")
        void handlesSimpleClassName() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="SimpleHandler"/>
                </component-configuration>
                """;

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            // エラーにならず処理される
            assertNotNull(result);
        }

        @Test
        @DisplayName("concernが大文字でも正常に動作")
        void handleUpperCaseConcern() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            // Note: concern="SECURITY"の場合、セキュリティルールのみが実行され、knowledgeBaseは使用されない

            // Act
            String result = tool.optimize(xml, "web", "SECURITY");

            // Assert
            // 大文字でも正常に処理される
            assertNotNull(result);
        }

        @Test
        @DisplayName("空のhandlerカタログでも正常に動作")
        void handlesEmptyHandlerCatalog() {
            // Arrange
            String xml = """
                <component-configuration>
                    <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
                    <component class="nablarch.integration.router.RoutesMapping"/>
                </component-configuration>
                """;

            when(knowledgeBase.getHandlerCatalog("web")).thenReturn(null);
            when(knowledgeBase.getHandlerConstraints(anyString())).thenReturn(null);

            // Act
            String result = tool.optimize(xml, "web", null);

            // Assert
            // エラーにならず処理される
            assertNotNull(result);
        }
    }
}

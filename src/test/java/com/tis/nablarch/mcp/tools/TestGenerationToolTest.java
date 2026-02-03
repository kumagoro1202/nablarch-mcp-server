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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link TestGenerationTool} の単体テスト。
 *
 * <p>Nablarch Testing Framework準拠のテストコード生成ツールをテストする。
 * テストタイプ別（unit, request-response, batch, messaging）の生成ロジック、
 * Excelテストデータ構造生成、RAGフォールバックをテスト対象とする。</p>
 *
 * <p>品質担保戦略（docs/quality-assurance-strategy.md）に準拠：
 * - @DisplayNameで意図明示
 * - AAA (Arrange-Act-Assert) パターン
 * - エッジケースの網羅</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestGenerationTool テスト")
class TestGenerationToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private TestGenerationTool tool;

    @BeforeEach
    void setUp() {
        tool = new TestGenerationTool(knowledgeBase);
    }

    // ========== 入力検証テスト ==========

    @Nested
    @DisplayName("入力検証テスト")
    class InputValidationTests {

        @Test
        @DisplayName("targetClassがnullの場合エラーメッセージを返す")
        void generateTest_targetClassNull_returnsErrorMessage() {
            // Arrange - 入力なし
            // Act
            String result = tool.generateTest(null, "unit", null, null, null, null);
            // Assert
            assertEquals("テスト対象クラスのFQCNを指定してください。", result);
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("targetClassが空白の場合エラーメッセージを返す")
        void generateTest_targetClassBlank_returnsErrorMessage() {
            // Arrange
            String targetClass = "   ";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertEquals("テスト対象クラスのFQCNを指定してください。", result);
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("testTypeがnullの場合エラーメッセージを返す")
        void generateTest_testTypeNull_returnsErrorMessage() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, null, null, null, null, null);
            // Assert
            assertEquals("テストタイプを指定してください（unit, request-response, batch, messaging）。", result);
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("testTypeが空白の場合エラーメッセージを返す")
        void generateTest_testTypeBlank_returnsErrorMessage() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "", null, null, null, null);
            // Assert
            assertEquals("テストタイプを指定してください（unit, request-response, batch, messaging）。", result);
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("testTypeが無効な場合エラーメッセージを返す")
        void generateTest_testTypeInvalid_returnsErrorMessage() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            String invalidTestType = "invalid-type";
            // Act
            String result = tool.generateTest(targetClass, invalidTestType, null, null, null, null);
            // Assert
            assertTrue(result.contains("不明なテストタイプ: invalid-type"));
            assertTrue(result.contains("有効なタイプ: unit, request-response, batch, messaging"));
            verifyNoInteractions(knowledgeBase);
        }
    }

    // ========== 正常系テスト ==========

    @Nested
    @DisplayName("正常系テスト - テストコード生成")
    class NormalGenerationTests {

        @BeforeEach
        void setUpMocks() {
            // 静的知識ベース検索のモック
            when(knowledgeBase.search(eq("test"), eq("testing")))
                    .thenReturn(List.of("テストパターン情報"));
        }

        @Test
        @DisplayName("unitタイプでテストコードを生成できる")
        void generateTest_unitType_generatesTestCode() {
            // Arrange
            String targetClass = "com.example.util.DateUtils";
            String testType = "unit";
            // Act
            String result = tool.generateTest(targetClass, testType, null, null, null, null);
            // Assert
            assertAll(
                    () -> assertTrue(result.contains("## テスト生成結果: DateUtils")),
                    () -> assertTrue(result.contains("テストタイプ: ユニットテスト")),
                    () -> assertTrue(result.contains("class DateUtilsTest")),
                    () -> assertTrue(result.contains("### テストケース一覧")),
                    () -> assertTrue(result.contains("### 適用されたNablarch規約"))
            );
        }

        @Test
        @DisplayName("request-responseタイプでテストコードを生成できる")
        void generateTest_requestResponseType_generatesTestCode() {
            // Arrange
            String targetClass = "com.example.action.UserRegistrationAction";
            String testType = "request-response";
            when(knowledgeBase.search(eq("SimpleDbAndHttpFwTestSupport"), eq(null)))
                    .thenReturn(List.of("リクエスト単体テスト情報"));
            // Act
            String result = tool.generateTest(targetClass, testType, null, null, null, null);
            // Assert
            assertAll(
                    () -> assertTrue(result.contains("## テスト生成結果: UserRegistrationAction")),
                    () -> assertTrue(result.contains("テストタイプ: リクエスト単体テスト")),
                    () -> assertTrue(result.contains("extends SimpleDbAndHttpFwTestSupport")),
                    () -> assertTrue(result.contains("Excelのシート名がテストメソッドに対応")),
                    () -> assertTrue(result.contains("期待ステータス"))
            );
        }

        @Test
        @DisplayName("batchタイプでテストコードを生成できる")
        void generateTest_batchType_generatesTestCode() {
            // Arrange
            String targetClass = "com.example.batch.CsvImportAction";
            String testType = "batch";
            when(knowledgeBase.search(eq("BatchRequestTestSupport"), eq(null)))
                    .thenReturn(List.of("バッチテスト情報"));
            // Act
            String result = tool.generateTest(targetClass, testType, null, null, null, null);
            // Assert
            assertAll(
                    () -> assertTrue(result.contains("## テスト生成結果: CsvImportAction")),
                    () -> assertTrue(result.contains("テストタイプ: バッチテスト")),
                    () -> assertTrue(result.contains("extends BatchRequestTestSupport")),
                    () -> assertTrue(result.contains("setupTable/expectedTableで入出力データを検証"))
            );
        }

        @Test
        @DisplayName("messagingタイプでテストコードを生成できる")
        void generateTest_messagingType_generatesTestCode() {
            // Arrange
            String targetClass = "com.example.messaging.OrderReceiveAction";
            String testType = "messaging";
            when(knowledgeBase.search(eq("MessagingRequestTestSupport"), eq(null)))
                    .thenReturn(List.of("メッセージングテスト情報"));
            // Act
            String result = tool.generateTest(targetClass, testType, null, null, null, null);
            // Assert
            assertAll(
                    () -> assertTrue(result.contains("## テスト生成結果: OrderReceiveAction")),
                    () -> assertTrue(result.contains("テストタイプ: メッセージングテスト")),
                    () -> assertTrue(result.contains("extends MessagingRequestTestSupport")),
                    () -> assertTrue(result.contains("requestMessage/expectedMessageシートでメッセージを定義"))
            );
        }
    }

    // ========== パラメータ解析テスト ==========

    @Nested
    @DisplayName("パラメータ解析テスト")
    class ParameterParsingTests {

        @BeforeEach
        void setUpMocks() {
            when(knowledgeBase.search(anyString(), anyString()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("format=nablarch-excelでExcel構造を含む")
        void generateTest_formatNablarchExcel_includesExcelStructure() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "request-response", "nablarch-excel", null, "true", null);
            // Assert
            assertTrue(result.contains("### Excelテストデータ構造"));
        }

        @Test
        @DisplayName("includeExcel=falseでExcel構造を含まない")
        void generateTest_includeExcelFalse_excludesExcelStructure() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, "false", null);
            // Assert
            assertFalse(result.contains("### Excelテストデータ構造"));
        }

        @Test
        @DisplayName("coverageTarget=minimalで最小限のテストケースを生成")
        void generateTest_coverageMinimal_generatesMinimalCases() {
            // Arrange
            String targetClass = "com.example.util.StringUtils";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, "minimal");
            // Assert
            assertTrue(result.contains("カバレッジ: minimal"));
        }

        @Test
        @DisplayName("coverageTarget=comprehensiveで包括的なテストケースを生成")
        void generateTest_coverageComprehensive_generatesComprehensiveCases() {
            // Arrange
            String targetClass = "com.example.util.StringUtils";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, "comprehensive");
            // Assert
            assertTrue(result.contains("カバレッジ: comprehensive"));
        }

        @Test
        @DisplayName("testCasesヒントでカスタムテストケースを生成")
        void generateTest_withTestCasesHint_generatesCustomCases() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            String testCases = "正常登録, バリデーションエラー, 重複エラー";
            // Act
            String result = tool.generateTest(targetClass, "request-response", null, testCases, null, null);
            // Assert
            assertTrue(result.contains("正常登録"));
        }
    }

    // ========== testType正規化テスト ==========

    @Nested
    @DisplayName("testType正規化テスト")
    class TestTypeNormalizationTests {

        @BeforeEach
        void setUpMocks() {
            when(knowledgeBase.search(anyString(), anyString()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("request_response（アンダースコア）も有効なtestTypeとして認識")
        void generateTest_testTypeWithUnderscore_isAccepted() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "request_response", null, null, null, null);
            // Assert
            assertTrue(result.contains("テストタイプ: リクエスト単体テスト"));
        }

        @Test
        @DisplayName("大文字小文字を区別しない")
        void generateTest_testTypeCaseInsensitive_isAccepted() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "BATCH", null, null, null, null);
            // Assert
            assertTrue(result.contains("テストタイプ: バッチテスト"));
        }
    }

    // ========== 出力フォーマットテスト ==========

    @Nested
    @DisplayName("出力フォーマットテスト")
    class OutputFormatTests {

        @BeforeEach
        void setUpMocks() {
            when(knowledgeBase.search(anyString(), anyString()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("出力にMarkdownフォーマットのコードブロックを含む")
        void generateTest_outputContainsCodeBlock() {
            // Arrange
            String targetClass = "com.example.util.DateUtils";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("```java"));
            assertTrue(result.contains("```"));
        }

        @Test
        @DisplayName("出力に参考ドキュメントリンクを含む")
        void generateTest_outputContainsDocumentationLinks() {
            // Arrange
            String targetClass = "com.example.util.DateUtils";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("[Nablarch テスティングフレームワーク]"));
            assertTrue(result.contains("https://nablarch.github.io"));
        }

        @Test
        @DisplayName("テストクラス名は対象クラス名+Testとなる")
        void generateTest_testClassNameFollowsConvention() {
            // Arrange
            String targetClass = "com.example.service.UserService";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("### テストクラス: UserServiceTest"));
            assertTrue(result.contains("class UserServiceTest"));
        }

        @Test
        @DisplayName("テストメソッド名は日本語で記述される")
        void generateTest_testMethodNamesInJapanese() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "request-response", null, null, null, null);
            // Assert
            // 日本語メソッド名が含まれることを確認
            assertTrue(result.matches("(?s).*void [\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]+.*"));
        }
    }

    // ========== エッジケーステスト ==========

    @Nested
    @DisplayName("エッジケーステスト")
    class EdgeCaseTests {

        @BeforeEach
        void setUpMocks() {
            when(knowledgeBase.search(anyString(), anyString()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("パッケージなしの単純クラス名でも動作する")
        void generateTest_simpleClassName_works() {
            // Arrange
            String targetClass = "UserAction";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("## テスト生成結果: UserAction"));
            assertTrue(result.contains("class UserActionTest"));
        }

        @Test
        @DisplayName("深いパッケージ階層のクラスでも動作する")
        void generateTest_deepPackage_works() {
            // Arrange
            String targetClass = "com.example.app.web.action.user.registration.UserRegistrationAction";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("## テスト生成結果: UserRegistrationAction"));
            assertTrue(result.contains("package com.example.app.web.action.user.registration"));
        }

        @Test
        @DisplayName("日本語を含むtestCasesを処理できる")
        void generateTest_japaneseTestCases_works() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            String testCases = "正常な新規登録処理, メールアドレス形式エラー, 重複登録エラー";
            // Act
            String result = tool.generateTest(targetClass, "request-response", null, testCases, null, null);
            // Assert
            assertFalse(result.contains("テストコードの生成に失敗"));
        }

        @Test
        @DisplayName("空のtestCasesはデフォルトケースを生成")
        void generateTest_emptyTestCases_generatesDefaultCases() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, "", null, null);
            // Assert
            assertTrue(result.contains("### テストケース一覧"));
        }
    }

    // ========== 規約適用テスト ==========

    @Nested
    @DisplayName("Nablarch規約適用テスト")
    class ConventionTests {

        @BeforeEach
        void setUpMocks() {
            when(knowledgeBase.search(anyString(), anyString()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("unitテストでJUnit5標準アサーション規約が適用される")
        void generateTest_unit_appliesJunit5Convention() {
            // Arrange
            String targetClass = "com.example.util.DateUtils";
            // Act
            String result = tool.generateTest(targetClass, "unit", null, null, null, null);
            // Assert
            assertTrue(result.contains("JUnit5標準アサーションを使用"));
        }

        @Test
        @DisplayName("request-responseテストでexecuteメソッド規約が適用される")
        void generateTest_requestResponse_appliesExecuteConvention() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "request-response", null, null, null, null);
            // Assert
            assertTrue(result.contains("executeメソッドでHTTPリクエストをシミュレート"));
        }

        @Test
        @DisplayName("全テストタイプでExcelテストデータ管理規約が適用される")
        void generateTest_allTypes_appliesExcelConvention() {
            // Arrange
            String targetClass = "com.example.action.UserAction";
            // Act
            String result = tool.generateTest(targetClass, "batch", null, null, null, null);
            // Assert
            assertTrue(result.contains("テストデータはExcelファイルで管理"));
        }
    }
}

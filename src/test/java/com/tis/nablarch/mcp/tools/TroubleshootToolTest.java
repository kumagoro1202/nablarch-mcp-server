package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link TroubleshootTool} の単体テスト。
 *
 * <p>NablarchKnowledgeBaseをモックし、エラー分析・解決策検索機能を検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TroubleshootToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private TroubleshootTool troubleshootTool;

    @BeforeEach
    void setUp() {
        troubleshootTool = new TroubleshootTool(knowledgeBase);
    }

    @Nested
    @DisplayName("入力検証")
    class InputValidation {

        @Test
        @DisplayName("エラーメッセージがnullの場合エラーを返す")
        void troubleshoot_エラーメッセージがnull() {
            String result = troubleshootTool.troubleshoot(null, null, null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
            assertTrue(result.contains("エラーメッセージを指定してください"));
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("エラーメッセージが空白の場合エラーを返す")
        void troubleshoot_エラーメッセージが空白() {
            String result = troubleshootTool.troubleshoot("   ", null, null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
            assertTrue(result.contains("エラーメッセージを指定してください"));
            verifyNoInteractions(knowledgeBase);
        }
    }

    @Nested
    @DisplayName("エラーカテゴリ分類")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CategoryClassification {

        @Test
        @DisplayName("ハンドラ関連エラーを正しく分類する")
        void troubleshoot_ハンドラカテゴリ() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "java.lang.ClassCastException: Cannot cast handler result",
                    "at nablarch.fw.handler.GlobalErrorHandler.handle",
                    null, null);

            assertNotNull(result);
            assertTrue(result.contains("ハンドラキュー関連"));
        }

        @Test
        @DisplayName("データベース関連エラーを正しく分類する")
        void troubleshoot_データベースカテゴリ() {
            when(knowledgeBase.search(any(), eq("error"))).thenReturn(List.of());
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "java.sql.SQLException: Connection is not available",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("データベース関連"));
        }

        @Test
        @DisplayName("バリデーション関連エラーを正しく分類する")
        void troubleshoot_バリデーションカテゴリ() {
            when(knowledgeBase.search(any(), eq("error"))).thenReturn(List.of());
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "nablarch.core.message.ApplicationException: validation error",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("バリデーション関連"));
        }

        @Test
        @DisplayName("設定関連エラーを正しく分類する")
        void troubleshoot_設定カテゴリ() {
            when(knowledgeBase.search(any(), eq("error"))).thenReturn(List.of());
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "nablarch.core.repository.di.ComponentCreationException",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("設定・コンポーネント定義関連"));
        }

        @Test
        @DisplayName("バッチ関連エラーを正しく分類する")
        void troubleshoot_バッチカテゴリ() {
            when(knowledgeBase.search(any(), eq("error"))).thenReturn(List.of());
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "nablarch.fw.handler.DuplicateProcessException: batch already running",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("バッチ処理関連"));
        }
    }

    @Nested
    @DisplayName("知識ベース検索")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class KnowledgeBaseSearch {

        @Test
        @DisplayName("エラーコード指定時に検索が実行される")
        void troubleshoot_エラーコード指定() {
            when(knowledgeBase.search(eq("ERR-001"), eq("error")))
                    .thenReturn(List.of("[エラー] ERR-001 (handler/critical) — ハンドラキュー順序エラー"));
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Handler queue error", null, "ERR-001", null);

            assertNotNull(result);
            assertTrue(result.contains("ERR-001"));
        }

        @Test
        @DisplayName("検索結果がある場合に関連情報を表示する")
        void troubleshoot_検索結果あり() {
            when(knowledgeBase.search(any(), eq("error")))
                    .thenReturn(List.of("[エラー] ERR-004 (database/critical) — Connection is not available"));
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "java.sql.SQLException: Connection is not available",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("解決策・関連情報"));
            assertTrue(result.contains("1 件の関連情報が見つかりました"));
        }

        @Test
        @DisplayName("検索結果がない場合に一般的なガイダンスを表示する")
        void troubleshoot_検索結果なし() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Unknown error message",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("一般的な確認事項"));
        }
    }

    @Nested
    @DisplayName("出力フォーマット")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class OutputFormat {

        @Test
        @DisplayName("結果にエラー分析セクションが含まれる")
        void troubleshoot_エラー分析セクション() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Test error", null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("# トラブルシューティング結果"));
            assertTrue(result.contains("## エラー分析"));
            assertTrue(result.contains("| カテゴリ |"));
        }

        @Test
        @DisplayName("結果に関連ドキュメントセクションが含まれる")
        void troubleshoot_関連ドキュメント() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Test error", null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("## 関連ドキュメント"));
            assertTrue(result.contains("Nablarch公式ドキュメント"));
        }

        @Test
        @DisplayName("ハンドラ名が抽出されて表示される")
        void troubleshoot_ハンドラ名抽出() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Error in GlobalErrorHandler",
                    "at nablarch.fw.handler.GlobalErrorHandler.handle",
                    null, null);

            assertNotNull(result);
            assertTrue(result.contains("GlobalErrorHandler"));
        }

        @Test
        @DisplayName("検索キーワードがメタデータに含まれる")
        void troubleshoot_検索キーワード表示() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "java.sql.SQLException: Test",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("検索キーワード:"));
        }
    }

    @Nested
    @DisplayName("スタックトレース解析")
    class StackTraceAnalysis {

        @Test
        @DisplayName("スタックトレースからコンポーネントを抽出する")
        void troubleshoot_スタックトレース解析() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String stackTrace = """
                    java.lang.NullPointerException
                        at nablarch.fw.web.handler.HttpCharacterEncodingHandler.handle(HttpCharacterEncodingHandler.java:50)
                        at nablarch.fw.handler.GlobalErrorHandler.handle(GlobalErrorHandler.java:30)
                    """;

            String result = troubleshootTool.troubleshoot(
                    "NullPointerException", stackTrace, null, null);

            assertNotNull(result);
            // スタックトレースが解析されてハンドラ関連と判定される
            assertTrue(result.contains("エラー分析"));
        }
    }

    @Nested
    @DisplayName("境界値テスト")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class BoundaryValueTests {

        @Test
        @DisplayName("空文字列のエラーメッセージでエラーを返す")
        void troubleshoot_空文字列() {
            String result = troubleshootTool.troubleshoot("", null, null, null);
            assertTrue(result.contains("[MCP_TOOL_002]"));
            assertTrue(result.contains("エラーメッセージを指定してください"));
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("非常に長いスタックトレースを処理できる")
        void troubleshoot_長いスタックトレース() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            // 100行のスタックトレースを生成
            StringBuilder longStackTrace = new StringBuilder();
            longStackTrace.append("java.lang.RuntimeException: Test exception\n");
            for (int i = 0; i < 100; i++) {
                longStackTrace.append("    at nablarch.fw.handler.Handler")
                        .append(i)
                        .append(".handle(Handler")
                        .append(i)
                        .append(".java:")
                        .append(i + 10)
                        .append(")\n");
            }

            String result = troubleshootTool.troubleshoot(
                    "RuntimeException: Test exception",
                    longStackTrace.toString(),
                    null, null);

            assertNotNull(result);
            assertTrue(result.contains("トラブルシューティング結果"));
            assertTrue(result.contains("ハンドラキュー関連"));
        }

        @Test
        @DisplayName("特殊文字を含むエラーメッセージを処理できる")
        void troubleshoot_特殊文字() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String errorWithSpecialChars = "Error: <xml>&entity; 'quoted' \"double\" \t\n日本語エラー";

            String result = troubleshootTool.troubleshoot(
                    errorWithSpecialChars, null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("トラブルシューティング結果"));
        }

        @Test
        @DisplayName("Unicodeを含むメッセージを処理できる")
        void troubleshoot_Unicode() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "エラー: あいうえお SQLException",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("データベース関連"));
        }
    }

    @Nested
    @DisplayName("エラーコード抽出")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class ErrorCodeExtraction {

        @Test
        @DisplayName("エラーメッセージからERR-XXX形式のコードを抽出する")
        void troubleshoot_エラーコード自動抽出() {
            when(knowledgeBase.search(eq("ERR-123"), eq("error")))
                    .thenReturn(List.of("[エラー] ERR-123 — テストエラー"));
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Application failed with ERR-123: handler configuration error",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("ERR-123"));
            verify(knowledgeBase).search(eq("ERR-123"), eq("error"));
        }

        @Test
        @DisplayName("無効なエラーコード形式は無視される")
        void troubleshoot_無効なエラーコード() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Test error", null, "INVALID-CODE", null);

            assertNotNull(result);
            // INVALID-CODEは表示されない（ERR-XXX形式ではないため）
            assertFalse(result.contains("INVALID-CODE"));
        }
    }

    @Nested
    @DisplayName("検索結果件数")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class SearchResultCount {

        @Test
        @DisplayName("5件以上の検索結果がある場合は省略表示される")
        void troubleshoot_多数の検索結果() {
            when(knowledgeBase.search(any(), eq("error")))
                    .thenReturn(List.of(
                            "[エラー1] ERR-001 — エラー1",
                            "[エラー2] ERR-002 — エラー2",
                            "[エラー3] ERR-003 — エラー3",
                            "[エラー4] ERR-004 — エラー4",
                            "[エラー5] ERR-005 — エラー5",
                            "[エラー6] ERR-006 — エラー6",
                            "[エラー7] ERR-007 — エラー7"
                    ));
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "SQLException: Connection error",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("7 件の関連情報が見つかりました"));
            assertTrue(result.contains("他 2 件省略"));
        }

        @Test
        @DisplayName("検索結果が5件以下の場合は全て表示される")
        void troubleshoot_少数の検索結果() {
            when(knowledgeBase.search(any(), eq("error")))
                    .thenReturn(List.of(
                            "[エラー1] ERR-001 — エラー1",
                            "[エラー2] ERR-002 — エラー2",
                            "[エラー3] ERR-003 — エラー3"
                    ));
            when(knowledgeBase.search(any(), eq(null))).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "SQLException: Connection error",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("3 件の関連情報が見つかりました"));
            assertFalse(result.contains("件省略"));
        }
    }

    @Nested
    @DisplayName("generalカテゴリ")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class GeneralCategory {

        @Test
        @DisplayName("カテゴリが特定できない場合はgeneralと判定される")
        void troubleshoot_generalカテゴリ() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Unknown mysterious error occurred",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("一般"));
            assertTrue(result.contains("一般的な確認事項"));
        }

        @Test
        @DisplayName("generalカテゴリでNablarch公式ドキュメントリンクが表示される")
        void troubleshoot_generalカテゴリのドキュメント() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Some error without specific keywords",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("Nablarch公式ドキュメント"));
            // ハンドラやデータベース固有のリンクは表示されない
            assertFalse(result.contains("ハンドラキュー設計"));
            assertFalse(result.contains("ユニバーサルDAO"));
        }
    }

    @Nested
    @DisplayName("カテゴリ固有ドキュメント")
    @MockitoSettings(strictness = Strictness.LENIENT)
    class CategorySpecificDocuments {

        @Test
        @DisplayName("ハンドラカテゴリでハンドラキュー設計リンクが表示される")
        void troubleshoot_ハンドラカテゴリのドキュメント() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "Handler dispatch error in queue",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("ハンドラキュー設計"));
        }

        @Test
        @DisplayName("データベースカテゴリでユニバーサルDAOリンクが表示される")
        void troubleshoot_データベースカテゴリのドキュメント() {
            when(knowledgeBase.search(any(), any())).thenReturn(List.of());

            String result = troubleshootTool.troubleshoot(
                    "java.sql.SQLException in DAO operation",
                    null, null, null);

            assertNotNull(result);
            assertTrue(result.contains("ユニバーサルDAO"));
        }
    }
}

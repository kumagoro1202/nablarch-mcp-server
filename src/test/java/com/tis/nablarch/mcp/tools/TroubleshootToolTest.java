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
            assertEquals("エラーメッセージを指定してください。", result);
            verifyNoInteractions(knowledgeBase);
        }

        @Test
        @DisplayName("エラーメッセージが空白の場合エラーを返す")
        void troubleshoot_エラーメッセージが空白() {
            String result = troubleshootTool.troubleshoot("   ", null, null, null);
            assertEquals("エラーメッセージを指定してください。", result);
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
}

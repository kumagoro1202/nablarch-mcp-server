package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link SearchApiTool} の単体テスト。
 *
 * <p>NablarchKnowledgeBaseをモックし、各入力パターンに対する
 * 出力形式・エラーメッセージを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class SearchApiToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private SearchApiTool searchApiTool;

    @BeforeEach
    void setUp() {
        searchApiTool = new SearchApiTool(knowledgeBase);
    }

    @Test
    void searchApi_キーワードがnullの場合エラーメッセージを返す() {
        String result = searchApiTool.searchApi(null, null);
        assertEquals("検索キーワードを指定してください。", result);
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void searchApi_キーワードが空白の場合エラーメッセージを返す() {
        String result = searchApiTool.searchApi("   ", null);
        assertEquals("検索キーワードを指定してください。", result);
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void searchApi_結果ありの場合フォーマット済みテキストを返す() {
        when(knowledgeBase.search("UniversalDao", null))
                .thenReturn(List.of(
                        "[モジュール] nablarch-common-dao (library) — 汎用データアクセス",
                        "[APIパターン] universal-dao (web) — UniversalDao CRUD操作 | FQCN: nablarch.common.dao.UniversalDao"));

        String result = searchApiTool.searchApi("UniversalDao", null);

        assertNotNull(result);
        assertTrue(result.contains("検索結果: \"UniversalDao\""));
        assertTrue(result.contains("件数: 2件"));
        assertTrue(result.contains("[モジュール] nablarch-common-dao"));
        assertTrue(result.contains("[APIパターン] universal-dao"));
        verify(knowledgeBase).search("UniversalDao", null);
    }

    @Test
    void searchApi_カテゴリフィルタ付きで検索結果を返す() {
        when(knowledgeBase.search("handler", "web"))
                .thenReturn(List.of(
                        "[ハンドラ] GlobalErrorHandler — 未処理例外キャッチ | FQCN: nablarch.fw.handler.GlobalErrorHandler | 必須: はい"));

        String result = searchApiTool.searchApi("handler", "web");

        assertNotNull(result);
        assertTrue(result.contains("検索結果: \"handler\""));
        assertTrue(result.contains("(カテゴリ: web)"));
        assertTrue(result.contains("件数: 1件"));
        assertTrue(result.contains("[ハンドラ] GlobalErrorHandler"));
        verify(knowledgeBase).search("handler", "web");
    }

    @Test
    void searchApi_カテゴリが空白の場合はnullとして扱う() {
        when(knowledgeBase.search("test", null))
                .thenReturn(List.of("[APIパターン] test-pattern (web) — テスト | FQCN: n/a"));

        String result = searchApiTool.searchApi("test", "   ");

        assertNotNull(result);
        assertFalse(result.contains("カテゴリ:"));
        verify(knowledgeBase).search("test", null);
    }

    @Test
    void searchApi_結果0件の場合は結果なしメッセージを返す() {
        when(knowledgeBase.search("nonexistent", null)).thenReturn(List.of());

        String result = searchApiTool.searchApi("nonexistent", null);

        assertEquals("検索結果なし: nonexistent", result);
    }

    @Test
    void searchApi_カテゴリ付きで結果0件の場合はカテゴリ情報も含む() {
        when(knowledgeBase.search("xyz", "batch")).thenReturn(List.of());

        String result = searchApiTool.searchApi("xyz", "batch");

        assertEquals("検索結果なし: xyz (カテゴリ: batch)", result);
    }
}

package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ValidateHandlerQueueTool} の単体テスト。
 *
 * <p>XML解析によるハンドラ名抽出と、NablarchKnowledgeBaseへの検証委譲をテストする。</p>
 */
@ExtendWith(MockitoExtension.class)
class ValidateHandlerQueueToolTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private ValidateHandlerQueueTool tool;

    @BeforeEach
    void setUp() {
        tool = new ValidateHandlerQueueTool(knowledgeBase);
    }

    // ========== 入力バリデーション ==========

    @Test
    void validateHandlerQueue_XMLがnullの場合エラーメッセージを返す() {
        String result = tool.validateHandlerQueue(null, "web");
        assertEquals("ハンドラキューXMLを指定してください。", result);
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void validateHandlerQueue_XMLが空白の場合エラーメッセージを返す() {
        String result = tool.validateHandlerQueue("   ", "web");
        assertEquals("ハンドラキューXMLを指定してください。", result);
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void validateHandlerQueue_アプリタイプがnullの場合エラーメッセージを返す() {
        String xml = "<component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>";
        String result = tool.validateHandlerQueue(xml, null);
        assertEquals("アプリケーションタイプを指定してください（web, rest, batch, messaging）。", result);
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void validateHandlerQueue_アプリタイプが空白の場合エラーメッセージを返す() {
        String xml = "<component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>";
        String result = tool.validateHandlerQueue(xml, "");
        assertEquals("アプリケーションタイプを指定してください（web, rest, batch, messaging）。", result);
        verifyNoInteractions(knowledgeBase);
    }

    // ========== XML解析 ==========

    @Test
    void validateHandlerQueue_class属性がないXMLの場合ハンドラ抽出失敗メッセージを返す() {
        String xml = "<handler-queue><property name=\"foo\" value=\"bar\"/></handler-queue>";
        String result = tool.validateHandlerQueue(xml, "web");
        assertTrue(result.contains("XMLからハンドラクラスを抽出できませんでした"));
        verifyNoInteractions(knowledgeBase);
    }

    @Test
    void extractHandlerNames_FQCNから単純クラス名を抽出する() {
        String xml = "<component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>"
                + "<component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>";
        List<String> names = tool.extractHandlerNames(xml);
        assertEquals(2, names.size());
        assertEquals("GlobalErrorHandler", names.get(0));
        assertEquals("HttpResponseHandler", names.get(1));
    }

    @Test
    void extractHandlerNames_ドットなしの単純名をそのまま返す() {
        String xml = "<handler class=\"GlobalErrorHandler\"/>";
        List<String> names = tool.extractHandlerNames(xml);
        assertEquals(1, names.size());
        assertEquals("GlobalErrorHandler", names.get(0));
    }

    @Test
    void extractHandlerNames_複数のハンドラを順序通りに抽出する() {
        String xml = "<handler-queue>\n"
                + "  <component class=\"nablarch.fw.web.handler.HttpCharacterEncodingHandler\"/>\n"
                + "  <component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>\n"
                + "  <component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>\n"
                + "  <component class=\"nablarch.fw.web.handler.SecureHandler\"/>\n"
                + "</handler-queue>";
        List<String> names = tool.extractHandlerNames(xml);
        assertEquals(4, names.size());
        assertEquals("HttpCharacterEncodingHandler", names.get(0));
        assertEquals("GlobalErrorHandler", names.get(1));
        assertEquals("HttpResponseHandler", names.get(2));
        assertEquals("SecureHandler", names.get(3));
    }

    @Test
    void extractHandlerNames_空XMLの場合は空リストを返す() {
        List<String> names = tool.extractHandlerNames("<handler-queue/>");
        assertTrue(names.isEmpty());
    }

    // ========== 検証委譲 ==========

    @Test
    void validateHandlerQueue_正常なXMLの場合KnowledgeBaseに委譲する() {
        String xml = "<component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>"
                + "<component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>";
        String expectedResult = "## 検証結果: OK\nアプリタイプ: web\nハンドラ数: 2\n";

        when(knowledgeBase.validateHandlerQueue(eq("web"),
                eq(List.of("GlobalErrorHandler", "HttpResponseHandler"))))
                .thenReturn(expectedResult);

        String result = tool.validateHandlerQueue(xml, "web");

        assertEquals(expectedResult, result);
        verify(knowledgeBase).validateHandlerQueue("web",
                List.of("GlobalErrorHandler", "HttpResponseHandler"));
    }

    @Test
    void validateHandlerQueue_検証エラーのある結果を返す() {
        String xml = "<handler-queue>\n"
                + "  <component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>\n"
                + "  <component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>\n"
                + "</handler-queue>";
        String errorResult = "## 検証結果: NG\nアプリタイプ: web\nハンドラ数: 2\n\n"
                + "### エラー (1件)\n"
                + "- 順序違反: GlobalErrorHandler (位置2) は HttpResponseHandler (位置1) より前に配置すべき\n";

        when(knowledgeBase.validateHandlerQueue(eq("web"),
                eq(List.of("HttpResponseHandler", "GlobalErrorHandler"))))
                .thenReturn(errorResult);

        String result = tool.validateHandlerQueue(xml, "web");

        assertTrue(result.contains("検証結果: NG"));
        assertTrue(result.contains("順序違反"));
    }
}

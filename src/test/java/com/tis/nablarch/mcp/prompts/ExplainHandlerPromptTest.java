package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ExplainHandlerPrompt} のユニットテスト。
 */
class ExplainHandlerPromptTest {

    private ExplainHandlerPrompt prompt;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new ExplainHandlerPrompt();
        prompt.init();
    }

    @Test
    void execute_knownHandler_returnsDetailedInfo() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "HttpCharacterEncodingHandler"));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("HttpCharacterEncodingHandler"));
    }

    @Test
    void execute_knownHandler_containsFqcn() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "HttpCharacterEncodingHandler"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("nablarch.fw.web.handler.HttpCharacterEncodingHandler"));
    }

    @Test
    void execute_knownHandler_containsAppType() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "GlobalErrorHandler"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("アプリケーションタイプ"));
    }

    @Test
    void execute_knownHandler_containsBasicInfo() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "HttpCharacterEncodingHandler"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("基本情報"));
        assertTrue(text.contains("FQCN"));
    }

    @Test
    void execute_unknownHandler_returnsNotFoundMessage() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "NonExistentHandler"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("見つかりませんでした"));
    }

    @Test
    void execute_caseInsensitiveMatch() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("handler_name", "httpcharacterencodinghandler"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("HttpCharacterEncodingHandler"));
    }

    @Test
    void execute_nullHandlerName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of()));
    }

    @Test
    void execute_blankHandlerName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("handler_name", "")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

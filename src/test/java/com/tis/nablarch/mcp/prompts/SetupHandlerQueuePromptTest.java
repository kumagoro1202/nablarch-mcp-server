package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SetupHandlerQueuePrompt} のユニットテスト。
 */
class SetupHandlerQueuePromptTest {

    private SetupHandlerQueuePrompt prompt;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new SetupHandlerQueuePrompt();
        prompt.init();
    }

    @Test
    void execute_webType_returnsHandlerQueueGuide() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "web"));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("web"));
        assertTrue(text.contains("ハンドラキュー"));
    }

    @Test
    void execute_restType_returnsHandlerQueueGuide() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "rest"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("rest"));
    }

    @Test
    void execute_batchType_returnsHandlerQueueGuide() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "batch"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("batch"));
    }

    @Test
    void execute_messagingType_returnsHandlerQueueGuide() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "messaging"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("messaging"));
    }

    @Test
    void execute_webType_containsHandlerInfo() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "web"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("HttpCharacterEncodingHandler"));
        assertTrue(text.contains("GlobalErrorHandler"));
    }

    @Test
    void execute_webType_containsConstraintInfo() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("app_type", "web"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("順序制約"));
    }

    @Test
    void execute_nullAppType_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of()));
    }

    @Test
    void execute_blankAppType_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of("app_type", "  ")));
    }

    @Test
    void execute_invalidAppType_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of("app_type", "invalid")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

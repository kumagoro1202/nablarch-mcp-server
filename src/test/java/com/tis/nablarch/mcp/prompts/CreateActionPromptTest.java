package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CreateActionPrompt} のユニットテスト。
 */
class CreateActionPromptTest {

    private CreateActionPrompt prompt;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new CreateActionPrompt();
        prompt.init();
    }

    @Test
    void execute_webType_returnsActionGuide() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("app_type", "web", "action_name", "UserSearchAction"));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("UserSearchAction"));
        assertTrue(text.contains("web"));
    }

    @Test
    void execute_restType_returnsActionGuide() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("app_type", "rest", "action_name", "ItemsAction"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("ItemsAction"));
        assertTrue(text.contains("rest"));
    }

    @Test
    void execute_batchType_returnsActionGuide() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("app_type", "batch", "action_name", "ImportBatchAction"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("ImportBatchAction"));
    }

    @Test
    void execute_webType_containsApiPatterns() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("app_type", "web", "action_name", "TestAction"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("action-class") || text.contains("推奨パターン"));
    }

    @Test
    void execute_containsNamingConvention() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("app_type", "web", "action_name", "SampleAction"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("命名規則"));
    }

    @Test
    void execute_nullAppType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("action_name", "TestAction")));
    }

    @Test
    void execute_invalidAppType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("app_type", "unknown", "action_name", "TestAction")));
    }

    @Test
    void execute_nullActionName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("app_type", "web")));
    }

    @Test
    void execute_blankActionName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("app_type", "web", "action_name", "")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

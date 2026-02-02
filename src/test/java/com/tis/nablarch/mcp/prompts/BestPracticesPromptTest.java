package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BestPracticesPrompt} のユニットテスト。
 */
class BestPracticesPromptTest {

    private BestPracticesPrompt prompt;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new BestPracticesPrompt();
        prompt.init();
    }

    @Test
    void execute_handlerQueueTopic_returnsPractices() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "handler-queue"));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("handler-queue"));
        assertTrue(text.contains("ベストプラクティス"));
    }

    @Test
    void execute_handlerQueueTopic_containsDesignPatterns() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "handler-queue"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("設計パターン"));
        assertTrue(text.contains("handler-queue-pattern") || text.contains("ハンドラキューパターン"));
    }

    @Test
    void execute_actionTopic_returnsPractices() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "action"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("action"));
        assertTrue(text.contains("推奨実装パターン"));
    }

    @Test
    void execute_validationTopic_returnsPractices() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "validation"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("validation"));
    }

    @Test
    void execute_databaseTopic_returnsPractices() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "database"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("database"));
    }

    @Test
    void execute_testingTopic_returnsPractices() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", "testing"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("testing"));
    }

    @Test
    void execute_allTopics_containGeneralNotes() {
        for (String topic : new String[]{"handler-queue", "action", "validation", "database", "testing"}) {
            McpSchema.GetPromptResult result = prompt.execute(Map.of("topic", topic));
            String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
            assertTrue(text.contains("注意事項"), "topic=" + topic + " should contain 注意事項");
        }
    }

    @Test
    void execute_nullTopic_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of()));
    }

    @Test
    void execute_blankTopic_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of("topic", "  ")));
    }

    @Test
    void execute_invalidTopic_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of("topic", "invalid")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

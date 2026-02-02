package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MigrationGuidePrompt} のユニットテスト。
 */
class MigrationGuidePromptTest {

    private MigrationGuidePrompt prompt;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new MigrationGuidePrompt();
        prompt.init();
    }

    @Test
    void execute_validVersions_returnsMigrationGuide() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5u21", "to_version", "6"));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("5u21"));
        assertTrue(text.contains("6"));
        assertTrue(text.contains("移行ガイド"));
    }

    @Test
    void execute_containsModuleList() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5u21", "to_version", "6"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("モジュール一覧"));
    }

    @Test
    void execute_containsKeyClasses() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5", "to_version", "6"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("主要クラス"));
    }

    @Test
    void execute_containsDependencies() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5", "to_version", "6"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("依存関係"));
    }

    @Test
    void execute_containsMigrationSteps() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5", "to_version", "6"));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("移行手順"));
        assertTrue(text.contains("nablarch-bom"));
    }

    @Test
    void execute_descriptionContainsVersions() {
        McpSchema.GetPromptResult result = prompt.execute(
                Map.of("from_version", "5u21", "to_version", "6"));
        assertTrue(result.description().contains("5u21"));
        assertTrue(result.description().contains("6"));
    }

    @Test
    void execute_nullFromVersion_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("to_version", "6")));
    }

    @Test
    void execute_blankFromVersion_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("from_version", "  ", "to_version", "6")));
    }

    @Test
    void execute_nullToVersion_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("from_version", "5")));
    }

    @Test
    void execute_blankToVersion_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                prompt.execute(Map.of("from_version", "5", "to_version", "")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

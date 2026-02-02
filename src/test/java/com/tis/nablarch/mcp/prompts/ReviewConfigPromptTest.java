package com.tis.nablarch.mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ReviewConfigPrompt} のユニットテスト。
 */
class ReviewConfigPromptTest {

    private ReviewConfigPrompt prompt;

    private static final String SAMPLE_XML = """
            <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
              <list name="handlerQueue">
                <component class="nablarch.fw.web.handler.HttpCharacterEncodingHandler"/>
                <component class="nablarch.fw.handler.GlobalErrorHandler"/>
              </list>
            </component-configuration>
            """;

    @BeforeEach
    void setUp() throws Exception {
        prompt = new ReviewConfigPrompt();
        prompt.init();
    }

    @Test
    void execute_withValidXml_returnsReview() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", SAMPLE_XML));
        assertNotNull(result);
        assertNotNull(result.messages());
        assertFalse(result.messages().isEmpty());
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("レビュー"));
    }

    @Test
    void execute_containsConstraintRules() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", SAMPLE_XML));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("ハンドラ順序制約"));
    }

    @Test
    void execute_containsErrorPatterns() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", SAMPLE_XML));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("よくある問題パターン"));
    }

    @Test
    void execute_containsCheckItems() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", SAMPLE_XML));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("確認事項"));
    }

    @Test
    void execute_longXml_isTruncated() {
        String longXml = "x".repeat(600);
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", longXml));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("省略"));
    }

    @Test
    void execute_xmlContentIncludedInOutput() {
        McpSchema.GetPromptResult result = prompt.execute(Map.of("config_xml", SAMPLE_XML));
        String text = ((McpSchema.TextContent) result.messages().get(0).content()).text();
        assertTrue(text.contains("component-configuration"));
    }

    @Test
    void execute_nullConfigXml_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of()));
    }

    @Test
    void execute_blankConfigXml_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(Map.of("config_xml", "  ")));
    }

    @Test
    void execute_nullArguments_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> prompt.execute(null));
    }
}

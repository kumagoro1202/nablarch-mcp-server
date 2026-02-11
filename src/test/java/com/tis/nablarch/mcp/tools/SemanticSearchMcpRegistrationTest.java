package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.rag.ingestion.OfficialDocsIngester;
import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.BM25SearchService;
import com.tis.nablarch.mcp.rag.search.VectorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SemanticSearchTool} のMCP Tool登録検証テスト。
 *
 * <p>McpServerConfigを読み込み、{@code semantic_search} ToolがMCPサーバに
 * 正しく登録されていることを検証する。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SemanticSearchMcpRegistrationTest {

    @Autowired
    private ToolCallbackProvider toolCallbackProvider;

    @MockitoBean
    private BM25SearchService bm25SearchService;

    @MockitoBean
    private VectorSearchService vectorSearchService;

    @MockitoBean
    private Reranker reranker;

    @MockitoBean
    @SuppressWarnings("unused")
    private OfficialDocsIngester officialDocsIngester;

    @Test
    @DisplayName("Tool登録確認: semantic_search がMCPツール一覧に存在する")
    void semanticSearchToolRegistered() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        assertNotNull(callbacks);
        assertTrue(callbacks.length > 0, "ツールが1つ以上登録されていること");

        boolean found = Arrays.stream(callbacks)
                .anyMatch(cb -> cb.getToolDefinition().name().equals("semantic_search"));

        assertTrue(found, "semantic_search ツールがMCPに登録されていること。" +
                " 登録済みツール: " + Arrays.stream(callbacks)
                .map(cb -> cb.getToolDefinition().name())
                .toList());
    }

    @Test
    @DisplayName("Tool説明文確認: 英語の説明文が設定されている")
    void semanticSearchToolHasDescription() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        ToolCallback semanticSearchCallback = Arrays.stream(callbacks)
                .filter(cb -> cb.getToolDefinition().name().equals("semantic_search"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("semantic_search ツールが見つからない"));

        String description = semanticSearchCallback.getToolDefinition().description();
        assertNotNull(description);
        assertFalse(description.isBlank(), "説明文が空でないこと");
        assertTrue(description.contains("Nablarch"), "Nablarchに関する説明を含むこと");
        assertTrue(description.contains("search"), "検索に関する説明を含むこと");
    }

    @Test
    @DisplayName("Tool入力スキーマ検証: query, appType等のパラメータが定義されている")
    void semanticSearchToolHasInputSchema() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
        ToolCallback semanticSearchCallback = Arrays.stream(callbacks)
                .filter(cb -> cb.getToolDefinition().name().equals("semantic_search"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("semantic_search ツールが見つからない"));

        String inputSchema = semanticSearchCallback.getToolDefinition().inputSchema();
        assertNotNull(inputSchema, "入力スキーマが定義されていること");
        // queryパラメータが必須であること
        assertTrue(inputSchema.contains("query"), "queryパラメータが定義されていること");
        // オプションパラメータ
        assertTrue(inputSchema.contains("appType") || inputSchema.contains("app_type"),
                "appTypeパラメータが定義されていること");
        assertTrue(inputSchema.contains("mode"),
                "modeパラメータが定義されていること");
    }

    @Test
    @DisplayName("既存ツール共存確認: search_api と semantic_search が共に登録されている")
    void bothSearchToolsRegistered() {
        ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();

        boolean hasSearchApi = Arrays.stream(callbacks)
                .anyMatch(cb -> cb.getToolDefinition().name().equals("search_api"));
        boolean hasSemanticSearch = Arrays.stream(callbacks)
                .anyMatch(cb -> cb.getToolDefinition().name().equals("semantic_search"));

        assertTrue(hasSearchApi, "search_api ツールが登録されていること");
        assertTrue(hasSemanticSearch, "semantic_search ツールが登録されていること");
    }
}

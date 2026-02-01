package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP Tool: search_api
 *
 * <p>Searches the Nablarch API documentation and returns matching
 * classes, methods, and usage patterns. This tool enables AI assistants
 * to find relevant Nablarch APIs when generating code.</p>
 */
@Service
public class SearchApiTool {

    private final NablarchKnowledgeBase knowledgeBase;

    public SearchApiTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Search the Nablarch API documentation.
     *
     * @param keyword  the search keyword (class name, method name, or concept)
     * @param category optional category filter (e.g., "handler", "library", "web", "batch")
     * @return search results as formatted text
     */
    @Tool(description = "Search the Nablarch API documentation for classes, methods, and patterns. "
            + "Use this when you need to find Nablarch APIs for code generation.")
    public String searchApi(
            @ToolParam(description = "Search keyword (class name, method name, or concept)") String keyword,
            @ToolParam(description = "Optional category filter: handler, library, web, batch, rest, messaging")
            String category) {
        // TODO: Implement full-text search over Nablarch knowledge base
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }
}

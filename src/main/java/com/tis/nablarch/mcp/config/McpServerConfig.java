package com.tis.nablarch.mcp.config;

import com.tis.nablarch.mcp.tools.SearchApiTool;
import com.tis.nablarch.mcp.tools.ValidateHandlerQueueTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration.
 *
 * <p>Registers MCP tools, resources, and prompts with the server.
 * Configures the STDIO transport for Phase 1.</p>
 */
@Configuration
public class McpServerConfig {

    /**
     * Registers MCP tools as Spring AI tool callbacks.
     *
     * @param searchApiTool             the API search tool
     * @param validateHandlerQueueTool  the handler queue validation tool
     * @return tool callback provider for MCP server auto-configuration
     */
    @Bean
    public ToolCallbackProvider nablarchTools(
            SearchApiTool searchApiTool,
            ValidateHandlerQueueTool validateHandlerQueueTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchApiTool, validateHandlerQueueTool)
                .build();
    }
}

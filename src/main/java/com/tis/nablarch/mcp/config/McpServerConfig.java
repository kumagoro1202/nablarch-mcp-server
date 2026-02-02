package com.tis.nablarch.mcp.config;

import com.tis.nablarch.mcp.tools.SearchApiTool;
import com.tis.nablarch.mcp.tools.ValidateHandlerQueueTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Server configuration.
 *
 * <p>Registers MCP tools, resources, and prompts with the server.
 * Configures the STDIO transport for Phase 1.</p>
 *
 * <p>Phase 1 registers the framework for all three MCP primitives:
 * <ul>
 *   <li>Tools: via {@code @Tool} annotation + ToolCallbackProvider (existing)</li>
 *   <li>Resources: via SyncResourceSpecification beans (new)</li>
 *   <li>Prompts: via SyncPromptSpecification beans (new)</li>
 * </ul>
 * Resource and Prompt handlers return stub content in Phase 1.
 * Full implementations will be provided in Wave 2.</p>
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

    /**
     * Registers MCP resources for Nablarch handler catalogs and guides.
     *
     * @return list of resource specifications for MCP server auto-configuration
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> nablarchResources() {
        return List.of(
            createHandlerResourceSpec("web", "Nablarch Web Handler Catalog",
                "Web application handler specifications and ordering constraints"),
            createHandlerResourceSpec("rest", "Nablarch REST Handler Catalog",
                "REST application handler specifications and ordering constraints"),
            createHandlerResourceSpec("batch", "Nablarch Batch Handler Catalog",
                "Batch application handler specifications and ordering constraints"),
            createHandlerResourceSpec("messaging", "Nablarch Messaging Handler Catalog",
                "Messaging application handler specifications and ordering constraints"),
            createGuideResourceSpec("setup", "Nablarch Setup Guide",
                "Nablarch project setup and configuration guide"),
            createGuideResourceSpec("testing", "Nablarch Testing Guide",
                "Nablarch testing patterns and best practices guide")
        );
    }

    /**
     * Registers MCP prompts for Nablarch development assistance.
     *
     * @return list of prompt specifications for MCP server auto-configuration
     */
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> nablarchPrompts() {
        return List.of(
            createPromptSpec("setup-handler-queue",
                "Set up a Nablarch handler queue configuration",
                List.of(arg("app_type", "Application type: web, rest, batch, messaging", true))),
            createPromptSpec("create-action",
                "Generate a Nablarch action class skeleton",
                List.of(
                    arg("app_type", "Application type: web, rest, batch, messaging", true),
                    arg("action_name", "Name of the action class to generate", true))),
            createPromptSpec("review-config",
                "Review a Nablarch XML configuration file for correctness",
                List.of(arg("config_xml", "XML configuration content to review", true))),
            createPromptSpec("explain-handler",
                "Get a detailed explanation of a Nablarch handler",
                List.of(arg("handler_name", "Name of the handler to explain", true))),
            createPromptSpec("migration-guide",
                "Get a migration guide between Nablarch versions",
                List.of(
                    arg("from_version", "Source Nablarch version", true),
                    arg("to_version", "Target Nablarch version", true))),
            createPromptSpec("best-practices",
                "Get Nablarch best practices for a specific topic",
                List.of(arg("topic", "Topic: handler-queue, action, validation, database, testing", true)))
        );
    }

    private static McpServerFeatures.SyncResourceSpecification createHandlerResourceSpec(
            String type, String name, String description) {
        String uri = "nablarch://handler/" + type;
        return new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource(uri, name, description, "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown",
                    "# " + name + "\n\n"
                    + "[Phase 1 stub] Handler specifications for " + type
                    + " applications will be provided in Wave 2.")))
        );
    }

    private static McpServerFeatures.SyncResourceSpecification createGuideResourceSpec(
            String topic, String name, String description) {
        String uri = "nablarch://guide/" + topic;
        return new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource(uri, name, description, "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown",
                    "# " + name + "\n\n"
                    + "[Phase 1 stub] " + description
                    + " will be provided in Wave 2.")))
        );
    }

    private static McpServerFeatures.SyncPromptSpecification createPromptSpec(
            String name, String description, List<McpSchema.PromptArgument> arguments) {
        return new McpServerFeatures.SyncPromptSpecification(
            new McpSchema.Prompt(name, description, arguments),
            (exchange, request) -> {
                String argSummary = request.arguments() != null
                    ? request.arguments().toString() : "none";
                return new McpSchema.GetPromptResult(
                    description,
                    List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(
                            "[Phase 1 stub] Prompt: " + name
                            + " (args: " + argSummary + ")"
                            + "\n\nFull prompt template will be provided in Wave 2.")))
                );
            }
        );
    }

    private static McpSchema.PromptArgument arg(String name, String description, boolean required) {
        return new McpSchema.PromptArgument(name, description, required);
    }
}

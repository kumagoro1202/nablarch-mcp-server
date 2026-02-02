package com.tis.nablarch.mcp.config;

import com.tis.nablarch.mcp.prompts.BestPracticesPrompt;
import com.tis.nablarch.mcp.prompts.CreateActionPrompt;
import com.tis.nablarch.mcp.prompts.ExplainHandlerPrompt;
import com.tis.nablarch.mcp.prompts.MigrationGuidePrompt;
import com.tis.nablarch.mcp.prompts.ReviewConfigPrompt;
import com.tis.nablarch.mcp.prompts.SetupHandlerQueuePrompt;
import com.tis.nablarch.mcp.resources.GuideResourceProvider;
import com.tis.nablarch.mcp.resources.HandlerResourceProvider;
import com.tis.nablarch.mcp.tools.SearchApiTool;
import com.tis.nablarch.mcp.tools.ValidateHandlerQueueTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCPサーバ構成クラス。
 *
 * <p>MCPツール、リソース、プロンプトをサーバに登録する。
 * Phase 1ではSTIOトランスポートを使用する。</p>
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
     * NablarchハンドラカタログおよびガイドのMCPリソースを登録する。
     *
     * <p>6種のハンドラリソースと6種のガイドリソース（計12リソース）を登録する。</p>
     *
     * @param handlerProvider ハンドラリソースプロバイダ
     * @param guideProvider ガイドリソースプロバイダ
     * @return MCPサーバ自動構成用のリソース仕様リスト
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
            HandlerResourceProvider handlerProvider,
            GuideResourceProvider guideProvider) {
        return List.of(
            createHandlerResourceSpec("web", "Nablarch Web Handler Catalog",
                "Web application handler specifications and ordering constraints",
                handlerProvider),
            createHandlerResourceSpec("rest", "Nablarch REST Handler Catalog",
                "REST application handler specifications and ordering constraints",
                handlerProvider),
            createHandlerResourceSpec("batch", "Nablarch Batch Handler Catalog",
                "Batch application handler specifications and ordering constraints",
                handlerProvider),
            createHandlerResourceSpec("messaging", "Nablarch Messaging Handler Catalog",
                "Messaging application handler specifications and ordering constraints",
                handlerProvider),
            createHandlerResourceSpec("http-messaging",
                "Nablarch HTTP Messaging Handler Catalog",
                "HTTP messaging handler specifications and ordering constraints",
                handlerProvider),
            createHandlerResourceSpec("jakarta-batch",
                "Nablarch Jakarta Batch Handler Catalog",
                "Jakarta Batch handler specifications and ordering constraints",
                handlerProvider),
            createGuideResourceSpec("setup", "Nablarch Setup Guide",
                "Nablarch project setup and configuration guide", guideProvider),
            createGuideResourceSpec("testing", "Nablarch Testing Guide",
                "Nablarch testing patterns and best practices guide", guideProvider),
            createGuideResourceSpec("validation", "Nablarch Validation Guide",
                "Nablarch validation patterns and design guide", guideProvider),
            createGuideResourceSpec("database", "Nablarch Database Guide",
                "Nablarch database access patterns and configuration guide",
                guideProvider),
            createGuideResourceSpec("handler-queue",
                "Nablarch Handler Queue Guide",
                "Nablarch handler queue architecture and configuration guide",
                guideProvider),
            createGuideResourceSpec("error-handling",
                "Nablarch Error Handling Guide",
                "Nablarch common errors and troubleshooting guide", guideProvider)
        );
    }

    /**
     * Nablarch開発支援用のMCP Promptを登録する。
     *
     * <p>6種のPromptテンプレートを登録し、各Promptクラスに処理を委譲する。</p>
     *
     * @param setupHandlerQueuePrompt ハンドラキュー構成Prompt
     * @param createActionPrompt アクションクラス生成Prompt
     * @param reviewConfigPrompt XML設定レビューPrompt
     * @param explainHandlerPrompt ハンドラ説明Prompt
     * @param migrationGuidePrompt 移行ガイドPrompt
     * @param bestPracticesPrompt ベストプラクティスPrompt
     * @return MCP Server自動構成用のPrompt仕様リスト
     */
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> nablarchPrompts(
            SetupHandlerQueuePrompt setupHandlerQueuePrompt,
            CreateActionPrompt createActionPrompt,
            ReviewConfigPrompt reviewConfigPrompt,
            ExplainHandlerPrompt explainHandlerPrompt,
            MigrationGuidePrompt migrationGuidePrompt,
            BestPracticesPrompt bestPracticesPrompt) {
        return List.of(
            promptSpec("setup-handler-queue",
                "Set up a Nablarch handler queue configuration",
                List.of(arg("app_type", "Application type: web, rest, batch, messaging", true)),
                setupHandlerQueuePrompt::execute),
            promptSpec("create-action",
                "Generate a Nablarch action class skeleton",
                List.of(
                    arg("app_type", "Application type: web, rest, batch, messaging", true),
                    arg("action_name", "Name of the action class to generate", true)),
                createActionPrompt::execute),
            promptSpec("review-config",
                "Review a Nablarch XML configuration file for correctness",
                List.of(arg("config_xml", "XML configuration content to review", true)),
                reviewConfigPrompt::execute),
            promptSpec("explain-handler",
                "Get a detailed explanation of a Nablarch handler",
                List.of(arg("handler_name", "Name of the handler to explain", true)),
                explainHandlerPrompt::execute),
            promptSpec("migration-guide",
                "Get a migration guide between Nablarch versions",
                List.of(
                    arg("from_version", "Source Nablarch version", true),
                    arg("to_version", "Target Nablarch version", true)),
                migrationGuidePrompt::execute),
            promptSpec("best-practices",
                "Get Nablarch best practices for a specific topic",
                List.of(arg("topic", "Topic: handler-queue, action, validation, database, testing", true)),
                bestPracticesPrompt::execute)
        );
    }

    private static McpServerFeatures.SyncResourceSpecification createHandlerResourceSpec(
            String type, String name, String description,
            HandlerResourceProvider provider) {
        String uri = "nablarch://handler/" + type;
        return new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource(uri, name, description, "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown",
                    provider.getHandlerMarkdown(type))))
        );
    }

    private static McpServerFeatures.SyncResourceSpecification createGuideResourceSpec(
            String topic, String name, String description,
            GuideResourceProvider provider) {
        String uri = "nablarch://guide/" + topic;
        return new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource(uri, name, description, "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown",
                    provider.getGuideMarkdown(topic))))
        );
    }

    /**
     * Prompt仕様を生成するヘルパーメソッド。
     *
     * @param name Prompt名
     * @param description Promptの説明
     * @param arguments 引数定義のリスト
     * @param handler 実行処理を担うPromptクラスのexecuteメソッド参照
     * @return MCP Prompt仕様
     */
    private static McpServerFeatures.SyncPromptSpecification promptSpec(
            String name, String description, List<McpSchema.PromptArgument> arguments,
            Function<Map<String, String>, McpSchema.GetPromptResult> handler) {
        return new McpServerFeatures.SyncPromptSpecification(
            new McpSchema.Prompt(name, description, arguments),
            (exchange, request) -> {
                Map<String, String> args = new java.util.HashMap<>();
                if (request.arguments() != null) {
                    request.arguments().forEach((k, v) -> args.put(k, v != null ? v.toString() : null));
                }
                return handler.apply(args);
            }
        );
    }

    /**
     * Prompt引数定義を生成するヘルパーメソッド。
     *
     * @param name 引数名
     * @param description 引数の説明
     * @param required 必須フラグ
     * @return MCP Prompt引数
     */
    private static McpSchema.PromptArgument arg(String name, String description, boolean required) {
        return new McpSchema.PromptArgument(name, description, required);
    }
}

package com.tis.nablarch.mcp.config;

import com.tis.nablarch.mcp.prompts.BestPracticesPrompt;
import com.tis.nablarch.mcp.prompts.CreateActionPrompt;
import com.tis.nablarch.mcp.prompts.ExplainHandlerPrompt;
import com.tis.nablarch.mcp.prompts.MigrationGuidePrompt;
import com.tis.nablarch.mcp.prompts.ReviewConfigPrompt;
import com.tis.nablarch.mcp.prompts.SetupHandlerQueuePrompt;
import com.tis.nablarch.mcp.resources.AntipatternResourceProvider;
import com.tis.nablarch.mcp.resources.ApiResourceProvider;
import com.tis.nablarch.mcp.resources.ConfigResourceProvider;
import com.tis.nablarch.mcp.resources.ExampleResourceProvider;
import com.tis.nablarch.mcp.resources.GuideResourceProvider;
import com.tis.nablarch.mcp.resources.HandlerResourceProvider;
import com.tis.nablarch.mcp.resources.PatternResourceProvider;
import com.tis.nablarch.mcp.resources.VersionResourceProvider;
import com.tis.nablarch.mcp.tools.DesignHandlerQueueTool;
import com.tis.nablarch.mcp.tools.SearchApiTool;
import com.tis.nablarch.mcp.tools.SemanticSearchTool;
import com.tis.nablarch.mcp.tools.ValidateHandlerQueueTool;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * MCPサーバ構成クラス。
 *
 * <p>MCPツール、リソース、プロンプトをサーバに登録する。</p>
 */
@Configuration
public class McpServerConfig {

    /**
     * MCPツールをSpring AIツールコールバックとして登録する。
     */
    @Bean
    public ToolCallbackProvider nablarchTools(
            SearchApiTool searchApiTool,
            ValidateHandlerQueueTool validateHandlerQueueTool,
            SemanticSearchTool semanticSearchTool,
            DesignHandlerQueueTool designHandlerQueueTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(searchApiTool, validateHandlerQueueTool,
                        semanticSearchTool, designHandlerQueueTool)
                .build();
    }

    /**
     * NablarchのMCPリソースを登録する。
     *
     * <p>Phase 1: handler (6) + guide (6) = 12リソース</p>
     * <p>Phase 3: api + pattern + example + config + antipattern + version</p>
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
            HandlerResourceProvider handlerProvider,
            GuideResourceProvider guideProvider,
            ApiResourceProvider apiProvider,
            PatternResourceProvider patternProvider,
            ExampleResourceProvider exampleProvider,
            ConfigResourceProvider configProvider,
            AntipatternResourceProvider antipatternProvider,
            VersionResourceProvider versionProvider) {

        List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();

        // Phase 1: handler (6)
        resources.add(createHandlerResourceSpec("web", "Nablarch Web Handler Catalog",
            "Webアプリケーション用ハンドラキュー仕様", handlerProvider));
        resources.add(createHandlerResourceSpec("rest", "Nablarch REST Handler Catalog",
            "RESTアプリケーション用ハンドラキュー仕様", handlerProvider));
        resources.add(createHandlerResourceSpec("batch", "Nablarch Batch Handler Catalog",
            "バッチアプリケーション用ハンドラキュー仕様", handlerProvider));
        resources.add(createHandlerResourceSpec("messaging", "Nablarch Messaging Handler Catalog",
            "メッセージングアプリケーション用ハンドラキュー仕様", handlerProvider));
        resources.add(createHandlerResourceSpec("http-messaging",
            "Nablarch HTTP Messaging Handler Catalog",
            "HTTPメッセージング用ハンドラキュー仕様", handlerProvider));
        resources.add(createHandlerResourceSpec("jakarta-batch",
            "Nablarch Jakarta Batch Handler Catalog",
            "Jakarta Batch用ハンドラキュー仕様", handlerProvider));

        // Phase 1: guide (6)
        resources.add(createGuideResourceSpec("setup", "Nablarch Setup Guide",
            "Nablarchプロジェクト設定ガイド", guideProvider));
        resources.add(createGuideResourceSpec("testing", "Nablarch Testing Guide",
            "Nablarchテストパターンガイド", guideProvider));
        resources.add(createGuideResourceSpec("validation", "Nablarch Validation Guide",
            "Nablarchバリデーションガイド", guideProvider));
        resources.add(createGuideResourceSpec("database", "Nablarch Database Guide",
            "Nablarchデータベースアクセスガイド", guideProvider));
        resources.add(createGuideResourceSpec("handler-queue", "Nablarch Handler Queue Guide",
            "Nablarchハンドラキューガイド", guideProvider));
        resources.add(createGuideResourceSpec("error-handling", "Nablarch Error Handling Guide",
            "Nablarchエラーハンドリングガイド", guideProvider));

        // Phase 3: api (モジュール一覧)
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://api", "Nablarch API Module List",
                "Nablarchモジュール一覧", "application/json", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "application/json", apiProvider.getModuleList())))
        ));

        // Phase 3: pattern (一覧)
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://pattern", "Nablarch Design Pattern Catalog",
                "Nablarch設計パターンカタログ", "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown", patternProvider.getPatternList())))
        ));

        // Phase 3: example (一覧)
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://example", "Nablarch Example Catalog",
                "Nablarchサンプルアプリケーション一覧", "application/json", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "application/json", exampleProvider.getExampleList())))
        ));

        // Phase 3: config (一覧)
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://config", "Nablarch Config Template Catalog",
                "Nablarch XML設定テンプレート一覧", "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown", configProvider.getTemplateList())))
        ));

        // Phase 3: antipattern (一覧)
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://antipattern", "Nablarch Antipattern Catalog",
                "Nablarchアンチパターンカタログ", "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "text/markdown", antipatternProvider.getAntipatternList())))
        ));

        // Phase 3: version
        resources.add(new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource("nablarch://version", "Nablarch Version Info",
                "Nablarchバージョン情報", "application/json", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(
                    request.uri(), "application/json", versionProvider.getVersionInfo())))
        ));

        // 各リソースの個別エンドポイントを動的に登録
        for (String moduleKey : apiProvider.getValidModuleKeys()) {
            final String mk = moduleKey;
            resources.add(new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("nablarch://api/" + moduleKey, "API: " + moduleKey,
                    moduleKey + "モジュールのクラス一覧", "application/json", null),
                (exchange, request) -> new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), "application/json", apiProvider.getClassList(mk))))
            ));
        }

        for (String name : patternProvider.getValidPatternNames()) {
            final String n = name;
            resources.add(new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("nablarch://pattern/" + name, "Pattern: " + name,
                    name + "パターン", "text/markdown", null),
                (exchange, request) -> new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), "text/markdown", patternProvider.getPatternDetail(n))))
            ));
        }

        for (String type : exampleProvider.getValidExampleTypes()) {
            final String t = type;
            resources.add(new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("nablarch://example/" + type, "Example: " + type,
                    type + "サンプル", "application/json", null),
                (exchange, request) -> new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), "application/json", exampleProvider.getExampleDetail(t))))
            ));
        }

        for (String name : configProvider.getValidTemplateNames()) {
            final String n = name;
            resources.add(new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("nablarch://config/" + name, "Config: " + name,
                    name + "設定テンプレート", "application/xml", null),
                (exchange, request) -> new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), "application/xml", configProvider.getTemplate(n))))
            ));
        }

        for (String name : antipatternProvider.getValidAntipatternNames()) {
            final String n = name;
            resources.add(new McpServerFeatures.SyncResourceSpecification(
                new McpSchema.Resource("nablarch://antipattern/" + name, "Antipattern: " + name,
                    name + "アンチパターン", "text/markdown", null),
                (exchange, request) -> new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                        request.uri(), "text/markdown", antipatternProvider.getAntipatternDetail(n))))
            ));
        }

        return resources;
    }

    /**
     * Nablarch開発支援用のMCP Promptを登録する。
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

    private static McpSchema.PromptArgument arg(String name, String description, boolean required) {
        return new McpSchema.PromptArgument(name, description, required);
    }
}

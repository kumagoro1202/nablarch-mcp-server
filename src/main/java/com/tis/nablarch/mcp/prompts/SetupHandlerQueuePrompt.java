package com.tis.nablarch.mcp.prompts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * setup-handler-queue Promptの実装。
 *
 * <p>指定されたアプリケーションタイプに応じたNablarchハンドラキュー構成ガイドを生成する。
 * handler-catalog.yaml、handler-constraints.yaml、config-templates.yaml から
 * 知識を読み込み、Markdown形式のセットアップガイドを出力する。</p>
 */
@Component
public class SetupHandlerQueuePrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> VALID_APP_TYPES = List.of("web", "rest", "batch", "messaging");

    private Map<String, Object> handlerCatalog;
    private Map<String, Object> constraintsData;
    private Map<String, Object> configTemplates;

    /**
     * 知識YAMLファイルを読み込んで初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/knowledge/handler-catalog.yaml")) {
            handlerCatalog = YAML.readValue(is, Map.class);
        }
        try (InputStream is = getClass().getResourceAsStream("/knowledge/handler-constraints.yaml")) {
            constraintsData = YAML.readValue(is, Map.class);
        }
        try (InputStream is = getClass().getResourceAsStream("/knowledge/config-templates.yaml")) {
            configTemplates = YAML.readValue(is, Map.class);
        }
    }

    /**
     * Promptを実行してハンドラキュー構成ガイドを生成する。
     *
     * @param arguments MCP Promptの引数マップ（app_type必須）
     * @return ハンドラキュー構成ガイドを含むPrompt結果
     * @throws IllegalArgumentException app_typeが未指定または不正な場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String appType = arguments != null ? arguments.get("app_type") : null;
        if (appType == null || appType.isBlank()) {
            throw new IllegalArgumentException("app_type は必須です。指定可能な値: " + VALID_APP_TYPES);
        }
        if (!VALID_APP_TYPES.contains(appType)) {
            throw new IllegalArgumentException("不正な app_type: " + appType + "。指定可能な値: " + VALID_APP_TYPES);
        }

        var sb = new StringBuilder();
        sb.append("# ").append(appType).append(" アプリケーションのハンドラキュー構成ガイド\n\n");

        // ハンドラ一覧を展開
        Map<String, Object> typeData = (Map<String, Object>) handlerCatalog.get(appType);
        if (typeData != null) {
            String desc = (String) typeData.get("description");
            sb.append("## 概要\n\n").append(desc).append("\n\n");

            List<Map<String, Object>> handlers = (List<Map<String, Object>>) typeData.get("handlers");
            if (handlers != null && !handlers.isEmpty()) {
                sb.append("## 推奨ハンドラキュー\n\n");
                sb.append("| 順序 | ハンドラ | FQCN | 必須 | スレッド | 説明 |\n");
                sb.append("|------|---------|------|------|---------|------|\n");
                for (var h : handlers) {
                    sb.append("| ").append(h.get("order"))
                      .append(" | ").append(h.get("name"))
                      .append(" | `").append(h.get("fqcn")).append("`")
                      .append(" | ").append(Boolean.TRUE.equals(h.get("required")) ? "はい" : "いいえ")
                      .append(" | ").append(h.get("thread"))
                      .append(" | ").append(h.get("description"))
                      .append(" |\n");
                }
                sb.append("\n");
            }
        }

        // 順序制約を展開
        List<Map<String, Object>> constraints = (List<Map<String, Object>>) constraintsData.get("constraints");
        if (constraints != null) {
            sb.append("## 順序制約\n\n");
            for (var c : constraints) {
                List<String> requiredBy = (List<String>) c.get("required_by_app_type");
                if (requiredBy != null && requiredBy.contains(appType)) {
                    sb.append("### ").append(c.get("handler")).append("\n\n");
                    sb.append("- **FQCN**: `").append(c.get("fqcn")).append("`\n");
                    sb.append("- **ルール**: ").append(c.get("rule")).append("\n");
                    if (c.get("must_before") != null) {
                        sb.append("- **前に配置**: ").append(c.get("must_before")).append("\n");
                    }
                    if (c.get("must_after") != null) {
                        sb.append("- **後に配置**: ").append(c.get("must_after")).append("\n");
                    }
                    sb.append("- **理由**: ").append(c.get("reason")).append("\n\n");
                }
            }
        }

        // XML設定テンプレートを展開
        List<Map<String, Object>> templates = (List<Map<String, Object>>) configTemplates.get("templates");
        if (templates != null) {
            sb.append("## XML設定テンプレート\n\n");
            for (var t : templates) {
                String tAppType = (String) t.get("app_type");
                if (appType.equals(tAppType)) {
                    sb.append("### ").append(t.get("name")).append("\n\n");
                    sb.append(t.get("description")).append("\n\n");
                    sb.append("```xml\n").append(t.get("template")).append("```\n\n");
                }
            }
        }

        return new McpSchema.GetPromptResult(
            appType + " アプリケーションのハンドラキュー構成ガイド",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}

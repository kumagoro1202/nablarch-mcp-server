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
 * create-action Promptの実装。
 *
 * <p>指定されたアプリケーションタイプとアクション名に基づいて、
 * Nablarchアクションクラスのスケルトンコード生成ガイドを出力する。
 * api-patterns.yaml からアプリタイプ別のパターンを読み込む。</p>
 */
@Component
public class CreateActionPrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> VALID_APP_TYPES = List.of("web", "rest", "batch", "messaging");

    private Map<String, Object> apiPatterns;

    /**
     * 知識YAMLファイルを読み込んで初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/knowledge/api-patterns.yaml")) {
            apiPatterns = YAML.readValue(is, Map.class);
        }
    }

    /**
     * Promptを実行してアクションクラス生成ガイドを出力する。
     *
     * @param arguments MCP Promptの引数マップ（app_type, action_name 必須）
     * @return アクションクラス生成ガイドを含むPrompt結果
     * @throws IllegalArgumentException 必須引数が未指定または不正な場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String appType = arguments != null ? arguments.get("app_type") : null;
        String actionName = arguments != null ? arguments.get("action_name") : null;

        if (appType == null || appType.isBlank()) {
            throw new IllegalArgumentException("app_type は必須です。指定可能な値: " + VALID_APP_TYPES);
        }
        if (!VALID_APP_TYPES.contains(appType)) {
            throw new IllegalArgumentException("不正な app_type: " + appType + "。指定可能な値: " + VALID_APP_TYPES);
        }
        if (actionName == null || actionName.isBlank()) {
            throw new IllegalArgumentException("action_name は必須です。");
        }

        var sb = new StringBuilder();
        sb.append("# ").append(actionName).append(" アクションクラス生成ガイド (").append(appType).append(")\n\n");
        sb.append("以下のガイドに従って、Nablarchの ").append(appType)
          .append(" アプリケーション向けアクションクラス `").append(actionName).append("` を作成してください。\n\n");

        // app_typeに一致するパターンを収集
        List<Map<String, Object>> patterns = (List<Map<String, Object>>) apiPatterns.get("patterns");
        if (patterns != null) {
            sb.append("## 推奨パターン\n\n");
            boolean found = false;
            for (var p : patterns) {
                String category = (String) p.get("category");
                if (appType.equals(category)) {
                    found = true;
                    sb.append("### ").append(p.get("name")).append("\n\n");
                    sb.append("**説明**: ").append(p.get("description")).append("\n\n");
                    if (p.get("fqcn") != null) {
                        sb.append("**関連FQCN**: `").append(p.get("fqcn")).append("`\n\n");
                    }
                    if (p.get("example") != null) {
                        sb.append("**コード例**:\n\n```java\n").append(p.get("example")).append("```\n\n");
                    }
                    List<String> related = (List<String>) p.get("related_patterns");
                    if (related != null && !related.isEmpty()) {
                        sb.append("**関連パターン**: ").append(String.join(", ", related)).append("\n\n");
                    }
                }
            }
            if (!found) {
                sb.append("指定されたアプリケーションタイプ `").append(appType)
                  .append("` に直接対応するパターンは見つかりませんでした。\n\n");
            }
        }

        sb.append("## アクションクラスの命名規則\n\n");
        sb.append("- クラス名: `").append(actionName).append("`\n");
        sb.append("- パッケージ: `{プロジェクトパッケージ}.action`\n");
        sb.append("- アクションメソッド名は `do` プレフィックスを使用（例: `doSearch`, `doRegister`）\n\n");

        return new McpSchema.GetPromptResult(
            actionName + " アクションクラス生成ガイド (" + appType + ")",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}

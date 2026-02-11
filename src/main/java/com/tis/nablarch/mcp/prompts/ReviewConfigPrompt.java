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
 * review-config Promptの実装。
 *
 * <p>NablarchのXML設定ファイルをレビューするための観点・チェックポイントを提示する。
 * handler-constraints.yaml の制約ルールと handler-catalog.yaml のハンドラ情報を基に、
 * よくある問題パターンと確認事項を出力する。</p>
 */
@Component
public class ReviewConfigPrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> constraintsData;
    private Map<String, Object> errorCatalog;

    /**
     * 知識YAMLファイルを読み込んで初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/knowledge/handler-constraints.yaml")) {
            constraintsData = YAML.readValue(is, Map.class);
        }
        try (InputStream is = getClass().getResourceAsStream("/knowledge/error-catalog.yaml")) {
            errorCatalog = YAML.readValue(is, Map.class);
        }
    }

    /**
     * Promptを実行してXML設定レビュー観点を生成する。
     *
     * @param arguments MCP Promptの引数マップ（config_xml 必須）
     * @return XML設定レビュー観点を含むPrompt結果
     * @throws IllegalArgumentException config_xmlが未指定の場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String configXml = arguments != null ? arguments.get("config_xml") : null;
        if (configXml == null || configXml.isBlank()) {
            throw new IllegalArgumentException("config_xml は必須です。レビュー対象のXML設定内容を指定してください。");
        }

        var sb = new StringBuilder();
        sb.append("# Nablarch XML設定レビュー\n\n");

        // レビュー対象の表示（先頭500文字まで）
        sb.append("## レビュー対象\n\n");
        sb.append("```xml\n");
        if (configXml.length() > 500) {
            sb.append(configXml, 0, 500).append("\n... (以下省略)\n");
        } else {
            sb.append(configXml).append("\n");
        }
        sb.append("```\n\n");

        // ハンドラ順序制約チェック
        sb.append("## チェックポイント: ハンドラ順序制約\n\n");
        sb.append("以下の制約ルールに基づいて、設定ファイル内のハンドラ順序を確認してください。\n\n");

        List<Map<String, Object>> constraints = (List<Map<String, Object>>) constraintsData.get("constraints");
        if (constraints != null) {
            for (var c : constraints) {
                sb.append("### ").append(c.get("handler")).append("\n\n");
                sb.append("- **FQCN**: `").append(c.get("fqcn")).append("`\n");
                sb.append("- **ルール**: ").append(c.get("rule")).append("\n");
                if (c.get("must_before") != null) {
                    sb.append("- **この前に配置すべきハンドラ**: ").append(c.get("must_before")).append("\n");
                }
                if (c.get("must_after") != null) {
                    sb.append("- **この後に配置すべきハンドラ**: ").append(c.get("must_after")).append("\n");
                }
                sb.append("- **理由**: ").append(c.get("reason")).append("\n\n");
            }
        }

        // よくある問題パターン
        sb.append("## よくある問題パターン\n\n");
        List<Map<String, Object>> errors = (List<Map<String, Object>>) errorCatalog.get("errors");
        if (errors != null) {
            for (var e : errors) {
                String category = (String) e.get("category");
                if ("handler".equals(category) || "config".equals(category)) {
                    sb.append("### ").append(e.get("id")).append(": ").append(e.get("error_message")).append("\n\n");
                    sb.append("**原因**: ").append(e.get("cause")).append("\n\n");
                    sb.append("**解決策**:\n").append(e.get("solution")).append("\n");
                }
            }
        }

        // 一般的な確認事項
        sb.append("## 一般的な確認事項\n\n");
        sb.append("1. 全ての必須ハンドラ（`required: true`）が含まれているか\n");
        sb.append("2. ハンドラのFQCNが正しいか（タイプミスに注意）\n");
        sb.append("3. コンポーネント定義のproperty名とvalue型が一致しているか\n");
        sb.append("4. データソース設定が環境に合っているか\n");
        sb.append("5. 文字エンコーディング設定が統一されているか\n\n");

        return new McpSchema.GetPromptResult(
            "Nablarch XML設定レビュー",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}

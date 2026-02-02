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
 * best-practices Promptの実装。
 *
 * <p>指定されたトピックに関するNablarchのベストプラクティスを提供する。
 * design-patterns.yaml から設計パターンを、api-patterns.yaml から
 * 実装パターンを読み込み、トピック別のガイダンスを生成する。</p>
 */
@Component
public class BestPracticesPrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> VALID_TOPICS = List.of(
        "handler-queue", "action", "validation", "database", "testing"
    );

    /** トピックからdesign-patternsのカテゴリへのマッピング。 */
    private static final Map<String, List<String>> TOPIC_TO_CATEGORIES = Map.of(
        "handler-queue", List.of("architecture"),
        "action", List.of("action"),
        "validation", List.of("validation"),
        "database", List.of("database"),
        "testing", List.of("testing")
    );

    /** トピックからapi-patternsのカテゴリへのマッピング。 */
    private static final Map<String, List<String>> TOPIC_TO_API_CATEGORIES = Map.of(
        "handler-queue", List.of(),
        "action", List.of("web", "rest", "batch"),
        "validation", List.of("web"),
        "database", List.of("web", "batch"),
        "testing", List.of()
    );

    private Map<String, Object> designPatterns;
    private Map<String, Object> apiPatterns;

    /**
     * 知識YAMLファイルを読み込んで初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/knowledge/design-patterns.yaml")) {
            designPatterns = YAML.readValue(is, Map.class);
        }
        try (InputStream is = getClass().getResourceAsStream("/knowledge/api-patterns.yaml")) {
            apiPatterns = YAML.readValue(is, Map.class);
        }
    }

    /**
     * Promptを実行してベストプラクティスガイドを生成する。
     *
     * @param arguments MCP Promptの引数マップ（topic 必須）
     * @return ベストプラクティスガイドを含むPrompt結果
     * @throws IllegalArgumentException topicが未指定または不正な場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String topic = arguments != null ? arguments.get("topic") : null;
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic は必須です。指定可能な値: " + VALID_TOPICS);
        }
        if (!VALID_TOPICS.contains(topic)) {
            throw new IllegalArgumentException("不正な topic: " + topic + "。指定可能な値: " + VALID_TOPICS);
        }

        var sb = new StringBuilder();
        sb.append("# Nablarch ベストプラクティス: ").append(topic).append("\n\n");

        // 設計パターンの展開
        List<String> dpCategories = TOPIC_TO_CATEGORIES.getOrDefault(topic, List.of());
        List<Map<String, Object>> dpPatterns = (List<Map<String, Object>>) designPatterns.get("patterns");
        if (dpPatterns != null && !dpCategories.isEmpty()) {
            sb.append("## 設計パターン\n\n");
            for (var p : dpPatterns) {
                String category = (String) p.get("category");
                if (dpCategories.contains(category)) {
                    sb.append("### ").append(p.get("name")).append("\n\n");
                    sb.append("**説明**: ").append(p.get("description")).append("\n\n");
                    if (p.get("problem") != null) {
                        sb.append("**課題**:\n").append(p.get("problem")).append("\n\n");
                    }
                    if (p.get("solution") != null) {
                        sb.append("**解決策**:\n").append(p.get("solution")).append("\n");
                    }
                    if (p.get("structure") != null) {
                        sb.append("**構造**:\n```\n").append(p.get("structure")).append("```\n\n");
                    }
                    if (p.get("code_example") != null) {
                        sb.append("**コード例**:\n```java\n").append(p.get("code_example")).append("```\n\n");
                    }
                    List<String> refs = (List<String>) p.get("references");
                    if (refs != null && !refs.isEmpty()) {
                        sb.append("**参考**: ").append(String.join(", ", refs)).append("\n\n");
                    }
                }
            }
        }

        // APIパターンの展開
        List<String> apiCategories = TOPIC_TO_API_CATEGORIES.getOrDefault(topic, List.of());
        List<Map<String, Object>> apPatterns = (List<Map<String, Object>>) apiPatterns.get("patterns");
        if (apPatterns != null && !apiCategories.isEmpty()) {
            sb.append("## 推奨実装パターン\n\n");
            for (var p : apPatterns) {
                String category = (String) p.get("category");
                if (apiCategories.contains(category)) {
                    sb.append("### ").append(p.get("name")).append(" (").append(category).append(")\n\n");
                    sb.append("**説明**: ").append(p.get("description")).append("\n\n");
                    if (p.get("fqcn") != null) {
                        sb.append("**関連FQCN**: `").append(p.get("fqcn")).append("`\n\n");
                    }
                    if (p.get("example") != null) {
                        sb.append("**コード例**:\n```java\n").append(p.get("example")).append("```\n\n");
                    }
                }
            }
        }

        // 一般的なベストプラクティス
        sb.append("## 一般的な注意事項\n\n");
        sb.append("- Nablarchの公式ドキュメントとサンプルプロジェクトを参照すること\n");
        sb.append("- ハンドラキューの順序制約を遵守すること\n");
        sb.append("- コンポーネント定義はXML形式で管理し、環境差分はenv.configで吸収すること\n");
        sb.append("- テストはnablarch-testing-junit5を使用すること\n\n");

        return new McpSchema.GetPromptResult(
            "Nablarch ベストプラクティス: " + topic,
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}

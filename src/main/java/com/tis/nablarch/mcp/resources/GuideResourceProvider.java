package com.tis.nablarch.mcp.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * guide/{topic} リソースプロバイダ。
 *
 * <p>各種ナレッジYAMLファイルを読み込み、
 * トピック別の開発ガイドをMarkdown形式で提供する。</p>
 */
@Component
public class GuideResourceProvider {

    private static final Set<String> VALID_TOPICS = Set.of(
            "setup", "testing", "validation", "database", "handler-queue", "error-handling");

    private List<Map<String, Object>> apiPatterns;
    private List<Map<String, Object>> configTemplates;
    private List<Map<String, Object>> errors;
    private List<Map<String, Object>> designPatterns;
    private Map<String, Object> handlerCatalog;
    private List<Map<String, Object>> handlerConstraints;

    /**
     * ナレッジYAMLファイルを読み込み初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};

        apiPatterns = loadList(mapper, "knowledge/api-patterns.yaml", "patterns", mapType);
        configTemplates = loadList(mapper, "knowledge/config-templates.yaml", "templates", mapType);
        errors = loadList(mapper, "knowledge/error-catalog.yaml", "errors", mapType);
        designPatterns = loadList(mapper, "knowledge/design-patterns.yaml", "patterns", mapType);

        try (InputStream is = loadResource("knowledge/handler-catalog.yaml")) {
            handlerCatalog = mapper.readValue(is, mapType);
        }

        handlerConstraints = loadList(mapper, "knowledge/handler-constraints.yaml",
                "constraints", mapType);
    }

    /**
     * 指定されたトピックの開発ガイドをMarkdown形式で返す。
     *
     * @param topic ガイドトピック（setup, testing, validation, database, handler-queue, error-handling）
     * @return Markdown形式の開発ガイド
     */
    public String getGuideMarkdown(String topic) {
        if (topic == null || !VALID_TOPICS.contains(topic)) {
            return "# Unknown Guide Topic\n\nUnknown guide topic: " + topic
                    + "\n\nValid topics: " + String.join(", ", VALID_TOPICS);
        }
        return switch (topic) {
            case "setup" -> buildSetupGuide();
            case "testing" -> buildTestingGuide();
            case "validation" -> buildValidationGuide();
            case "database" -> buildDatabaseGuide();
            case "handler-queue" -> buildHandlerQueueGuide();
            case "error-handling" -> buildErrorHandlingGuide();
            default -> "# Unknown Guide Topic\n\nUnknown guide topic: " + topic;
        };
    }

    private String buildSetupGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Setup Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarchプロジェクトの初期構築に必要な設定テンプレートとガイドです。\n\n");

        sb.append("## Configuration Templates\n\n");
        List<Map<String, Object>> templates = filterByNames(configTemplates,
                "web-xml", "web-component", "db-connection");
        for (Map<String, Object> template : templates) {
            appendTemplate(sb, template);
        }

        sb.append("---\n*Source: config-templates.yaml*\n");
        return sb.toString();
    }

    private String buildTestingGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Testing Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarchアプリケーションのテストパターンとベストプラクティスです。\n\n");

        sb.append("## Test Patterns\n\n");
        List<Map<String, Object>> patterns = filterByNames(apiPatterns,
                "request-unit-test", "excel-test-data");
        for (Map<String, Object> pattern : patterns) {
            appendApiPattern(sb, pattern);
        }

        sb.append("---\n*Source: api-patterns.yaml*\n");
        return sb.toString();
    }

    private String buildValidationGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Validation Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarchのバリデーション機能に関するパターンとガイドです。\n\n");

        sb.append("## API Patterns\n\n");
        List<Map<String, Object>> patterns = filterByNames(apiPatterns,
                "form-validation", "inject-form-on-error");
        for (Map<String, Object> pattern : patterns) {
            appendApiPattern(sb, pattern);
        }

        sb.append("## Design Patterns\n\n");
        List<Map<String, Object>> dPatterns = filterByNames(designPatterns,
                "form-validation-pattern");
        for (Map<String, Object> pattern : dPatterns) {
            appendDesignPattern(sb, pattern);
        }

        sb.append("---\n*Source: api-patterns.yaml, design-patterns.yaml*\n");
        return sb.toString();
    }

    private String buildDatabaseGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Database Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarchのデータベースアクセスに関するパターンと設定ガイドです。\n\n");

        sb.append("## API Patterns\n\n");
        List<Map<String, Object>> patterns = filterByNames(apiPatterns,
                "universal-dao", "sql-file", "entity-class", "exclusive-control");
        for (Map<String, Object> pattern : patterns) {
            appendApiPattern(sb, pattern);
        }

        sb.append("## Configuration\n\n");
        List<Map<String, Object>> templates = filterByNames(configTemplates,
                "db-connection");
        for (Map<String, Object> template : templates) {
            appendTemplate(sb, template);
        }

        sb.append("---\n*Source: api-patterns.yaml, config-templates.yaml*\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildHandlerQueueGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Handler Queue Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarchのハンドラキューアーキテクチャの概要と")
                .append("アプリケーションタイプ別の構成です。\n\n");

        sb.append("## Application Types\n\n");
        sb.append("| Type | Handlers | Description |\n");
        sb.append("|------|----------|-------------|\n");
        for (String appType : List.of("web", "rest", "batch",
                "messaging", "http-messaging", "jakarta-batch")) {
            Map<String, Object> appData =
                    (Map<String, Object>) handlerCatalog.get(appType);
            if (appData != null) {
                List<?> handlers = (List<?>) appData.get("handlers");
                sb.append("| ").append(appType)
                        .append(" | ").append(handlers != null ? handlers.size() : 0)
                        .append(" | ").append(appData.get("description"))
                        .append(" |\n");
            }
        }
        sb.append("\n");

        sb.append("## Key Ordering Constraints\n\n");
        sb.append("| Handler | Rule | Reason |\n");
        sb.append("|---------|------|--------|\n");
        for (Map<String, Object> constraint : handlerConstraints) {
            String name = (String) constraint.get("handler");
            String rule = (String) constraint.get("rule");
            String reason = (String) constraint.get("reason");
            if (reason != null) {
                reason = reason.replace("\n", " ").trim();
                if (reason.length() > 80) {
                    reason = reason.substring(0, 77) + "...";
                }
            }
            sb.append("| ").append(name).append(" | ").append(rule)
                    .append(" | ").append(reason != null ? reason : "")
                    .append(" |\n");
        }

        sb.append("\n---\n*Source: handler-catalog.yaml, handler-constraints.yaml*\n");
        return sb.toString();
    }

    private String buildErrorHandlingGuide() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch Error Handling Guide\n\n");
        sb.append("## Overview\n\n");
        sb.append("Nablarch開発でよく遭遇するエラーとその解決方法です。\n\n");

        Map<String, List<Map<String, Object>>> byCategory = new LinkedHashMap<>();
        for (Map<String, Object> error : errors) {
            String category = (String) error.get("category");
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(error);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : byCategory.entrySet()) {
            sb.append("## ").append(capitalize(entry.getKey())).append(" Errors\n\n");
            for (Map<String, Object> error : entry.getValue()) {
                appendError(sb, error);
            }
        }

        sb.append("---\n*Source: error-catalog.yaml*\n");
        return sb.toString();
    }

    // --- ヘルパーメソッド ---

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadList(ObjectMapper mapper, String path,
            String key, TypeReference<Map<String, Object>> mapType) throws IOException {
        try (InputStream is = loadResource(path)) {
            Map<String, Object> data = mapper.readValue(is, mapType);
            return (List<Map<String, Object>>) data.get(key);
        }
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private List<Map<String, Object>> filterByNames(
            List<Map<String, Object>> items, String... names) {
        Set<String> nameSet = Set.of(names);
        return items.stream()
                .filter(item -> nameSet.contains(item.get("name")))
                .toList();
    }

    private void appendApiPattern(StringBuilder sb, Map<String, Object> pattern) {
        sb.append("### ").append(pattern.get("name")).append("\n\n");
        sb.append(pattern.get("description")).append("\n\n");
        Object fqcn = pattern.get("fqcn");
        if (fqcn != null) {
            sb.append("- **FQCN**: `").append(fqcn).append("`\n\n");
        }
        Object example = pattern.get("example");
        if (example != null) {
            sb.append("```java\n").append(example).append("```\n\n");
        }
    }

    private void appendDesignPattern(StringBuilder sb, Map<String, Object> pattern) {
        sb.append("### ").append(pattern.get("name")).append("\n\n");
        sb.append(pattern.get("description")).append("\n\n");
        Object problem = pattern.get("problem");
        if (problem != null) {
            sb.append("**Problem**: ").append(problem).append("\n\n");
        }
        Object solution = pattern.get("solution");
        if (solution != null) {
            sb.append("**Solution**:\n").append(solution).append("\n");
        }
        Object codeExample = pattern.get("code_example");
        if (codeExample != null) {
            sb.append("```java\n").append(codeExample).append("```\n\n");
        }
    }

    private void appendTemplate(StringBuilder sb, Map<String, Object> template) {
        sb.append("### ").append(template.get("name")).append("\n\n");
        sb.append(template.get("description")).append("\n\n");
        Object content = template.get("template");
        if (content != null) {
            sb.append("```xml\n").append(content).append("```\n\n");
        }
    }

    private void appendError(StringBuilder sb, Map<String, Object> error) {
        sb.append("### ").append(error.get("id")).append(": ")
                .append(error.get("error_message")).append("\n\n");
        sb.append("- **Category**: ").append(error.get("category")).append("\n");
        sb.append("- **Severity**: ").append(error.get("severity")).append("\n");
        sb.append("- **Cause**: ").append(error.get("cause")).append("\n\n");
        Object solution = error.get("solution");
        if (solution != null) {
            sb.append("**Solution**:\n").append(solution).append("\n");
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

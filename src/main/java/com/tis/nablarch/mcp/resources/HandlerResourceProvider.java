package com.tis.nablarch.mcp.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * handler/{app_type} リソースプロバイダ。
 *
 * <p>ハンドラカタログYAMLとハンドラ制約YAMLを読み込み、
 * アプリケーションタイプ別のハンドラキュー仕様をMarkdown形式で提供する。</p>
 */
@Component
public class HandlerResourceProvider {

    private static final Set<String> VALID_APP_TYPES = Set.of(
            "web", "rest", "batch", "messaging", "http-messaging", "jakarta-batch");

    private Map<String, Object> handlerCatalog;
    private List<Map<String, Object>> constraints;

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

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("knowledge/handler-catalog.yaml")) {
            handlerCatalog = mapper.readValue(is, mapType);
        }

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("knowledge/handler-constraints.yaml")) {
            Map<String, Object> data = mapper.readValue(is, mapType);
            constraints = (List<Map<String, Object>>) data.get("constraints");
        }
    }

    /**
     * 指定されたアプリケーションタイプのハンドラキュー仕様をMarkdown形式で返す。
     *
     * @param appType アプリケーションタイプ（web, rest, batch, messaging, http-messaging, jakarta-batch）
     * @return Markdown形式のハンドラキュー仕様
     */
    @SuppressWarnings("unchecked")
    public String getHandlerMarkdown(String appType) {
        if (appType == null || !VALID_APP_TYPES.contains(appType)) {
            return "# Unknown Application Type\n\nUnknown application type: " + appType
                    + "\n\nValid types: " + String.join(", ", VALID_APP_TYPES);
        }

        Map<String, Object> appData = (Map<String, Object>) handlerCatalog.get(appType);
        if (appData == null) {
            return "# Unknown Application Type\n\nUnknown application type: " + appType;
        }

        String description = (String) appData.get("description");
        List<Map<String, Object>> handlers =
                (List<Map<String, Object>>) appData.get("handlers");

        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch ").append(formatAppType(appType))
                .append(" Application Handler Queue\n\n");
        sb.append(description).append("\n\n");
        sb.append("## Handler Queue (in order)\n\n");

        for (int i = 0; i < handlers.size(); i++) {
            appendHandler(sb, i + 1, handlers.get(i));
        }

        appendConstraintsSummary(sb, appType);

        sb.append("\n---\n*Source: handler-catalog.yaml, handler-constraints.yaml*\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void appendHandler(StringBuilder sb, int index, Map<String, Object> handler) {
        String name = (String) handler.get("name");
        String fqcn = (String) handler.get("fqcn");
        boolean required = Boolean.TRUE.equals(handler.get("required"));
        String thread = String.valueOf(handler.get("thread"));
        String desc = (String) handler.get("description");

        sb.append("### ").append(index).append(". ").append(name);
        sb.append(required ? " [Required]" : " [Optional]").append("\n");
        sb.append("- **FQCN**: `").append(fqcn).append("`\n");
        sb.append("- **Thread**: ").append(thread).append("\n");
        sb.append("- **Description**: ").append(desc).append("\n");

        Map<String, Object> handlerConstraints =
                (Map<String, Object>) handler.get("constraints");
        if (handlerConstraints != null) {
            sb.append("- **Constraints**:\n");
            appendConstraintList(sb, "Must be before",
                    (List<String>) handlerConstraints.get("must_before"));
            appendConstraintList(sb, "Must be after",
                    (List<String>) handlerConstraints.get("must_after"));
        }
        sb.append("\n");
    }

    private void appendConstraintList(StringBuilder sb, String label, List<String> items) {
        if (items != null && !items.isEmpty()) {
            sb.append("  - ").append(label).append(": ")
                    .append(String.join(", ", items)).append("\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendConstraintsSummary(StringBuilder sb, String appType) {
        sb.append("## Ordering Constraints Summary\n\n");
        sb.append("| Handler | Rule | Details |\n");
        sb.append("|---------|------|---------|\n");

        for (Map<String, Object> constraint : constraints) {
            List<String> requiredBy =
                    (List<String>) constraint.get("required_by_app_type");
            if (requiredBy != null && !requiredBy.contains(appType)) {
                continue;
            }
            String name = (String) constraint.get("handler");
            String rule = (String) constraint.get("rule");
            String reason = (String) constraint.get("reason");
            if (reason != null) {
                reason = reason.replace("\n", " ").trim();
                if (reason.length() > 100) {
                    reason = reason.substring(0, 97) + "...";
                }
            }
            sb.append("| ").append(name).append(" | ").append(rule)
                    .append(" | ").append(reason != null ? reason : "").append(" |\n");
        }
    }

    private static String formatAppType(String appType) {
        return switch (appType) {
            case "web" -> "Web";
            case "rest" -> "REST";
            case "batch" -> "Batch";
            case "messaging" -> "Messaging";
            case "http-messaging" -> "HTTP Messaging";
            case "jakarta-batch" -> "Jakarta Batch";
            default -> appType;
        };
    }
}

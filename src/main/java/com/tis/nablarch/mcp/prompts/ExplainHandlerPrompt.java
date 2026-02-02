package com.tis.nablarch.mcp.prompts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * explain-handler Promptの実装。
 *
 * <p>指定されたNablarchハンドラの詳細情報を提供する。
 * handler-catalog.yaml からハンドラの基本情報（FQCN、説明、順序等）を、
 * handler-constraints.yaml から順序制約情報を抽出して出力する。</p>
 */
@Component
public class ExplainHandlerPrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> handlerCatalog;
    private Map<String, Object> constraintsData;

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
    }

    /**
     * Promptを実行してハンドラの詳細説明を生成する。
     *
     * @param arguments MCP Promptの引数マップ（handler_name 必須）
     * @return ハンドラ詳細説明を含むPrompt結果
     * @throws IllegalArgumentException handler_nameが未指定の場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String handlerName = arguments != null ? arguments.get("handler_name") : null;
        if (handlerName == null || handlerName.isBlank()) {
            throw new IllegalArgumentException("handler_name は必須です。ハンドラ名を指定してください。");
        }

        var sb = new StringBuilder();
        sb.append("# ").append(handlerName).append(" の詳細説明\n\n");

        // 全アプリタイプからハンドラを検索
        Map<String, Object> foundHandler = null;
        List<String> foundInTypes = new ArrayList<>();

        for (String type : List.of("web", "rest", "batch", "messaging")) {
            Map<String, Object> typeData = (Map<String, Object>) handlerCatalog.get(type);
            if (typeData == null) continue;
            List<Map<String, Object>> handlers = (List<Map<String, Object>>) typeData.get("handlers");
            if (handlers == null) continue;
            for (var h : handlers) {
                String name = (String) h.get("name");
                if (handlerName.equalsIgnoreCase(name)) {
                    if (foundHandler == null) {
                        foundHandler = h;
                    }
                    foundInTypes.add(type);
                }
            }
        }

        if (foundHandler != null) {
            sb.append("## 基本情報\n\n");
            sb.append("| 項目 | 値 |\n|------|-----|\n");
            sb.append("| **名前** | ").append(foundHandler.get("name")).append(" |\n");
            sb.append("| **FQCN** | `").append(foundHandler.get("fqcn")).append("` |\n");
            sb.append("| **説明** | ").append(foundHandler.get("description")).append(" |\n");
            sb.append("| **スレッド** | ").append(foundHandler.get("thread")).append(" |\n");
            sb.append("| **必須** | ").append(Boolean.TRUE.equals(foundHandler.get("required")) ? "はい" : "いいえ").append(" |\n");
            sb.append("| **推奨順序** | ").append(foundHandler.get("order")).append(" |\n");
            sb.append("\n");

            sb.append("## 使用するアプリケーションタイプ\n\n");
            for (String type : foundInTypes) {
                sb.append("- ").append(type).append("\n");
            }
            sb.append("\n");

            // ハンドラに設定された制約
            Map<String, Object> handlerConstraints = (Map<String, Object>) foundHandler.get("constraints");
            if (handlerConstraints != null) {
                sb.append("## ハンドラ内制約\n\n");
                if (handlerConstraints.get("must_before") != null) {
                    sb.append("- **この前に配置すべき**: ").append(handlerConstraints.get("must_before")).append("\n");
                }
                if (handlerConstraints.get("must_after") != null) {
                    sb.append("- **この後に配置すべき**: ").append(handlerConstraints.get("must_after")).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("指定されたハンドラ `").append(handlerName).append("` はカタログに見つかりませんでした。\n\n");
            sb.append("ハンドラ名を確認してください。利用可能なハンドラは handler-catalog.yaml に定義されています。\n\n");
        }

        // handler-constraints.yaml から追加制約を検索
        List<Map<String, Object>> constraints = (List<Map<String, Object>>) constraintsData.get("constraints");
        if (constraints != null) {
            boolean constraintFound = false;
            for (var c : constraints) {
                String cName = (String) c.get("handler");
                if (handlerName.equalsIgnoreCase(cName)) {
                    if (!constraintFound) {
                        sb.append("## 順序制約（詳細）\n\n");
                        constraintFound = true;
                    }
                    sb.append("- **ルール**: ").append(c.get("rule")).append("\n");
                    if (c.get("must_before") != null) {
                        sb.append("- **前に配置すべきハンドラ**: ").append(c.get("must_before")).append("\n");
                    }
                    if (c.get("must_after") != null) {
                        sb.append("- **後に配置すべきハンドラ**: ").append(c.get("must_after")).append("\n");
                    }
                    sb.append("- **理由**: ").append(c.get("reason")).append("\n\n");
                }
            }
        }

        return new McpSchema.GetPromptResult(
            handlerName + " の詳細説明",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}

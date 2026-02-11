package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nablarchハンドラキュー設計ツール。
 *
 * <p>アプリケーションタイプと要件に基づいて、
 * 最適なハンドラキュー構成を設計し、XML設定を自動生成する。</p>
 *
 * <p>ハンドラ情報（名前、FQCN、説明、順序）はすべて知識ベースYAMLから取得する。
 * ハードコードによる二重管理を排除し、YAMLを唯一の情報源として使用する。</p>
 *
 * @see <a href="docs/designs/15_tool-design-handler-queue.md">設計書</a>
 */
@Service
public class DesignHandlerQueueTool {

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public DesignHandlerQueueTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * アプリケーションタイプと要件に基づいてハンドラキューを設計する。
     *
     * @param appType アプリケーションタイプ（web, rest, batch, messaging）
     * @param requirements オプション要件（session, csrf, multipart, async等をカンマ区切り）
     * @param includeComments XMLにコメントを含めるか
     * @return 設計結果（XML設定と説明を含むMarkdown形式）
     */
    @Tool(name = "design_handler_queue", description = "Designs a Nablarch handler queue configuration based on application type and requirements. "
            + "Generates optimized XML configuration with proper handler ordering and validates constraints.")
    public String design(
            @ToolParam(description = "Application type: web, rest, batch, or messaging")
            String appType,
            @ToolParam(description = "Optional requirements (comma-separated): session, csrf, multipart, async, security, logging",
                    required = false)
            String requirements,
            @ToolParam(description = "Include explanatory comments in generated XML (default: true)",
                    required = false)
            Boolean includeComments) {

        // 入力検証
        if (appType == null || appType.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("アプリケーションタイプ（app_type）を指定してください")
                    .hint("有効な値: " + String.join(", ", knowledgeBase.getAvailableAppTypes()))
                    .build();
        }

        String normalizedAppType = appType.toLowerCase().trim();
        List<HandlerEntry> yamlHandlers = knowledgeBase.getHandlerEntries(normalizedAppType);
        if (yamlHandlers.isEmpty()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("不明なアプリケーションタイプ: " + appType)
                    .hint("有効な値: " + String.join(", ", knowledgeBase.getAvailableAppTypes()))
                    .build();
        }

        boolean withComments = includeComments == null || includeComments;
        Set<String> reqSet = parseRequirements(requirements);

        // ハンドラリストを構築（YAMLから読み込み）
        List<HandlerInfo> handlers = buildHandlerList(normalizedAppType, yamlHandlers, reqSet);

        // 順序制約を適用してソート
        handlers = applyOrderingConstraints(handlers);

        // 検証実行
        List<String> handlerNames = handlers.stream()
                .map(h -> h.name)
                .collect(Collectors.toList());
        String validationResult = knowledgeBase.validateHandlerQueue(normalizedAppType, handlerNames);

        // 結果生成
        return generateResult(normalizedAppType, handlers, reqSet, validationResult, withComments);
    }

    /**
     * 要件文字列をパースする。
     */
    private Set<String> parseRequirements(String requirements) {
        if (requirements == null || requirements.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(requirements.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * アプリタイプと要件に基づいてハンドラリストを構築する。
     * ハンドラ情報はすべてYAML知識ベースから取得する。
     */
    private List<HandlerInfo> buildHandlerList(String appType,
                                                List<HandlerEntry> yamlHandlers,
                                                Set<String> requirements) {
        // YAML名→FQCNのインデックスを構築
        Map<String, String> fqcnIndex = new HashMap<>();
        Map<String, String> descIndex = new HashMap<>();
        for (HandlerEntry h : yamlHandlers) {
            fqcnIndex.put(h.name, h.fqcn);
            descIndex.put(h.name, h.description != null ? h.description : "");
        }

        // YAMLのorder順でハンドラを追加
        List<HandlerInfo> handlers = new ArrayList<>();
        for (HandlerEntry h : yamlHandlers) {
            handlers.add(new HandlerInfo(h.name, h.fqcn,
                    h.description != null ? h.description : "", h.required));
        }

        // 要件に応じて追加ハンドラを挿入
        if (requirements.contains("csrf") && "web".equals(appType)) {
            if (handlers.stream().noneMatch(h -> h.name.equals("CsrfTokenVerificationHandler"))) {
                String fqcn = fqcnIndex.getOrDefault("CsrfTokenVerificationHandler",
                        "nablarch.fw.web.handler.CsrfTokenVerificationHandler");
                String desc = descIndex.getOrDefault("CsrfTokenVerificationHandler",
                        "CSRF対策トークン検証");
                handlers.add(new HandlerInfo("CsrfTokenVerificationHandler", fqcn, desc, false));
            }
        }

        if (requirements.contains("security")) {
            if (handlers.stream().noneMatch(h -> h.name.equals("SecureHandler"))) {
                String fqcn = fqcnIndex.getOrDefault("SecureHandler",
                        "nablarch.fw.web.handler.SecureHandler");
                String desc = descIndex.getOrDefault("SecureHandler",
                        "セキュリティヘッダー付与");
                int insertIdx = findInsertPosition(handlers, "HttpResponseHandler", true);
                handlers.add(insertIdx, new HandlerInfo("SecureHandler", fqcn, desc, false));
            }
        }

        return handlers;
    }

    /**
     * 順序制約を適用してハンドラリストをソートする。
     */
    private List<HandlerInfo> applyOrderingConstraints(List<HandlerInfo> handlers) {
        List<HandlerInfo> sorted = new ArrayList<>(handlers);

        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            positionMap.put(sorted.get(i).name, i);
        }

        boolean changed;
        int maxIterations = sorted.size() * 2;
        int iterations = 0;

        do {
            changed = false;
            iterations++;

            for (int i = 0; i < sorted.size(); i++) {
                HandlerInfo handler = sorted.get(i);
                HandlerConstraintEntry constraint = knowledgeBase.getHandlerConstraints(handler.name);
                if (constraint == null) continue;

                if (constraint.mustBefore != null) {
                    for (String target : constraint.mustBefore) {
                        Integer targetPos = positionMap.get(target);
                        if (targetPos != null && i >= targetPos) {
                            sorted.remove(i);
                            sorted.add(targetPos, handler);
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) break;

                if (constraint.mustAfter != null) {
                    for (String target : constraint.mustAfter) {
                        Integer targetPos = positionMap.get(target);
                        if (targetPos != null && i <= targetPos) {
                            sorted.remove(i);
                            sorted.add(targetPos + 1, handler);
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) break;
            }

            if (changed) {
                positionMap.clear();
                for (int i = 0; i < sorted.size(); i++) {
                    positionMap.put(sorted.get(i).name, i);
                }
            }

        } while (changed && iterations < maxIterations);

        return sorted;
    }

    /**
     * ハンドラの挿入位置を見つける。
     */
    private int findInsertPosition(List<HandlerInfo> handlers, String targetName, boolean after) {
        for (int i = 0; i < handlers.size(); i++) {
            if (handlers.get(i).name.equals(targetName)) {
                return after ? i + 1 : i;
            }
        }
        return handlers.size();
    }

    /**
     * 結果をMarkdown形式で生成する。
     */
    private String generateResult(String appType, List<HandlerInfo> handlers,
                                  Set<String> requirements, String validationResult,
                                  boolean withComments) {
        StringBuilder sb = new StringBuilder();

        sb.append("## ハンドラキュー設計結果\n\n");
        sb.append("**アプリタイプ**: ").append(appType).append("\n");
        sb.append("**ハンドラ数**: ").append(handlers.size()).append("\n");
        if (!requirements.isEmpty()) {
            sb.append("**適用要件**: ").append(String.join(", ", requirements)).append("\n");
        }
        sb.append("\n");

        // ハンドラ一覧テーブル
        sb.append("### ハンドラ構成\n\n");
        sb.append("| # | ハンドラ | 説明 | 必須 |\n");
        sb.append("|---|----------|------|------|\n");
        for (int i = 0; i < handlers.size(); i++) {
            HandlerInfo h = handlers.get(i);
            sb.append("| ").append(i + 1).append(" | ");
            sb.append(h.name).append(" | ");
            sb.append(h.description).append(" | ");
            sb.append(h.required ? "○" : "-").append(" |\n");
        }
        sb.append("\n");

        // XML設定
        sb.append("### XML設定\n\n");
        sb.append("```xml\n");
        sb.append(generateXml(handlers, appType, withComments));
        sb.append("```\n\n");

        // 検証結果
        sb.append("### 検証結果\n\n");
        sb.append(validationResult).append("\n");

        // 補足説明
        sb.append("### 補足\n\n");
        sb.append("- このXML設定を`component-configuration`ファイルに配置してください\n");
        sb.append("- 環境に応じてDB接続設定、ルーティング設定を調整してください\n");
        sb.append("- セキュリティ要件に応じてSecureHandlerの設定を見直してください\n");

        return sb.toString();
    }

    /**
     * XML設定を生成する。
     */
    private String generateXml(List<HandlerInfo> handlers, String appType, boolean withComments) {
        StringBuilder sb = new StringBuilder();

        if (withComments) {
            sb.append("<!-- ").append(appType).append("アプリケーション用ハンドラキュー設定 -->\n");
        }

        sb.append("<list name=\"handlerQueue\">\n");

        for (HandlerInfo h : handlers) {
            if (withComments && !h.description.isEmpty()) {
                sb.append("  <!-- ").append(h.description).append(" -->\n");
            }
            sb.append("  <component class=\"").append(h.fqcn).append("\"/>\n");
        }

        sb.append("</list>\n");

        return sb.toString();
    }

    /**
     * ハンドラ情報。
     */
    private record HandlerInfo(String name, String fqcn, String description, boolean required) {}
}

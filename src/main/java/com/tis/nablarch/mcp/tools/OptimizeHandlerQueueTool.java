package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Nablarchハンドラキュー最適化ツール。
 *
 * <p>既存のハンドラキューXML設定を分析し、3観点（正確性・セキュリティ・パフォーマンス）
 * から最適化提案を生成する。</p>
 *
 * <p>最適化ルール：</p>
 * <ul>
 *   <li>正確性（COR-001〜004）: 必須ハンドラ、順序制約、配置違反</li>
 *   <li>セキュリティ（SEC-001〜005）: SecureHandler、CSRF、セッション</li>
 *   <li>パフォーマンス（PERF-001〜005）: 不要・重複ハンドラ、配置最適化</li>
 * </ul>
 *
 * @see <a href="docs/designs/21_tool-optimize-handler-queue.md">設計書</a>
 */
@Service
public class OptimizeHandlerQueueTool {

    /** XML class属性抽出パターン */
    private static final Pattern CLASS_ATTR_PATTERN =
            Pattern.compile("class\\s*=\\s*\"([^\"]+)\"");

    /** 開発環境専用ハンドラ（本番で削除推奨） */
    private static final Set<String> DEVELOPMENT_ONLY_HANDLERS = Set.of(
            "HotDeployHandler",
            "DumpVariableHandler",
            "RequestDumpHandler",
            "HttpAccessLogHandler"
    );

    /** セキュリティ必須ハンドラ（Web/REST） */
    private static final Set<String> SECURITY_HANDLERS = Set.of(
            "SecureHandler",
            "CsrfTokenVerificationHandler"
    );

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public OptimizeHandlerQueueTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * ハンドラキューXMLを分析し、最適化提案を生成する。
     *
     * @param currentXml 現在のハンドラキューXML設定
     * @param appType アプリケーションタイプ（null可、自動推定）
     * @param concern 最適化観点（all, correctness, security, performance）
     * @return Markdown形式の最適化提案
     */
    @Tool(name = "optimize_handler_queue", description = "Analyzes an existing Nablarch handler queue XML and generates optimization proposals "
            + "from correctness, security, and performance perspectives.")
    public String optimize(
            @ToolParam(description = "Current handler queue XML configuration")
            String currentXml,
            @ToolParam(description = "Application type: web, rest, batch, messaging (auto-detected if not specified)",
                    required = false)
            String appType,
            @ToolParam(description = "Optimization concern: all, correctness, security, performance",
                    required = false)
            String concern) {

        // 入力検証
        if (currentXml == null || currentXml.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("ハンドラキューXMLが指定されていません")
                    .build();
        }

        // XML解析
        List<HandlerEntry> handlers = parseXml(currentXml);
        if (handlers.isEmpty()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("XMLからハンドラを抽出できませんでした")
                    .hint("component要素のclass属性が正しく設定されているか確認してください")
                    .build();
        }

        // app_type推定
        String detectedAppType = appType;
        if (detectedAppType == null || detectedAppType.isBlank()) {
            detectedAppType = detectAppType(handlers);
            if (detectedAppType == null) {
                return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                        .message("アプリケーションタイプを自動推定できませんでした")
                        .hint("app_typeパラメータを明示的に指定してください")
                        .build();
            }
        }

        // 最適化観点の正規化
        String normalizedConcern = (concern == null || concern.isBlank()) ? "all" : concern.toLowerCase();

        // 最適化ルール実行
        List<Proposal> proposals = new ArrayList<>();

        if ("all".equals(normalizedConcern) || "correctness".equals(normalizedConcern)) {
            proposals.addAll(runCorrectnessRules(handlers, detectedAppType));
        }
        if ("all".equals(normalizedConcern) || "security".equals(normalizedConcern)) {
            proposals.addAll(runSecurityRules(handlers, detectedAppType));
        }
        if ("all".equals(normalizedConcern) || "performance".equals(normalizedConcern)) {
            proposals.addAll(runPerformanceRules(handlers, detectedAppType));
        }

        // 結果生成
        return generateResult(handlers, detectedAppType, proposals, currentXml);
    }

    /**
     * XMLからハンドラ一覧を抽出する。
     */
    private List<HandlerEntry> parseXml(String xml) {
        List<HandlerEntry> entries = new ArrayList<>();
        Matcher matcher = CLASS_ATTR_PATTERN.matcher(xml);
        int order = 0;
        while (matcher.find()) {
            String fqcn = matcher.group(1);
            String simpleName = extractSimpleName(fqcn);
            entries.add(new HandlerEntry(order++, fqcn, simpleName));
        }
        return entries;
    }

    /**
     * FQCNからシンプル名を抽出する。
     */
    private String extractSimpleName(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    /**
     * ハンドラ構成からアプリケーションタイプを推定する。
     */
    private String detectAppType(List<HandlerEntry> handlers) {
        Set<String> names = handlers.stream()
                .map(h -> h.simpleName)
                .collect(Collectors.toSet());

        if (names.contains("JaxRsResponseHandler")) {
            return "rest";
        }
        if (names.contains("HttpResponseHandler") && names.contains("RoutesMapping")) {
            return "web";
        }
        if (names.contains("RequestThreadLoopHandler")) {
            return "messaging";
        }
        if (names.contains("MultiThreadExecutionHandler") && names.contains("DataReadHandler")) {
            return "batch";
        }
        if (names.contains("ProcessStopHandler")) {
            return "batch";
        }
        if (names.contains("HttpResponseHandler")) {
            return "web";
        }

        return null;
    }

    /**
     * 正確性観点の最適化ルールを実行する。
     */
    private List<Proposal> runCorrectnessRules(List<HandlerEntry> handlers, String appType) {
        List<Proposal> proposals = new ArrayList<>();
        Set<String> handlerNames = handlers.stream()
                .map(h -> h.simpleName)
                .collect(Collectors.toSet());

        // COR-001: 必須ハンドラ欠落チェック
        Map<String, Object> catalog = knowledgeBase.getHandlerCatalog(appType);
        if (catalog != null && catalog.get("handlers") != null) {
            @SuppressWarnings("unchecked")
            List<?> catalogHandlers = (List<?>) catalog.get("handlers");
            for (Object h : catalogHandlers) {
                if (h instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> handler = (Map<String, Object>) h;
                    Boolean required = (Boolean) handler.get("required");
                    String name = (String) handler.get("name");
                    if (Boolean.TRUE.equals(required) && !handlerNames.contains(name)) {
                        proposals.add(new Proposal(
                                "COR-001",
                                "correctness",
                                "high",
                                "add",
                                name,
                                "必須ハンドラが欠落しています",
                                name + "を追加してください"
                        ));
                    }
                }
            }
        }

        // COR-002: 順序制約違反チェック
        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < handlers.size(); i++) {
            positionMap.put(handlers.get(i).simpleName, i);
        }

        for (HandlerEntry handler : handlers) {
            HandlerConstraintEntry constraint = knowledgeBase.getHandlerConstraints(handler.simpleName);
            if (constraint == null) continue;

            if (constraint.mustBefore != null) {
                for (String target : constraint.mustBefore) {
                    Integer targetPos = positionMap.get(target);
                    if (targetPos != null && handler.order >= targetPos) {
                        proposals.add(new Proposal(
                                "COR-002",
                                "correctness",
                                "high",
                                "reorder",
                                handler.simpleName,
                                handler.simpleName + "は" + target + "より前に配置すべきです",
                                handler.simpleName + "を" + target + "の前に移動してください"
                        ));
                    }
                }
            }

            if (constraint.mustAfter != null) {
                for (String target : constraint.mustAfter) {
                    Integer targetPos = positionMap.get(target);
                    if (targetPos != null && handler.order <= targetPos) {
                        proposals.add(new Proposal(
                                "COR-002",
                                "correctness",
                                "high",
                                "reorder",
                                handler.simpleName,
                                handler.simpleName + "は" + target + "より後に配置すべきです",
                                handler.simpleName + "を" + target + "の後に移動してください"
                        ));
                    }
                }
            }
        }

        return proposals;
    }

    /**
     * セキュリティ観点の最適化ルールを実行する。
     */
    private List<Proposal> runSecurityRules(List<HandlerEntry> handlers, String appType) {
        List<Proposal> proposals = new ArrayList<>();
        Set<String> handlerNames = handlers.stream()
                .map(h -> h.simpleName)
                .collect(Collectors.toSet());

        // Web/RESTアプリケーションのみ
        if (!"web".equals(appType) && !"rest".equals(appType)) {
            return proposals;
        }

        // SEC-001: SecureHandler未設定
        if (!handlerNames.contains("SecureHandler")) {
            proposals.add(new Proposal(
                    "SEC-001",
                    "security",
                    "high",
                    "add",
                    "SecureHandler",
                    "セキュリティヘッダーが設定されていません",
                    "HttpResponseHandlerの内側にSecureHandlerを追加してください"
            ));
        }

        // SEC-002: CSRF対策未設定（Webのみ）
        if ("web".equals(appType) && !handlerNames.contains("CsrfTokenVerificationHandler")) {
            proposals.add(new Proposal(
                    "SEC-002",
                    "security",
                    "high",
                    "add",
                    "CsrfTokenVerificationHandler",
                    "CSRF対策が設定されていません",
                    "フォーム送信を処理する場合はCsrfTokenVerificationHandlerを追加してください"
            ));
        }

        // SEC-003: セッションストア未設定（Webのみ）
        if ("web".equals(appType) && !handlerNames.contains("SessionStoreHandler")
                && !handlerNames.contains("InMemorySessionStoreHandler")) {
            proposals.add(new Proposal(
                    "SEC-003",
                    "security",
                    "medium",
                    "add",
                    "SessionStoreHandler",
                    "セッション管理が設定されていません",
                    "セッションを使用する場合はSessionStoreHandlerを追加してください"
            ));
        }

        // SEC-005: 本番不要ハンドラ残存
        for (String devHandler : DEVELOPMENT_ONLY_HANDLERS) {
            if (handlerNames.contains(devHandler)) {
                proposals.add(new Proposal(
                        "SEC-005",
                        "security",
                        "medium",
                        "remove",
                        devHandler,
                        "開発環境専用のハンドラが残存しています",
                        "本番環境では" + devHandler + "を削除してください"
                ));
            }
        }

        return proposals;
    }

    /**
     * パフォーマンス観点の最適化ルールを実行する。
     */
    private List<Proposal> runPerformanceRules(List<HandlerEntry> handlers, String appType) {
        List<Proposal> proposals = new ArrayList<>();

        // PERF-002: 重複ハンドラチェック
        Map<String, List<HandlerEntry>> duplicateMap = handlers.stream()
                .collect(Collectors.groupingBy(h -> h.simpleName));

        for (Map.Entry<String, List<HandlerEntry>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                proposals.add(new Proposal(
                        "PERF-002",
                        "performance",
                        "medium",
                        "remove",
                        entry.getKey(),
                        "同一ハンドラが重複して設定されています",
                        entry.getKey() + "の重複を削除してください"
                ));
            }
        }

        // PERF-001: 不要ハンドラの除去（開発用）
        Set<String> handlerNames = handlers.stream()
                .map(h -> h.simpleName)
                .collect(Collectors.toSet());

        for (String devHandler : DEVELOPMENT_ONLY_HANDLERS) {
            if (handlerNames.contains(devHandler)) {
                // SEC-005で既に報告されている可能性があるが、パフォーマンス観点でも指摘
                boolean alreadyProposed = proposals.stream()
                        .anyMatch(p -> p.handler.equals(devHandler));
                if (!alreadyProposed) {
                    proposals.add(new Proposal(
                            "PERF-001",
                            "performance",
                            "medium",
                            "remove",
                            devHandler,
                            "パフォーマンスに影響するデバッグ用ハンドラが残存しています",
                            devHandler + "を削除してパフォーマンスを改善してください"
                    ));
                }
            }
        }

        // PERF-005: ログハンドラの非同期化推奨
        if (handlerNames.contains("AccessLogHandler")) {
            proposals.add(new Proposal(
                    "PERF-005",
                    "performance",
                    "low",
                    "configure",
                    "AccessLogHandler",
                    "アクセスログ出力がパフォーマンスに影響する可能性があります",
                    "高負荷環境では非同期ログ出力の検討を推奨します"
            ));
        }

        return proposals;
    }

    /**
     * Markdown形式の結果を生成する。
     */
    private String generateResult(List<HandlerEntry> handlers, String appType,
                                  List<Proposal> proposals, String currentXml) {
        StringBuilder sb = new StringBuilder();

        sb.append("## ハンドラキュー最適化分析\n\n");
        sb.append("**アプリタイプ**: ").append(appType).append("\n");
        sb.append("**ハンドラ数**: ").append(handlers.size()).append("\n");
        sb.append("**検出された最適化ポイント**: ").append(proposals.size()).append("件\n\n");

        if (proposals.isEmpty()) {
            sb.append("最適化の提案はありません。ハンドラキューは適切に構成されています。\n");
            return sb.toString();
        }

        // サマリ
        sb.append("### サマリ\n\n");
        sb.append("| 観点 | 件数 | 高 | 中 | 低 |\n");
        sb.append("|------|------|-----|-----|-----|\n");

        Map<String, List<Proposal>> byConcern = proposals.stream()
                .collect(Collectors.groupingBy(p -> p.concern));

        for (String concern : List.of("correctness", "security", "performance")) {
            List<Proposal> ps = byConcern.getOrDefault(concern, List.of());
            long high = ps.stream().filter(p -> "high".equals(p.severity)).count();
            long medium = ps.stream().filter(p -> "medium".equals(p.severity)).count();
            long low = ps.stream().filter(p -> "low".equals(p.severity)).count();

            String label = switch (concern) {
                case "correctness" -> "正確性";
                case "security" -> "セキュリティ";
                case "performance" -> "パフォーマンス";
                default -> concern;
            };

            sb.append("| ").append(label).append(" | ").append(ps.size())
                    .append(" | ").append(high)
                    .append(" | ").append(medium)
                    .append(" | ").append(low).append(" |\n");
        }

        sb.append("\n---\n\n");

        // 詳細提案（重大度でソート）
        proposals.sort(Comparator
                .comparing((Proposal p) -> severityOrder(p.severity))
                .thenComparing(p -> p.id));

        for (Proposal p : proposals) {
            String severityEmoji = switch (p.severity) {
                case "high" -> "🔴";
                case "medium" -> "🟡";
                case "low" -> "🟢";
                default -> "⚪";
            };

            String severityLabel = switch (p.severity) {
                case "high" -> "高";
                case "medium" -> "中";
                case "low" -> "低";
                default -> p.severity;
            };

            String concernLabel = switch (p.concern) {
                case "correctness" -> "正確性";
                case "security" -> "セキュリティ";
                case "performance" -> "パフォーマンス";
                default -> p.concern;
            };

            String typeLabel = switch (p.type) {
                case "add" -> "ハンドラ追加";
                case "remove" -> "ハンドラ削除";
                case "reorder" -> "順序変更";
                case "replace" -> "ハンドラ置換";
                case "configure" -> "設定変更";
                default -> p.type;
            };

            sb.append("### ").append(severityEmoji).append(" [").append(p.id).append("] ");
            sb.append(p.handler).append("（").append(severityLabel).append("）\n\n");
            sb.append("**観点**: ").append(concernLabel).append("\n");
            sb.append("**タイプ**: ").append(typeLabel).append("\n");
            sb.append("**問題**: ").append(p.reason).append("\n");
            sb.append("**修正提案**: ").append(p.suggestedFix).append("\n\n");

            // Before/After例（add/removeの場合）
            if ("add".equals(p.type) || "remove".equals(p.type)) {
                sb.append("#### Before\n");
                sb.append("```xml\n");
                sb.append(generateBeforeSnippet(handlers, p)).append("\n");
                sb.append("```\n\n");

                sb.append("#### After\n");
                sb.append("```xml\n");
                sb.append(generateAfterSnippet(handlers, p)).append("\n");
                sb.append("```\n\n");
            }

            sb.append("---\n\n");
        }

        return sb.toString();
    }

    /**
     * 重大度の順序（ソート用）。
     */
    private int severityOrder(String severity) {
        return switch (severity) {
            case "high" -> 0;
            case "medium" -> 1;
            case "low" -> 2;
            default -> 3;
        };
    }

    /**
     * Before XMLスニペットを生成する。
     */
    private String generateBeforeSnippet(List<HandlerEntry> handlers, Proposal p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- 現在のハンドラキュー（一部抜粋） -->\n");

        int startIdx = Math.max(0, findHandlerIndex(handlers, p.handler) - 1);
        int endIdx = Math.min(handlers.size(), startIdx + 3);

        for (int i = startIdx; i < endIdx; i++) {
            sb.append("<component class=\"").append(handlers.get(i).fqcn).append("\"/>\n");
        }

        return sb.toString();
    }

    /**
     * After XMLスニペットを生成する。
     */
    private String generateAfterSnippet(List<HandlerEntry> handlers, Proposal p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- 推奨構成（一部抜粋） -->\n");

        int startIdx = Math.max(0, findHandlerIndex(handlers, p.handler) - 1);
        int endIdx = Math.min(handlers.size(), startIdx + 3);

        if ("add".equals(p.type)) {
            // 追加提案の場合
            for (int i = startIdx; i < endIdx; i++) {
                sb.append("<component class=\"").append(handlers.get(i).fqcn).append("\"/>\n");
                // 適切な位置に新しいハンドラを挿入
                if (i == startIdx && shouldInsertAfter(handlers.get(i).simpleName, p.handler)) {
                    sb.append("<component class=\"nablarch.fw.web.handler.")
                            .append(p.handler).append("\"/> <!-- 追加 -->\n");
                }
            }
        } else if ("remove".equals(p.type)) {
            // 削除提案の場合
            for (int i = startIdx; i < endIdx; i++) {
                if (!handlers.get(i).simpleName.equals(p.handler)) {
                    sb.append("<component class=\"").append(handlers.get(i).fqcn).append("\"/>\n");
                } else {
                    sb.append("<!-- ").append(handlers.get(i).fqcn).append(" を削除 -->\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * ハンドラのインデックスを検索する。
     */
    private int findHandlerIndex(List<HandlerEntry> handlers, String name) {
        for (int i = 0; i < handlers.size(); i++) {
            if (handlers.get(i).simpleName.equals(name)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 指定ハンドラの後に挿入すべきかを判定する。
     */
    private boolean shouldInsertAfter(String existingHandler, String newHandler) {
        // SecureHandlerはHttpResponseHandlerの後に挿入
        if ("SecureHandler".equals(newHandler) && "HttpResponseHandler".equals(existingHandler)) {
            return true;
        }
        return false;
    }

    /**
     * ハンドラエントリ。
     */
    private record HandlerEntry(int order, String fqcn, String simpleName) {}

    /**
     * 最適化提案。
     */
    private record Proposal(
            String id,
            String concern,
            String severity,
            String type,
            String handler,
            String reason,
            String suggestedFix
    ) {}
}

package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
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
 * <p>UC1（ハンドラキュー設計）に対応し、以下の機能を提供する：</p>
 * <ul>
 *   <li>アプリタイプ別の推奨ハンドラキュー生成</li>
 *   <li>知識ベースからのハンドラ情報取得</li>
 *   <li>順序制約に基づいた自動並び替え</li>
 *   <li>XML設定の自動生成</li>
 *   <li>生成結果の検証</li>
 * </ul>
 *
 * @see <a href="docs/designs/15_tool-design-handler-queue.md">設計書</a>
 */
@Service
public class DesignHandlerQueueTool {

    private final NablarchKnowledgeBase knowledgeBase;

    /** アプリタイプ別の推奨ハンドラ順序（デフォルト構成） */
    private static final Map<String, List<String>> DEFAULT_HANDLER_ORDERS = Map.of(
            "web", List.of(
                    "StatusCodeConvertHandler",
                    "HttpResponseHandler",
                    "GlobalErrorHandler",
                    "SecureHandler",
                    "SessionStoreHandler",
                    "DbConnectionManagementHandler",
                    "RequestPathJavaPackageMapping",
                    "MultipartHandler",
                    "HttpCharacterEncodingHandler",
                    "HttpAccessLogHandler",
                    "PackageMapping",
                    "RoutesMapping",
                    "DispatchHandler"
            ),
            "rest", List.of(
                    "StatusCodeConvertHandler",
                    "HttpResponseHandler",
                    "GlobalErrorHandler",
                    "SecureHandler",
                    "DbConnectionManagementHandler",
                    "RequestPathJavaPackageMapping",
                    "HttpAccessLogHandler",
                    "JaxRsResponseHandler",
                    "DispatchHandler"
            ),
            "batch", List.of(
                    "StatusCodeConvertHandler",
                    "GlobalErrorHandler",
                    "DbConnectionManagementHandler",
                    "TransactionManagementHandler",
                    "LoopHandler",
                    "RetryHandler",
                    "DataReadHandler",
                    "ProcessStopHandler",
                    "MultiThreadExecutionHandler",
                    "DispatchHandler"
            ),
            "messaging", List.of(
                    "StatusCodeConvertHandler",
                    "GlobalErrorHandler",
                    "DbConnectionManagementHandler",
                    "TransactionManagementHandler",
                    "RequestThreadLoopHandler",
                    "RetryHandler",
                    "DataReadHandler",
                    "DispatchHandler"
            )
    );

    /** ハンドラのFQCNマッピング */
    private static final Map<String, String> HANDLER_FQCN_MAP = new LinkedHashMap<>();
    static {
        // 共通
        HANDLER_FQCN_MAP.put("StatusCodeConvertHandler", "nablarch.fw.handler.StatusCodeConvertHandler");
        HANDLER_FQCN_MAP.put("GlobalErrorHandler", "nablarch.fw.handler.GlobalErrorHandler");
        HANDLER_FQCN_MAP.put("DbConnectionManagementHandler", "nablarch.common.handler.DbConnectionManagementHandler");
        HANDLER_FQCN_MAP.put("TransactionManagementHandler", "nablarch.common.handler.TransactionManagementHandler");
        HANDLER_FQCN_MAP.put("DispatchHandler", "nablarch.fw.handler.DispatchHandler");
        HANDLER_FQCN_MAP.put("RetryHandler", "nablarch.fw.handler.RetryHandler");

        // Web
        HANDLER_FQCN_MAP.put("HttpResponseHandler", "nablarch.fw.web.handler.HttpResponseHandler");
        HANDLER_FQCN_MAP.put("SecureHandler", "nablarch.fw.web.handler.SecureHandler");
        HANDLER_FQCN_MAP.put("SessionStoreHandler", "nablarch.common.web.session.SessionStoreHandler");
        HANDLER_FQCN_MAP.put("RequestPathJavaPackageMapping", "nablarch.integration.router.RequestPathJavaPackageMapping");
        HANDLER_FQCN_MAP.put("MultipartHandler", "nablarch.fw.web.handler.MultipartHandler");
        HANDLER_FQCN_MAP.put("HttpCharacterEncodingHandler", "nablarch.fw.web.handler.HttpCharacterEncodingHandler");
        HANDLER_FQCN_MAP.put("HttpAccessLogHandler", "nablarch.fw.web.handler.HttpAccessLogHandler");
        HANDLER_FQCN_MAP.put("PackageMapping", "nablarch.integration.router.PackageMapping");
        HANDLER_FQCN_MAP.put("RoutesMapping", "nablarch.integration.router.RoutesMapping");
        HANDLER_FQCN_MAP.put("CsrfTokenVerificationHandler", "nablarch.fw.web.handler.CsrfTokenVerificationHandler");

        // REST
        HANDLER_FQCN_MAP.put("JaxRsResponseHandler", "nablarch.fw.jaxrs.JaxRsResponseHandler");

        // Batch
        HANDLER_FQCN_MAP.put("LoopHandler", "nablarch.fw.handler.LoopHandler");
        HANDLER_FQCN_MAP.put("DataReadHandler", "nablarch.fw.handler.DataReadHandler");
        HANDLER_FQCN_MAP.put("ProcessStopHandler", "nablarch.fw.handler.ProcessStopHandler");
        HANDLER_FQCN_MAP.put("MultiThreadExecutionHandler", "nablarch.fw.handler.MultiThreadExecutionHandler");

        // Messaging
        HANDLER_FQCN_MAP.put("RequestThreadLoopHandler", "nablarch.fw.messaging.handler.RequestThreadLoopHandler");
    }

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
    @Tool(description = "Designs a Nablarch handler queue configuration based on application type and requirements. "
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
            return "エラー: アプリケーションタイプ（app_type）を指定してください。\n"
                    + "有効な値: web, rest, batch, messaging";
        }

        String normalizedAppType = appType.toLowerCase().trim();
        if (!DEFAULT_HANDLER_ORDERS.containsKey(normalizedAppType)) {
            return "エラー: 不明なアプリケーションタイプ: " + appType + "\n"
                    + "有効な値: " + String.join(", ", DEFAULT_HANDLER_ORDERS.keySet());
        }

        boolean withComments = includeComments == null || includeComments;
        Set<String> reqSet = parseRequirements(requirements);

        // ハンドラリストを構築
        List<HandlerInfo> handlers = buildHandlerList(normalizedAppType, reqSet);

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
     */
    private List<HandlerInfo> buildHandlerList(String appType, Set<String> requirements) {
        List<String> baseHandlers = DEFAULT_HANDLER_ORDERS.get(appType);
        List<HandlerInfo> handlers = new ArrayList<>();

        // 基本ハンドラを追加
        for (String name : baseHandlers) {
            String fqcn = HANDLER_FQCN_MAP.getOrDefault(name,
                    "nablarch.fw.handler." + name);
            String description = getHandlerDescription(name);
            handlers.add(new HandlerInfo(name, fqcn, description, true));
        }

        // 要件に応じて追加ハンドラを挿入
        if (requirements.contains("csrf") && "web".equals(appType)) {
            if (handlers.stream().noneMatch(h -> h.name.equals("CsrfTokenVerificationHandler"))) {
                String fqcn = HANDLER_FQCN_MAP.get("CsrfTokenVerificationHandler");
                handlers.add(new HandlerInfo("CsrfTokenVerificationHandler", fqcn,
                        "CSRF対策トークン検証", false));
            }
        }

        if (requirements.contains("security")) {
            if (handlers.stream().noneMatch(h -> h.name.equals("SecureHandler"))) {
                String fqcn = HANDLER_FQCN_MAP.get("SecureHandler");
                int insertIdx = findInsertPosition(handlers, "HttpResponseHandler", true);
                handlers.add(insertIdx, new HandlerInfo("SecureHandler", fqcn,
                        "セキュリティヘッダー付与", false));
            }
        }

        return handlers;
    }

    /**
     * 順序制約を適用してハンドラリストをソートする。
     */
    private List<HandlerInfo> applyOrderingConstraints(List<HandlerInfo> handlers) {
        // 現状の順序は既にデフォルト構成に従っているため、
        // 知識ベースの制約に違反する場合のみ調整する
        List<HandlerInfo> sorted = new ArrayList<>(handlers);

        // 制約チェック用のマップ
        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            positionMap.put(sorted.get(i).name, i);
        }

        // 各ハンドラの制約をチェック
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

                // mustBefore制約をチェック
                if (constraint.mustBefore != null) {
                    for (String target : constraint.mustBefore) {
                        Integer targetPos = positionMap.get(target);
                        if (targetPos != null && i >= targetPos) {
                            // targetより前に移動
                            sorted.remove(i);
                            sorted.add(targetPos, handler);
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) break;

                // mustAfter制約をチェック
                if (constraint.mustAfter != null) {
                    for (String target : constraint.mustAfter) {
                        Integer targetPos = positionMap.get(target);
                        if (targetPos != null && i <= targetPos) {
                            // targetより後に移動
                            sorted.remove(i);
                            sorted.add(targetPos + 1, handler);
                            changed = true;
                            break;
                        }
                    }
                }

                if (changed) break;
            }

            // 位置マップを更新
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
     * ハンドラの説明を取得する。
     */
    private String getHandlerDescription(String name) {
        return switch (name) {
            case "StatusCodeConvertHandler" -> "ステータスコード変換";
            case "HttpResponseHandler" -> "HTTPレスポンス処理";
            case "GlobalErrorHandler" -> "グローバルエラー処理";
            case "SecureHandler" -> "セキュリティヘッダー付与";
            case "SessionStoreHandler" -> "セッションストア管理";
            case "DbConnectionManagementHandler" -> "DBコネクション管理";
            case "TransactionManagementHandler" -> "トランザクション管理";
            case "RequestPathJavaPackageMapping" -> "リクエストパス→パッケージマッピング";
            case "MultipartHandler" -> "マルチパートリクエスト処理";
            case "HttpCharacterEncodingHandler" -> "文字エンコーディング設定";
            case "HttpAccessLogHandler" -> "アクセスログ出力";
            case "PackageMapping" -> "パッケージベースルーティング";
            case "RoutesMapping" -> "ルートベースルーティング";
            case "JaxRsResponseHandler" -> "JAX-RSレスポンス処理";
            case "DispatchHandler" -> "アクションディスパッチ";
            case "LoopHandler" -> "ループ制御";
            case "DataReadHandler" -> "データ読み込み";
            case "ProcessStopHandler" -> "プロセス停止制御";
            case "MultiThreadExecutionHandler" -> "マルチスレッド実行";
            case "RequestThreadLoopHandler" -> "リクエストスレッドループ";
            case "RetryHandler" -> "リトライ制御";
            case "CsrfTokenVerificationHandler" -> "CSRF対策トークン検証";
            default -> "";
        };
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

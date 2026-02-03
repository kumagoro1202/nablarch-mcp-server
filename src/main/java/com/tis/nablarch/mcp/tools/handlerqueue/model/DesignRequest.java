package com.tis.nablarch.mcp.tools.handlerqueue.model;

import java.util.ArrayList;
import java.util.List;

/**
 * ハンドラキュー設計リクエストDTO。
 */
public class DesignRequest {
    private final String appType;
    private final boolean authentication;
    private final boolean csrfProtection;
    private final boolean cors;
    private final boolean fileUpload;
    private final boolean multiDb;
    private final List<String> customHandlerFqcns;

    public DesignRequest(String appType, boolean authentication, boolean csrfProtection,
                         boolean cors, boolean fileUpload, boolean multiDb, String customHandlers) {
        this.appType = appType;
        this.authentication = authentication;
        this.csrfProtection = csrfProtection;
        this.cors = cors;
        this.fileUpload = fileUpload;
        this.multiDb = multiDb;
        this.customHandlerFqcns = parseCustomHandlers(customHandlers);
    }

    private static List<String> parseCustomHandlers(String customHandlers) {
        if (customHandlers == null || customHandlers.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String fqcn : customHandlers.split(",")) {
            String trimmed = fqcn.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return List.copyOf(result);
    }

    public String getAppType() { return appType; }
    public boolean isAuthentication() { return authentication; }
    public boolean isCsrfProtection() { return csrfProtection; }
    public boolean isCors() { return cors; }
    public boolean isFileUpload() { return fileUpload; }
    public boolean isMultiDb() { return multiDb; }
    public List<String> getCustomHandlerFqcns() { return customHandlerFqcns; }

    public String requirementsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("認証: ").append(authentication ? "あり" : "なし");
        sb.append(", CORS: ").append(cors ? "あり" : "なし");
        sb.append(", CSRF: ").append(csrfProtection ? "あり" : "なし");
        sb.append(", ファイルアップロード: ").append(fileUpload ? "あり" : "なし");
        sb.append(", マルチDB: ").append(multiDb ? "あり" : "なし");
        if (!customHandlerFqcns.isEmpty()) {
            sb.append(", カスタムハンドラ: ").append(customHandlerFqcns.size()).append("件");
        }
        return sb.toString();
    }
}

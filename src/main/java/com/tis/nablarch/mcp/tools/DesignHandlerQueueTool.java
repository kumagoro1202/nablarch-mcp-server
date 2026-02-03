package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.tools.handlerqueue.HandlerQueueDesigner;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCPツール: design_handler_queue。
 *
 * <p>指定されたアプリケーションタイプと要件に基づいて、
 * Nablarchハンドラキュー構成を自動設計し、XML設定を生成する。</p>
 */
@Service
public class DesignHandlerQueueTool {

    private final HandlerQueueDesigner designer;

    public DesignHandlerQueueTool(HandlerQueueDesigner designer) {
        this.designer = designer;
    }

    @Tool(description = "Design a Nablarch handler queue configuration based on the "
            + "specified application type and requirements. Generates a complete XML "
            + "configuration with proper handler ordering.")
    public String designHandlerQueue(
            @ToolParam(description = "Application type: web, rest, batch, resident-batch, "
                    + "mom-messaging, http-messaging")
            String appType,
            @ToolParam(description = "Whether authentication is required")
            Boolean authentication,
            @ToolParam(description = "Whether CSRF protection is required")
            Boolean csrfProtection,
            @ToolParam(description = "Whether CORS support is required")
            Boolean cors,
            @ToolParam(description = "Whether file upload is required")
            Boolean fileUpload,
            @ToolParam(description = "Whether multiple DB connections are required")
            Boolean multiDb,
            @ToolParam(description = "Comma-separated FQCNs of custom handlers")
            String customHandlers) {

        if (appType == null || appType.isBlank()) {
            return "アプリケーションタイプを指定してください"
                    + "（web, rest, batch, resident-batch, mom-messaging, http-messaging）。";
        }

        return designer.design(
                appType.trim().toLowerCase(),
                Boolean.TRUE.equals(authentication),
                Boolean.TRUE.equals(csrfProtection),
                Boolean.TRUE.equals(cors),
                Boolean.TRUE.equals(fileUpload),
                Boolean.TRUE.equals(multiDb),
                customHandlers);
    }
}

package com.tis.nablarch.mcp.tools.handlerqueue;

import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import com.tis.nablarch.mcp.tools.handlerqueue.model.DesignRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ハンドラキューXML生成クラス。
 */
@Component
public class HandlerQueueXmlGenerator {
    private static final String INDENT = "  ";

    public String generate(List<HandlerEntry> handlers, DesignRequest request) {
        StringBuilder xml = new StringBuilder();
        xml.append("<!-- ").append(request.getAppType()).append(" アプリケーション用ハンドラキュー -->\n");
        xml.append("<!-- 要件: ").append(request.requirementsSummary()).append(" -->\n");
        xml.append("<list name=\"handlerQueue\">\n");

        for (HandlerEntry handler : handlers) {
            if (isRoutingHandler(handler.name)) {
                xml.append(INDENT).append("<component-ref name=\"routesMapping\"/>\n");
            } else if (requiresProperties(handler, request)) {
                generateComponentWithProperties(xml, handler, request);
            } else {
                xml.append(INDENT).append("<component class=\"").append(handler.fqcn).append("\"/>\n");
            }
        }
        xml.append("</list>");
        return xml.toString();
    }

    private boolean isRoutingHandler(String name) {
        return "PackageMapping".equals(name) || "RequestPathJavaPackageMapping".equals(name);
    }

    private boolean requiresProperties(HandlerEntry handler, DesignRequest request) {
        String name = handler.name;
        if ("CorsPreflightRequestHandler".equals(name) && request.isCors()) return true;
        if ("HttpCharacterEncodingHandler".equals(name)) return true;
        if ("MultipartHandler".equals(name) && request.isFileUpload()) return true;
        return false;
    }

    private void generateComponentWithProperties(StringBuilder xml, HandlerEntry handler,
            DesignRequest request) {
        xml.append(INDENT).append("<component class=\"").append(handler.fqcn).append("\">\n");
        switch (handler.name) {
            case "HttpCharacterEncodingHandler":
                xml.append(INDENT).append(INDENT)
                        .append("<property name=\"defaultEncoding\" value=\"UTF-8\"/>\n");
                break;
            case "CorsPreflightRequestHandler":
                xml.append(INDENT).append(INDENT).append("<property name=\"allowOrigins\">\n");
                xml.append(INDENT).append(INDENT).append(INDENT)
                        .append("<list><value>*</value></list>\n");
                xml.append(INDENT).append(INDENT).append("</property>\n");
                break;
            case "MultipartHandler":
                xml.append(INDENT).append(INDENT)
                        .append("<property name=\"maxFileSize\" value=\"10485760\"/>\n");
                break;
        }
        xml.append(INDENT).append("</component>\n");
    }
}

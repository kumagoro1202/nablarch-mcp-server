package com.tis.nablarch.mcp.tools.handlerqueue;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.tools.handlerqueue.model.DesignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 要件からハンドラへのマッピングを行うクラス。
 */
@Component
public class RequirementsMapper {
    private static final Logger log = LoggerFactory.getLogger(RequirementsMapper.class);
    private static final Pattern FQCN_PATTERN = Pattern.compile(
            "^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$");
    private final NablarchKnowledgeBase knowledgeBase;

    public RequirementsMapper(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public List<HandlerEntry> applyRequirements(List<HandlerEntry> baseHandlers,
            DesignRequest request, HybridSearchService hybridSearchService) {
        Set<String> existingNames = baseHandlers.stream()
                .map(h -> h.name).collect(Collectors.toCollection(LinkedHashSet::new));
        List<HandlerEntry> result = new ArrayList<>(baseHandlers);
        List<HandlerEntry> allHandlers = knowledgeBase.getAllHandlers(request.getAppType());

        for (HandlerEntry handler : allHandlers) {
            if (existingNames.contains(handler.name)) continue;
            if (shouldInclude(handler, request)) {
                result.add(handler);
                existingNames.add(handler.name);
            }
        }

        for (String fqcn : request.getCustomHandlerFqcns()) {
            if (!FQCN_PATTERN.matcher(fqcn).matches()) {
                log.warn("カスタムハンドラのFQCN形式が不正: {}", fqcn);
                continue;
            }
            String simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
            if (!existingNames.contains(simpleName)) {
                result.add(createCustomHandler(simpleName, fqcn));
                existingNames.add(simpleName);
            }
        }
        return result;
    }

    private boolean shouldInclude(HandlerEntry handler, DesignRequest request) {
        String name = handler.name;
        if (name.contains("Multipart") && request.isFileUpload()) return true;
        if (name.contains("Csrf") && request.isCsrfProtection()) return true;
        if (name.contains("Cors") && request.isCors()) return true;
        if (name.contains("AccessLog")) return true;
        if (name.contains("Normalization") && "web".equals(request.getAppType())) return true;
        return false;
    }

    private HandlerEntry createCustomHandler(String name, String fqcn) {
        HandlerEntry entry = new HandlerEntry();
        entry.name = name;
        entry.fqcn = fqcn;
        entry.description = "カスタムハンドラ";
        entry.order = 100;
        entry.required = false;
        entry.thread = "main";
        return entry;
    }
}

package com.tis.nablarch.mcp.tools.handlerqueue;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.tools.handlerqueue.model.DesignRequest;
import com.tis.nablarch.mcp.tools.handlerqueue.model.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ハンドラキュー設計のオーケストレーター。
 */
@Component
public class HandlerQueueDesigner {
    private static final Logger log = LoggerFactory.getLogger(HandlerQueueDesigner.class);

    private final NablarchKnowledgeBase knowledgeBase;
    private final HandlerQueueSorter sorter;
    private final HandlerQueueXmlGenerator xmlGenerator;
    private final HandlerQueueValidator validator;
    private final RequirementsMapper requirementsMapper;

    @Autowired(required = false)
    private HybridSearchService hybridSearchService;

    public HandlerQueueDesigner(NablarchKnowledgeBase knowledgeBase, HandlerQueueSorter sorter,
            HandlerQueueXmlGenerator xmlGenerator, HandlerQueueValidator validator,
            RequirementsMapper requirementsMapper) {
        this.knowledgeBase = knowledgeBase;
        this.sorter = sorter;
        this.xmlGenerator = xmlGenerator;
        this.validator = validator;
        this.requirementsMapper = requirementsMapper;
    }

    public String design(String appType, boolean authentication, boolean csrfProtection,
            boolean cors, boolean fileUpload, boolean multiDb, String customHandlers) {

        if (!knowledgeBase.isValidAppType(appType)) {
            return "エラー: 不明なアプリケーションタイプ: " + appType
                    + "。有効値: " + String.join(", ", knowledgeBase.getAvailableAppTypes());
        }

        DesignRequest request = new DesignRequest(appType, authentication, csrfProtection,
                cors, fileUpload, multiDb, customHandlers);

        boolean ragAvailable = hybridSearchService != null;
        log.info("ハンドラキュー設計開始: app_type={}, RAG={}", appType, ragAvailable ? "有効" : "無効");

        List<HandlerEntry> baseHandlers = knowledgeBase.getRequiredHandlers(appType);
        List<HandlerEntry> allHandlers = requirementsMapper.applyRequirements(
                baseHandlers, request, hybridSearchService);
        List<HandlerEntry> orderedHandlers = sorter.sort(
                allHandlers, knowledgeBase.getAllHandlerConstraints());
        String xmlConfig = xmlGenerator.generate(orderedHandlers, request);

        ValidationResult validationResult = validator.validate(appType, orderedHandlers);
        if (!validationResult.isValid()) {
            log.warn("制約検証失敗: {} 件のエラー。リトライ実行", validationResult.getErrors().size());
            orderedHandlers = sorter.sortWithFixes(allHandlers, knowledgeBase.getAllHandlerConstraints(),
                    validationResult.getErrors());
            xmlConfig = xmlGenerator.generate(orderedHandlers, request);
            validationResult = validator.validate(appType, orderedHandlers);
        }

        return formatResult(request, orderedHandlers, xmlConfig, validationResult, ragAvailable);
    }

    private String formatResult(DesignRequest request, List<HandlerEntry> handlers,
            String xmlConfig, ValidationResult validation, boolean ragAvailable) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ハンドラキュー設計結果\n\n");
        sb.append("**アプリタイプ**: ").append(request.getAppType()).append("\n");
        sb.append("**要件**: ").append(request.requirementsSummary()).append("\n\n");

        if (!ragAvailable) {
            sb.append("> 注意: RAG検索が利用できないため、静的知識ベースのみで設計しています。\n\n");
        }

        sb.append("### ハンドラキュー (").append(handlers.size()).append("ハンドラ)\n\n");
        sb.append("| # | ハンドラ名 | FQCN | 役割 | スレッド | 必須 |\n");
        sb.append("|---|-----------|------|------|---------|------|\n");
        for (int i = 0; i < handlers.size(); i++) {
            HandlerEntry h = handlers.get(i);
            sb.append("| ").append(i + 1).append(" | ").append(h.name).append(" | ")
                    .append(h.fqcn).append(" | ").append(truncate(h.description, 30)).append(" | ")
                    .append(h.thread != null ? h.thread : "main").append(" | ")
                    .append(h.required ? "○" : "-").append(" |\n");
        }
        sb.append("\n### XML設定\n\n```xml\n").append(xmlConfig).append("\n```\n\n");

        if (!validation.isValid()) {
            sb.append("### ⚠️ 検証警告\n\n以下の制約違反を解消できませんでした。\n\n");
            for (String error : validation.getErrors()) sb.append("- ").append(error).append("\n");
            sb.append("\n");
        }
        if (!validation.getWarnings().isEmpty()) {
            sb.append("### 注意事項\n\n");
            for (String warning : validation.getWarnings()) sb.append("- ").append(warning).append("\n");
            sb.append("\n");
        }
        sb.append("### 参考ドキュメント\n\n- [Nablarch ハンドラキュー構成]")
                .append("(https://nablarch.github.io/docs/LATEST/doc/)\n");
        return sb.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

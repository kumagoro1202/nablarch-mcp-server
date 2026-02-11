package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCPツール: search_api。
 *
 * <p>Nablarch知識ベースからAPIパターン・モジュール・ハンドラ・設計パターン・
 * エラー情報をキーワード検索する。AIアシスタントがNablarchのAPIを
 * 理解してコード生成するために使用する。</p>
 */
@Service
public class SearchApiTool {

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public SearchApiTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Nablarch APIドキュメントを検索する。
     *
     * @param keyword 検索キーワード（クラス名、メソッド名、概念）
     * @param category カテゴリフィルタ（handler, library, web, batch, rest, messaging等）
     * @return 検索結果のフォーマット済みテキスト
     */
    @Tool(name = "search_api", description = "Search the Nablarch API documentation for classes, methods, and patterns. "
            + "Use this when you need to find Nablarch APIs for code generation.")
    public String searchApi(
            @ToolParam(description = "Search keyword (class name, method name, or concept)") String keyword,
            @ToolParam(description = "Optional category filter: handler, library, web, batch, rest, messaging",
                    required = false)
            String category) {
        if (keyword == null || keyword.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("検索キーワードを指定してください")
                    .build();
        }

        String effectiveCategory = (category != null && !category.isBlank()) ? category : null;
        List<String> results = knowledgeBase.search(keyword, effectiveCategory);

        if (results.isEmpty()) {
            return "検索結果なし: " + keyword
                    + (effectiveCategory != null ? " (カテゴリ: " + effectiveCategory + ")" : "");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("検索結果: \"").append(keyword).append("\"");
        if (effectiveCategory != null) {
            sb.append(" (カテゴリ: ").append(effectiveCategory).append(")");
        }
        sb.append("\n件数: ").append(results.size()).append("件\n\n");

        for (String result : results) {
            sb.append(result).append("\n");
        }

        return sb.toString();
    }
}

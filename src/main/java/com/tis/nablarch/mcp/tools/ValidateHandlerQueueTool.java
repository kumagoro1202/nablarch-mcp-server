package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCPツール: validate_handler_queue。
 *
 * <p>NablarchハンドラキューXML設定を検証する。
 * ハンドラの順序制約・必須ハンドラの有無・互換性をチェックし、
 * 検証結果をエラー・警告付きで返す。</p>
 */
@Service
public class ValidateHandlerQueueTool {

    private final NablarchKnowledgeBase knowledgeBase;

    /** XML class属性からFQCNを抽出するパターン */
    private static final Pattern CLASS_ATTR_PATTERN =
            Pattern.compile("class\\s*=\\s*\"([^\"]+)\"");

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public ValidateHandlerQueueTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * NablarchハンドラキューXML設定を検証する。
     *
     * <p>入力XMLからハンドラクラス名を抽出し、
     * 知識ベースの制約ルールに基づいて順序・必須・互換性を検証する。</p>
     *
     * @param handlerQueueXml ハンドラキューXML設定内容
     * @param applicationType アプリケーションタイプ（web, rest, batch, messaging）
     * @return 検証結果（エラー・警告を含むフォーマット済みテキスト）
     */
    @Tool(name = "validate_handler_queue", description = "Validate a Nablarch handler queue XML configuration. "
            + "Checks handler ordering constraints, required handlers, and best practices. "
            + "Use this to verify handler queue configurations before deployment.")
    public String validateHandlerQueue(
            @ToolParam(description = "Handler queue XML configuration content") String handlerQueueXml,
            @ToolParam(description = "Application type: web, rest, batch, or messaging")
            String applicationType) {
        if (handlerQueueXml == null || handlerQueueXml.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("ハンドラキューXMLを指定してください")
                    .build();
        }
        if (applicationType == null || applicationType.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("アプリケーションタイプを指定してください")
                    .hint("有効な値: web, rest, batch, messaging")
                    .build();
        }

        List<String> handlerNames = extractHandlerNames(handlerQueueXml);
        if (handlerNames.isEmpty()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("XMLからハンドラクラスを抽出できませんでした")
                    .hint("class属性を持つcomponentまたはhandler要素を含むXMLを指定してください")
                    .build();
        }

        return knowledgeBase.validateHandlerQueue(applicationType, handlerNames);
    }

    /**
     * XML文字列からハンドラクラス名（単純名）を抽出する。
     *
     * <p>class属性のFQCNから末尾の単純クラス名を取得する。</p>
     *
     * @param xml ハンドラキューXML
     * @return ハンドラ名のリスト（出現順）
     */
    List<String> extractHandlerNames(String xml) {
        List<String> names = new ArrayList<>();
        Matcher matcher = CLASS_ATTR_PATTERN.matcher(xml);
        while (matcher.find()) {
            String fqcn = matcher.group(1);
            String simpleName = fqcn.contains(".")
                    ? fqcn.substring(fqcn.lastIndexOf('.') + 1)
                    : fqcn;
            names.add(simpleName);
        }
        return names;
    }
}

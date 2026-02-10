package com.tis.nablarch.mcp.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tis.nablarch.mcp.codegen.CodeGenerator;
import com.tis.nablarch.mcp.codegen.GenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * MCPツール: generate_code。
 *
 * <p>Nablarch準拠のJavaコード（Action、Form、SQL定義、Entity、Handler、Interceptor）を
 * 生成する。知識ベースからコーディング規約を取得し、Nablarchのベストプラクティスに
 * 従ったコードを出力する。</p>
 *
 * @see CodeGenerator
 */
@Service
public class CodeGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(CodeGenerationTool.class);

    private static final Set<String> VALID_TYPES =
            Set.of("action", "form", "sql", "entity", "handler", "interceptor");
    private static final Set<String> VALID_APP_TYPES =
            Set.of("web", "rest", "batch", "messaging");

    private final CodeGenerator codeGenerator;
    private final ObjectMapper objectMapper;

    /**
     * コンストラクタ。
     *
     * @param codeGenerator コード生成エンジン
     */
    public CodeGenerationTool(CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Nablarch準拠のコードを生成する。
     *
     * @param type 生成対象タイプ（action, form, sql, entity, handler, interceptor）
     * @param name 生成するクラス/ファイルの名前
     * @param appType アプリケーションタイプ（web, rest, batch, messaging）
     * @param specifications タイプ固有の詳細パラメータ（JSON文字列）
     * @return 生成結果のMarkdownフォーマット文字列
     */
    @Tool(description = "Generate Nablarch-compliant code (Action, Form, SQL, Entity, Handler, "
            + "Interceptor). Produces boilerplate code following Nablarch coding conventions "
            + "and best practices. Use this when developers need skeleton code that adheres "
            + "to Nablarch patterns.")
    public String generateCode(
            @ToolParam(description = "Type of code to generate: action, form, sql, entity, handler, interceptor")
            String type,
            @ToolParam(description = "Name for the generated class/file (e.g. 'UserRegistration', 'Product')")
            String name,
            @ToolParam(description = "Application type: web (default), rest, batch, messaging")
            String appType,
            @ToolParam(description = "Type-specific parameters as JSON (fields, queries, routing, etc.)")
            String specifications) {

        // 入力検証
        if (type == null || type.isBlank()) {
            return "生成対象タイプを指定してください。"
                    + "有効値: action, form, sql, entity, handler, interceptor";
        }
        if (name == null || name.isBlank()) {
            return "生成するクラス/ファイルの名前を指定してください。";
        }

        String effectiveType = type.toLowerCase().trim();
        if (!VALID_TYPES.contains(effectiveType)) {
            return "不正な生成対象タイプ: " + type
                    + "。有効値: action, form, sql, entity, handler, interceptor";
        }

        String effectiveAppType = (appType != null && !appType.isBlank())
                ? appType.toLowerCase().trim() : "web";
        if (!VALID_APP_TYPES.contains(effectiveAppType)) {
            return "不正なアプリケーションタイプ: " + appType
                    + "。有効値: web, rest, batch, messaging";
        }

        // specifications のパース
        Map<String, Object> specs = parseSpecifications(specifications);

        // コード生成実行
        GenerationResult result;
        try {
            result = codeGenerator.generate(effectiveType, name, effectiveAppType, specs);
        } catch (Exception e) {
            log.error("コード生成でエラー: type={}, name={}, appType={}",
                    effectiveType, name, effectiveAppType, e);
            throw new RuntimeException(
                    "コード生成中にエラーが発生しました。入力パラメータを確認してください。");
        }

        return formatResult(result);
    }

    /**
     * JSON文字列のspecificationsをMapにパースする。
     * パース失敗時は空Mapを返す。
     */
    private Map<String, Object> parseSpecifications(String specifications) {
        if (specifications == null || specifications.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(specifications,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("specificationsのパースに失敗。空のspecificationsで続行: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 生成結果をMarkdown形式に整形する。
     */
    private String formatResult(GenerationResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 生成結果: ").append(result.name());
        sb.append(" (").append(result.appType()).append("/").append(result.type()).append(")\n\n");

        // 適用された規約
        if (!result.conventionsApplied().isEmpty()) {
            sb.append("### 適用されたNablarch規約\n");
            for (String convention : result.conventionsApplied()) {
                sb.append("- ").append(convention).append("\n");
            }
            sb.append("\n");
        }

        // 必要な依存モジュール
        if (!result.dependencies().isEmpty()) {
            sb.append("### 必要な依存モジュール\n");
            for (String dep : result.dependencies()) {
                sb.append("- ").append(dep).append("\n");
            }
            sb.append("\n");
        }

        // 警告
        if (!result.warnings().isEmpty()) {
            sb.append("### 注意事項\n");
            for (String warning : result.warnings()) {
                sb.append("- ").append(warning).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n\n");

        // 生成ファイル
        if (result.files().isEmpty()) {
            sb.append("ファイルが生成されませんでした。\n");
        } else {
            for (int i = 0; i < result.files().size(); i++) {
                GenerationResult.GeneratedFile file = result.files().get(i);
                sb.append("### ファイル ").append(i + 1).append(": ")
                        .append(file.fileName()).append("\n");
                sb.append("パス: `").append(file.path()).append("`\n\n");
                sb.append("```").append(file.language()).append("\n");
                sb.append(file.content());
                sb.append("```\n\n");
                if (i < result.files().size() - 1) {
                    sb.append("---\n\n");
                }
            }
        }

        return sb.toString();
    }
}

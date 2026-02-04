package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCPツール: troubleshoot。
 *
 * <p>Nablarch固有のエラーメッセージ・スタックトレースを解析し、
 * 知識ベースから解決策を検索して提供する。AIアシスタントがNablarch開発の
 * トラブルシューティングを支援するために使用する。</p>
 *
 * <p>対応エラーカテゴリ:</p>
 * <ul>
 *   <li>handler: ハンドラキュー関連エラー</li>
 *   <li>database: DB接続・トランザクション関連エラー</li>
 *   <li>validation: バリデーション関連エラー</li>
 *   <li>config: 設定・コンポーネント定義関連エラー</li>
 *   <li>batch: バッチ処理関連エラー</li>
 *   <li>general: 一般的なエラー</li>
 * </ul>
 */
@Service
public class TroubleshootTool {

    private static final Logger log = LoggerFactory.getLogger(TroubleshootTool.class);

    /** Nablarchパッケージのパターン */
    private static final Pattern NABLARCH_PACKAGE_PATTERN =
            Pattern.compile("nablarch\\.[\\w.]+");

    /** エラーコードパターン（ERR-XXX形式） */
    private static final Pattern ERROR_CODE_PATTERN =
            Pattern.compile("ERR-\\d{3}");

    /** FQCNパターン（スタックトレースから抽出用） */
    private static final Pattern FQCN_PATTERN =
            Pattern.compile("at\\s+(nablarch[\\w.]+)\\.");

    /** ハンドラ名パターン */
    private static final Pattern HANDLER_PATTERN =
            Pattern.compile("([A-Z][a-zA-Z]+Handler)");

    /** エラーカテゴリ判定用キーワード */
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "handler", List.of("handler", "queue", "dispatch", "mapping", "classcast"),
            "database", List.of("sql", "database", "connection", "transaction", "dao",
                    "jdbc", "optimisticlock", "nodata"),
            "validation", List.of("validation", "required", "length", "charset", "applicationexception"),
            "config", List.of("component", "repository", "config", "xml", "property", "di."),
            "batch", List.of("batch", "duplicate", "datareader", "loop"),
            "general", List.of("threadcontext", "httpresponse", "noclassdef")
    );

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public TroubleshootTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Nablarchエラーのトラブルシューティングを行う。
     *
     * <p>エラーメッセージとスタックトレースを解析し、
     * エラーカテゴリの分類、原因の特定、解決策の提案を行う。</p>
     *
     * @param errorMessage エラーメッセージ本文
     * @param stackTrace スタックトレース全文（オプション）
     * @param errorCode Nablarchエラーコード（ERR-XXX形式、オプション）
     * @param environment 環境情報（JSON文字列、オプション）
     * @return トラブルシューティング結果のMarkdownフォーマット文字列
     */
    @Tool(description = "Troubleshoot Nablarch-specific errors by analyzing error messages "
            + "and stack traces. Returns error classification, root cause analysis, "
            + "and recommended solutions from the knowledge base.")
    public String troubleshoot(
            @ToolParam(description = "Error message text to analyze")
            String errorMessage,
            @ToolParam(description = "Full stack trace (optional but recommended)")
            String stackTrace,
            @ToolParam(description = "Nablarch error code (e.g., ERR-001) if available")
            String errorCode,
            @ToolParam(description = "Environment info as JSON (e.g., app type, Nablarch version)")
            String environment) {

        // 入力検証
        if (errorMessage == null || errorMessage.isBlank()) {
            return "エラーメッセージを指定してください。";
        }

        log.debug("トラブルシューティング開始: errorMessage={}, errorCode={}",
                truncate(errorMessage, 50), errorCode);

        // 1. エラー分析
        ErrorAnalysis analysis = analyzeError(errorMessage, stackTrace, errorCode);

        // 2. 知識ベース検索
        List<String> searchResults = searchKnowledgeBase(analysis);

        // 3. 結果フォーマット
        return formatResult(analysis, searchResults, environment);
    }

    /**
     * エラーを分析してカテゴリ・関連コンポーネントを特定する。
     */
    private ErrorAnalysis analyzeError(String errorMessage, String stackTrace, String errorCode) {
        ErrorAnalysis analysis = new ErrorAnalysis();

        // 小文字化テキスト（カテゴリ判定・コンポーネント抽出用）
        String fullTextLower = errorMessage.toLowerCase();
        if (stackTrace != null) {
            fullTextLower += " " + stackTrace.toLowerCase();
        }

        // 元のテキスト（ハンドラ名抽出用、大文字小文字を保持）
        String fullTextOriginal = errorMessage;
        if (stackTrace != null) {
            fullTextOriginal += " " + stackTrace;
        }

        // エラーコードの抽出・検証
        if (errorCode != null && ERROR_CODE_PATTERN.matcher(errorCode).matches()) {
            analysis.errorCode = errorCode;
        } else {
            // エラーメッセージからERR-XXX形式を抽出
            Matcher codeMatcher = ERROR_CODE_PATTERN.matcher(errorMessage);
            if (codeMatcher.find()) {
                analysis.errorCode = codeMatcher.group();
            }
        }

        // カテゴリ判定（小文字テキストを使用）
        analysis.category = classifyCategory(fullTextLower);

        // Nablarchコンポーネントの抽出（小文字テキストを使用）
        analysis.relatedComponents = extractNablarchComponents(fullTextLower);

        // ハンドラ名の抽出（元のテキストを使用、大文字パターン検出のため）
        analysis.relatedHandlers = extractHandlers(fullTextOriginal);

        // 検索キーワードの生成
        analysis.searchKeywords = generateSearchKeywords(errorMessage, analysis);

        return analysis;
    }

    /**
     * エラーカテゴリを判定する。
     */
    private String classifyCategory(String fullText) {
        Map<String, Integer> scores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (fullText.contains(keyword)) {
                    score++;
                }
            }
            scores.put(entry.getKey(), score);
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse("general");
    }

    /**
     * Nablarchコンポーネント名を抽出する。
     */
    private Set<String> extractNablarchComponents(String text) {
        Set<String> components = new LinkedHashSet<>();
        Matcher matcher = NABLARCH_PACKAGE_PATTERN.matcher(text);
        while (matcher.find()) {
            String pkg = matcher.group();
            // 意味のある部分を抽出（例: nablarch.fw.web → fw.web）
            if (pkg.length() > 8) {
                components.add(pkg);
            }
        }
        return components;
    }

    /**
     * ハンドラ名を抽出する。
     */
    private Set<String> extractHandlers(String text) {
        Set<String> handlers = new LinkedHashSet<>();
        Matcher matcher = HANDLER_PATTERN.matcher(text);
        while (matcher.find()) {
            handlers.add(matcher.group(1));
        }
        return handlers;
    }

    /**
     * 検索キーワードを生成する。
     */
    private List<String> generateSearchKeywords(String errorMessage, ErrorAnalysis analysis) {
        List<String> keywords = new ArrayList<>();

        // エラーコードがあれば最優先
        if (analysis.errorCode != null) {
            keywords.add(analysis.errorCode);
        }

        // 例外クラス名
        String[] words = errorMessage.split("[\\s:]+");
        for (String word : words) {
            if (word.endsWith("Exception") || word.endsWith("Error")) {
                keywords.add(word);
            }
        }

        // 関連ハンドラ
        keywords.addAll(analysis.relatedHandlers);

        // カテゴリ
        if (analysis.category != null && !analysis.category.equals("general")) {
            keywords.add(analysis.category);
        }

        // 重複排除
        return keywords.stream().distinct().limit(5).toList();
    }

    /**
     * 知識ベースを検索する。
     */
    private List<String> searchKnowledgeBase(ErrorAnalysis analysis) {
        Set<String> allResults = new LinkedHashSet<>();

        // 各キーワードで検索
        for (String keyword : analysis.searchKeywords) {
            List<String> results = knowledgeBase.search(keyword, "error");
            allResults.addAll(results);
        }

        // エラー以外のカテゴリでも検索（関連情報取得）
        if (!analysis.relatedHandlers.isEmpty()) {
            for (String handler : analysis.relatedHandlers) {
                List<String> results = knowledgeBase.search(handler, "handler");
                allResults.addAll(results);
            }
        }

        // カテゴリで追加検索
        if (analysis.category != null) {
            List<String> categoryResults = knowledgeBase.search(analysis.category, null);
            // 最大5件追加
            int count = 0;
            for (String result : categoryResults) {
                if (!allResults.contains(result) && count < 5) {
                    allResults.add(result);
                    count++;
                }
            }
        }

        return new ArrayList<>(allResults);
    }

    /**
     * トラブルシューティング結果をフォーマットする。
     */
    private String formatResult(ErrorAnalysis analysis, List<String> searchResults,
            String environment) {
        StringBuilder sb = new StringBuilder();

        sb.append("# トラブルシューティング結果\n\n");

        // エラー分析セクション
        sb.append("## エラー分析\n\n");
        sb.append("| 項目 | 値 |\n");
        sb.append("|------|----|n");
        sb.append("| カテゴリ | ").append(formatCategory(analysis.category)).append(" |\n");
        if (analysis.errorCode != null) {
            sb.append("| エラーコード | ").append(analysis.errorCode).append(" |\n");
        }
        if (!analysis.relatedHandlers.isEmpty()) {
            sb.append("| 関連ハンドラ | ")
                    .append(String.join(", ", analysis.relatedHandlers)).append(" |\n");
        }
        if (!analysis.relatedComponents.isEmpty()) {
            sb.append("| 関連コンポーネント | ")
                    .append(String.join(", ", limitSet(analysis.relatedComponents, 3)))
                    .append(" |\n");
        }
        sb.append("\n");

        // 解決策セクション
        sb.append("## 解決策・関連情報\n\n");
        if (searchResults.isEmpty()) {
            sb.append("知識ベースに直接一致する解決策が見つかりませんでした。\n\n");
            sb.append("### 一般的な確認事項\n\n");
            sb.append(getGeneralGuidance(analysis.category));
        } else {
            sb.append("知識ベースから ").append(searchResults.size()).append(" 件の関連情報が見つかりました。\n\n");
            int count = 0;
            for (String result : searchResults) {
                sb.append(result).append("\n\n");
                count++;
                if (count >= 5) {
                    if (searchResults.size() > 5) {
                        sb.append("（他 ").append(searchResults.size() - 5).append(" 件省略）\n\n");
                    }
                    break;
                }
            }
        }

        // 追加リソースセクション
        sb.append("## 関連ドキュメント\n\n");
        sb.append("- [Nablarch公式ドキュメント](https://nablarch.github.io/docs/LATEST/doc/)\n");
        sb.append("- [Nablarch Application Framework解説書](https://nablarch.github.io/docs/LATEST/doc/application_framework/application_framework/index.html)\n");
        if (analysis.category.equals("handler")) {
            sb.append("- [ハンドラキュー設計](https://nablarch.github.io/docs/LATEST/doc/application_framework/application_framework/handlers/index.html)\n");
        } else if (analysis.category.equals("database")) {
            sb.append("- [ユニバーサルDAO](https://nablarch.github.io/docs/LATEST/doc/application_framework/application_framework/libraries/database/database.html)\n");
        }

        // メタデータ
        sb.append("\n---\n");
        sb.append("*検索キーワード: ").append(String.join(", ", analysis.searchKeywords)).append("*\n");

        return sb.toString();
    }

    /**
     * カテゴリを日本語でフォーマットする。
     */
    private String formatCategory(String category) {
        return switch (category) {
            case "handler" -> "ハンドラキュー関連";
            case "database" -> "データベース関連";
            case "validation" -> "バリデーション関連";
            case "config" -> "設定・コンポーネント定義関連";
            case "batch" -> "バッチ処理関連";
            default -> "一般";
        };
    }

    /**
     * 一般的なガイダンスを取得する。
     */
    private String getGeneralGuidance(String category) {
        return switch (category) {
            case "handler" -> """
                    1. ハンドラキューの順序を確認してください
                    2. 必須ハンドラ（GlobalErrorHandler, DbConnectionManagementHandler等）が含まれているか確認
                    3. handler-catalog.yaml の推奨構成を参照してください
                    """;
            case "database" -> """
                    1. データソース設定（JDBC URL、ユーザー、パスワード）を確認してください
                    2. DbConnectionManagementHandlerがハンドラキューに含まれているか確認
                    3. トランザクション管理の設定を確認してください
                    """;
            case "validation" -> """
                    1. フォームクラスのバリデーションアノテーションを確認してください
                    2. メッセージ定義ファイルにエラーメッセージが定義されているか確認
                    3. @OnErrorでエラー遷移先が設定されているか確認してください
                    """;
            case "config" -> """
                    1. コンポーネント定義XMLの構文エラーがないか確認してください
                    2. クラスパスに必要なモジュールが含まれているか確認
                    3. コンポーネント名とproperty名の整合性を確認してください
                    """;
            case "batch" -> """
                    1. バッチプロセスの多重起動チェック設定を確認してください
                    2. DataReaderの設定とSQLを確認してください
                    3. ループハンドラの設定を確認してください
                    """;
            default -> """
                    1. エラーメッセージとスタックトレースを確認してください
                    2. 関連するNablarchモジュールがクラスパスに含まれているか確認
                    3. コンポーネント定義とハンドラキューの設定を見直してください
                    """;
        };
    }

    /**
     * Setの要素数を制限する。
     */
    private Set<String> limitSet(Set<String> set, int limit) {
        Set<String> limited = new LinkedHashSet<>();
        int count = 0;
        for (String s : set) {
            if (count >= limit) break;
            limited.add(s);
            count++;
        }
        return limited;
    }

    /**
     * 文字列を指定長に切り詰める。
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * エラー分析結果を保持する内部クラス。
     */
    private static class ErrorAnalysis {
        String errorCode;
        String category;
        Set<String> relatedComponents = new LinkedHashSet<>();
        Set<String> relatedHandlers = new LinkedHashSet<>();
        List<String> searchKeywords = new ArrayList<>();
    }
}

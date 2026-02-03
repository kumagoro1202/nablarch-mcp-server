package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.DesignPatternEntry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nablarchデザインパターン推薦ツール。
 *
 * <p>ユーザーの自然言語要件に基づいて、Nablarch固有の11種デザインパターンから
 * 最適なものをスコアリング付きで推薦する。</p>
 *
 * <p>4フェーズ処理フロー：</p>
 * <ol>
 *   <li>要件分析 - キーワード抽出、カテゴリ推定</li>
 *   <li>候補収集 - 静的知識ベースからパターン収集</li>
 *   <li>スコアリング - S1〜S4の重み付けスコア計算</li>
 *   <li>ランキング生成 - Markdownフォーマット出力</li>
 * </ol>
 *
 * @see <a href="docs/designs/20_tool-recommend-pattern.md">設計書</a>
 */
@Service
public class RecommendPatternTool {

    /** スコアリング重み: キーワード一致度 */
    private static final double WEIGHT_KEYWORD = 0.40;
    /** スコアリング重み: カテゴリ一致度 */
    private static final double WEIGHT_CATEGORY = 0.25;
    /** スコアリング重み: app_type適合度 */
    private static final double WEIGHT_APP_TYPE = 0.20;
    /** スコアリング重み: 制約一致度 */
    private static final double WEIGHT_CONSTRAINT = 0.15;

    /** 最小スコア閾値 */
    private static final double MIN_SCORE_THRESHOLD = 0.20;

    /** カテゴリ別キーワードマッピング */
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
            "architecture", Arrays.asList("ハンドラ", "キュー", "パイプライン", "アーキテクチャ", "構造", "handler", "queue"),
            "data-access", Arrays.asList("DB", "SQL", "DAO", "CRUD", "データベース", "クエリ", "排他", "ロック", "database"),
            "action", Arrays.asList("アクション", "コントローラ", "リクエスト", "画面", "action", "controller"),
            "validation", Arrays.asList("バリデーション", "入力チェック", "フォーム", "validation", "form"),
            "security", Arrays.asList("認証", "CSRF", "トークン", "二重送信", "セキュリティ", "security"),
            "handler", Arrays.asList("インターセプタ", "フィルタ", "横断", "interceptor", "filter")
    );

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public RecommendPatternTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 要件に基づいてNablarchデザインパターンを推薦する。
     *
     * @param requirement 自然言語要件記述（必須、10文字以上）
     * @param appType アプリケーションタイプ（web, rest, batch, messaging）
     * @param constraints 追加の制約条件（カンマ区切り）
     * @param maxResults 返却する最大候補数（1〜11、デフォルト3）
     * @return Markdown形式の推薦結果
     */
    @Tool(description = "Recommends Nablarch design patterns based on requirements. "
            + "Analyzes natural language requirements and returns scored pattern recommendations "
            + "with rationale and code examples.")
    public String recommend(
            @ToolParam(description = "Natural language description of the requirement (min 10 chars)")
            String requirement,
            @ToolParam(description = "Application type: web, rest, batch, messaging", required = false)
            String appType,
            @ToolParam(description = "Additional constraints (comma-separated)", required = false)
            String constraints,
            @ToolParam(description = "Maximum number of results (1-11, default 3)", required = false)
            Integer maxResults) {

        // 入力検証
        if (requirement == null || requirement.isBlank()) {
            return "エラー: 要件テキストが指定されていません";
        }
        if (requirement.length() < 10) {
            return "エラー: 要件テキストが短すぎます（10文字以上必要）";
        }

        Set<String> validAppTypes = knowledgeBase.getAvailableAppTypes();
        if (appType != null && !appType.isBlank() && !validAppTypes.contains(appType.toLowerCase())) {
            return "エラー: 不明なアプリケーションタイプ: " + appType
                    + "\n有効なタイプ: " + String.join(", ", validAppTypes);
        }

        int resultLimit = (maxResults != null && maxResults >= 1 && maxResults <= 11)
                ? maxResults : 3;

        // Phase 1: 要件分析
        AnalyzedRequirement analyzed = analyzeRequirement(requirement);

        // Phase 2: 候補収集
        List<DesignPatternEntry> candidates = knowledgeBase.getAllDesignPatterns();
        if (candidates.isEmpty()) {
            return "エラー: デザインパターンカタログが空です";
        }

        // Phase 3: スコアリング
        List<String> constraintList = parseConstraints(constraints);
        List<ScoredPattern> scoredPatterns = candidates.stream()
                .map(pattern -> scorePattern(pattern, analyzed, appType, constraintList))
                .filter(sp -> sp.totalScore >= MIN_SCORE_THRESHOLD)
                .sorted(Comparator.comparingDouble(ScoredPattern::totalScore).reversed())
                .limit(resultLimit)
                .collect(Collectors.toList());

        if (scoredPatterns.isEmpty()) {
            return "指定された条件に一致するパターンが見つかりませんでした。\n"
                    + "要件を別の言葉で表現するか、制約条件を緩和してください。";
        }

        // Phase 4: ランキング生成
        return generateRanking(requirement, appType, scoredPatterns);
    }

    /**
     * 要件テキストを分析し、キーワードとカテゴリを抽出する。
     */
    private AnalyzedRequirement analyzeRequirement(String requirement) {
        String lowerReq = requirement.toLowerCase();
        Set<String> keywords = new LinkedHashSet<>();
        String estimatedCategory = null;
        double maxCategoryScore = 0.0;

        for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            List<String> categoryKws = entry.getValue();
            int matchCount = 0;

            for (String kw : categoryKws) {
                if (lowerReq.contains(kw.toLowerCase())) {
                    keywords.add(kw);
                    matchCount++;
                }
            }

            double score = (double) matchCount / categoryKws.size();
            if (score > maxCategoryScore) {
                maxCategoryScore = score;
                estimatedCategory = category;
            }
        }

        // 追加キーワード抽出（要件から重要な単語を抽出）
        extractAdditionalKeywords(requirement, keywords);

        return new AnalyzedRequirement(keywords, estimatedCategory, maxCategoryScore);
    }

    /**
     * 要件から追加のキーワードを抽出する。
     */
    private void extractAdditionalKeywords(String requirement, Set<String> keywords) {
        // 重要な技術用語パターン
        String[] importantTerms = {
                "トランザクション", "接続", "プール", "非同期", "バッチ", "メッセージ",
                "ファイル", "ダウンロード", "アップロード", "設定", "コンポーネント",
                "リポジトリ", "DI", "依存性注入", "システム", "リクエスト", "レスポンス"
        };

        String lowerReq = requirement.toLowerCase();
        for (String term : importantTerms) {
            if (lowerReq.contains(term.toLowerCase())) {
                keywords.add(term);
            }
        }
    }

    /**
     * パターンをスコアリングする。
     */
    private ScoredPattern scorePattern(DesignPatternEntry pattern,
                                       AnalyzedRequirement analyzed,
                                       String appType,
                                       List<String> constraints) {

        // S1: キーワード一致度
        double s1 = calculateKeywordScore(pattern, analyzed.keywords);

        // S2: カテゴリ一致度
        double s2 = calculateCategoryScore(pattern, analyzed.estimatedCategory);

        // S3: app_type適合度
        double s3 = calculateAppTypeScore(pattern, appType);

        // S4: 制約一致度
        double s4 = calculateConstraintScore(pattern, constraints);

        double totalScore = s1 * WEIGHT_KEYWORD
                + s2 * WEIGHT_CATEGORY
                + s3 * WEIGHT_APP_TYPE
                + s4 * WEIGHT_CONSTRAINT;

        return new ScoredPattern(pattern, totalScore, s1, s2, s3, s4);
    }

    /**
     * キーワード一致度スコアを計算する。
     */
    private double calculateKeywordScore(DesignPatternEntry pattern, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0.5; // キーワードなしの場合は中間スコア
        }

        String searchText = buildSearchText(pattern).toLowerCase();
        int matchCount = 0;

        for (String keyword : keywords) {
            if (searchText.contains(keyword.toLowerCase())) {
                matchCount++;
            }
        }

        return (double) matchCount / keywords.size();
    }

    /**
     * パターンの検索対象テキストを構築する。
     */
    private String buildSearchText(DesignPatternEntry pattern) {
        StringBuilder sb = new StringBuilder();
        if (pattern.name != null) sb.append(pattern.name).append(" ");
        if (pattern.description != null) sb.append(pattern.description).append(" ");
        if (pattern.problem != null) sb.append(pattern.problem).append(" ");
        if (pattern.solution != null) sb.append(pattern.solution).append(" ");
        if (pattern.category != null) sb.append(pattern.category).append(" ");
        return sb.toString();
    }

    /**
     * カテゴリ一致度スコアを計算する。
     */
    private double calculateCategoryScore(DesignPatternEntry pattern, String estimatedCategory) {
        if (estimatedCategory == null || pattern.category == null) {
            return 0.5;
        }
        return pattern.category.equalsIgnoreCase(estimatedCategory) ? 1.0 : 0.3;
    }

    /**
     * app_type適合度スコアを計算する。
     */
    private double calculateAppTypeScore(DesignPatternEntry pattern, String appType) {
        if (appType == null || appType.isBlank()) {
            return 0.8; // app_type未指定の場合は高めのスコア
        }
        if (pattern.applicableAppTypes == null || pattern.applicableAppTypes.isEmpty()) {
            return 0.5; // パターンに制限がない場合は中間スコア
        }

        String normalizedAppType = appType.toLowerCase();
        boolean matches = pattern.applicableAppTypes.stream()
                .anyMatch(t -> t.equalsIgnoreCase(normalizedAppType));

        return matches ? 1.0 : 0.1;
    }

    /**
     * 制約一致度スコアを計算する。
     */
    private double calculateConstraintScore(DesignPatternEntry pattern, List<String> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return 0.7; // 制約なしの場合は高めのスコア
        }

        String searchText = buildSearchText(pattern).toLowerCase();
        int matchCount = 0;

        for (String constraint : constraints) {
            if (searchText.contains(constraint.toLowerCase())) {
                matchCount++;
            }
        }

        return constraints.isEmpty() ? 0.7 : (double) matchCount / constraints.size();
    }

    /**
     * 制約文字列をパースする。
     */
    private List<String> parseConstraints(String constraints) {
        if (constraints == null || constraints.isBlank()) {
            return List.of();
        }
        return Arrays.stream(constraints.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Markdownフォーマットのランキング結果を生成する。
     */
    private String generateRanking(String requirement, String appType, List<ScoredPattern> patterns) {
        StringBuilder sb = new StringBuilder();

        sb.append("## デザインパターン推薦結果\n\n");
        sb.append("**要件**: ").append(truncate(requirement, 100)).append("\n");
        if (appType != null && !appType.isBlank()) {
            sb.append("**アプリタイプ**: ").append(appType).append("\n");
        }
        sb.append("**候補数**: ").append(patterns.size()).append("件\n\n");
        sb.append("---\n\n");

        String[] rankEmoji = {"🥇", "🥈", "🥉"};

        for (int i = 0; i < patterns.size(); i++) {
            ScoredPattern sp = patterns.get(i);
            DesignPatternEntry pattern = sp.pattern;
            String emoji = i < rankEmoji.length ? rankEmoji[i] : "🔹";
            int scorePercent = (int) Math.round(sp.totalScore * 100);

            sb.append("### ").append(emoji).append(" 第").append(i + 1).append("位: ");
            sb.append(pattern.name != null ? pattern.name : "N/A");
            sb.append("（スコア: ").append(scorePercent).append("%）\n\n");

            sb.append("**カテゴリ**: ").append(pattern.category != null ? pattern.category : "N/A").append("\n");

            // 適合理由
            sb.append("**適合理由**: ");
            sb.append(generateFitReason(sp)).append("\n\n");

            // ソリューション概要
            if (pattern.solution != null && !pattern.solution.isBlank()) {
                sb.append("#### ソリューション概要\n");
                sb.append(pattern.solution).append("\n\n");
            }

            // コード例
            if (pattern.codeExample != null && !pattern.codeExample.isBlank()) {
                sb.append("#### コード例\n");
                sb.append("```java\n").append(pattern.codeExample.trim()).append("\n```\n\n");
            }

            // スコア内訳
            sb.append("#### スコア内訳\n");
            sb.append("| ファクター | スコア |\n");
            sb.append("|-----------|--------|\n");
            sb.append("| キーワード一致 | ").append(formatScore(sp.keywordScore)).append(" |\n");
            sb.append("| カテゴリ一致 | ").append(formatScore(sp.categoryScore)).append(" |\n");
            sb.append("| app_type適合 | ").append(formatScore(sp.appTypeScore)).append(" |\n");
            sb.append("| 制約一致 | ").append(formatScore(sp.constraintScore)).append(" |\n\n");

            // 対応アプリタイプ
            if (pattern.applicableAppTypes != null && !pattern.applicableAppTypes.isEmpty()) {
                sb.append("**対応アプリタイプ**: ");
                sb.append(String.join(", ", pattern.applicableAppTypes)).append("\n\n");
            }

            // リソースURI
            sb.append("**📖 詳細**: `nablarch://pattern/").append(pattern.name).append("`\n\n");

            if (i < patterns.size() - 1) {
                sb.append("---\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 適合理由テキストを生成する。
     */
    private String generateFitReason(ScoredPattern sp) {
        List<String> reasons = new ArrayList<>();

        if (sp.keywordScore >= 0.7) {
            reasons.add("要件のキーワードと高い一致度");
        } else if (sp.keywordScore >= 0.4) {
            reasons.add("要件のキーワードと部分的に一致");
        }

        if (sp.categoryScore >= 0.8) {
            reasons.add("カテゴリが要件と一致");
        }

        if (sp.appTypeScore >= 0.9) {
            reasons.add("指定アプリタイプに最適");
        }

        if (sp.constraintScore >= 0.7) {
            reasons.add("制約条件を満たす");
        }

        if (reasons.isEmpty()) {
            return "一般的な適用可能性に基づく推薦";
        }

        return String.join("、", reasons);
    }

    /**
     * スコアをパーセント表示にフォーマットする。
     */
    private String formatScore(double score) {
        return String.format("%d%%", (int) Math.round(score * 100));
    }

    /**
     * 文字列を指定長で切り詰める。
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 分析済み要件を表すレコード。
     */
    private record AnalyzedRequirement(
            Set<String> keywords,
            String estimatedCategory,
            double categoryConfidence
    ) {}

    /**
     * スコア付きパターンを表すレコード。
     */
    private record ScoredPattern(
            DesignPatternEntry pattern,
            double totalScore,
            double keywordScore,
            double categoryScore,
            double appTypeScore,
            double constraintScore
    ) {}
}

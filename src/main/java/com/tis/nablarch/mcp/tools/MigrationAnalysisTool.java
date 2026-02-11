package com.tis.nablarch.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Nablarchバージョン移行影響分析ツール。
 *
 * <p>Nablarch 5から6への移行において、既存コードの非推奨API使用を検出し、
 * 移行影響を分析して修正提案を生成する。</p>
 *
 * <p>処理フロー：</p>
 * <ol>
 *   <li>コードタイプ検出（Java/XML/POM/Properties）</li>
 *   <li>破壊的変更パターンマッチング</li>
 *   <li>影響分析・優先順位付け</li>
 *   <li>修正提案生成</li>
 * </ol>
 *
 * @see <a href="docs/designs/19_tool-analyze-migration.md">設計書</a>
 */
@Service
public class MigrationAnalysisTool {

    /** サポートするソースバージョン */
    private static final Set<String> SUPPORTED_SOURCE_VERSIONS = Set.of("5", "5.0", "5.1", "5.2");

    /** サポートするターゲットバージョン */
    private static final Set<String> SUPPORTED_TARGET_VERSIONS = Set.of("6", "6.0", "6.1");

    /** 破壊的変更パターンDB */
    private final List<MigrationPattern> migrationPatterns;

    /**
     * コンストラクタ。
     */
    public MigrationAnalysisTool() {
        this.migrationPatterns = initializeMigrationPatterns();
    }

    /**
     * 移行パターンDBを初期化する。
     */
    private List<MigrationPattern> initializeMigrationPatterns() {
        List<MigrationPattern> patterns = new ArrayList<>();

        // BC-001: javax.servlet → jakarta.servlet
        patterns.add(new MigrationPattern(
                "BC-001",
                "namespace",
                Pattern.compile("javax\\.servlet"),
                "jakarta.servlet",
                true,
                "trivial",
                "Jakarta EE 9以降ではjavax.servletがjakarta.servletに変更されました",
                "パッケージインポートを一括置換してください"
        ));

        // BC-002: javax.persistence → jakarta.persistence
        patterns.add(new MigrationPattern(
                "BC-002",
                "namespace",
                Pattern.compile("javax\\.persistence"),
                "jakarta.persistence",
                true,
                "trivial",
                "Jakarta EE 9以降ではjavax.persistenceがjakarta.persistenceに変更されました",
                "パッケージインポートを一括置換してください"
        ));

        // BC-003: DbAccessSupport削除
        patterns.add(new MigrationPattern(
                "BC-003",
                "api_removal",
                Pattern.compile("DbAccessSupport|extends\\s+DbAccessSupport"),
                null,
                false,
                "major",
                "DbAccessSupportクラスはNablarch 6で削除されました",
                "UniversalDaoまたはBasicDatabaseAccessを使用するようリファクタリングしてください"
        ));

        // BC-004: javax.annotation → jakarta.annotation
        patterns.add(new MigrationPattern(
                "BC-004",
                "namespace",
                Pattern.compile("javax\\.annotation\\.(?!processing)"),
                "jakarta.annotation",
                true,
                "trivial",
                "Jakarta EE 9以降ではjavax.annotationがjakarta.annotationに変更されました",
                "パッケージインポートを一括置換してください"
        ));

        // BC-005: SqlResultSet + search()メソッド変更
        patterns.add(new MigrationPattern(
                "BC-005",
                "api_change",
                Pattern.compile("SqlResultSet\\.search\\s*\\(|search\\s*\\(.*SqlResultSet"),
                null,
                false,
                "moderate",
                "SqlResultSetのsearchメソッドのシグネチャが変更されました",
                "戻り値の型と引数を確認し、新しいAPIに合わせて修正してください"
        ));

        // BC-006: javax.servlet-api依存関係
        patterns.add(new MigrationPattern(
                "BC-006",
                "dependency",
                Pattern.compile("<artifactId>\\s*javax\\.servlet-api\\s*</artifactId>"),
                "jakarta.servlet-api",
                true,
                "trivial",
                "javax.servlet-apiはjakarta.servlet-apiに置き換える必要があります",
                "pom.xmlの依存関係を更新してください"
        ));

        // BC-007: nablarch-bom 5.x
        patterns.add(new MigrationPattern(
                "BC-007",
                "dependency",
                Pattern.compile("<artifactId>\\s*nablarch-bom\\s*</artifactId>\\s*<version>\\s*5\\.[^<]+</version>"),
                "nablarch-bom:6.x",
                true,
                "trivial",
                "Nablarch BOMのバージョンを6.xにアップグレードする必要があります",
                "pom.xmlのnablarch-bomバージョンを6.xに更新してください"
        ));

        // BC-008: javax.validation → jakarta.validation
        patterns.add(new MigrationPattern(
                "BC-008",
                "namespace",
                Pattern.compile("javax\\.validation"),
                "jakarta.validation",
                true,
                "trivial",
                "Jakarta EE 9以降ではjavax.validationがjakarta.validationに変更されました",
                "パッケージインポートを一括置換してください"
        ));

        // BC-009: javax.inject → jakarta.inject
        patterns.add(new MigrationPattern(
                "BC-009",
                "namespace",
                Pattern.compile("javax\\.inject"),
                "jakarta.inject",
                true,
                "trivial",
                "Jakarta EE 9以降ではjavax.injectがjakarta.injectに変更されました",
                "パッケージインポートを一括置換してください"
        ));

        // BC-010: 非推奨Handler
        patterns.add(new MigrationPattern(
                "BC-010",
                "api_removal",
                Pattern.compile("HttpAccessLogHandler|ThreadContextClearHandler\\.Legacy"),
                null,
                false,
                "moderate",
                "一部のHandlerがNablarch 6で非推奨または削除されました",
                "公式移行ガイドを参照し、代替Handlerに置き換えてください"
        ));

        return Collections.unmodifiableList(patterns);
    }

    /**
     * コードの移行影響を分析する。
     *
     * @param codeSnippet 分析対象のコード（必須）
     * @param sourceVersion 移行元バージョン（デフォルト: "5"）
     * @param targetVersion 移行先バージョン（デフォルト: "6"）
     * @param analysisScope 分析範囲（full, namespace, dependency, api）
     * @return Markdown形式の分析結果
     */
    @Tool(name = "analyze_migration", description = "Analyzes Nablarch version migration impact. "
            + "Detects deprecated API usage, breaking changes, and generates migration recommendations "
            + "for upgrading from Nablarch 5.x to 6.x.")
    public String analyzeMigration(
            @ToolParam(description = "Code snippet to analyze (Java, XML, or POM)")
            String codeSnippet,
            @ToolParam(description = "Source Nablarch version (default: 5)", required = false)
            String sourceVersion,
            @ToolParam(description = "Target Nablarch version (default: 6)", required = false)
            String targetVersion,
            @ToolParam(description = "Analysis scope: full, namespace, dependency, api (default: full)", required = false)
            String analysisScope) {

        // 入力検証
        if (codeSnippet == null || codeSnippet.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("分析対象のコードが指定されていません")
                    .build();
        }

        String srcVer = normalizeVersion(sourceVersion, "5");
        String tgtVer = normalizeVersion(targetVersion, "6");

        if (!SUPPORTED_SOURCE_VERSIONS.contains(srcVer)) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("サポートされていない移行元バージョン: " + srcVer)
                    .hint("サポート対象: " + String.join(", ", SUPPORTED_SOURCE_VERSIONS))
                    .build();
        }

        if (!SUPPORTED_TARGET_VERSIONS.contains(tgtVer)) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("サポートされていない移行先バージョン: " + tgtVer)
                    .hint("サポート対象: " + String.join(", ", SUPPORTED_TARGET_VERSIONS))
                    .build();
        }

        String scope = (analysisScope != null && !analysisScope.isBlank())
                ? analysisScope.toLowerCase() : "full";

        // コードタイプ検出
        CodeType codeType = detectCodeType(codeSnippet);

        // パターンマッチング
        List<DetectedIssue> issues = analyzeCode(codeSnippet, scope);

        // 結果生成
        return generateReport(codeSnippet, srcVer, tgtVer, codeType, issues);
    }

    /**
     * バージョン文字列を正規化する。
     */
    private String normalizeVersion(String version, String defaultVersion) {
        if (version == null || version.isBlank()) {
            return defaultVersion;
        }
        String v = version.trim();
        // "5.1.2" → "5.1"、"6.0.1" → "6.0" のように正規化
        if (v.matches("\\d+\\.\\d+\\.\\d+")) {
            v = v.substring(0, v.lastIndexOf('.'));
        }
        // "5.x" → "5" のように正規化
        if (v.endsWith(".x")) {
            v = v.substring(0, v.length() - 2);
        }
        return v;
    }

    /**
     * コードタイプを検出する。
     */
    private CodeType detectCodeType(String code) {
        String trimmed = code.trim();

        // XML判定
        if (trimmed.startsWith("<?xml") || trimmed.startsWith("<project")
                || trimmed.startsWith("<component") || trimmed.startsWith("<list")) {
            if (trimmed.contains("<dependency>") || trimmed.contains("<artifactId>")) {
                return CodeType.POM;
            }
            return CodeType.XML;
        }

        // Properties判定
        if (trimmed.contains("=") && !trimmed.contains("{") && !trimmed.contains("import")) {
            long equalsLines = trimmed.lines()
                    .filter(line -> line.matches("^[\\w.]+\\s*=.*"))
                    .count();
            if (equalsLines > 0 && equalsLines >= trimmed.lines().count() / 2) {
                return CodeType.PROPERTIES;
            }
        }

        // Java判定（デフォルト）
        return CodeType.JAVA;
    }

    /**
     * コードを分析し、移行問題を検出する。
     */
    private List<DetectedIssue> analyzeCode(String code, String scope) {
        List<DetectedIssue> issues = new ArrayList<>();

        for (MigrationPattern pattern : migrationPatterns) {
            // スコープフィルタリング
            if (!matchesScope(pattern.category, scope)) {
                continue;
            }

            Matcher matcher = pattern.pattern.matcher(code);
            while (matcher.find()) {
                String matchedText = matcher.group();
                int lineNumber = calculateLineNumber(code, matcher.start());

                issues.add(new DetectedIssue(
                        pattern.id,
                        pattern.category,
                        matchedText,
                        lineNumber,
                        pattern.autoFixable,
                        pattern.effort,
                        pattern.description,
                        pattern.recommendation,
                        pattern.replacement
                ));
            }
        }

        return issues;
    }

    /**
     * スコープがパターンカテゴリに一致するか判定する。
     */
    private boolean matchesScope(String category, String scope) {
        if ("full".equals(scope)) {
            return true;
        }
        return category.equals(scope) || category.startsWith(scope);
    }

    /**
     * 文字位置から行番号を計算する。
     */
    private int calculateLineNumber(String code, int charPosition) {
        return (int) code.substring(0, charPosition).chars()
                .filter(ch -> ch == '\n')
                .count() + 1;
    }

    /**
     * Markdown形式のレポートを生成する。
     */
    private String generateReport(String code, String srcVer, String tgtVer,
                                  CodeType codeType, List<DetectedIssue> issues) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Nablarch移行影響分析レポート\n\n");

        // メタデータ
        sb.append("| 項目 | 値 |\n");
        sb.append("|------|-----|\n");
        sb.append("| 移行元バージョン | Nablarch ").append(srcVer).append(" |\n");
        sb.append("| 移行先バージョン | Nablarch ").append(tgtVer).append(" |\n");
        sb.append("| コードタイプ | ").append(codeType.displayName).append(" |\n");
        sb.append("| 検出問題数 | ").append(issues.size()).append("件 |\n\n");

        if (issues.isEmpty()) {
            sb.append("✅ **移行に影響する問題は検出されませんでした。**\n\n");
            sb.append("ただし、以下の点を手動で確認することを推奨します：\n");
            sb.append("- 使用しているNablarchモジュールの互換性\n");
            sb.append("- カスタムHandler/Interceptorの動作\n");
            sb.append("- 設定ファイル（XML）の構造変更\n");
            return sb.toString();
        }

        // サマリ
        long autoFixable = issues.stream().filter(i -> i.autoFixable).count();
        long manualRequired = issues.size() - autoFixable;

        sb.append("### サマリ\n\n");
        sb.append("| 分類 | 件数 |\n");
        sb.append("|------|------|\n");
        sb.append("| 🔧 自動修正可能 | ").append(autoFixable).append("件 |\n");
        sb.append("| 🔨 手動修正必要 | ").append(manualRequired).append("件 |\n\n");

        // 工数見積もり
        Map<String, Long> effortCounts = issues.stream()
                .collect(Collectors.groupingBy(i -> i.effort, Collectors.counting()));
        sb.append("### 工数見積もり\n\n");
        sb.append("| 工数レベル | 件数 | 目安 |\n");
        sb.append("|-----------|------|------|\n");
        sb.append("| trivial | ").append(effortCounts.getOrDefault("trivial", 0L))
                .append("件 | 数分/件 |\n");
        sb.append("| moderate | ").append(effortCounts.getOrDefault("moderate", 0L))
                .append("件 | 数時間/件 |\n");
        sb.append("| major | ").append(effortCounts.getOrDefault("major", 0L))
                .append("件 | 数日/件 |\n\n");

        // 詳細
        sb.append("---\n\n");
        sb.append("### 検出された問題\n\n");

        // カテゴリごとにグループ化
        Map<String, List<DetectedIssue>> byCategory = issues.stream()
                .collect(Collectors.groupingBy(i -> i.category));

        for (Map.Entry<String, List<DetectedIssue>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<DetectedIssue> categoryIssues = entry.getValue();

            sb.append("#### ").append(getCategoryDisplayName(category)).append("\n\n");

            for (DetectedIssue issue : categoryIssues) {
                String icon = issue.autoFixable ? "🔧" : "🔨";
                sb.append(icon).append(" **").append(issue.id).append("**: ");
                sb.append(issue.description).append("\n\n");

                sb.append("- **検出箇所**: 行").append(issue.lineNumber).append("\n");
                sb.append("- **該当コード**: `").append(truncate(issue.matchedText, 60)).append("`\n");
                sb.append("- **工数**: ").append(issue.effort).append("\n");
                sb.append("- **推奨対応**: ").append(issue.recommendation).append("\n");

                if (issue.autoFixable && issue.replacement != null) {
                    sb.append("- **修正後**: `").append(issue.replacement).append("`\n");
                }

                sb.append("\n");
            }
        }

        // 推奨事項
        sb.append("---\n\n");
        sb.append("### 推奨移行手順\n\n");
        sb.append("1. **自動修正可能な問題を先に対応**\n");
        sb.append("   - 一括置換ツール（IDE機能やsed）を使用\n");
        sb.append("   - namespace変更は全ファイル一括で実施\n\n");
        sb.append("2. **手動修正が必要な問題に対応**\n");
        sb.append("   - API削除は代替実装の検討が必要\n");
        sb.append("   - 公式移行ガイドを参照\n\n");
        sb.append("3. **テスト実行**\n");
        sb.append("   - 単体テストの実行確認\n");
        sb.append("   - 結合テストでの動作確認\n\n");

        // 参考リソース
        sb.append("### 参考リソース\n\n");
        sb.append("- 📖 `nablarch://guide/migration-5to6` - 公式移行ガイド\n");
        sb.append("- 📖 `nablarch://handler/web` - ハンドラカタログ\n");
        sb.append("- 📖 `nablarch://api/database` - データベースアクセスAPI\n");

        return sb.toString();
    }

    /**
     * カテゴリの表示名を取得する。
     */
    private String getCategoryDisplayName(String category) {
        return switch (category) {
            case "namespace" -> "名前空間の変更（javax → jakarta）";
            case "dependency" -> "依存関係の変更";
            case "api_removal" -> "削除されたAPI";
            case "api_change" -> "APIシグネチャの変更";
            default -> category;
        };
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
     * コードタイプを表す列挙型。
     */
    private enum CodeType {
        JAVA("Java"),
        XML("XML (Component Definition)"),
        POM("POM (Maven)"),
        PROPERTIES("Properties");

        final String displayName;

        CodeType(String displayName) {
            this.displayName = displayName;
        }
    }

    /**
     * 移行パターンを表すレコード。
     */
    private record MigrationPattern(
            String id,
            String category,
            Pattern pattern,
            String replacement,
            boolean autoFixable,
            String effort,
            String description,
            String recommendation
    ) {}

    /**
     * 検出された問題を表すレコード。
     */
    private record DetectedIssue(
            String id,
            String category,
            String matchedText,
            int lineNumber,
            boolean autoFixable,
            String effort,
            String description,
            String recommendation,
            String replacement
    ) {}
}

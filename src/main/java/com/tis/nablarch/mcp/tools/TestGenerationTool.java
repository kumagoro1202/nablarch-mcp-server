package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import com.tis.nablarch.mcp.tools.testgen.ClassInfo;
import com.tis.nablarch.mcp.tools.testgen.ExcelStructureGenerator;
import com.tis.nablarch.mcp.tools.testgen.TestCodeGenerator;
import com.tis.nablarch.mcp.tools.testgen.TestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCPツール: generate_test。
 *
 * <p>Nablarch Testing Framework（JUnit5 + Excelテストデータ）に準拠した
 * テストコードを生成するツール。テスト対象クラスのFQCNとテストタイプを
 * 指定することで、テストクラス・テストメソッド・Excelテストデータ構造を出力する。</p>
 *
 * <p>RAGパイプラインが利用可能な場合はテストパターンとテスト規約を検索して
 * コンテキストを補強する。利用不可能な場合は静的知識ベースにフォールバックする。</p>
 *
 * @see TestType
 * @see TestCodeGenerator
 * @see ExcelStructureGenerator
 */
@Service
public class TestGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationTool.class);

    /**
     * RAG検索で取得する結果件数。
     */
    private static final int RAG_TOP_K = 5;

    /**
     * リランキングに渡す候補数。
     */
    private static final int RAG_CANDIDATE_K = 20;

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * ハイブリッド検索サービス（RAG Phase 2以降で利用可能）。
     */
    @Autowired(required = false)
    private HybridSearchService hybridSearchService;

    /**
     * リランカー（RAG Phase 2以降で利用可能）。
     */
    @Autowired(required = false)
    private Reranker reranker;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public TestGenerationTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Nablarch Testing Frameworkに準拠したテストコードを生成する。
     *
     * @param targetClass テスト対象クラスのFQCN
     * @param testType テストタイプ（unit, request-response, batch, messaging）
     * @param format 出力フォーマット（junit5, nablarch-excel）
     * @param testCases 生成するテストケースの指示（自然言語、任意）
     * @param includeExcel Excelテストデータ構造を含めるか（true/false）
     * @param coverageTarget カバレッジ目標（minimal, standard, comprehensive）
     * @return テストコードとExcelテストデータ構造のMarkdownフォーマット文字列
     */
    @Tool(description = "Generate test code for Nablarch applications using the Nablarch Testing "
            + "Framework (JUnit5 + Excel test data). Supports unit tests, request-response tests "
            + "(web/REST), batch tests, and messaging tests. Generates test classes, test methods, "
            + "and Excel test data structure definitions following Nablarch conventions.")
    public String generateTest(
            @ToolParam(description = "Fully qualified class name of the class to test "
                    + "(e.g. com.example.action.UserRegistrationAction)")
            String targetClass,
            @ToolParam(description = "Test type: unit (plain JUnit5), request-response "
                    + "(web/REST request testing), batch (batch action), messaging (MOM messaging)")
            String testType,
            @ToolParam(description = "Output format: junit5 (default) or nablarch-excel "
                    + "(includes Excel test data structure)",
                    required = false)
            String format,
            @ToolParam(description = "Optional: natural language description of test cases "
                    + "(e.g. 'normal registration, validation error, duplicate email')",
                    required = false)
            String testCases,
            @ToolParam(description = "Include Excel test data structure: true (default) or false",
                    required = false)
            String includeExcel,
            @ToolParam(description = "Coverage target: minimal, standard (default), comprehensive",
                    required = false)
            String coverageTarget) {

        // 入力検証
        if (targetClass == null || targetClass.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("テスト対象クラスのFQCNを指定してください")
                    .build();
        }
        if (testType == null || testType.isBlank()) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("テストタイプを指定してください")
                    .hint("有効なタイプ: unit, request-response, batch, messaging")
                    .build();
        }

        TestType effectiveTestType = TestType.parse(testType);
        if (effectiveTestType == null) {
            return ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_002)
                    .message("不明なテストタイプ: " + testType)
                    .hint("有効なタイプ: unit, request-response, batch, messaging")
                    .build();
        }

        // パラメータ正規化
        String effectiveFormat = parseFormat(format);
        boolean effectiveIncludeExcel = parseIncludeExcel(includeExcel);
        String effectiveCoverage = parseCoverageTarget(coverageTarget);

        try {
            return doGenerate(targetClass, effectiveTestType, effectiveFormat,
                    effectiveIncludeExcel, effectiveCoverage, nullIfBlank(testCases));
        } catch (Exception e) {
            log.error("generate_test実行中にエラーが発生: {}", e.getMessage(), e);
            throw ErrorResponseBuilder.of(ErrorCode.MCP_TOOL_004)
                    .message("テストコードの生成に失敗しました")
                    .detail("入力パラメータを確認してください")
                    .hint("search_apiツールで \"request-unit-test\" を検索、"
                            + "またはnablarch://guide/testingリソースを参照")
                    .toException(e);
        }
    }

    /**
     * テスト生成のメイン処理。
     */
    private String doGenerate(String targetClass, TestType testType,
                              String format, boolean includeExcel,
                              String coverageTarget, String testCasesHint) {

        ClassInfo classInfo = ClassInfo.parse(targetClass);

        // RAGコンテキスト検索
        List<String> ragContext = searchTestContext(classInfo, testType);

        // テストケースリスト構築
        List<TestCodeGenerator.TestCase> testCaseList =
                TestCodeGenerator.buildTestCases(testCasesHint, coverageTarget,
                        testType.name().toLowerCase().replace("_", "-"));

        // テストコード生成
        String testCode = TestCodeGenerator.generate(
                classInfo, testType, coverageTarget, testCasesHint);

        // Excel構造生成
        String excelStructure = includeExcel
                ? ExcelStructureGenerator.generate(classInfo, testType, testCaseList)
                : "";

        // 適用規約の収集
        List<String> conventions = collectAppliedConventions(testType);

        // 結果整形
        return formatResult(classInfo, testType, format, coverageTarget,
                testCode, excelStructure, testCaseList, conventions, ragContext);
    }

    /**
     * RAGパイプラインを使用してテスト関連のコンテキストを検索する。
     *
     * <p>RAGが利用不可能な場合は静的知識ベースにフォールバックする。</p>
     */
    private List<String> searchTestContext(ClassInfo classInfo, TestType testType) {
        List<String> context = new ArrayList<>();

        if (hybridSearchService != null && reranker != null) {
            try {
                context.addAll(searchWithRag(classInfo, testType));
            } catch (Exception e) {
                log.warn("RAG検索でエラーが発生。静的知識ベースにフォールバック: {}",
                        e.getMessage());
                context.addAll(searchWithStaticKb(testType));
            }
        } else {
            context.addAll(searchWithStaticKb(testType));
        }

        return context;
    }

    /**
     * RAGパイプラインでテストパターンを検索する。
     */
    private List<String> searchWithRag(ClassInfo classInfo, TestType testType) {
        List<String> context = new ArrayList<>();

        // 検索1: テストパターン
        SearchFilters docFilter = new SearchFilters(
                null, null, null, "documentation", null);
        List<SearchResult> candidates = hybridSearchService.search(
                "Nablarch " + testType.label() + " テストパターン テスト例",
                docFilter, RAG_CANDIDATE_K, SearchMode.HYBRID);
        List<SearchResult> patternResults = candidates.isEmpty()
                ? candidates : reranker.rerank(
                "Nablarch " + testType.label() + " テストパターン",
                candidates, RAG_TOP_K);

        for (SearchResult r : patternResults) {
            context.add(r.content());
        }

        // 検索2: テスト規約
        List<SearchResult> convCandidates = hybridSearchService.search(
                "Nablarch テスティングフレームワーク " + testType.superClassName()
                        + " Excel テストデータ 規約",
                docFilter, RAG_CANDIDATE_K, SearchMode.HYBRID);
        List<SearchResult> convResults = convCandidates.isEmpty()
                ? convCandidates : reranker.rerank(
                "Nablarch テスト規約 " + testType.superClassName(),
                convCandidates, RAG_TOP_K);

        for (SearchResult r : convResults) {
            context.add(r.content());
        }

        return context;
    }

    /**
     * 静的知識ベースからテスト関連情報を検索する。
     */
    private List<String> searchWithStaticKb(TestType testType) {
        List<String> context = new ArrayList<>();
        List<String> testResults = knowledgeBase.search("test", "testing");
        context.addAll(testResults);

        // テストタイプ固有の検索
        if (testType.hasSuperClass()) {
            List<String> superClassResults =
                    knowledgeBase.search(testType.superClassName(), null);
            context.addAll(superClassResults);
        }

        return context;
    }

    /**
     * 適用されたNablarch規約を収集する。
     */
    private List<String> collectAppliedConventions(TestType testType) {
        List<String> conventions = new ArrayList<>();
        conventions.add("テストメソッド名は日本語で振る舞いを記述");
        conventions.add("テストデータはExcelファイルで管理（Nablarch Testing Framework規約）");

        if (testType.hasSuperClass()) {
            conventions.add("テストスーパークラス: " + testType.superClassName()
                    + " (" + testType.superClassFqcn() + ")");
        }

        switch (testType) {
            case REQUEST_RESPONSE -> {
                conventions.add("executeメソッドでHTTPリクエストをシミュレート");
                conventions.add("Excelのシート名がテストメソッドに対応");
            }
            case BATCH -> {
                conventions.add("バッチ実行はexecuteメソッドで実行");
                conventions.add("setupTable/expectedTableで入出力データを検証");
            }
            case MESSAGING -> {
                conventions.add("requestMessage/expectedMessageシートでメッセージを定義");
            }
            default -> {
                conventions.add("JUnit5標準アサーションを使用");
            }
        }

        return conventions;
    }

    /**
     * 結果をMarkdown形式に整形する。
     */
    private String formatResult(ClassInfo classInfo, TestType testType,
                                String format, String coverageTarget,
                                String testCode, String excelStructure,
                                List<TestCodeGenerator.TestCase> testCases,
                                List<String> conventions,
                                List<String> ragContext) {

        StringBuilder sb = new StringBuilder();
        sb.append("## テスト生成結果: ").append(classInfo.className()).append("\n");
        sb.append("テストタイプ: ").append(testType.label());
        sb.append(" | フォーマット: ").append(format);
        sb.append(" | カバレッジ: ").append(coverageTarget).append("\n\n---\n\n");

        // テストクラス
        sb.append("### テストクラス: ").append(classInfo.testClassName()).append("\n\n");
        sb.append("```java\n").append(testCode).append("```\n\n");

        // Excelテストデータ構造
        if (!excelStructure.isEmpty()) {
            sb.append("---\n\n").append(excelStructure).append("\n\n");
        }

        // テストケース一覧
        sb.append("---\n\n### テストケース一覧\n\n");
        sb.append("| # | テストメソッド | 説明 |");
        if (testType == TestType.REQUEST_RESPONSE) {
            sb.append(" 期待ステータス |");
        }
        sb.append("\n");
        sb.append("|---|-------------|------|");
        if (testType == TestType.REQUEST_RESPONSE) {
            sb.append("-------------|");
        }
        sb.append("\n");

        for (int i = 0; i < testCases.size(); i++) {
            TestCodeGenerator.TestCase tc = testCases.get(i);
            sb.append("| ").append(i + 1).append(" | `")
                    .append(tc.methodName()).append("` | ")
                    .append(tc.description()).append(" |");
            if (testType == TestType.REQUEST_RESPONSE) {
                sb.append(" ").append(tc.expectedStatus()).append(" |");
            }
            sb.append("\n");
        }

        // 適用規約
        sb.append("\n---\n\n### 適用されたNablarch規約\n\n");
        for (String conv : conventions) {
            sb.append("- ").append(conv).append("\n");
        }

        // 参考ドキュメント
        sb.append("\n### 参考ドキュメント\n\n");
        sb.append("- [Nablarch テスティングフレームワーク]")
                .append("(https://nablarch.github.io/docs/LATEST/doc/")
                .append("development_tools/testing_framework/)\n");
        if (testType == TestType.REQUEST_RESPONSE) {
            sb.append("- [リクエスト単体テスト]")
                    .append("(https://nablarch.github.io/docs/LATEST/doc/")
                    .append("development_tools/testing_framework/guide/")
                    .append("development_guide/06_TestFWGuide/")
                    .append("01_Abstract.html)\n");
        }

        return sb.toString();
    }

    // --- パラメータ解析ヘルパー ---

    private String parseFormat(String format) {
        if (format == null || format.isBlank()) {
            return "junit5";
        }
        return "nablarch-excel".equalsIgnoreCase(format) ? "nablarch-excel" : "junit5";
    }

    private boolean parseIncludeExcel(String includeExcel) {
        if (includeExcel == null || includeExcel.isBlank()) {
            return true;
        }
        return !"false".equalsIgnoreCase(includeExcel);
    }

    private String parseCoverageTarget(String coverageTarget) {
        if (coverageTarget == null || coverageTarget.isBlank()) {
            return "standard";
        }
        return switch (coverageTarget.toLowerCase()) {
            case "minimal" -> "minimal";
            case "comprehensive" -> "comprehensive";
            default -> "standard";
        };
    }

    private String nullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }
}

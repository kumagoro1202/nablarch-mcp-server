package com.tis.nablarch.mcp.tools.testgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Nablarch Testing Framework準拠のテストコードを生成するクラス。
 *
 * <p>テストタイプに応じたテストクラス・テストメソッドのJavaソースコードを
 * 文字列として生成する。RAG検索結果やユーザー指定のテストケースを考慮して
 * テストメソッドを構築する。</p>
 */
public class TestCodeGenerator {

    private TestCodeGenerator() {
    }

    /**
     * テストコードを生成する。
     *
     * @param classInfo テスト対象クラス情報
     * @param testType テストタイプ
     * @param coverageTarget カバレッジ目標
     * @param testCasesHint ユーザー指定のテストケース指示（nullの場合は自動生成）
     * @return Javaテストコード文字列
     */
    public static String generate(ClassInfo classInfo, TestType testType,
                                  String coverageTarget, String testCasesHint) {
        return switch (testType) {
            case UNIT -> generateUnitTest(classInfo, coverageTarget, testCasesHint);
            case REQUEST_RESPONSE -> generateRequestTest(classInfo, coverageTarget, testCasesHint);
            case BATCH -> generateBatchTest(classInfo, coverageTarget, testCasesHint);
            case MESSAGING -> generateMessagingTest(classInfo, coverageTarget, testCasesHint);
        };
    }

    /**
     * ユニットテストコードを生成する。
     */
    private static String generateUnitTest(ClassInfo ci, String coverage, String hint) {
        List<TestCase> cases = buildTestCases(hint, coverage, "unit");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ci.packageName()).append(";\n\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import static org.junit.jupiter.api.Assertions.*;\n\n");
        sb.append("/**\n");
        sb.append(" * {@link ").append(ci.className()).append("}のユニットテスト。\n");
        sb.append(" */\n");
        sb.append("public class ").append(ci.testClassName()).append(" {\n\n");

        for (TestCase tc : cases) {
            sb.append("    @Test\n");
            sb.append("    public void ").append(tc.methodName).append("() {\n");
            sb.append("        // TODO: テスト実装\n");
            sb.append("        // ").append(tc.description).append("\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * リクエスト単体テストコードを生成する。
     */
    private static String generateRequestTest(ClassInfo ci, String coverage, String hint) {
        List<TestCase> cases = buildTestCases(hint, coverage, "request-response");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ci.packageName()).append(";\n\n");
        sb.append("import nablarch.test.core.http.SimpleDbAndHttpFwTestSupport;\n");
        sb.append("import nablarch.fw.web.HttpResponse;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("/**\n");
        sb.append(" * {@link ").append(ci.className()).append("}のリクエスト単体テスト。\n");
        sb.append(" */\n");
        sb.append("public class ").append(ci.testClassName()).append("\n");
        sb.append("        extends SimpleDbAndHttpFwTestSupport {\n\n");

        for (TestCase tc : cases) {
            sb.append("    @Test\n");
            sb.append("    public void ").append(tc.methodName).append("() {\n");
            sb.append("        execute(\"").append(tc.sheetName).append("\",");
            sb.append(" new BasicHttpResponse(").append(tc.expectedStatus).append("));\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * バッチテストコードを生成する。
     */
    private static String generateBatchTest(ClassInfo ci, String coverage, String hint) {
        List<TestCase> cases = buildTestCases(hint, coverage, "batch");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ci.packageName()).append(";\n\n");
        sb.append("import nablarch.test.core.batch.BatchRequestTestSupport;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("/**\n");
        sb.append(" * {@link ").append(ci.className()).append("}のバッチテスト。\n");
        sb.append(" */\n");
        sb.append("public class ").append(ci.testClassName()).append("\n");
        sb.append("        extends BatchRequestTestSupport {\n\n");

        for (TestCase tc : cases) {
            sb.append("    @Test\n");
            sb.append("    public void ").append(tc.methodName).append("() {\n");
            sb.append("        execute(\"").append(tc.sheetName).append("\");\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * メッセージングテストコードを生成する。
     */
    private static String generateMessagingTest(ClassInfo ci, String coverage, String hint) {
        List<TestCase> cases = buildTestCases(hint, coverage, "messaging");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(ci.packageName()).append(";\n\n");
        sb.append("import nablarch.test.core.messaging.MessagingRequestTestSupport;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("/**\n");
        sb.append(" * {@link ").append(ci.className()).append("}のメッセージングテスト。\n");
        sb.append(" */\n");
        sb.append("public class ").append(ci.testClassName()).append("\n");
        sb.append("        extends MessagingRequestTestSupport {\n\n");

        for (TestCase tc : cases) {
            sb.append("    @Test\n");
            sb.append("    public void ").append(tc.methodName).append("() {\n");
            sb.append("        execute(\"").append(tc.sheetName).append("\");\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * テストケースリストを構築する。
     *
     * <p>ユーザー指定のヒントがある場合はそれを解析してテストケースを生成する。
     * ない場合はカバレッジ目標に応じたデフォルトテストケースを生成する。</p>
     *
     * @param hint ユーザー指定のテストケースヒント
     * @param coverage カバレッジ目標
     * @param type テストタイプ
     * @return テストケースリスト
     */
    public static List<TestCase> buildTestCases(String hint, String coverage, String type) {
        if (hint != null && !hint.isBlank()) {
            return parseTestCasesFromHint(hint, type);
        }
        return generateDefaultTestCases(coverage, type);
    }

    /**
     * ユーザー指定のヒント文字列からテストケースを解析する。
     */
    private static List<TestCase> parseTestCasesFromHint(String hint, String type) {
        List<TestCase> cases = new ArrayList<>();
        String[] parts = hint.split("[,、]");
        int index = 1;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String methodName = toMethodName(trimmed);
            String sheetName = toSheetName(trimmed, index);
            int status = guessStatusCode(trimmed, type);
            cases.add(new TestCase(methodName, sheetName, trimmed, status));
            index++;
        }
        return cases;
    }

    /**
     * デフォルトテストケースを生成する。
     */
    private static List<TestCase> generateDefaultTestCases(String coverage, String type) {
        List<TestCase> cases = new ArrayList<>();

        // 正常系は必ず生成
        cases.add(new TestCase(
                "正常に処理が完了すること",
                "normalCase",
                "正常系: 代表的な入力で正しく処理される",
                "request-response".equals(type) ? 200 : 0));

        if ("minimal".equals(coverage)) {
            return cases;
        }

        // standard: 主要な異常系を追加
        cases.add(new TestCase(
                "バリデーションエラーの場合エラーとなること",
                "validationError",
                "異常系: 必須項目が未入力",
                "request-response".equals(type) ? 400 : 0));

        cases.add(new TestCase(
                "存在しないデータへのアクセスでエラーとなること",
                "notFoundError",
                "異常系: 対象データが存在しない",
                "request-response".equals(type) ? 404 : 0));

        if ("comprehensive".equals(coverage)) {
            // comprehensive: 追加のエッジケース
            cases.add(new TestCase(
                    "境界値で正しく処理されること",
                    "boundaryValue",
                    "境界値: 最大長・最小値等の境界値テスト",
                    "request-response".equals(type) ? 200 : 0));

            cases.add(new TestCase(
                    "データ重複時にエラーとなること",
                    "duplicateError",
                    "異常系: 一意制約違反",
                    "request-response".equals(type) ? 409 : 0));

            cases.add(new TestCase(
                    "排他制御エラーの場合エラーとなること",
                    "optimisticLockError",
                    "異常系: 楽観的ロックエラー",
                    "request-response".equals(type) ? 409 : 0));
        }

        return cases;
    }

    /**
     * 日本語説明文からテストメソッド名を生成する。
     */
    private static String toMethodName(String description) {
        // 日本語メソッド名をそのまま使う（Nablarch規約に従う）
        String cleaned = description.replaceAll("[\\s　]+", "");
        if (!cleaned.endsWith("こと")) {
            cleaned = cleaned + "であること";
        }
        return cleaned;
    }

    /**
     * テストケース説明からExcelシート名を生成する。
     */
    private static String toSheetName(String description, int index) {
        // 英語ベースのシート名を推定
        if (description.contains("正常") || description.contains("normal")) {
            return "normalCase";
        }
        if (description.contains("バリデーション") || description.contains("validation")) {
            return "validationError";
        }
        if (description.contains("重複") || description.contains("duplicate")) {
            return "duplicateError";
        }
        if (description.contains("エラー") || description.contains("error")) {
            return "errorCase" + index;
        }
        return "testCase" + index;
    }

    /**
     * テストケース説明から期待HTTPステータスコードを推定する。
     */
    private static int guessStatusCode(String description, String type) {
        if (!"request-response".equals(type)) {
            return 0;
        }
        if (description.contains("正常") || description.contains("成功")
                || description.contains("登録") || description.contains("更新")) {
            return 200;
        }
        if (description.contains("バリデーション")) {
            return 400;
        }
        if (description.contains("存在しない") || description.contains("見つから")) {
            return 404;
        }
        if (description.contains("重複") || description.contains("排他")) {
            return 409;
        }
        return 200;
    }

    /**
     * テストケースを表す内部レコード。
     *
     * @param methodName テストメソッド名
     * @param sheetName Excelシート名
     * @param description テストケースの説明
     * @param expectedStatus 期待HTTPステータスコード（request-response以外は0）
     */
    public record TestCase(String methodName, String sheetName, String description, int expectedStatus) {
    }
}

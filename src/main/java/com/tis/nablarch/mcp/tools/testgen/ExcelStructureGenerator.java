package com.tis.nablarch.mcp.tools.testgen;

import java.util.List;

/**
 * NablarchテスティングフレームワークのExcelテストデータ構造を生成するクラス。
 *
 * <p>テストタイプに応じたExcelシート定義をMarkdown形式のテーブルとして出力する。
 * Nablarch固有のExcelベーステスト（testShots、setupTable、expectedTable等）の
 * シート構造を網羅する。</p>
 */
public class ExcelStructureGenerator {

    private ExcelStructureGenerator() {
    }

    /**
     * Excelテストデータ構造のMarkdownを生成する。
     *
     * @param classInfo テスト対象クラス情報
     * @param testType テストタイプ
     * @param testCases テストケースリスト
     * @return Markdown形式のExcelシート構造定義
     */
    public static String generate(ClassInfo classInfo, TestType testType,
                                  List<TestCodeGenerator.TestCase> testCases) {
        return switch (testType) {
            case UNIT -> generateUnitExcel(classInfo, testCases);
            case REQUEST_RESPONSE -> generateRequestExcel(classInfo, testCases);
            case BATCH -> generateBatchExcel(classInfo, testCases);
            case MESSAGING -> generateMessagingExcel(classInfo, testCases);
        };
    }

    /**
     * ユニットテスト用のExcel構造（DB操作用）。
     */
    private static String generateUnitExcel(ClassInfo ci,
                                            List<TestCodeGenerator.TestCase> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Excelテストデータ構造\n\n");
        sb.append("**ファイルパス**: `").append(ci.excelDataPath())
                .append(ci.testClassName()).append(".xlsx`\n\n");
        sb.append("ユニットテストではExcelテストデータは任意です。\n");
        sb.append("DB操作を伴うテストの場合、`DbAccessTestSupport`を継承し");
        sb.append("以下のシート構造を使用してください。\n\n");

        sb.append("| シート名 | 用途 | 説明 |\n");
        sb.append("|---------|------|------|\n");
        sb.append("| `setupTable={TABLE}` | 事前データ | テスト前にDBに投入するデータ |\n");
        sb.append("| `expectedTable={TABLE}` | 期待データ | テスト後に検証するDBデータ |\n");

        return sb.toString();
    }

    /**
     * リクエスト単体テスト用のExcel構造。
     */
    private static String generateRequestExcel(ClassInfo ci,
                                               List<TestCodeGenerator.TestCase> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Excelテストデータ構造\n\n");
        sb.append("**ファイルパス**: `").append(ci.excelDataPath())
                .append(ci.testClassName()).append(".xlsx`\n\n");

        // テストケース別シート
        sb.append("#### テストケース別シート\n\n");
        sb.append("| シート名 | 説明 | 期待ステータス |\n");
        sb.append("|---------|------|-------------|\n");

        for (TestCodeGenerator.TestCase tc : cases) {
            sb.append("| `").append(tc.sheetName()).append("` | ");
            sb.append(tc.description()).append(" | ");
            sb.append(tc.expectedStatus()).append(" |\n");
        }

        sb.append("\n#### 共通シート\n\n");
        sb.append("| シート名 | 用途 | 説明 |\n");
        sb.append("|---------|------|------|\n");
        sb.append("| `testShots` | リクエスト定義 | HTTPメソッド、URI、パラメータを定義 |\n");
        sb.append("| `setupTable={TABLE}` | 事前データ | テスト前にDBに投入するデータ |\n");
        sb.append("| `expectedTable={TABLE}` | 期待データ | テスト後に検証するDBデータ |\n");
        sb.append("| `expectedMessages` | 期待メッセージ | バリデーションエラーメッセージ等の検証 |\n");

        sb.append("\n#### testShotsシートの列定義\n\n");
        sb.append("| 列名 | 説明 | 例 |\n");
        sb.append("|----|------|-----|\n");
        sb.append("| no | テスト番号 | 1 |\n");
        sb.append("| description | テスト説明 | 正常登録 |\n");
        sb.append("| expectedStatusCode | 期待HTTPステータス | 200 |\n");
        sb.append("| setUpTable | 事前データシート参照 | setupTable=USERS |\n");
        sb.append("| expectedTable | 期待データシート参照 | expectedTable=USERS |\n");

        return sb.toString();
    }

    /**
     * バッチテスト用のExcel構造。
     */
    private static String generateBatchExcel(ClassInfo ci,
                                             List<TestCodeGenerator.TestCase> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Excelテストデータ構造\n\n");
        sb.append("**ファイルパス**: `").append(ci.excelDataPath())
                .append(ci.testClassName()).append(".xlsx`\n\n");

        sb.append("#### テストケース別シート\n\n");
        sb.append("| シート名 | 説明 |\n");
        sb.append("|---------|------|\n");

        for (TestCodeGenerator.TestCase tc : cases) {
            sb.append("| `").append(tc.sheetName()).append("` | ");
            sb.append(tc.description()).append(" |\n");
        }

        sb.append("\n#### 共通シート\n\n");
        sb.append("| シート名 | 用途 | 説明 |\n");
        sb.append("|---------|------|------|\n");
        sb.append("| `testShots` | バッチ実行定義 | requestPath, userId等のパラメータ |\n");
        sb.append("| `setupTable={TABLE}` | 事前データ | バッチ処理対象の入力データ |\n");
        sb.append("| `expectedTable={TABLE}` | 期待データ | バッチ処理後の期待結果データ |\n");
        sb.append("| `setupFile` | 入力ファイル | CSV/固定長等の入力ファイル定義 |\n");
        sb.append("| `expectedLog` | 期待ログ | 処理件数・エラーログ等の期待値 |\n");

        return sb.toString();
    }

    /**
     * メッセージングテスト用のExcel構造。
     */
    private static String generateMessagingExcel(ClassInfo ci,
                                                 List<TestCodeGenerator.TestCase> cases) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Excelテストデータ構造\n\n");
        sb.append("**ファイルパス**: `").append(ci.excelDataPath())
                .append(ci.testClassName()).append(".xlsx`\n\n");

        sb.append("#### テストケース別シート\n\n");
        sb.append("| シート名 | 説明 |\n");
        sb.append("|---------|------|\n");

        for (TestCodeGenerator.TestCase tc : cases) {
            sb.append("| `").append(tc.sheetName()).append("` | ");
            sb.append(tc.description()).append(" |\n");
        }

        sb.append("\n#### 共通シート\n\n");
        sb.append("| シート名 | 用途 | 説明 |\n");
        sb.append("|---------|------|------|\n");
        sb.append("| `requestMessage` | リクエストメッセージ | 送信メッセージのヘッダ・ボディ定義 |\n");
        sb.append("| `expectedMessage` | 応答メッセージ | 期待する応答メッセージの検証 |\n");
        sb.append("| `setupTable={TABLE}` | 事前データ | テスト前のDBデータ投入 |\n");
        sb.append("| `expectedTable={TABLE}` | 期待データ | テスト後の期待DBデータ |\n");

        return sb.toString();
    }
}

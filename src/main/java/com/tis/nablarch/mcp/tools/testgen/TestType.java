package com.tis.nablarch.mcp.tools.testgen;

/**
 * Nablarchテストの種類を表すenum。
 *
 * <p>各テストタイプはNablarch Testing Frameworkの対応するスーパークラスを持つ。
 * テストコード生成時に適切なスーパークラスとimport文を選択するために使用される。</p>
 */
public enum TestType {

    /**
     * ユニットテスト（JUnit5標準）。
     *
     * <p>スーパークラスなし。純粋なJUnit5テストを生成する。</p>
     */
    UNIT("ユニットテスト", null, null),

    /**
     * リクエスト単体テスト（Web/REST）。
     *
     * <p>HTTPリクエストをシミュレートしてアクションクラスをテストする。</p>
     */
    REQUEST_RESPONSE(
            "リクエスト単体テスト",
            "SimpleDbAndHttpFwTestSupport",
            "nablarch.test.core.http.SimpleDbAndHttpFwTestSupport"),

    /**
     * バッチテスト。
     *
     * <p>バッチアクションの実行とDB入出力を検証する。</p>
     */
    BATCH(
            "バッチテスト",
            "BatchRequestTestSupport",
            "nablarch.test.core.batch.BatchRequestTestSupport"),

    /**
     * メッセージングテスト（MOM）。
     *
     * <p>メッセージング処理のリクエスト・レスポンスを検証する。</p>
     */
    MESSAGING(
            "メッセージングテスト",
            "MessagingRequestTestSupport",
            "nablarch.test.core.messaging.MessagingRequestTestSupport");

    private final String label;
    private final String superClassName;
    private final String superClassFqcn;

    TestType(String label, String superClassName, String superClassFqcn) {
        this.label = label;
        this.superClassName = superClassName;
        this.superClassFqcn = superClassFqcn;
    }

    /**
     * テストタイプのラベル（表示名）を返す。
     *
     * @return テストタイプのラベル
     */
    public String label() {
        return label;
    }

    /**
     * テストスーパークラス名を返す。
     *
     * @return スーパークラス名（UNITの場合はnull）
     */
    public String superClassName() {
        return superClassName;
    }

    /**
     * テストスーパークラスのFQCNを返す。
     *
     * @return スーパークラスのFQCN（UNITの場合はnull）
     */
    public String superClassFqcn() {
        return superClassFqcn;
    }

    /**
     * テストスーパークラスを持つかどうかを返す。
     *
     * @return スーパークラスを持つ場合true
     */
    public boolean hasSuperClass() {
        return superClassName != null;
    }

    /**
     * 文字列からTestTypeを解析する。
     *
     * <p>以下の形式を受け付ける:</p>
     * <ul>
     *   <li>unit, UNIT</li>
     *   <li>request-response, REQUEST_RESPONSE, request_response</li>
     *   <li>batch, BATCH</li>
     *   <li>messaging, MESSAGING</li>
     * </ul>
     *
     * @param testType テストタイプ文字列
     * @return 対応するTestType、不明な場合はnull
     */
    public static TestType parse(String testType) {
        if (testType == null || testType.isBlank()) {
            return null;
        }

        String normalized = testType.trim().toLowerCase().replace("-", "_");

        return switch (normalized) {
            case "unit" -> UNIT;
            case "request_response" -> REQUEST_RESPONSE;
            case "batch" -> BATCH;
            case "messaging" -> MESSAGING;
            default -> null;
        };
    }
}

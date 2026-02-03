package com.tis.nablarch.mcp.tools.testgen;

/**
 * テスト対象クラスのFQCN解析結果を表すレコード。
 *
 * @param fqcn 完全修飾クラス名
 * @param packageName パッケージ名
 * @param className 単純クラス名
 * @param testClassName テストクラス名（{className}Test）
 */
public record ClassInfo(
        String fqcn,
        String packageName,
        String className,
        String testClassName
) {
    /**
     * FQCNを解析してClassInfoを生成する。
     *
     * @param fqcn 完全修飾クラス名
     * @return 解析結果
     */
    public static ClassInfo parse(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        String pkg = lastDot > 0 ? fqcn.substring(0, lastDot) : "";
        String cls = lastDot > 0 ? fqcn.substring(lastDot + 1) : fqcn;
        return new ClassInfo(fqcn, pkg, cls, cls + "Test");
    }

    /**
     * Excelテストデータの配置パスを返す。
     *
     * <p>Nablarch規約: src/test/resources/{パッケージパス}/{テストクラス名}/</p>
     *
     * @return テストデータの配置パス
     */
    public String excelDataPath() {
        String pkgPath = packageName.replace('.', '/');
        return "src/test/resources/" + pkgPath + "/" + testClassName + "/";
    }
}

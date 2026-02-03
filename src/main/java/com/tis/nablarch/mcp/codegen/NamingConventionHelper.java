package com.tis.nablarch.mcp.codegen;

/**
 * Nablarchの命名規約に基づくヘルパーユーティリティ。
 *
 * <p>クラス名、パッケージ名、ファイルパス等の命名変換を提供する。</p>
 */
public final class NamingConventionHelper {

    private NamingConventionHelper() {
    }

    /**
     * PascalCaseの名前からAction用クラス名を生成する。
     * 末尾に "Action" が付いていなければ付与する。
     *
     * @param name ベース名（例: "Product"）
     * @return Actionクラス名（例: "ProductAction"）
     */
    public static String toActionClassName(String name) {
        return name.endsWith("Action") ? name : name + "Action";
    }

    /**
     * PascalCaseの名前からForm用クラス名を生成する。
     *
     * @param name ベース名
     * @return Formクラス名（例: "ProductForm"）
     */
    public static String toFormClassName(String name) {
        return name.endsWith("Form") ? name : name + "Form";
    }

    /**
     * PascalCaseの名前からHandler用クラス名を生成する。
     *
     * @param name ベース名
     * @return Handlerクラス名（例: "AuditLogHandler"）
     */
    public static String toHandlerClassName(String name) {
        return name.endsWith("Handler") ? name : name + "Handler";
    }

    /**
     * PascalCaseの名前からInterceptor用クラス名を生成する。
     *
     * @param name ベース名
     * @return Interceptorクラス名（例: "AuditLogInterceptor"）
     */
    public static String toInterceptorClassName(String name) {
        return name.endsWith("Interceptor") ? name : name + "Interceptor";
    }

    /**
     * PascalCaseをlowercaseパス名に変換する。
     *
     * @param name PascalCase名（例: "UserRegistration"）
     * @return パス名（例: "userregistration"）
     */
    public static String toPathName(String name) {
        return stripSuffix(name).toLowerCase();
    }

    /**
     * PascalCaseを大文字スネークケースのテーブル名に変換する。
     *
     * @param name PascalCase名（例: "ProductCategory"）
     * @return テーブル名（例: "PRODUCT_CATEGORY"）
     */
    public static String toTableName(String name) {
        String base = stripSuffix(name);
        return toSnakeCase(base).toUpperCase();
    }

    /**
     * PascalCaseを大文字スネークケースのカラム名に変換する。
     *
     * @param fieldName camelCase名（例: "productName"）
     * @return カラム名（例: "PRODUCT_NAME"）
     */
    public static String toColumnName(String fieldName) {
        return toSnakeCase(fieldName).toUpperCase();
    }

    /**
     * フィールド名の先頭を大文字にする（getter/setter生成用）。
     *
     * @param fieldName フィールド名（例: "productName"）
     * @return 先頭大文字名（例: "ProductName"）
     */
    public static String capitalize(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * IDフィールド名を推定する。
     *
     * @param entityName Entity名（例: "Product"）
     * @return IDフィールド名（例: "productId"）
     */
    public static String toIdFieldName(String entityName) {
        String base = stripSuffix(entityName);
        return Character.toLowerCase(base.charAt(0)) + base.substring(1) + "Id";
    }

    /**
     * Javaソースファイルのパスを生成する。
     *
     * @param basePackage ベースパッケージ（例: "com.example"）
     * @param subPackage サブパッケージ（例: "action"）
     * @param className クラス名（例: "ProductAction"）
     * @return ファイルパス（例: "src/main/java/com/example/action/ProductAction.java"）
     */
    public static String toJavaFilePath(String basePackage, String subPackage, String className) {
        String packagePath = (basePackage + "." + subPackage).replace('.', '/');
        return "src/main/java/" + packagePath + "/" + className + ".java";
    }

    /**
     * SQL定義ファイルのパスを生成する。
     *
     * @param basePackage ベースパッケージ
     * @param entityName Entity名
     * @return ファイルパス
     */
    public static String toSqlFilePath(String basePackage, String entityName) {
        String packagePath = (basePackage + ".entity").replace('.', '/');
        return "src/main/resources/" + packagePath + "/" + entityName + ".sql";
    }

    private static String stripSuffix(String name) {
        for (String suffix : new String[]{"Action", "Form", "Handler", "Interceptor"}) {
            if (name.endsWith(suffix) && name.length() > suffix.length()) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    private static String toSnakeCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}

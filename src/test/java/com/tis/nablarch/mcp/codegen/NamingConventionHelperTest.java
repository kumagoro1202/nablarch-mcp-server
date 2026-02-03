package com.tis.nablarch.mcp.codegen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class NamingConventionHelperTest {

    @Test
    void toActionClassName_サフィックスなしの場合Actionを付与() {
        assertEquals("ProductAction", NamingConventionHelper.toActionClassName("Product"));
    }

    @Test
    void toActionClassName_サフィックスありの場合そのまま() {
        assertEquals("ProductAction", NamingConventionHelper.toActionClassName("ProductAction"));
    }

    @Test
    void toFormClassName_サフィックスなしの場合Formを付与() {
        assertEquals("ProductForm", NamingConventionHelper.toFormClassName("Product"));
    }

    @ParameterizedTest
    @CsvSource({"Product, product", "UserRegistration, userregistration"})
    void toPathName_PascalCaseをlowercaseに変換(String input, String expected) {
        assertEquals(expected, NamingConventionHelper.toPathName(input));
    }

    @ParameterizedTest
    @CsvSource({"Product, PRODUCT", "ProductCategory, PRODUCT_CATEGORY"})
    void toTableName_PascalCaseを大文字スネークケースに変換(String input, String expected) {
        assertEquals(expected, NamingConventionHelper.toTableName(input));
    }

    @Test
    void capitalize_先頭を大文字に変換() {
        assertEquals("ProductName", NamingConventionHelper.capitalize("productName"));
    }

    @Test
    void capitalize_nullの場合nullを返す() {
        assertNull(NamingConventionHelper.capitalize(null));
    }

    @Test
    void toJavaFilePath_パッケージとクラス名からファイルパスを生成() {
        String path = NamingConventionHelper.toJavaFilePath("com.example", "action", "ProductAction");
        assertEquals("src/main/java/com/example/action/ProductAction.java", path);
    }
}

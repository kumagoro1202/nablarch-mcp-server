package com.tis.nablarch.mcp.rag.parser;

import com.tis.nablarch.mcp.rag.chunking.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link JavaSourceParser}のテスト。
 */
class JavaSourceParserTest {

    private JavaSourceParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParser();
    }

    @Test
    void parse_正常系_クラスとメソッド抽出() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        // クラスヘッダ + list() + create() = 3
        assertTrue(results.size() >= 3, "クラスヘッダ+2メソッド以上: " + results.size());
    }

    @Test
    void parse_正常系_コンテンツタイプがJAVA() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        boolean allJavaOrJavadoc = results.stream()
                .allMatch(doc -> doc.contentType() == ContentType.JAVA || doc.contentType() == ContentType.JAVADOC);
        assertTrue(allJavaOrJavadoc, "コンテンツタイプがJAVA又はJAVADOC");
    }

    @Test
    void parse_正常系_FQCNメタデータ() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        assertFalse(results.isEmpty());
        ParsedDocument first = results.get(0);
        assertEquals("nablarch.example.action.SampleAction", first.metadata().get("fqcn"));
        assertEquals("SampleAction", first.metadata().get("class_name"));
        assertEquals("nablarch.example.action", first.metadata().get("package_name"));
    }

    @Test
    void parse_正常系_メソッド名メタデータ() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        boolean hasListMethod = results.stream()
                .anyMatch(doc -> "list".equals(doc.metadata().get("method_name")));
        boolean hasCreateMethod = results.stream()
                .anyMatch(doc -> "create".equals(doc.metadata().get("method_name")));
        assertTrue(hasListMethod, "listメソッドが抽出されていること");
        assertTrue(hasCreateMethod, "createメソッドが抽出されていること");
    }

    @Test
    void parse_正常系_メソッドチャンクにクラス情報含む() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        boolean hasClassContext = results.stream()
                .filter(doc -> "method".equals(doc.metadata().get("element_type")))
                .anyMatch(doc -> doc.content().contains("// Class: SampleAction"));
        assertTrue(hasClassContext, "メソッドチャンクにクラス名コンテキストが含まれること");
    }

    @Test
    void parse_正常系_フィールド情報含む() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        boolean hasFields = results.stream()
                .anyMatch(doc -> doc.content().contains("userService"));
        assertTrue(hasFields, "フィールド情報が含まれること");
    }

    @Test
    void parse_正常系_sourceメタデータ() throws IOException {
        String java = loadTestData("testdata/SampleAction.java");
        List<ParsedDocument> results = parser.parse(java, "SampleAction.java");

        assertFalse(results.isEmpty());
        assertEquals("github", results.get(0).metadata().get("source"));
        assertEquals("code", results.get(0).metadata().get("source_type"));
    }

    @Test
    void parse_正常系_シンプルクラス() {
        String java = "package com.example;\npublic class Simple {\n    public void doSomething() {\n        System.out.println(\"hello\");\n    }\n}";
        List<ParsedDocument> results = parser.parse(java, "Simple.java");

        assertFalse(results.isEmpty());
    }

    @Test
    void parse_異常系_contentがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(null, "Test.java"));
    }

    @Test
    void parse_異常系_contentが空() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("", "Test.java"));
    }

    @Test
    void parse_異常系_sourceUrlがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("public class Test {}", null));
    }

    private String loadTestData(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "テストデータが見つからない: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

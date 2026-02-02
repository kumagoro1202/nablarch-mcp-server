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
 * {@link XmlConfigParser}のテスト。
 */
class XmlConfigParserTest {

    private XmlConfigParser parser;

    @BeforeEach
    void setUp() {
        parser = new XmlConfigParser();
    }

    @Test
    void parse_正常系_要素単位分割() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        // webFrontController + dbConnectionManager = 2要素
        assertEquals(2, results.size(), "トップレベル要素数: " + results.size());
    }

    @Test
    void parse_正常系_コンテンツタイプがXML() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        for (ParsedDocument doc : results) {
            assertEquals(ContentType.XML, doc.contentType());
        }
    }

    @Test
    void parse_正常系_ファイルパスコメント付与() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).content().contains("<!-- File: handler-queue.xml -->"));
    }

    @Test
    void parse_正常系_親要素コメント付与() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).content().contains("<!-- Parent:"));
    }

    @Test
    void parse_正常系_name属性メタデータ() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        boolean hasWebFrontController = results.stream()
                .anyMatch(doc -> "webFrontController".equals(doc.metadata().get("element_name")));
        assertTrue(hasWebFrontController, "name属性がメタデータに含まれること");
    }

    @Test
    void parse_正常系_class属性がfqcnメタデータ() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        boolean hasFqcn = results.stream()
                .anyMatch(doc -> doc.metadata().containsKey("fqcn")
                        && doc.metadata().get("fqcn").contains("nablarch"));
        assertTrue(hasFqcn, "class属性がfqcnメタデータに含まれること");
    }

    @Test
    void parse_正常系_sourceメタデータ() throws IOException {
        String xml = loadTestData("testdata/sample-config.xml");
        List<ParsedDocument> results = parser.parse(xml, "handler-queue.xml");

        assertFalse(results.isEmpty());
        assertEquals("github", results.get(0).metadata().get("source"));
        assertEquals("config", results.get(0).metadata().get("source_type"));
    }

    @Test
    void parse_正常系_シンプルXML() {
        String xml = "<root><item name=\"test\">value</item></root>";
        List<ParsedDocument> results = parser.parse(xml, "simple.xml");

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void parse_正常系_不正XMLはテキストとして返す() {
        String invalidXml = "<unclosed><tag>no closing tag";
        List<ParsedDocument> results = parser.parse(invalidXml, "invalid.xml");

        // パースエラーでもテキストとして返す
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).metadata().containsKey("parse_error"));
    }

    @Test
    void parse_異常系_contentがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(null, "test.xml"));
    }

    @Test
    void parse_異常系_contentが空() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("", "test.xml"));
    }

    @Test
    void parse_異常系_sourceUrlがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("<root/>", null));
    }

    private String loadTestData(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "テストデータが見つからない: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

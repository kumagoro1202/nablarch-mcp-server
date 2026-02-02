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
 * {@link HtmlDocumentParser}のテスト。
 */
class HtmlDocumentParserTest {

    private HtmlDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new HtmlDocumentParser();
    }

    @Test
    void parse_正常系_セクション分割() throws IOException {
        String html = loadTestData("testdata/sample.html");
        List<ParsedDocument> results = parser.parse(html, "https://nablarch.github.io/docs/handler-queue");

        assertFalse(results.isEmpty());
        // イントロ + 概要 + 設定方法 + XML設定例 = 4セクション
        assertTrue(results.size() >= 3, "少なくとも3セクション以上: " + results.size());
    }

    @Test
    void parse_正常系_コンテンツタイプがHTML() throws IOException {
        String html = loadTestData("testdata/sample.html");
        List<ParsedDocument> results = parser.parse(html, "https://example.com/test");

        for (ParsedDocument doc : results) {
            assertEquals(ContentType.HTML, doc.contentType());
        }
    }

    @Test
    void parse_正常系_メタデータにtitle含む() throws IOException {
        String html = loadTestData("testdata/sample.html");
        List<ParsedDocument> results = parser.parse(html, "https://example.com/test");

        assertFalse(results.isEmpty());
        ParsedDocument first = results.get(0);
        assertEquals("Nablarch ハンドラキュー", first.metadata().get("title"));
        assertEquals("nablarch-document", first.metadata().get("source"));
    }

    @Test
    void parse_正常系_コードブロック保持() throws IOException {
        String html = loadTestData("testdata/sample.html");
        List<ParsedDocument> results = parser.parse(html, "https://example.com/test");

        boolean hasCodeBlock = results.stream()
                .anyMatch(doc -> doc.content().contains("```java"));
        assertTrue(hasCodeBlock, "コードブロックが保持されていること");
    }

    @Test
    void parse_正常系_テーブルをMarkdown変換() throws IOException {
        String html = loadTestData("testdata/sample.html");
        List<ParsedDocument> results = parser.parse(html, "https://example.com/test");

        boolean hasTable = results.stream()
                .anyMatch(doc -> doc.content().contains("| ") && doc.content().contains("---"));
        assertTrue(hasTable, "テーブルがMarkdown形式に変換されていること");
    }

    @Test
    void parse_正常系_見出しなしHTML() {
        String html = "<html><body><p>シンプルなテキスト。</p></body></html>";
        List<ParsedDocument> results = parser.parse(html, "https://example.com/simple");

        assertEquals(1, results.size());
        assertTrue(results.get(0).content().contains("シンプルなテキスト"));
    }

    @Test
    void parse_異常系_contentがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse(null, "https://example.com"));
    }

    @Test
    void parse_異常系_contentが空() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("", "https://example.com"));
    }

    @Test
    void parse_異常系_sourceUrlがnull() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("<html><body>test</body></html>", null));
    }

    @Test
    void parse_異常系_sourceUrlが空() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parse("<html><body>test</body></html>", ""));
    }

    private String loadTestData(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "テストデータが見つからない: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

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
 * {@link MarkdownDocumentParser}のテスト。
 */
class MarkdownDocumentParserTest {

    private MarkdownDocumentParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownDocumentParser();
    }

    @Test
    void parse_正常系_見出し分割() throws IOException {
        String md = loadTestData("testdata/sample.md");
        List<ParsedDocument> results = parser.parse(md, "https://fintan.jp/nablarch-validation");

        // イントロ + Bean Validation + アノテーション定義 + ドメインバリデーション + エラーメッセージ
        assertTrue(results.size() >= 4, "少なくとも4セクション以上: " + results.size());
    }

    @Test
    void parse_正常系_コンテンツタイプがMARKDOWN() throws IOException {
        String md = loadTestData("testdata/sample.md");
        List<ParsedDocument> results = parser.parse(md, "https://fintan.jp/test");

        for (ParsedDocument doc : results) {
            assertEquals(ContentType.MARKDOWN, doc.contentType());
        }
    }

    @Test
    void parse_正常系_メタデータにtitle含む() throws IOException {
        String md = loadTestData("testdata/sample.md");
        List<ParsedDocument> results = parser.parse(md, "https://fintan.jp/test");

        assertFalse(results.isEmpty());
        boolean hasTitleMetadata = results.stream()
                .anyMatch(doc -> "Nablarchバリデーション入門".equals(doc.metadata().get("title")));
        assertTrue(hasTitleMetadata, "タイトルメタデータが含まれること");
    }

    @Test
    void parse_正常系_コードフェンス保持() throws IOException {
        String md = loadTestData("testdata/sample.md");
        List<ParsedDocument> results = parser.parse(md, "https://fintan.jp/test");

        boolean hasCodeFence = results.stream()
                .anyMatch(doc -> doc.content().contains("```java"));
        assertTrue(hasCodeFence, "コードフェンスが保持されていること");
    }

    @Test
    void parse_正常系_コードフェンス内の見出しは無視() {
        String md = "## セクション1\n\nテキスト\n\n```\n## これは見出しではない\n```\n\n## セクション2\n\n別テキスト";
        List<ParsedDocument> results = parser.parse(md, "https://example.com/test");

        assertEquals(2, results.size(), "コードフェンス内の ## は見出しとして扱わない");
    }

    @Test
    void parse_正常系_見出しなしMarkdown() {
        String md = "シンプルなテキスト。見出しなし。";
        List<ParsedDocument> results = parser.parse(md, "https://example.com/simple");

        assertEquals(1, results.size());
        assertTrue(results.get(0).content().contains("シンプルなテキスト"));
    }

    @Test
    void parse_正常系_headingLevelメタデータ() throws IOException {
        String md = loadTestData("testdata/sample.md");
        List<ParsedDocument> results = parser.parse(md, "https://fintan.jp/test");

        boolean hasH2 = results.stream()
                .anyMatch(doc -> "2".equals(doc.metadata().get("heading_level")));
        boolean hasH3 = results.stream()
                .anyMatch(doc -> "3".equals(doc.metadata().get("heading_level")));
        assertTrue(hasH2, "h2レベルの見出しが存在すること");
        assertTrue(hasH3, "h3レベルの見出しが存在すること");
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
                () -> parser.parse("# Test", null));
    }

    private String loadTestData(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "テストデータが見つからない: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

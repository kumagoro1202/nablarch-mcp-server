package com.tis.nablarch.mcp.rag.chunking;

import com.tis.nablarch.mcp.rag.parser.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ChunkingService}のテスト。
 */
class ChunkingServiceTest {

    private ChunkingService service;

    @BeforeEach
    void setUp() {
        service = new ChunkingService();
    }

    // --- HTML チャンキング ---

    @Test
    void chunk_HTML_短いコンテンツは1チャンク() {
        ParsedDocument doc = new ParsedDocument(
                "## ハンドラキュー\n\nNablarchのリクエスト処理パイプラインである。短いコンテンツはそのまま1チャンクになる。",
                Map.of("source", "nablarch-document"),
                "https://example.com/handler",
                ContentType.HTML
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size());
        assertEquals(ContentType.HTML, chunks.get(0).contentType());
        assertEquals(0, chunks.get(0).chunkIndex());
        assertEquals(1, chunks.get(0).totalChunks());
    }

    @Test
    void chunk_HTML_長いコンテンツは複数チャンク() {
        // 512トークン超の長いコンテンツを生成（日本語テキスト、1トークン≒2文字）
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            sb.append("## セクション").append(i).append("\n\n");
            sb.append("ハンドラキューはNablarchフレームワークの中核的なアーキテクチャパターンであり、");
            sb.append("リクエスト処理を複数のハンドラによるパイプラインで実行する仕組みである。");
            sb.append("各ハンドラは単一責務を持ち、前段の結果を受けて後段に渡す。\n\n");
        }

        ParsedDocument doc = new ParsedDocument(
                sb.toString(),
                Map.of("source", "nablarch-document"),
                "https://example.com/long",
                ContentType.HTML
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertTrue(chunks.size() > 1, "長いコンテンツは複数チャンクに分割: " + chunks.size());
    }

    @Test
    void chunk_HTML_メタデータ引き継ぎ() {
        ParsedDocument doc = new ParsedDocument(
                "## テスト\n\nNablarchのハンドラキューはリクエスト処理パイプラインとして機能する重要なアーキテクチャパターンである。",
                Map.of("source", "nablarch-document", "title", "テスト文書"),
                "https://example.com/test",
                ContentType.HTML
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertFalse(chunks.isEmpty());
        assertEquals("nablarch-document", chunks.get(0).metadata().get("source"));
        assertEquals("テスト文書", chunks.get(0).metadata().get("title"));
        assertEquals("HTML", chunks.get(0).metadata().get("content_type"));
        assertEquals("0", chunks.get(0).metadata().get("chunk_index"));
    }

    // --- MARKDOWN チャンキング ---

    @Test
    void chunk_MARKDOWN_短いコンテンツ() {
        ParsedDocument doc = new ParsedDocument(
                "## バリデーション\n\nBean Validationベースのバリデーション機能を提供する。Nablarchフレームワークで標準的に使用される。",
                Map.of("source", "fintan"),
                "https://fintan.jp/validation",
                ContentType.MARKDOWN
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size());
        assertEquals(ContentType.MARKDOWN, chunks.get(0).contentType());
    }

    // --- JAVADOC チャンキング ---

    @Test
    void chunk_JAVADOC_1ドキュメント1チャンク() {
        ParsedDocument doc = new ParsedDocument(
                "/**\n * ハンドラインターフェース。\n * リクエスト処理パイプラインの各段階を担当するハンドラの基本インターフェースである。\n * @param <I> 入力型\n * @param <O> 出力型\n */\npublic interface Handler<I, O> {\n    O handle(I input, ExecutionContext context);\n}",
                Map.of("source", "github", "fqcn", "nablarch.fw.Handler"),
                "Handler.java",
                ContentType.JAVADOC
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size(), "JAVADOCは1ドキュメント=1チャンク");
        assertEquals(ContentType.JAVADOC, chunks.get(0).contentType());
    }

    // --- JAVA チャンキング ---

    @Test
    void chunk_JAVA_短いメソッドは1チャンク() {
        ParsedDocument doc = new ParsedDocument(
                "// Class: SampleAction\npublic HttpResponse list(HttpRequest request, ExecutionContext context) {\n    context.setRequestScopedVar(\"users\", userService.findAll());\n    return new HttpResponse(\"/WEB-INF/view/user/list.jsp\");\n}",
                Map.of("source", "github", "element_type", "method"),
                "SampleAction.java",
                ContentType.JAVA
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size());
        assertEquals(ContentType.JAVA, chunks.get(0).contentType());
    }

    // --- XML チャンキング ---

    @Test
    void chunk_XML_1要素1チャンク() {
        ParsedDocument doc = new ParsedDocument(
                "<!-- File: handler-queue.xml -->\n<component name=\"webFrontController\" class=\"nablarch.fw.web.servlet.WebFrontController\">\n  <property name=\"handlerQueue\">\n    <list>\n      <component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>\n    </list>\n  </property>\n</component>",
                Map.of("source", "github", "element_type", "component"),
                "handler-queue.xml",
                ContentType.XML
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size(), "XMLは1要素=1チャンク");
        assertEquals(ContentType.XML, chunks.get(0).contentType());
    }

    // --- TEXT チャンキング ---

    @Test
    void chunk_TEXT_短いテキスト() {
        ParsedDocument doc = new ParsedDocument(
                "Nablarchはミッションクリティカルシステム向けのJava開発基盤である。ハンドラキューアーキテクチャを採用している。",
                Map.of("source", "nablarch-document"),
                "readme.txt",
                ContentType.TEXT
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size());
        assertEquals(ContentType.TEXT, chunks.get(0).contentType());
    }

    @Test
    void chunk_TEXT_長いテキストは複数チャンク() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Nablarchフレームワークはミッションクリティカルシステム向けのJava開発基盤であり、");
            sb.append("ハンドラキューアーキテクチャにより柔軟なリクエスト処理パイプラインを提供する。");
            sb.append("各ハンドラは独立した単一責務を持つ。\n\n");
        }

        ParsedDocument doc = new ParsedDocument(
                sb.toString(),
                Map.of("source", "nablarch-document"),
                "readme.txt",
                ContentType.TEXT
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertTrue(chunks.size() > 1, "長いテキストは複数チャンクに分割: " + chunks.size());
    }

    // --- 共通テスト ---

    @Test
    void chunk_チャンクインデックスが連番() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Nablarchのハンドラキューはリクエスト処理のパイプラインモデルを実現する中核的な仕組みである。\n\n");
        }

        ParsedDocument doc = new ParsedDocument(
                sb.toString(), Map.of(), "test.txt", ContentType.TEXT
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
            assertEquals(chunks.size(), chunks.get(i).totalChunks());
        }
    }

    @Test
    void chunk_短いコンテンツでもMinChars以上なら返す() {
        // 50文字以上のコンテンツは1チャンクとして返るべき
        ParsedDocument doc = new ParsedDocument(
                "Nablarchフレームワークはミッションクリティカルシステム向けのJava開発基盤である。ハンドラキューアーキテクチャを採用する。",
                Map.of(), "short.txt", ContentType.TEXT
        );

        List<DocumentChunkDto> chunks = service.chunk(doc);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).content().length() >= 50);
    }

    @Test
    void chunk_異常系_documentがnull() {
        assertThrows(IllegalArgumentException.class, () -> service.chunk(null));
    }

    // --- tokensToChars テスト ---

    @Test
    void tokensToChars_英語テキスト() {
        // 英語100%: 1token ≒ 4chars
        int chars = ChunkingService.tokensToChars(100, "This is English text only");
        assertTrue(chars >= 350 && chars <= 450, "英語テキスト: " + chars);
    }

    @Test
    void tokensToChars_日本語テキスト() {
        // 日本語100%: 1token ≒ 2chars
        int chars = ChunkingService.tokensToChars(100, "日本語のテキストのみ");
        assertTrue(chars >= 180 && chars <= 220, "日本語テキスト: " + chars);
    }

    @Test
    void tokensToChars_混合テキスト() {
        int chars = ChunkingService.tokensToChars(100, "日本語とEnglishの混合テキスト");
        assertTrue(chars > 200 && chars < 400, "混合テキスト: " + chars);
    }

    // --- calculateJapaneseRatio テスト ---

    @Test
    void calculateJapaneseRatio_全日本語() {
        double ratio = ChunkingService.calculateJapaneseRatio("日本語テキスト");
        assertTrue(ratio > 0.9, "全日本語の比率: " + ratio);
    }

    @Test
    void calculateJapaneseRatio_全英語() {
        double ratio = ChunkingService.calculateJapaneseRatio("English text only");
        assertEquals(0.0, ratio, 0.01);
    }

    @Test
    void calculateJapaneseRatio_空文字() {
        assertEquals(0.0, ChunkingService.calculateJapaneseRatio(""));
    }

    @Test
    void calculateJapaneseRatio_null() {
        assertEquals(0.0, ChunkingService.calculateJapaneseRatio(null));
    }
}

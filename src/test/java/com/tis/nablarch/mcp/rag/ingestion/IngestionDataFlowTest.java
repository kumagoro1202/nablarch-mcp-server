package com.tis.nablarch.mcp.rag.ingestion;

import com.tis.nablarch.mcp.rag.chunking.ChunkingService;
import com.tis.nablarch.mcp.rag.chunking.ContentType;
import com.tis.nablarch.mcp.rag.chunking.DocumentChunkDto;
import com.tis.nablarch.mcp.rag.parser.HtmlDocumentParser;
import com.tis.nablarch.mcp.rag.parser.MarkdownDocumentParser;
import com.tis.nablarch.mcp.rag.parser.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 取込パイプラインデータフロー検証テスト。
 *
 * <p>実際のHtmlDocumentParser/MarkdownDocumentParser/ChunkingServiceを使用し、
 * パース→チャンキング→DTO変換のデータフロー整合性を検証する。
 * モックは使用せず、実コンポーネント間のデータ受け渡しを確認する。</p>
 */
class IngestionDataFlowTest {

    private HtmlDocumentParser htmlParser;
    private MarkdownDocumentParser markdownParser;
    private ChunkingService chunkingService;

    /** テスト用HTMLコンテンツ（テストリソースから読み込み） */
    private String sampleHtml;

    /** テスト用Markdownコンテンツ（テストリソースから読み込み） */
    private String sampleMarkdown;

    @BeforeEach
    void setUp() throws IOException {
        htmlParser = new HtmlDocumentParser();
        markdownParser = new MarkdownDocumentParser();
        chunkingService = new ChunkingService();

        sampleHtml = loadTestResource("testdata/ingestion/sample-official-doc.html");
        sampleMarkdown = loadTestResource("testdata/ingestion/sample-fintan-article.md");
    }

    /**
     * テストリソースファイルを読み込む。
     */
    private String loadTestResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("テストリソースが見つからない: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ===== HTML データフロー =====

    @Nested
    @DisplayName("HTML→ParsedDocument→DocumentChunkDto変換整合性")
    class HtmlDataFlowTest {

        @Test
        @DisplayName("HTMLパース結果のタイトル・URL・コンテンツが正しく引き継がれる")
        void htmlParseThenChunkPreservesData() {
            String sourceUrl = "http://example.com/docs/architecture.html";

            // ステップ1: HTMLパース
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            assertFalse(parsedDocs.isEmpty(), "パース結果が空でないこと");

            // 各ParsedDocumentの基本プロパティ検証
            for (ParsedDocument doc : parsedDocs) {
                assertNotNull(doc.content(), "contentがnullでないこと");
                assertFalse(doc.content().isBlank(), "contentが空でないこと");
                assertEquals(sourceUrl, doc.sourceUrl(), "sourceUrlが保持されること");
                assertEquals(ContentType.HTML, doc.contentType(), "contentTypeがHTMLであること");
                assertNotNull(doc.metadata(), "metadataがnullでないこと");
            }

            // ステップ2: チャンキング
            for (ParsedDocument doc : parsedDocs) {
                List<DocumentChunkDto> chunks = chunkingService.chunk(doc);

                for (DocumentChunkDto chunk : chunks) {
                    assertNotNull(chunk.content(), "チャンクcontentがnullでないこと");
                    assertFalse(chunk.content().isBlank(), "チャンクcontentが空でないこと");
                    assertEquals(ContentType.HTML, chunk.contentType(), "チャンクcontentTypeがHTMLであること");

                    // メタデータが引き継がれていること
                    assertNotNull(chunk.metadata(), "チャンクmetadataがnullでないこと");
                    assertEquals("HTML", chunk.metadata().get("content_type"),
                            "content_typeメタデータが設定されること");
                }
            }
        }

        @Test
        @DisplayName("HTMLからセクション単位でParsedDocumentが生成される")
        void htmlParsesIntoSections() {
            String sourceUrl = "http://example.com/docs/test.html";
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            // サンプルHTMLには h2 が4つ + h3 が2つ = 計6見出し + イントロの可能性
            assertTrue(parsedDocs.size() >= 2, "複数のセクションに分割されること (実際: " + parsedDocs.size() + ")");

            // タイトルメタデータの確認
            boolean hasTitleMetadata = parsedDocs.stream()
                    .anyMatch(doc -> doc.metadata().containsKey("title"));
            assertTrue(hasTitleMetadata, "少なくとも1つのドキュメントにtitleメタデータがあること");
        }

        @Test
        @DisplayName("HTMLパース結果にsource_urlメタデータが含まれる")
        void htmlParseIncludesSourceUrl() {
            String sourceUrl = "http://example.com/docs/test.html";
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            for (ParsedDocument doc : parsedDocs) {
                assertEquals(sourceUrl, doc.metadata().get("source_url"),
                        "source_urlメタデータがパース結果に含まれること");
            }
        }
    }

    // ===== Markdown データフロー =====

    @Nested
    @DisplayName("Markdown→ParsedDocument→DocumentChunkDto変換整合性")
    class MarkdownDataFlowTest {

        @Test
        @DisplayName("Markdownパース結果のタイトル・URL・コンテンツが正しく引き継がれる")
        void markdownParseThenChunkPreservesData() {
            String sourceUrl = "https://fintan.jp/page/123/";

            // ステップ1: Markdownパース
            List<ParsedDocument> parsedDocs = markdownParser.parse(sampleMarkdown, sourceUrl);

            assertFalse(parsedDocs.isEmpty(), "パース結果が空でないこと");

            for (ParsedDocument doc : parsedDocs) {
                assertNotNull(doc.content());
                assertFalse(doc.content().isBlank());
                assertEquals(sourceUrl, doc.sourceUrl());
                assertEquals(ContentType.MARKDOWN, doc.contentType());
                assertNotNull(doc.metadata());
            }

            // ステップ2: チャンキング
            for (ParsedDocument doc : parsedDocs) {
                List<DocumentChunkDto> chunks = chunkingService.chunk(doc);

                for (DocumentChunkDto chunk : chunks) {
                    assertNotNull(chunk.content());
                    assertFalse(chunk.content().isBlank());
                    assertEquals(ContentType.MARKDOWN, chunk.contentType());
                    assertNotNull(chunk.metadata());
                    assertEquals("MARKDOWN", chunk.metadata().get("content_type"));
                }
            }
        }

        @Test
        @DisplayName("Markdownから見出し単位でParsedDocumentが生成される")
        void markdownParsesIntoSections() {
            String sourceUrl = "https://fintan.jp/page/456/";
            List<ParsedDocument> parsedDocs = markdownParser.parse(sampleMarkdown, sourceUrl);

            // サンプルMarkdownには ## が4つ + ### が1つ + イントロ
            assertTrue(parsedDocs.size() >= 2, "複数のセクションに分割されること (実際: " + parsedDocs.size() + ")");
        }
    }

    // ===== メタデータ伝搬テスト =====

    @Nested
    @DisplayName("メタデータの伝搬（source, sourceType, contentType）")
    class MetadataPropagationTest {

        @Test
        @DisplayName("HTMLパーサーのメタデータがチャンクまで伝搬する")
        void htmlMetadataPropagation() {
            String sourceUrl = "http://example.com/docs/test.html";
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            assertFalse(parsedDocs.isEmpty());

            ParsedDocument firstDoc = parsedDocs.get(0);
            // HtmlDocumentParserのbuildMetadataで設定されるメタデータ
            assertEquals("nablarch-document", firstDoc.metadata().get("source"),
                    "sourceが'nablarch-document'であること");
            assertEquals("documentation", firstDoc.metadata().get("source_type"),
                    "source_typeが'documentation'であること");
            assertEquals("ja", firstDoc.metadata().get("language"),
                    "languageが'ja'であること");
            assertEquals(sourceUrl, firstDoc.metadata().get("source_url"),
                    "source_urlが保持されること");

            // チャンキング後もメタデータが保持される
            List<DocumentChunkDto> chunks = chunkingService.chunk(firstDoc);
            if (!chunks.isEmpty()) {
                DocumentChunkDto firstChunk = chunks.get(0);
                // ChunkingServiceはdocument.metadata()をコピーしてcontent_type等を追加する
                assertEquals("nablarch-document", firstChunk.metadata().get("source"),
                        "チャンクにもsourceが伝搬すること");
                assertEquals("documentation", firstChunk.metadata().get("source_type"),
                        "チャンクにもsource_typeが伝搬すること");
                assertEquals("ja", firstChunk.metadata().get("language"),
                        "チャンクにもlanguageが伝搬すること");
                assertEquals("HTML", firstChunk.metadata().get("content_type"),
                        "content_typeがチャンクに追加されること");
            }
        }

        @Test
        @DisplayName("Markdownパーサーのメタデータがチャンクまで伝搬する")
        void markdownMetadataPropagation() {
            String sourceUrl = "https://fintan.jp/page/789/";
            List<ParsedDocument> parsedDocs = markdownParser.parse(sampleMarkdown, sourceUrl);

            assertFalse(parsedDocs.isEmpty());

            ParsedDocument firstDoc = parsedDocs.get(0);
            // MarkdownDocumentParserのbuildMetadataで設定されるメタデータ
            assertEquals("fintan", firstDoc.metadata().get("source"),
                    "sourceが'fintan'であること");
            assertEquals("documentation", firstDoc.metadata().get("source_type"),
                    "source_typeが'documentation'であること");
            assertEquals("ja", firstDoc.metadata().get("language"),
                    "languageが'ja'であること");

            // チャンキング後
            List<DocumentChunkDto> chunks = chunkingService.chunk(firstDoc);
            if (!chunks.isEmpty()) {
                DocumentChunkDto firstChunk = chunks.get(0);
                assertEquals("fintan", firstChunk.metadata().get("source"));
                assertEquals("documentation", firstChunk.metadata().get("source_type"));
                assertEquals("MARKDOWN", firstChunk.metadata().get("content_type"));
            }
        }

        @Test
        @DisplayName("チャンクインデックスと総チャンク数がDTOに正しく設定される")
        void chunkIndexAndTotalAreCorrect() {
            String sourceUrl = "http://example.com/docs/test.html";
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            for (ParsedDocument doc : parsedDocs) {
                List<DocumentChunkDto> chunks = chunkingService.chunk(doc);

                for (int i = 0; i < chunks.size(); i++) {
                    DocumentChunkDto chunk = chunks.get(i);
                    assertEquals(i, chunk.chunkIndex(),
                            "chunkIndexが正しいこと");
                    assertEquals(chunks.size(), chunk.totalChunks(),
                            "totalChunksが正しいこと");
                    assertEquals(String.valueOf(i), chunk.metadata().get("chunk_index"),
                            "メタデータのchunk_indexが正しいこと");
                    assertEquals(String.valueOf(chunks.size()), chunk.metadata().get("total_chunks"),
                            "メタデータのtotal_chunksが正しいこと");
                }
            }
        }
    }

    // ===== チャンクサイズ・オーバーラップ検証 =====

    @Nested
    @DisplayName("チャンクサイズ・オーバーラップの検証")
    class ChunkSizeAndOverlapTest {

        @Test
        @DisplayName("HTMLチャンクが最小サイズ(50文字)以上であること")
        void htmlChunksAboveMinSize() {
            String sourceUrl = "http://example.com/docs/test.html";
            List<ParsedDocument> parsedDocs = htmlParser.parse(sampleHtml, sourceUrl);

            for (ParsedDocument doc : parsedDocs) {
                List<DocumentChunkDto> chunks = chunkingService.chunk(doc);
                for (DocumentChunkDto chunk : chunks) {
                    assertTrue(chunk.content().length() >= 50,
                            "チャンクは最小50文字以上: 実際=" + chunk.content().length());
                }
            }
        }

        @Test
        @DisplayName("Markdownチャンクが最小サイズ(50文字)以上であること")
        void markdownChunksAboveMinSize() {
            String sourceUrl = "https://fintan.jp/page/test/";
            List<ParsedDocument> parsedDocs = markdownParser.parse(sampleMarkdown, sourceUrl);

            for (ParsedDocument doc : parsedDocs) {
                List<DocumentChunkDto> chunks = chunkingService.chunk(doc);
                for (DocumentChunkDto chunk : chunks) {
                    assertTrue(chunk.content().length() >= 50,
                            "チャンクは最小50文字以上: 実際=" + chunk.content().length());
                }
            }
        }

        @Test
        @DisplayName("大きなコンテンツが適切にチャンク分割される")
        void largeContentIsSplitIntoChunks() {
            // 512トークン ≒ 1024文字（日本語50%仮定で約3文字/token）を超えるコンテンツ
            StringBuilder largeContent = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                largeContent.append("Nablarchのハンドラキューは処理をパイプラインで制御する仕組みである。");
                largeContent.append("各ハンドラは単一責務を持ち連鎖的に実行される。");
            }

            ParsedDocument largeDoc = new ParsedDocument(
                    largeContent.toString(),
                    Map.of("source", "test", "language", "ja"),
                    "http://test/large-doc",
                    ContentType.HTML
            );

            List<DocumentChunkDto> chunks = chunkingService.chunk(largeDoc);

            assertTrue(chunks.size() > 1,
                    "大きなコンテンツは複数チャンクに分割されること (実際: " + chunks.size() + ")");

            // 全チャンクのインデックスが連続していること
            for (int i = 0; i < chunks.size(); i++) {
                assertEquals(i, chunks.get(i).chunkIndex());
                assertEquals(chunks.size(), chunks.get(i).totalChunks());
            }
        }

        @Test
        @DisplayName("短いコンテンツは1チャンクとして返される")
        void shortContentReturnsSingleChunk() {
            // 50文字以上だが短いコンテンツ（ChunkingServiceのMIN_CHUNK_CHARS=50を超える長さ）
            String shortContent = "Nablarchフレームワークのアーキテクチャ概要を説明する。実行制御基盤の仕組みを理解する。ハンドラキューの動作を確認する。";

            ParsedDocument shortDoc = new ParsedDocument(
                    shortContent,
                    Map.of("source", "test", "language", "ja"),
                    "http://test/short-doc",
                    ContentType.HTML
            );

            List<DocumentChunkDto> chunks = chunkingService.chunk(shortDoc);

            assertEquals(1, chunks.size(), "短いコンテンツは1チャンクであること");
            assertEquals(shortContent, chunks.get(0).content(), "コンテンツがそのまま保持されること");
        }
    }
}

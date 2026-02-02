package com.tis.nablarch.mcp.rag.ingestion;

import com.tis.nablarch.mcp.db.entity.DocumentChunk;
import com.tis.nablarch.mcp.db.repository.DocumentChunkRepository;
import com.tis.nablarch.mcp.embedding.EmbeddingClient;
import com.tis.nablarch.mcp.rag.chunking.ChunkingService;
import com.tis.nablarch.mcp.rag.chunking.ContentType;
import com.tis.nablarch.mcp.rag.chunking.DocumentChunkDto;
import com.tis.nablarch.mcp.rag.parser.HtmlDocumentParser;
import com.tis.nablarch.mcp.rag.parser.ParsedDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OfficialDocsIngester のテスト。
 *
 * <p>全ての外部依存をモック化し、パイプラインの統合動作を検証する。</p>
 */
class OfficialDocsIngesterTest {

    private HtmlDocumentParser htmlParser;
    private ChunkingService chunkingService;
    private EmbeddingClient embeddingClient;
    private DocumentChunkRepository repository;
    private IngestionConfig ingestionConfig;
    private OfficialDocsIngester ingester;

    /** テスト用のインデックスHTML（リンク2つ含む） */
    private static final String INDEX_HTML = "<html><body>"
            + "<a href=\"page1.html\">Page 1</a>"
            + "<a href=\"page2.html\">Page 2</a>"
            + "</body></html>";

    /** テスト用のドキュメントHTML */
    private static final String DOC_HTML = "<html><head><title>テスト</title></head>"
            + "<body><h2>セクション1</h2><p>テスト内容。テスト内容を書いている。"
            + "十分な長さのコンテンツが必要である。最低50文字以上。</p></body></html>";

    @BeforeEach
    void setUp() {
        htmlParser = mock(HtmlDocumentParser.class);
        chunkingService = mock(ChunkingService.class);
        embeddingClient = mock(EmbeddingClient.class);
        repository = mock(DocumentChunkRepository.class);
        ingestionConfig = new IngestionConfig();
        ingestionConfig.getOfficialDocs().setBaseUrl("http://test-server/docs/");
        ingestionConfig.getOfficialDocs().setBatchSize(5);
        ingestionConfig.getOfficialDocs().setDelayMs(0); // テスト用にディレイなし
        ingestionConfig.getOfficialDocs().setMaxRetries(1);
        ingestionConfig.getOfficialDocs().setEnabled(true);
    }

    /**
     * テスト対象のingesterを構築する。fetchHtmlをオーバーライドしてモック化する。
     */
    private OfficialDocsIngester createIngesterWithMockFetch(Map<String, String> urlToHtml) {
        return new OfficialDocsIngester(
                htmlParser, chunkingService, embeddingClient, repository, ingestionConfig) {
            @Override
            String fetchHtml(String url) {
                String html = urlToHtml.get(url);
                if (html == null) {
                    throw new RuntimeException("Not found: " + url);
                }
                return html;
            }
        };
    }

    @Nested
    @DisplayName("ingestAll")
    class IngestAllTests {

        @Test
        @DisplayName("正常取込フロー: クローリング→パース→チャンク→Embedding→格納")
        void normalIngestionFlow() {
            // インデックスとドキュメントHTMLを準備
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/", INDEX_HTML,
                    "http://test-server/docs/page1.html", DOC_HTML,
                    "http://test-server/docs/page2.html", DOC_HTML
            ));

            // パーサーモック
            ParsedDocument parsedDoc = new ParsedDocument(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/page1.html"),
                    "http://test-server/docs/page1.html", ContentType.HTML);
            when(htmlParser.parse(anyString(), anyString())).thenReturn(List.of(parsedDoc));

            // チャンキングモック
            DocumentChunkDto chunkDto = new DocumentChunkDto(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/page1.html"),
                    0, 1, ContentType.HTML);
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(List.of(chunkDto));

            // Embeddingモック
            float[] mockEmbedding = new float[]{0.1f, 0.2f, 0.3f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding, mockEmbedding));

            // Repositoryモック
            DocumentChunk savedEntity = new DocumentChunk();
            savedEntity.setId(1L);
            when(repository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
                DocumentChunk arg = invocation.getArgument(0);
                arg.setId((long) (Math.random() * 10000));
                return arg;
            });

            // 実行
            IngestionResult result = ingester.ingestAll();

            // 検証
            assertEquals(2, result.processedCount());
            assertEquals(2, result.successCount());
            assertEquals(0, result.errorCount());
            assertTrue(result.errors().isEmpty());

            // パーサーが2回呼ばれた
            verify(htmlParser, times(2)).parse(anyString(), anyString());
            // チャンキングが2回呼ばれた
            verify(chunkingService, times(2)).chunk(any(ParsedDocument.class));
            // Embeddingが呼ばれた
            verify(embeddingClient, atLeastOnce()).embedBatch(anyList());
            // Repositoryが呼ばれた
            verify(repository, atLeastOnce()).save(any(DocumentChunk.class));
            verify(repository, atLeastOnce()).updateEmbedding(anyLong(), anyString());
        }

        @Test
        @DisplayName("無効化されている場合は空結果を返す")
        void disabledReturnsEmpty() {
            ingestionConfig.getOfficialDocs().setEnabled(false);
            ingester = createIngesterWithMockFetch(Map.of());

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            assertEquals(0, result.successCount());
            verifyNoInteractions(htmlParser, chunkingService, embeddingClient, repository);
        }

        @Test
        @DisplayName("インデックスページにリンクがない場合は空結果を返す")
        void noLinksReturnsEmpty() {
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/", "<html><body>No links</body></html>"
            ));

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            verifyNoInteractions(htmlParser, chunkingService, embeddingClient, repository);
        }

        @Test
        @DisplayName("個別ドキュメント障害隔離: 1ページ失敗しても他は続行")
        void individualDocumentFailureIsolation() {
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/", INDEX_HTML,
                    "http://test-server/docs/page1.html", DOC_HTML
                    // page2.html は存在しない → 例外発生
            ));

            // page1のパース/チャンク/Embedding設定
            ParsedDocument parsedDoc = new ParsedDocument(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/page1.html"),
                    "http://test-server/docs/page1.html", ContentType.HTML);
            when(htmlParser.parse(eq(DOC_HTML), anyString())).thenReturn(List.of(parsedDoc));

            DocumentChunkDto chunkDto = new DocumentChunkDto(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/page1.html"),
                    0, 1, ContentType.HTML);
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(List.of(chunkDto));

            float[] mockEmbedding = new float[]{0.1f, 0.2f, 0.3f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding));

            when(repository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
                DocumentChunk arg = invocation.getArgument(0);
                arg.setId(1L);
                return arg;
            });

            IngestionResult result = ingester.ingestAll();

            // page1は成功、page2は失敗
            assertEquals(2, result.processedCount());
            assertEquals(1, result.successCount());
            assertEquals(1, result.errorCount());
            assertFalse(result.errors().isEmpty());
            assertTrue(result.errors().get(0).url().contains("page2.html"));
        }

        @Test
        @DisplayName("空結果: パーサーが空リストを返す場合も成功扱い")
        void emptyParseResultCountsAsSuccess() {
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/",
                    "<html><body><a href=\"empty.html\">Empty</a></body></html>",
                    "http://test-server/docs/empty.html", "<html><body></body></html>"
            ));

            when(htmlParser.parse(anyString(), anyString())).thenReturn(List.of());

            IngestionResult result = ingester.ingestAll();

            assertEquals(1, result.processedCount());
            assertEquals(1, result.successCount());
            assertEquals(0, result.errorCount());
        }
    }

    @Nested
    @DisplayName("ingestIncremental")
    class IngestIncrementalTests {

        @Test
        @DisplayName("増分取込も全URLを処理する（Last-Modified非対応のため）")
        void incrementalProcessesAllUrls() {
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/", INDEX_HTML,
                    "http://test-server/docs/page1.html", DOC_HTML,
                    "http://test-server/docs/page2.html", DOC_HTML
            ));

            ParsedDocument parsedDoc = new ParsedDocument(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/page1.html"),
                    "http://test-server/docs/page1.html", ContentType.HTML);
            when(htmlParser.parse(anyString(), anyString())).thenReturn(List.of(parsedDoc));

            DocumentChunkDto chunkDto = new DocumentChunkDto(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja"),
                    0, 1, ContentType.HTML);
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(List.of(chunkDto));

            float[] mockEmbedding = new float[]{0.1f, 0.2f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding, mockEmbedding));

            when(repository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
                DocumentChunk arg = invocation.getArgument(0);
                arg.setId((long) (Math.random() * 10000));
                return arg;
            });

            IngestionResult result = ingester.ingestIncremental(
                    Instant.parse("2026-01-01T00:00:00Z"));

            assertEquals(2, result.processedCount());
            assertEquals(2, result.successCount());
        }

        @Test
        @DisplayName("無効化されている場合は空結果を返す")
        void disabledReturnsEmpty() {
            ingestionConfig.getOfficialDocs().setEnabled(false);
            ingester = createIngesterWithMockFetch(Map.of());

            IngestionResult result = ingester.ingestIncremental(Instant.now());

            assertEquals(0, result.processedCount());
        }
    }

    @Nested
    @DisplayName("バッチサイズ制御")
    class BatchSizeTests {

        @Test
        @DisplayName("バッチサイズを超えるとEmbeddingが複数回呼ばれる")
        void batchSizeControlsEmbeddingCalls() {
            // バッチサイズ2に設定
            ingestionConfig.getOfficialDocs().setBatchSize(2);

            // 3ページのインデックス
            String indexHtml = "<html><body>"
                    + "<a href=\"p1.html\">1</a>"
                    + "<a href=\"p2.html\">2</a>"
                    + "<a href=\"p3.html\">3</a>"
                    + "</body></html>";

            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/", indexHtml,
                    "http://test-server/docs/p1.html", DOC_HTML,
                    "http://test-server/docs/p2.html", DOC_HTML,
                    "http://test-server/docs/p3.html", DOC_HTML
            ));

            ParsedDocument parsedDoc = new ParsedDocument(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/p1.html"),
                    "http://test-server/docs/p1.html", ContentType.HTML);
            when(htmlParser.parse(anyString(), anyString())).thenReturn(List.of(parsedDoc));

            DocumentChunkDto chunkDto = new DocumentChunkDto(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja"),
                    0, 1, ContentType.HTML);
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(List.of(chunkDto));

            float[] mockEmbedding = new float[]{0.1f};
            // バッチサイズ2で3チャンク → embedBatch2回（2+1）
            when(embeddingClient.embedBatch(anyList()))
                    .thenReturn(List.of(mockEmbedding, mockEmbedding))
                    .thenReturn(List.of(mockEmbedding));

            when(repository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
                DocumentChunk arg = invocation.getArgument(0);
                arg.setId((long) (Math.random() * 10000));
                return arg;
            });

            IngestionResult result = ingester.ingestAll();

            assertEquals(3, result.processedCount());
            assertEquals(3, result.successCount());
            // embedBatchが2回呼ばれる（batch2 + remainder1）
            verify(embeddingClient, atLeast(2)).embedBatch(anyList());
        }
    }

    @Nested
    @DisplayName("getSourceName")
    class SourceNameTests {

        @Test
        @DisplayName("データソース名が正しい")
        void sourceNameIsCorrect() {
            ingester = createIngesterWithMockFetch(Map.of());
            assertEquals("nablarch-official-docs", ingester.getSourceName());
        }
    }

    @Nested
    @DisplayName("URL解決")
    class UrlResolveTests {

        @Test
        @DisplayName("相対URLをベースURLから解決する")
        void resolveRelativeUrl() {
            assertEquals("http://example.com/docs/page.html",
                    OfficialDocsIngester.resolveUrl("http://example.com/docs/", "page.html"));
        }

        @Test
        @DisplayName("./で始まる相対URLを解決する")
        void resolveDotSlashUrl() {
            assertEquals("http://example.com/docs/page.html",
                    OfficialDocsIngester.resolveUrl("http://example.com/docs/", "./page.html"));
        }

        @Test
        @DisplayName("絶対URLはそのまま返す")
        void absoluteUrlPassThrough() {
            assertEquals("https://other.com/page.html",
                    OfficialDocsIngester.resolveUrl("http://example.com/docs/", "https://other.com/page.html"));
        }

        @Test
        @DisplayName("ベースURLが末尾スラッシュなしでも正しく解決する")
        void baseUrlWithoutTrailingSlash() {
            assertEquals("http://example.com/docs/page.html",
                    OfficialDocsIngester.resolveUrl("http://example.com/docs/index.html", "page.html"));
        }
    }

    @Nested
    @DisplayName("ベクトル文字列変換")
    class VectorStringTests {

        @Test
        @DisplayName("float配列をpgvector文字列に変換する")
        void arrayToVectorString() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            String result = OfficialDocsIngester.arrayToVectorString(embedding);
            assertTrue(result.startsWith("["));
            assertTrue(result.endsWith("]"));
            assertTrue(result.contains("0.1"));
            assertTrue(result.contains("0.2"));
            assertTrue(result.contains("0.3"));
        }

        @Test
        @DisplayName("空配列は空ベクトルになる")
        void emptyArray() {
            String result = OfficialDocsIngester.arrayToVectorString(new float[]{});
            assertEquals("[]", result);
        }
    }

    @Nested
    @DisplayName("エンティティ変換")
    class EntityConversionTests {

        @Test
        @DisplayName("DocumentChunkDtoからDocumentChunkエンティティに正しく変換される")
        void dtoToEntityConversion() {
            ingester = createIngesterWithMockFetch(Map.of(
                    "http://test-server/docs/",
                    "<html><body><a href=\"single.html\">S</a></body></html>",
                    "http://test-server/docs/single.html", DOC_HTML
            ));

            ParsedDocument parsedDoc = new ParsedDocument(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/single.html"),
                    "http://test-server/docs/single.html", ContentType.HTML);
            when(htmlParser.parse(anyString(), anyString())).thenReturn(List.of(parsedDoc));

            DocumentChunkDto chunkDto = new DocumentChunkDto(
                    "テスト内容。十分な長さのコンテンツが必要。テスト内容を繰り返す。",
                    Map.of("source", "nablarch-document", "language", "ja",
                            "source_url", "http://test-server/docs/single.html"),
                    0, 1, ContentType.HTML);
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(List.of(chunkDto));

            float[] mockEmbedding = new float[]{0.5f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding));

            ArgumentCaptor<DocumentChunk> entityCaptor = ArgumentCaptor.forClass(DocumentChunk.class);
            when(repository.save(entityCaptor.capture())).thenAnswer(invocation -> {
                DocumentChunk arg = invocation.getArgument(0);
                arg.setId(42L);
                return arg;
            });

            ingester.ingestAll();

            DocumentChunk savedEntity = entityCaptor.getValue();
            assertEquals("nablarch-official-docs", savedEntity.getSource());
            assertEquals("documentation", savedEntity.getSourceType());
            assertEquals("ja", savedEntity.getLanguage());
            assertEquals("http://test-server/docs/single.html", savedEntity.getUrl());

            // Embeddingも更新されたか検証
            verify(repository).updateEmbedding(eq(42L), contains("0.5"));
        }
    }
}

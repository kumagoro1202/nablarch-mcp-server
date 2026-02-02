package com.tis.nablarch.mcp.rag.ingestion;

import com.tis.nablarch.mcp.db.entity.DocumentChunk;
import com.tis.nablarch.mcp.db.repository.DocumentChunkRepository;
import com.tis.nablarch.mcp.embedding.EmbeddingClient;
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
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 取込パイプライン統合テスト。
 *
 * <p>OfficialDocsIngester及びFintanIngesterのパイプライン全体フローを検証する。
 * 外部依存（WebClient, EmbeddingClient, DocumentChunkRepository）はモックで差替え、
 * パイプラインのInter-Component統合（パース→チャンキング→Embedding→格納）を検証する。</p>
 */
class IngestionPipelineIntegrationTest {

    private HtmlDocumentParser htmlParser;
    private MarkdownDocumentParser markdownParser;
    private ChunkingService chunkingService;
    private EmbeddingClient embeddingClient;
    private DocumentChunkRepository repository;

    /** ID自動採番用カウンタ */
    private final AtomicLong idCounter = new AtomicLong(1);

    /** テスト用インデックスHTML（3ページ分のリンク） */
    private static final String INDEX_HTML_3PAGES = "<html><body>"
            + "<a href=\"page1.html\">Page 1</a>"
            + "<a href=\"page2.html\">Page 2</a>"
            + "<a href=\"page3.html\">Page 3</a>"
            + "</body></html>";

    /** テスト用ドキュメントHTML */
    private static final String SAMPLE_DOC_HTML = "<html><head><title>テストドキュメント</title></head>"
            + "<body><h2>セクション1</h2>"
            + "<p>Nablarchの実行制御基盤は、アプリケーションのライフサイクルを管理する。"
            + "リクエスト受付からレスポンス返却までの一連の処理フローを制御する。</p></body></html>";

    /** テスト用Markdownコンテンツ */
    private static final String SAMPLE_MARKDOWN = "# テスト記事タイトル\n\n"
            + "本記事ではNablarchフレームワークを使用した開発手順を解説する。\n\n"
            + "## セットアップ\n\n"
            + "Nablarchのプロジェクトは、Maven Archetypeを使用して初期構築する。"
            + "必要な依存関係と設定ファイルが自動生成される。\n\n"
            + "## テスト戦略\n\n"
            + "RESTful APIのテストは、リクエスト単体テスト機能を使用する。"
            + "テストデータのセットアップはExcelファイルで行う。";

    @BeforeEach
    void setUp() {
        htmlParser = mock(HtmlDocumentParser.class);
        markdownParser = mock(MarkdownDocumentParser.class);
        chunkingService = mock(ChunkingService.class);
        embeddingClient = mock(EmbeddingClient.class);
        repository = mock(DocumentChunkRepository.class);
        idCounter.set(1);
    }

    /**
     * Repository.save()のモック設定（ID自動採番）。
     */
    private void setupRepositoryMock() {
        when(repository.save(any(DocumentChunk.class))).thenAnswer(invocation -> {
            DocumentChunk arg = invocation.getArgument(0);
            arg.setId(idCounter.getAndIncrement());
            return arg;
        });
    }

    /**
     * テスト用のParsedDocumentを生成する。
     */
    private ParsedDocument createParsedDocument(String content, String url, ContentType type) {
        return new ParsedDocument(content,
                Map.of("source", "test", "language", "ja", "source_url", url),
                url, type);
    }

    /**
     * テスト用のDocumentChunkDtoを生成する。
     */
    private DocumentChunkDto createChunkDto(String content, String url, ContentType type) {
        return new DocumentChunkDto(content,
                Map.of("source", "test", "language", "ja", "source_url", url),
                0, 1, type);
    }

    // ===== OfficialDocsIngester パイプライン =====

    @Nested
    @DisplayName("OfficialDocsIngester全件取込フロー")
    class OfficialDocsFullIngestionTest {

        private IngestionConfig ingestionConfig;

        @BeforeEach
        void setUpConfig() {
            ingestionConfig = new IngestionConfig();
            ingestionConfig.getOfficialDocs().setBaseUrl("http://test/docs/");
            ingestionConfig.getOfficialDocs().setBatchSize(10);
            ingestionConfig.getOfficialDocs().setDelayMs(0);
            ingestionConfig.getOfficialDocs().setMaxRetries(1);
            ingestionConfig.getOfficialDocs().setEnabled(true);
        }

        /**
         * fetchHtmlをオーバーライドしたIngesterを構築する。
         */
        private OfficialDocsIngester createIngester(Map<String, String> urlToHtml) {
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

        @Test
        @DisplayName("パイプライン全体フロー: HTML取得→パース→チャンキング→Embedding→格納")
        void fullPipelineFlow() {
            // 2ページのインデックス
            String indexHtml = "<html><body>"
                    + "<a href=\"page1.html\">P1</a>"
                    + "<a href=\"page2.html\">P2</a>"
                    + "</body></html>";

            OfficialDocsIngester ingester = createIngester(Map.of(
                    "http://test/docs/", indexHtml,
                    "http://test/docs/page1.html", SAMPLE_DOC_HTML,
                    "http://test/docs/page2.html", SAMPLE_DOC_HTML
            ));

            // パーサーモック: 各URLに対して1つのParsedDocumentを返す
            when(htmlParser.parse(anyString(), anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(1);
                return List.of(createParsedDocument(
                        "パース済みコンテンツ。十分な長さのテスト用テキストを含む文章である。", url, ContentType.HTML));
            });

            // チャンキングモック: 各ParsedDocumentに対して1つのチャンクを返す
            when(chunkingService.chunk(any(ParsedDocument.class))).thenAnswer(invocation -> {
                ParsedDocument doc = invocation.getArgument(0);
                return List.of(createChunkDto(doc.content(), doc.sourceUrl(), ContentType.HTML));
            });

            // Embeddingモック
            float[] mockEmbedding = new float[]{0.1f, 0.2f, 0.3f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding, mockEmbedding));

            // Repositoryモック
            setupRepositoryMock();

            // 実行
            IngestionResult result = ingester.ingestAll();

            // 結果検証
            assertEquals(2, result.processedCount(), "処理対象は2ページ");
            assertEquals(2, result.successCount(), "2ページとも成功");
            assertEquals(0, result.errorCount(), "エラーなし");
            assertTrue(result.errors().isEmpty());

            // パイプライン各ステージの呼出回数検証
            verify(htmlParser, times(2)).parse(anyString(), anyString());
            verify(chunkingService, times(2)).chunk(any(ParsedDocument.class));
            verify(embeddingClient, atLeastOnce()).embedBatch(anyList());
            verify(repository, times(2)).save(any(DocumentChunk.class));
            verify(repository, times(2)).updateEmbedding(anyLong(), anyString());
        }
    }

    // ===== FintanIngester パイプライン =====

    @Nested
    @DisplayName("FintanIngester全件取込フロー")
    class FintanFullIngestionTest {

        private FintanIngestionConfig fintanConfig;

        @BeforeEach
        void setUpConfig() {
            fintanConfig = new FintanIngestionConfig();
            fintanConfig.setBaseUrl("https://fintan.jp/");
            fintanConfig.setSearchTags(List.of("Nablarch"));
            fintanConfig.setBatchSize(10);
            fintanConfig.setDelayMs(0);
            fintanConfig.setMaxRetries(1);
            fintanConfig.setEnabled(true);
        }

        /**
         * fetchWithRetryとfetchArticleUrlsをオーバーライドしたFintanIngesterを構築する。
         */
        private FintanIngester createIngester(List<String> articleUrls, Map<String, String> urlToContent) {
            return new FintanIngester(
                    markdownParser, htmlParser, chunkingService,
                    embeddingClient, repository, null, fintanConfig) {

                @Override
                List<String> fetchArticleUrls() {
                    return articleUrls;
                }

                @Override
                String fetchWithRetry(String url) {
                    String content = urlToContent.get(url);
                    if (content == null) {
                        throw new RuntimeException("Not found: " + url);
                    }
                    return content;
                }

                @Override
                void sleep(long millis) {
                    // テスト用にスリープしない
                }
            };
        }

        @Test
        @DisplayName("パイプライン全体フロー: Markdown取得→パース→チャンキング→Embedding→格納")
        void fullPipelineFlow() {
            List<String> urls = List.of(
                    "https://fintan.jp/page/101/",
                    "https://fintan.jp/page/102/"
            );

            FintanIngester ingester = createIngester(urls, Map.of(
                    "https://fintan.jp/page/101/", SAMPLE_MARKDOWN,
                    "https://fintan.jp/page/102/", SAMPLE_MARKDOWN
            ));

            // FintanIngesterはisHtml()でMarkdownと判定し、markdownParserを使用
            when(markdownParser.parse(anyString(), anyString())).thenAnswer(invocation -> {
                String url = invocation.getArgument(1);
                return List.of(createParsedDocument(
                        "Markdownパース済みコンテンツ。テスト用の十分な長さのテキストである。",
                        url, ContentType.MARKDOWN));
            });

            when(chunkingService.chunk(any(ParsedDocument.class))).thenAnswer(invocation -> {
                ParsedDocument doc = invocation.getArgument(0);
                return List.of(createChunkDto(doc.content(), doc.sourceUrl(), ContentType.MARKDOWN));
            });

            float[] mockEmbedding = new float[]{0.4f, 0.5f, 0.6f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding));

            setupRepositoryMock();

            // 実行
            IngestionResult result = ingester.ingestAll();

            // 結果検証
            assertEquals(2, result.processedCount());
            assertEquals(2, result.successCount());
            assertEquals(0, result.errorCount());

            // パイプライン各ステージ検証
            verify(markdownParser, times(2)).parse(anyString(), anyString());
            verify(chunkingService, times(2)).chunk(any(ParsedDocument.class));
            verify(embeddingClient, times(2)).embedBatch(anyList());
            verify(repository, times(2)).save(any(DocumentChunk.class));
            verify(repository, times(2)).updateEmbedding(anyLong(), anyString());
        }
    }

    // ===== 増分取込テスト =====

    @Nested
    @DisplayName("増分取込（incremental）フロー")
    class IncrementalIngestionTest {

        @Test
        @DisplayName("OfficialDocsIngester: since指定で増分取込を実行")
        void officialDocsIncrementalIngestion() {
            IngestionConfig config = new IngestionConfig();
            config.getOfficialDocs().setBaseUrl("http://test/docs/");
            config.getOfficialDocs().setBatchSize(10);
            config.getOfficialDocs().setDelayMs(0);
            config.getOfficialDocs().setMaxRetries(1);
            config.getOfficialDocs().setEnabled(true);

            // 1ページだけのインデックス
            String indexHtml = "<html><body><a href=\"updated.html\">Updated</a></body></html>";

            OfficialDocsIngester ingester = new OfficialDocsIngester(
                    htmlParser, chunkingService, embeddingClient, repository, config) {
                @Override
                String fetchHtml(String url) {
                    if (url.equals("http://test/docs/")) {
                        return indexHtml;
                    }
                    return SAMPLE_DOC_HTML;
                }
            };

            when(htmlParser.parse(anyString(), anyString())).thenReturn(
                    List.of(createParsedDocument(
                            "更新済みコンテンツ。十分な長さのテスト用テキストを含む文章である。",
                            "http://test/docs/updated.html", ContentType.HTML)));
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(
                    List.of(createChunkDto(
                            "更新済みチャンク。十分な長さのテスト用テキストを含む文章である。",
                            "http://test/docs/updated.html", ContentType.HTML)));
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(new float[]{0.1f}));
            setupRepositoryMock();

            // since = 1時間前
            Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
            IngestionResult result = ingester.ingestIncremental(since);

            // 公式ドキュメントは全URLを処理（Last-Modified非対応のため）
            assertEquals(1, result.processedCount());
            assertEquals(1, result.successCount());
            assertEquals(0, result.errorCount());
        }

        @Test
        @DisplayName("FintanIngester: since指定で増分取込（新規URLのみ処理）")
        void fintanIncrementalIngestion() {
            FintanIngestionConfig config = new FintanIngestionConfig();
            config.setBaseUrl("https://fintan.jp/");
            config.setSearchTags(List.of("Nablarch"));
            config.setBatchSize(10);
            config.setDelayMs(0);
            config.setMaxRetries(1);
            config.setEnabled(true);

            // 既存チャンク（page/101は取込済み）
            DocumentChunk existingChunk = new DocumentChunk();
            existingChunk.setUrl("https://fintan.jp/page/101/");
            existingChunk.setSource("fintan");
            when(repository.findBySource("fintan")).thenReturn(List.of(existingChunk));

            FintanIngester ingester = new FintanIngester(
                    markdownParser, htmlParser, chunkingService,
                    embeddingClient, repository, null, config) {
                @Override
                List<String> fetchArticleUrls() {
                    // 2つのURL（101は既存、102は新規）
                    return List.of(
                            "https://fintan.jp/page/101/",
                            "https://fintan.jp/page/102/"
                    );
                }

                @Override
                String fetchWithRetry(String url) {
                    return SAMPLE_MARKDOWN;
                }

                @Override
                void sleep(long millis) {
                    // テスト用にスリープしない
                }
            };

            when(markdownParser.parse(anyString(), anyString())).thenReturn(
                    List.of(createParsedDocument(
                            "新規記事コンテンツ。テスト用の十分な長さのテキストである。",
                            "https://fintan.jp/page/102/", ContentType.MARKDOWN)));
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(
                    List.of(createChunkDto(
                            "新規記事チャンク。テスト用の十分な長さのテキストである。",
                            "https://fintan.jp/page/102/", ContentType.MARKDOWN)));
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(new float[]{0.2f}));
            setupRepositoryMock();

            Instant since = Instant.now().minus(1, ChronoUnit.HOURS);
            IngestionResult result = ingester.ingestIncremental(since);

            // page/102のみ処理（page/101は既存のため除外）
            assertEquals(1, result.processedCount());
            assertEquals(1, result.successCount());
            assertEquals(0, result.errorCount());
        }
    }

    // ===== 障害隔離テスト =====

    @Nested
    @DisplayName("障害隔離テスト")
    class FaultIsolationTest {

        @Test
        @DisplayName("パース失敗が1件あっても他のドキュメントは正常に処理される")
        void parseFailureIsolation() {
            IngestionConfig config = new IngestionConfig();
            config.getOfficialDocs().setBaseUrl("http://test/docs/");
            config.getOfficialDocs().setBatchSize(10);
            config.getOfficialDocs().setDelayMs(0);
            config.getOfficialDocs().setMaxRetries(1);
            config.getOfficialDocs().setEnabled(true);

            OfficialDocsIngester ingester = new OfficialDocsIngester(
                    htmlParser, chunkingService, embeddingClient, repository, config) {
                @Override
                String fetchHtml(String url) {
                    if (url.equals("http://test/docs/")) {
                        return INDEX_HTML_3PAGES;
                    }
                    return SAMPLE_DOC_HTML;
                }
            };

            // page1: パース成功
            // page2: パース時に例外
            // page3: パース成功
            when(htmlParser.parse(anyString(), eq("http://test/docs/page1.html")))
                    .thenReturn(List.of(createParsedDocument(
                            "1件目のパース結果。十分な長さのテスト用テキストを含む文章である。",
                            "http://test/docs/page1.html", ContentType.HTML)));
            when(htmlParser.parse(anyString(), eq("http://test/docs/page2.html")))
                    .thenThrow(new RuntimeException("パースエラー: 不正なHTML"));
            when(htmlParser.parse(anyString(), eq("http://test/docs/page3.html")))
                    .thenReturn(List.of(createParsedDocument(
                            "3件目のパース結果。十分な長さのテスト用テキストを含む文章である。",
                            "http://test/docs/page3.html", ContentType.HTML)));

            when(chunkingService.chunk(any(ParsedDocument.class))).thenAnswer(invocation -> {
                ParsedDocument doc = invocation.getArgument(0);
                return List.of(createChunkDto(doc.content(), doc.sourceUrl(), ContentType.HTML));
            });

            float[] mockEmbedding = new float[]{0.1f, 0.2f};
            when(embeddingClient.embedBatch(anyList())).thenReturn(List.of(mockEmbedding, mockEmbedding));
            setupRepositoryMock();

            IngestionResult result = ingester.ingestAll();

            // 3件処理、2件成功、1件エラー
            assertEquals(3, result.processedCount());
            assertEquals(2, result.successCount());
            assertEquals(1, result.errorCount());
            assertEquals(1, result.errors().size());
            assertTrue(result.errors().get(0).contains("page2.html"));

            // page1とpage3のみEmbedding→格納される
            verify(repository, times(2)).save(any(DocumentChunk.class));
            verify(repository, times(2)).updateEmbedding(anyLong(), anyString());
        }
    }

    // ===== バッチサイズ制御テスト =====

    @Nested
    @DisplayName("バッチサイズ制御")
    class BatchSizeControlTest {

        @Test
        @DisplayName("30チャンク、バッチサイズ10 → embedBatch 3回呼出")
        void batchSizeControlsEmbeddingBatchCalls() {
            IngestionConfig config = new IngestionConfig();
            config.getOfficialDocs().setBaseUrl("http://test/docs/");
            config.getOfficialDocs().setBatchSize(10);
            config.getOfficialDocs().setDelayMs(0);
            config.getOfficialDocs().setMaxRetries(1);
            config.getOfficialDocs().setEnabled(true);

            // 1ページのインデックス（1ページから30チャンク生成）
            String indexHtml = "<html><body><a href=\"big-page.html\">Big</a></body></html>";

            OfficialDocsIngester ingester = new OfficialDocsIngester(
                    htmlParser, chunkingService, embeddingClient, repository, config) {
                @Override
                String fetchHtml(String url) {
                    if (url.equals("http://test/docs/")) {
                        return indexHtml;
                    }
                    return SAMPLE_DOC_HTML;
                }
            };

            // パーサーは1つのParsedDocumentを返す
            when(htmlParser.parse(anyString(), anyString())).thenReturn(
                    List.of(createParsedDocument(
                            "大量コンテンツ。十分な長さのテスト用テキストを含む文章である。",
                            "http://test/docs/big-page.html", ContentType.HTML)));

            // チャンキングで30チャンクを生成
            List<DocumentChunkDto> thirtyChunks = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                thirtyChunks.add(new DocumentChunkDto(
                        "チャンク" + i + "のコンテンツ。十分な長さのテスト用テキストを含む。",
                        Map.of("source", "test", "language", "ja"),
                        i, 30, ContentType.HTML));
            }
            when(chunkingService.chunk(any(ParsedDocument.class))).thenReturn(thirtyChunks);

            // Embeddingモック（バッチサイズ10で呼ばれる）
            List<float[]> tenEmbeddings = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tenEmbeddings.add(new float[]{0.1f * i});
            }
            when(embeddingClient.embedBatch(anyList())).thenReturn(tenEmbeddings);

            setupRepositoryMock();

            IngestionResult result = ingester.ingestAll();

            assertEquals(1, result.processedCount());
            assertEquals(1, result.successCount());

            // embedBatchが3回呼ばれる（10 + 10 + 10）
            // ただしOfficialDocsIngesterのembedAndStoreはbatchSize単位で処理する
            // processUrlsで蓄積→batchSize(10)超過で1回、残り20→embedAndStoreで10+10=2回
            // 合計: embedBatchは3回呼出
            verify(embeddingClient, times(3)).embedBatch(anyList());

            // 30チャンク全て格納
            verify(repository, times(30)).save(any(DocumentChunk.class));
            verify(repository, times(30)).updateEmbedding(anyLong(), anyString());
        }
    }

    // ===== 空データソーステスト =====

    @Nested
    @DisplayName("空データソーステスト")
    class EmptyDataSourceTest {

        @Test
        @DisplayName("OfficialDocs: URL一覧が空 → IngestionResult(0,0,0,[])")
        void officialDocsEmptyUrlList() {
            IngestionConfig config = new IngestionConfig();
            config.getOfficialDocs().setBaseUrl("http://test/docs/");
            config.getOfficialDocs().setDelayMs(0);
            config.getOfficialDocs().setEnabled(true);

            OfficialDocsIngester ingester = new OfficialDocsIngester(
                    htmlParser, chunkingService, embeddingClient, repository, config) {
                @Override
                String fetchHtml(String url) {
                    // インデックスページにはリンクなし
                    return "<html><body><p>No documents available</p></body></html>";
                }
            };

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            assertEquals(0, result.successCount());
            assertEquals(0, result.errorCount());
            assertTrue(result.errors().isEmpty());

            // 下流ステージは一切呼ばれない
            verifyNoInteractions(chunkingService, embeddingClient, repository);
        }

        @Test
        @DisplayName("FintanIngester: 記事URL一覧が空 → IngestionResult(0,0,0,[])")
        void fintanEmptyArticleList() {
            FintanIngestionConfig config = new FintanIngestionConfig();
            config.setBaseUrl("https://fintan.jp/");
            config.setSearchTags(List.of("Nablarch"));
            config.setEnabled(true);
            config.setDelayMs(0);

            FintanIngester ingester = new FintanIngester(
                    markdownParser, htmlParser, chunkingService,
                    embeddingClient, repository, null, config) {
                @Override
                List<String> fetchArticleUrls() {
                    return List.of(); // 空のURL一覧
                }

                @Override
                void sleep(long millis) {
                    // テスト用にスリープしない
                }
            };

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            assertEquals(0, result.successCount());
            assertEquals(0, result.errorCount());
            assertTrue(result.errors().isEmpty());

            verifyNoInteractions(chunkingService, embeddingClient);
        }

        @Test
        @DisplayName("OfficialDocs: 無効化状態 → 空結果")
        void officialDocsDisabled() {
            IngestionConfig config = new IngestionConfig();
            config.getOfficialDocs().setEnabled(false);

            OfficialDocsIngester ingester = new OfficialDocsIngester(
                    htmlParser, chunkingService, embeddingClient, repository, config) {
                @Override
                String fetchHtml(String url) {
                    fail("無効化状態ではfetchHtmlは呼ばれないはず");
                    return null;
                }
            };

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            assertEquals(0, result.successCount());
            assertEquals(0, result.errorCount());
            assertTrue(result.errors().isEmpty());
        }

        @Test
        @DisplayName("FintanIngester: 無効化状態 → 空結果")
        void fintanDisabled() {
            FintanIngestionConfig config = new FintanIngestionConfig();
            config.setEnabled(false);

            FintanIngester ingester = new FintanIngester(
                    markdownParser, htmlParser, chunkingService,
                    embeddingClient, repository, null, config) {
                @Override
                void sleep(long millis) {
                }
            };

            IngestionResult result = ingester.ingestAll();

            assertEquals(0, result.processedCount());
            assertEquals(0, result.successCount());
            assertEquals(0, result.errorCount());
        }
    }
}

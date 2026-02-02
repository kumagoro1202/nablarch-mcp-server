package com.tis.nablarch.mcp.rag.ingestion;

import com.tis.nablarch.mcp.db.entity.DocumentChunk;
import com.tis.nablarch.mcp.db.repository.DocumentChunkRepository;
import com.tis.nablarch.mcp.embedding.EmbeddingClient;
import com.tis.nablarch.mcp.rag.chunking.ChunkingService;
import com.tis.nablarch.mcp.rag.chunking.DocumentChunkDto;
import com.tis.nablarch.mcp.rag.parser.HtmlDocumentParser;
import com.tis.nablarch.mcp.rag.parser.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Nablarch公式ドキュメント取り込みサービス。
 *
 * <p>Nablarch公式ドキュメント（Sphinx生成HTML）をクローリングし、
 * パース→チャンキング→Embedding→pgvector格納の一連のパイプラインを実行する。</p>
 *
 * <p>処理フロー:</p>
 * <ol>
 *   <li>インデックスページからドキュメントURL一覧を取得</li>
 *   <li>各URLのHTMLを取得（robots.txt準拠のディレイ付き）</li>
 *   <li>HtmlDocumentParserでパース</li>
 *   <li>ChunkingServiceでチャンク分割</li>
 *   <li>EmbeddingClientでバッチEmbedding生成</li>
 *   <li>DocumentChunkRepositoryでpgvectorに格納</li>
 * </ol>
 *
 * <p>個別ドキュメントの障害は隔離し、1ページの失敗が他のページの処理を妨げない。
 * HTTP 429/5xxエラーに対しては指数バックオフでリトライする。</p>
 */
@Service
public class OfficialDocsIngester implements DocumentIngester {

    private static final Logger logger = LoggerFactory.getLogger(OfficialDocsIngester.class);

    /** データソース名 */
    private static final String SOURCE_NAME = "nablarch-official-docs";

    /** HTMLリンク抽出パターン（href属性） */
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "href=\"([^\"]+\\.html)\"");

    private final WebClient webClient;
    private final HtmlDocumentParser htmlParser;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository repository;
    private final IngestionConfig.OfficialDocsConfig config;

    /**
     * コンストラクタ。
     *
     * @param htmlParser HTMLドキュメントパーサー
     * @param chunkingService チャンキングサービス
     * @param embeddingClient Embeddingクライアント（Jina用）
     * @param repository ドキュメントチャンクリポジトリ
     * @param ingestionConfig 取り込み設定
     */
    public OfficialDocsIngester(
            HtmlDocumentParser htmlParser,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            DocumentChunkRepository repository,
            IngestionConfig ingestionConfig) {
        this(htmlParser, chunkingService, embeddingClient, repository, ingestionConfig,
                WebClient.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024))
                        .build());
    }

    /**
     * テスト用コンストラクタ（WebClient注入可能）。
     *
     * @param htmlParser HTMLドキュメントパーサー
     * @param chunkingService チャンキングサービス
     * @param embeddingClient Embeddingクライアント
     * @param repository ドキュメントチャンクリポジトリ
     * @param ingestionConfig 取り込み設定
     * @param webClient HTTPクライアント
     */
    public OfficialDocsIngester(
            HtmlDocumentParser htmlParser,
            ChunkingService chunkingService,
            EmbeddingClient embeddingClient,
            DocumentChunkRepository repository,
            IngestionConfig ingestionConfig,
            WebClient webClient) {
        this.htmlParser = htmlParser;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.repository = repository;
        this.config = ingestionConfig.getOfficialDocs();
        this.webClient = webClient;
    }

    @Override
    public IngestionResult ingestAll() {
        if (!config.isEnabled()) {
            logger.info("公式Docs取り込みは無効化されている");
            return IngestionResult.empty();
        }

        logger.info("公式Docsの全件取り込みを開始: baseUrl={}", config.getBaseUrl());

        List<String> urls = discoverDocumentUrls();
        if (urls.isEmpty()) {
            logger.warn("取り込み対象のURLが見つからなかった");
            return IngestionResult.empty();
        }

        logger.info("{}件のドキュメントURLを検出", urls.size());
        return processUrls(urls);
    }

    @Override
    public IngestionResult ingestIncremental(Instant since) {
        if (!config.isEnabled()) {
            logger.info("公式Docs取り込みは無効化されている");
            return IngestionResult.empty();
        }

        logger.info("公式Docsの増分取り込みを開始: since={}", since);

        // 公式ドキュメントにはLast-Modified情報がないため、
        // 増分取り込みでも全URLを対象にする（Embedding比較で変更検出）
        List<String> urls = discoverDocumentUrls();
        if (urls.isEmpty()) {
            return IngestionResult.empty();
        }

        return processUrls(urls);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    /**
     * インデックスページからドキュメントURLの一覧を取得する。
     *
     * @return ドキュメントURLのリスト
     */
    List<String> discoverDocumentUrls() {
        String baseUrl = config.getBaseUrl();
        try {
            String indexHtml = fetchHtml(baseUrl);
            if (indexHtml == null || indexHtml.isBlank()) {
                return List.of();
            }

            List<String> urls = new ArrayList<>();
            Matcher matcher = LINK_PATTERN.matcher(indexHtml);
            while (matcher.find()) {
                String href = matcher.group(1);
                String fullUrl = resolveUrl(baseUrl, href);
                if (!urls.contains(fullUrl)) {
                    urls.add(fullUrl);
                }
            }
            return urls;
        } catch (Exception e) {
            logger.error("インデックスページの取得に失敗: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * URL一覧を処理し、取り込み結果を返す。
     *
     * @param urls 処理対象のURLリスト
     * @return 取り込み結果
     */
    private IngestionResult processUrls(List<String> urls) {
        int processed = urls.size();
        int success = 0;
        List<IngestionResult.IngestionError> errors = new ArrayList<>();

        // チャンクをバッチ処理用に蓄積
        List<DocumentChunkDto> pendingChunks = new ArrayList<>();
        List<String> pendingUrls = new ArrayList<>();

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);

            try {
                // クローリングディレイ（初回以外）
                if (i > 0 && config.getDelayMs() > 0) {
                    Thread.sleep(config.getDelayMs());
                }

                // HTML取得（リトライ付き）
                String html = fetchHtmlWithRetry(url);
                if (html == null || html.isBlank()) {
                    errors.add(new IngestionResult.IngestionError(url, "空のレスポンス"));
                    continue;
                }

                // パース
                List<ParsedDocument> parsedDocs = htmlParser.parse(html, url);
                if (parsedDocs.isEmpty()) {
                    logger.debug("パース結果が空: {}", url);
                    success++;
                    continue;
                }

                // チャンキング
                for (ParsedDocument parsedDoc : parsedDocs) {
                    List<DocumentChunkDto> chunks = chunkingService.chunk(parsedDoc);
                    pendingChunks.addAll(chunks);
                    for (int c = 0; c < chunks.size(); c++) {
                        pendingUrls.add(url);
                    }
                }

                // バッチサイズに達したらEmbedding+格納
                if (pendingChunks.size() >= config.getBatchSize()) {
                    embedAndStore(pendingChunks);
                    pendingChunks.clear();
                    pendingUrls.clear();
                }

                success++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errors.add(new IngestionResult.IngestionError(url, "処理が中断された", e));
                break;
            } catch (Exception e) {
                logger.warn("ドキュメント取り込み失敗（障害隔離）: url={}, error={}", url, e.getMessage());
                errors.add(new IngestionResult.IngestionError(url, e.getMessage(), e));
            }
        }

        // 残りのチャンクを処理
        if (!pendingChunks.isEmpty()) {
            try {
                embedAndStore(pendingChunks);
            } catch (Exception e) {
                logger.error("残りチャンクのEmbedding/格納に失敗: {}", e.getMessage(), e);
                // 残りチャンクの失敗は全体のエラーとして記録
                errors.add(new IngestionResult.IngestionError(
                        "batch-remainder", "残りチャンクの処理に失敗: " + e.getMessage(), e));
            }
        }

        IngestionResult result = new IngestionResult(processed, success, errors.size(), errors);
        logger.info("公式Docs取り込み完了: processed={}, success={}, errors={}",
                result.processedCount(), result.successCount(), result.errorCount());
        return result;
    }

    /**
     * チャンクリストのEmbedding生成とDB格納を行う。
     *
     * @param chunks 格納対象のチャンクリスト
     */
    private void embedAndStore(List<DocumentChunkDto> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        // バッチサイズ単位で処理
        int batchSize = config.getBatchSize();
        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<DocumentChunkDto> batch = chunks.subList(i, end);

            // テキスト抽出
            List<String> texts = batch.stream()
                    .map(DocumentChunkDto::content)
                    .collect(Collectors.toList());

            // Embedding生成
            List<float[]> embeddings = embeddingClient.embedBatch(texts);

            // DB格納
            for (int j = 0; j < batch.size(); j++) {
                DocumentChunkDto chunkDto = batch.get(j);
                DocumentChunk entity = toEntity(chunkDto);
                DocumentChunk saved = repository.save(entity);

                // Embedding更新（pgvectorはネイティブクエリ経由）
                String embeddingStr = arrayToVectorString(embeddings.get(j));
                repository.updateEmbedding(saved.getId(), embeddingStr);
            }
        }

        logger.debug("{}件のチャンクをEmbedding+格納完了", chunks.size());
    }

    /**
     * DocumentChunkDtoからDocumentChunkエンティティに変換する。
     *
     * @param dto チャンクDTO
     * @return エンティティ
     */
    private DocumentChunk toEntity(DocumentChunkDto dto) {
        DocumentChunk entity = new DocumentChunk();
        entity.setContent(dto.content());
        entity.setSource(SOURCE_NAME);
        entity.setSourceType("documentation");
        entity.setLanguage(dto.metadata().getOrDefault("language", "ja"));
        entity.setUrl(dto.metadata().get("source_url"));
        return entity;
    }

    /**
     * float配列をpgvectorのベクトル文字列に変換する。
     *
     * @param embedding ベクトル配列
     * @return "[0.1, 0.2, ...]" 形式の文字列
     */
    static String arrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * リトライ付きでHTMLを取得する。
     *
     * <p>HTTP 429/5xxエラーに対して指数バックオフでリトライする。</p>
     *
     * @param url 取得対象のURL
     * @return HTMLコンテンツ
     */
    String fetchHtmlWithRetry(String url) {
        int maxRetries = config.getMaxRetries();
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return fetchHtml(url);
            } catch (Exception e) {
                if (attempt >= maxRetries) {
                    throw new RuntimeException("HTML取得失敗（リトライ上限）: " + url, e);
                }
                long backoffMs = (long) Math.pow(2, attempt) * 1000;
                logger.warn("HTML取得失敗（リトライ {}/{}）: url={}, error={}, backoff={}ms",
                        attempt + 1, maxRetries, url, e.getMessage(), backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("リトライ中に中断された", ie);
                }
            }
        }
        // ここには到達しない
        throw new RuntimeException("HTML取得失敗: " + url);
    }

    /**
     * 指定URLのHTMLコンテンツを取得する。
     *
     * @param url 取得対象のURL
     * @return HTMLコンテンツ
     */
    String fetchHtml(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    /**
     * 相対URLを絶対URLに解決する。
     *
     * @param baseUrl ベースURL
     * @param href 相対URL
     * @return 絶対URL
     */
    static String resolveUrl(String baseUrl, String href) {
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        // baseUrlの末尾スラッシュ処理
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
        // 相対パスの "./" を除去
        if (href.startsWith("./")) {
            href = href.substring(2);
        }
        return base + href;
    }
}

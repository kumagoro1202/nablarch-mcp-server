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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fintan技術記事取込パイプライン。
 *
 * <p>Fintan（fintan.jp）のNablarch関連技術記事を取得し、
 * パース→チャンキング→Embedding→pgvector格納の一連のパイプラインを実行する。</p>
 *
 * <p>処理フロー:</p>
 * <ol>
 *   <li>記事URL一覧取得（Nablarchタグでフィルタ）</li>
 *   <li>各記事の本文取得（WebClient）</li>
 *   <li>コンテンツタイプ判定→パース（HTML/Markdown）</li>
 *   <li>チャンキング（ChunkingService）</li>
 *   <li>Embedding（Jina v4）</li>
 *   <li>document_chunksテーブルへ格納</li>
 * </ol>
 *
 * @see DocumentIngester
 * @see FintanIngestionConfig
 */
@Service
@ConditionalOnProperty(name = "nablarch.mcp.ingestion.enabled", havingValue = "true", matchIfMissing = false)
public class FintanIngester implements DocumentIngester {

    private static final Logger log = LoggerFactory.getLogger(FintanIngester.class);

    private static final String SOURCE_NAME = "fintan";
    private static final String SOURCE_TYPE = "documentation";

    private final MarkdownDocumentParser markdownParser;
    private final HtmlDocumentParser htmlParser;
    private final ChunkingService chunkingService;
    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository repository;
    private final WebClient webClient;
    private final FintanIngestionConfig config;

    /**
     * コンストラクタ。
     *
     * @param markdownParser Markdownパーサー
     * @param htmlParser HTMLパーサー
     * @param chunkingService チャンキングサービス
     * @param embeddingClient Embeddingクライアント（Jina v4）
     * @param repository ドキュメントチャンクリポジトリ
     * @param webClient HTTPクライアント
     * @param config Fintan取込設定
     */
    public FintanIngester(
            MarkdownDocumentParser markdownParser,
            HtmlDocumentParser htmlParser,
            ChunkingService chunkingService,
            @org.springframework.beans.factory.annotation.Qualifier("document") EmbeddingClient embeddingClient,
            DocumentChunkRepository repository,
            WebClient webClient,
            FintanIngestionConfig config) {
        this.markdownParser = markdownParser;
        this.htmlParser = htmlParser;
        this.chunkingService = chunkingService;
        this.embeddingClient = embeddingClient;
        this.repository = repository;
        this.webClient = webClient;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Nablarchタグ付きの全Fintan記事を取得して取り込む。</p>
     */
    @Override
    public IngestionResult ingestAll() {
        if (!config.isEnabled()) {
            log.info("Fintan取込パイプラインは無効です");
            return new IngestionResult(0, 0, 0, List.of());
        }

        log.info("Fintan全記事取込を開始します (tags: {})", config.getSearchTags());

        List<String> articleUrls = fetchArticleUrls();
        return processArticles(articleUrls);
    }

    /**
     * {@inheritDoc}
     *
     * <p>指定時刻以降に更新されたFintan記事のみを取り込む。
     * 現時点ではFintan APIが更新日時フィルタを直接サポートしていないため、
     * 全記事URLを取得した後、既存チャンクの有無で増分判定を行う。</p>
     */
    @Override
    public IngestionResult ingestIncremental(Instant since) {
        if (!config.isEnabled()) {
            log.info("Fintan取込パイプラインは無効です");
            return new IngestionResult(0, 0, 0, List.of());
        }

        log.info("Fintan増分取込を開始します (since: {}, tags: {})", since, config.getSearchTags());

        List<String> articleUrls = fetchArticleUrls();

        // 既存チャンクのURLを取得し、新規記事のみフィルタリング
        List<DocumentChunk> existingChunks = repository.findBySource(SOURCE_NAME);
        var existingUrls = existingChunks.stream()
                .map(DocumentChunk::getUrl)
                .collect(Collectors.toSet());

        List<String> newUrls = articleUrls.stream()
                .filter(url -> !existingUrls.contains(url))
                .toList();

        log.info("増分取込: 全{}件中、新規{}件を取込", articleUrls.size(), newUrls.size());

        return processArticles(newUrls);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    /**
     * Fintan記事のURL一覧を取得する。
     *
     * <p>Nablarchタグ付き記事のURLリストを返す。
     * Fintan APIまたはHTMLスクレイピングで取得する。</p>
     *
     * @return 記事URLのリスト
     */
    List<String> fetchArticleUrls() {
        String searchUrl = buildSearchUrl();
        log.debug("Fintan記事一覧取得: {}", searchUrl);

        try {
            String html = webClient.get()
                    .uri(searchUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (html == null || html.isBlank()) {
                log.warn("Fintan記事一覧が空です: {}", searchUrl);
                return List.of();
            }

            return extractArticleUrls(html);
        } catch (Exception e) {
            log.error("Fintan記事一覧の取得に失敗: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 検索URLを構築する。
     *
     * @return タグフィルタ付きの検索URL
     */
    private String buildSearchUrl() {
        String baseUrl = config.getBaseUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String tags = String.join(",", config.getSearchTags());
        return baseUrl + "?tag=" + tags;
    }

    /**
     * HTML一覧ページから記事URLを抽出する。
     *
     * <p>Fintanの記事一覧HTMLをパースし、個別記事のURLを抽出する。
     * aタグのhref属性から記事URLパターンにマッチするものを返す。</p>
     *
     * @param html 一覧ページのHTML
     * @return 記事URLリスト
     */
    List<String> extractArticleUrls(String html) {
        List<String> urls = new ArrayList<>();
        // Fintanの記事URLパターン: /page/数字/ または /blog/...
        var pattern = java.util.regex.Pattern.compile(
                "href=\"(" + java.util.regex.Pattern.quote(config.getBaseUrl())
                        + "(?:page/\\d+/?|blog/[^\"]+))\"");
        var matcher = pattern.matcher(html);
        while (matcher.find()) {
            String url = matcher.group(1);
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        log.debug("記事URL抽出: {}件", urls.size());
        return urls;
    }

    /**
     * 記事リストを処理する。
     *
     * <p>各記事について取得→パース→チャンキング→Embedding→格納を実行する。
     * 個別記事の障害は隔離し、他の記事の処理を継続する。</p>
     *
     * @param articleUrls 記事URLリスト
     * @return 取込結果
     */
    private IngestionResult processArticles(List<String> articleUrls) {
        int processed = 0;
        int success = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        for (String url : articleUrls) {
            processed++;
            try {
                processArticle(url);
                success++;
                log.info("記事取込完了 ({}/{}): {}", processed, articleUrls.size(), url);
            } catch (Exception e) {
                errors++;
                String errorMsg = String.format("記事取込失敗 [%s]: %s", url, e.getMessage());
                errorMessages.add(errorMsg);
                log.warn(errorMsg, e);
            }

            // robots.txt準拠のディレイ
            if (processed < articleUrls.size()) {
                sleep(config.getDelayMs());
            }
        }

        log.info("Fintan取込完了: 処理={}, 成功={}, エラー={}", processed, success, errors);
        return new IngestionResult(processed, success, errors, errorMessages);
    }

    /**
     * 単一記事を処理する。
     *
     * <p>リトライ付きで記事を取得し、パース→チャンキング→Embedding→格納を実行する。</p>
     *
     * @param url 記事URL
     * @throws RuntimeException リトライ上限を超えて失敗した場合
     */
    void processArticle(String url) {
        String content = fetchWithRetry(url);

        // コンテンツタイプ判定とパース
        List<ParsedDocument> parsedDocs = parseContent(content, url);

        // チャンキング
        List<DocumentChunkDto> allChunks = new ArrayList<>();
        for (ParsedDocument doc : parsedDocs) {
            allChunks.addAll(chunkingService.chunk(doc));
        }

        if (allChunks.isEmpty()) {
            log.debug("チャンク生成なし: {}", url);
            return;
        }

        // Embeddingとバッチ格納
        embedAndStore(allChunks, url);
    }

    /**
     * コンテンツタイプを判定し、適切なパーサーで解析する。
     *
     * <p>HTMLタグを含む場合はHtmlDocumentParser、
     * そうでない場合はMarkdownDocumentParserを使用する。</p>
     *
     * @param content 記事コンテンツ
     * @param url 記事URL
     * @return パース結果
     */
    List<ParsedDocument> parseContent(String content, String url) {
        if (isHtml(content)) {
            log.debug("HTMLとして解析: {}", url);
            return htmlParser.parse(content, url);
        } else {
            log.debug("Markdownとして解析: {}", url);
            return markdownParser.parse(content, url);
        }
    }

    /**
     * コンテンツがHTMLかどうかを判定する。
     *
     * @param content コンテンツ文字列
     * @return HTMLの場合true
     */
    boolean isHtml(String content) {
        if (content == null) {
            return false;
        }
        String trimmed = content.trim().toLowerCase();
        return trimmed.startsWith("<!doctype") || trimmed.startsWith("<html")
                || trimmed.contains("<head>") || trimmed.contains("<body>");
    }

    /**
     * チャンクのEmbeddingを生成し、DBに格納する。
     *
     * <p>バッチサイズに応じてEmbeddingを分割生成し、
     * 各チャンクをDocumentChunkエンティティとして保存した後、
     * Embeddingベクトルをネイティブクエリで更新する。</p>
     *
     * @param chunks チャンクDTOリスト
     * @param sourceUrl 元記事のURL
     */
    private void embedAndStore(List<DocumentChunkDto> chunks, String sourceUrl) {
        int batchSize = config.getBatchSize();

        for (int i = 0; i < chunks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunks.size());
            List<DocumentChunkDto> batch = chunks.subList(i, end);

            // バッチEmbedding生成
            List<String> contents = batch.stream()
                    .map(DocumentChunkDto::content)
                    .toList();
            List<float[]> embeddings = embeddingClient.embedBatch(contents);

            // 格納
            for (int j = 0; j < batch.size(); j++) {
                DocumentChunkDto chunkDto = batch.get(j);
                float[] embedding = embeddings.get(j);

                DocumentChunk entity = toEntity(chunkDto, sourceUrl);
                DocumentChunk saved = repository.save(entity);

                // Embeddingベクトルをネイティブクエリで更新
                String vectorStr = toVectorString(embedding);
                repository.updateEmbedding(saved.getId(), vectorStr);
            }
        }

        log.debug("チャンク格納完了: {}件 (URL: {})", chunks.size(), sourceUrl);
    }

    /**
     * DocumentChunkDtoからDocumentChunkエンティティを生成する。
     *
     * @param dto チャンクDTO
     * @param sourceUrl 元記事URL
     * @return DocumentChunkエンティティ
     */
    private DocumentChunk toEntity(DocumentChunkDto dto, String sourceUrl) {
        DocumentChunk entity = new DocumentChunk();
        entity.setContent(dto.content());
        entity.setSource(SOURCE_NAME);
        entity.setSourceType(SOURCE_TYPE);
        entity.setUrl(sourceUrl);
        entity.setLanguage(dto.metadata().getOrDefault("language", "ja"));

        Map<String, String> meta = dto.metadata();
        if (meta.containsKey("module")) {
            entity.setModule(meta.get("module"));
        }
        if (meta.containsKey("app_type")) {
            entity.setAppType(meta.get("app_type"));
        }
        if (meta.containsKey("fqcn")) {
            entity.setFqcn(meta.get("fqcn"));
        }

        return entity;
    }

    /**
     * float配列をpgvectorのベクトル文字列表現に変換する。
     *
     * @param embedding Embeddingベクトル
     * @return "[0.1,0.2,...]" 形式の文字列
     */
    private String toVectorString(float[] embedding) {
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
     * リトライ付きでURLのコンテンツを取得する。
     *
     * @param url 取得対象URL
     * @return コンテンツ文字列
     * @throws RuntimeException リトライ上限を超えて失敗した場合
     */
    String fetchWithRetry(String url) {
        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String content = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (content != null && !content.isBlank()) {
                    return content;
                }
                throw new RuntimeException("空のコンテンツが返されました: " + url);
            } catch (Exception e) {
                lastException = e;
                log.warn("記事取得リトライ ({}/{}): {} - {}", attempt, maxRetries, url, e.getMessage());
                if (attempt < maxRetries) {
                    sleep(config.getDelayMs() * attempt); // 指数的バックオフ
                }
            }
        }

        throw new RuntimeException(
                "記事取得に失敗（リトライ上限超過）: " + url, lastException);
    }

    /**
     * 指定ミリ秒スリープする。
     *
     * @param millis スリープ時間（ミリ秒）
     */
    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("スリープが中断されました");
        }
    }
}

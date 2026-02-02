package com.tis.nablarch.mcp.rag.rerank;

import com.tis.nablarch.mcp.rag.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Cross-Encoderリランキング実装。
 *
 * <p>Jina Reranker v2 (jina-reranker-v2-base-multilingual) を HTTP API で呼び出し、
 * ハイブリッド検索結果の候補をクエリとの関連度で再順位付けする。</p>
 *
 * <p>Top-50候補から → Top-K (5-10) に絞り込む。
 * API呼び出しが失敗した場合は、元のスコア順をそのまま返却する（degraded mode）。</p>
 *
 * @see Reranker
 * @see RerankProperties
 */
@Service
@EnableConfigurationProperties(RerankProperties.class)
public class CrossEncoderReranker implements Reranker {

    private static final Logger log = LoggerFactory.getLogger(CrossEncoderReranker.class);

    private final WebClient webClient;
    private final RerankProperties properties;

    /**
     * コンストラクタ。
     *
     * @param webClientBuilder WebClientビルダー
     * @param properties リランキング設定
     */
    public CrossEncoderReranker(
            WebClient.Builder webClientBuilder,
            RerankProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getJina().getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getJina().getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (topK < 1) {
            topK = properties.getJina().getTopK();
        }

        try {
            return doRerank(query, candidates, topK);
        } catch (Exception e) {
            log.warn("リランキングAPI呼び出し失敗。元のスコア順で返却（degraded mode）: {}", e.getMessage());
            return fallback(candidates, topK);
        }
    }

    /**
     * Jina Reranker APIを呼び出してリランキングを実行する。
     *
     * @param query クエリ
     * @param candidates 候補リスト
     * @param topK 返却件数
     * @return リランキング済み結果
     */
    private List<SearchResult> doRerank(String query, List<SearchResult> candidates, int topK) {
        // 候補のcontentをドキュメントリストに変換
        List<String> documents = candidates.stream()
                .map(SearchResult::content)
                .toList();

        // APIリクエスト構築
        int effectiveTopN = Math.min(topK, candidates.size());
        RerankRequest request = new RerankRequest(
                properties.getJina().getModel(),
                query,
                documents,
                effectiveTopN,
                false
        );

        // Jina Reranker API呼び出し
        Duration timeout = Duration.ofMillis(properties.getJina().getTimeoutMs());
        RerankResponse response = webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RerankResponse.class)
                .block(timeout);

        if (response == null || response.results() == null || response.results().isEmpty()) {
            log.warn("リランキングAPIレスポンスが空。元のスコア順で返却。");
            return fallback(candidates, topK);
        }

        // レスポンスからSearchResultを再構築（スコアをrerankerスコアで置換）
        return response.results().stream()
                .filter(r -> r.index() >= 0 && r.index() < candidates.size())
                .sorted(Comparator.comparingDouble(RerankResponse.Result::relevanceScore).reversed())
                .limit(topK)
                .map(r -> {
                    SearchResult original = candidates.get(r.index());
                    return new SearchResult(
                            original.id(),
                            original.content(),
                            r.relevanceScore(),
                            original.metadata(),
                            original.sourceUrl()
                    );
                })
                .toList();
    }

    /**
     * フォールバック: 元のスコア順で上位topK件を返す。
     *
     * @param candidates 候補リスト
     * @param topK 返却件数
     * @return 元のスコア降順でtopK件
     */
    private List<SearchResult> fallback(List<SearchResult> candidates, int topK) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK)
                .toList();
    }
}

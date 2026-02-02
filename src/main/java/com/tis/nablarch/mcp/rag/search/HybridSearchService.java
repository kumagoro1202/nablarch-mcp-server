package com.tis.nablarch.mcp.rag.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ハイブリッド検索サービス。
 *
 * <p>BM25キーワード検索とベクトルセマンティック検索を
 * Reciprocal Rank Fusion (RRF) で統合し、
 * 単一手法を上回る検索精度を実現する。</p>
 *
 * <p>検索モード:</p>
 * <ul>
 *   <li>{@link SearchMode#HYBRID} — BM25 + Vector + RRF統合（デフォルト）</li>
 *   <li>{@link SearchMode#KEYWORD} — BM25キーワード検索のみ</li>
 *   <li>{@link SearchMode#VECTOR} — ベクトルセマンティック検索のみ</li>
 * </ul>
 *
 * <p>グレースフルデグレード: 一方の検索が失敗した場合、
 * もう一方の結果のみで応答する。両方失敗時は空リストを返す。</p>
 *
 * @see BM25SearchService
 * @see VectorSearchService
 * @see SearchMode
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    /**
     * RRFスムージングパラメータ。原論文 (Cormack et al., 2009) の推奨値。
     */
    static final int DEFAULT_RRF_K = 60;

    /**
     * RRF統合前の候補取得件数。各検索エンジンからこの件数を取得し、
     * RRFで統合後にtopKに絞り込む。
     */
    static final int CANDIDATE_K = 50;

    /**
     * ベクトル検索のタイムアウト（秒）。
     * Embedding API呼び出しを含むため、BM25より長めに設定。
     */
    static final int VECTOR_TIMEOUT_SECONDS = 10;

    private final BM25SearchService bm25SearchService;
    private final VectorSearchService vectorSearchService;

    /**
     * コンストラクタ。
     *
     * @param bm25SearchService BM25キーワード検索サービス
     * @param vectorSearchService ベクトル類似度検索サービス
     */
    public HybridSearchService(
            BM25SearchService bm25SearchService,
            VectorSearchService vectorSearchService) {
        this.bm25SearchService = bm25SearchService;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * ハイブリッド検索を実行する。
     *
     * <p>指定された検索モードに応じて、BM25検索・ベクトル検索・
     * またはその両方をRRFで統合した結果を返す。</p>
     *
     * @param query 検索クエリ（自然言語テキスト）
     * @param filters メタデータフィルタ条件（nullの場合フィルタなし）
     * @param topK 返却する結果数（1以上）
     * @param mode 検索モード（nullの場合HYBRID）
     * @return 検索結果リスト（スコア降順）
     * @throws IllegalArgumentException queryがnullまたは空白、topKが1未満の場合
     */
    public List<SearchResult> search(
            String query, SearchFilters filters, int topK, SearchMode mode) {

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("検索クエリはnullまたは空白であってはならない");
        }
        if (topK < 1) {
            throw new IllegalArgumentException("topKは1以上でなければならない");
        }

        SearchMode effectiveMode = (mode != null) ? mode : SearchMode.HYBRID;
        log.debug("ハイブリッド検索実行: query='{}', mode={}, topK={}", query, effectiveMode, topK);

        return switch (effectiveMode) {
            case KEYWORD -> bm25SearchService.search(query, filters, topK);
            case VECTOR -> vectorSearchService.search(query, filters, topK);
            case HYBRID -> executeHybridSearch(query, filters, topK);
        };
    }

    /**
     * HYBRID モードの検索を実行する。
     *
     * <p>BM25検索とベクトル検索を{@link CompletableFuture}で並列実行し、
     * RRFで統合する。一方が失敗した場合はもう一方の結果のみで応答する。</p>
     *
     * @param query 検索クエリ
     * @param filters フィルタ条件
     * @param topK 返却する結果数
     * @return RRF統合済みの検索結果リスト
     */
    private List<SearchResult> executeHybridSearch(
            String query, SearchFilters filters, int topK) {

        CompletableFuture<List<SearchResult>> bm25Future = CompletableFuture
                .supplyAsync(() -> bm25SearchService.search(query, filters, CANDIDATE_K))
                .exceptionally(ex -> {
                    log.warn("BM25検索が失敗。ベクトル検索のみで応答: {}", ex.getMessage());
                    return Collections.emptyList();
                });

        CompletableFuture<List<SearchResult>> vectorFuture = CompletableFuture
                .supplyAsync(() -> vectorSearchService.search(query, filters, CANDIDATE_K))
                .orTimeout(VECTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.warn("ベクトル検索が失敗。BM25検索のみで応答: {}", ex.getMessage());
                    return Collections.emptyList();
                });

        List<SearchResult> bm25Results = bm25Future.join();
        List<SearchResult> vectorResults = vectorFuture.join();

        if (bm25Results.isEmpty() && vectorResults.isEmpty()) {
            log.info("BM25・ベクトル検索ともに結果なし: query='{}'", query);
            return Collections.emptyList();
        }

        // 一方のみの結果がある場合はRRFせず直接返す
        if (bm25Results.isEmpty()) {
            return vectorResults.stream().limit(topK).toList();
        }
        if (vectorResults.isEmpty()) {
            return bm25Results.stream().limit(topK).toList();
        }

        return rrfMerge(bm25Results, vectorResults, topK, DEFAULT_RRF_K);
    }

    /**
     * Reciprocal Rank Fusion (RRF) で2つの検索結果を統合する。
     *
     * <p>RRFスコア: {@code score(d) = Σ 1 / (k + rank_i(d))}</p>
     *
     * @param bm25Results BM25検索結果（スコア降順）
     * @param vectorResults ベクトル検索結果（スコア降順）
     * @param topK 返却する結果数
     * @param k RRFスムージングパラメータ
     * @return RRF統合済みの検索結果リスト
     */
    List<SearchResult> rrfMerge(
            List<SearchResult> bm25Results,
            List<SearchResult> vectorResults,
            int topK, int k) {

        // id → RRFスコア
        Map<String, Double> rrfScores = new HashMap<>();
        // id → SearchResult（最初に見つかったものを保持）
        Map<String, SearchResult> resultMap = new LinkedHashMap<>();

        // BM25結果のRRFスコアを計算
        for (int rank = 0; rank < bm25Results.size(); rank++) {
            SearchResult result = bm25Results.get(rank);
            double rrfScore = 1.0 / (k + rank + 1); // rank は1始まり
            rrfScores.merge(result.id(), rrfScore, Double::sum);
            resultMap.putIfAbsent(result.id(), result);
        }

        // ベクトル結果のRRFスコアを加算
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            SearchResult result = vectorResults.get(rank);
            double rrfScore = 1.0 / (k + rank + 1);
            rrfScores.merge(result.id(), rrfScore, Double::sum);
            resultMap.putIfAbsent(result.id(), result);
        }

        // RRFスコアでソートし、topK件返却
        return rrfScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> {
                    SearchResult original = resultMap.get(entry.getKey());
                    // スコアをRRFスコアで置き換えた新しいSearchResultを返す
                    return new SearchResult(
                            original.id(),
                            original.content(),
                            entry.getValue(),
                            original.metadata(),
                            original.sourceUrl()
                    );
                })
                .toList();
    }
}

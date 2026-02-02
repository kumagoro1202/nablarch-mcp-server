package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.rag.rerank.Reranker;
import com.tis.nablarch.mcp.rag.search.HybridSearchService;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tis.nablarch.mcp.rag.query.QueryAnalyzer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCPツール: semantic_search。
 *
 * <p>Nablarch知識ベースに対するセマンティック検索ツール。
 * ハイブリッド検索（BM25+ベクトル）とCross-Encoderリランキングを組み合わせた
 * RAGパイプラインで高精度な検索結果を提供する。</p>
 *
 * <p>Phase 1の{@code search_api}ツールの上位互換として機能する。</p>
 *
 * <p>パイプライン:
 * クエリ → [QueryAnalyzer(optional)] → HybridSearch → Rerank → 結果整形</p>
 *
 * @see HybridSearchService
 * @see Reranker
 */
@Service
public class SemanticSearchTool {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchTool.class);

    /**
     * デフォルトのtopK値。
     */
    static final int DEFAULT_TOP_K = 5;

    /**
     * ハイブリッド検索からリランキングに渡す候補数。
     */
    static final int CANDIDATE_K = 50;

    private final HybridSearchService hybridSearchService;
    private final Reranker reranker;

    /**
     * QueryAnalyzer（足軽8号が並行実装中。未注入時はnull）。
     *
     * <p>注入された場合はクエリ拡張に使用する。
     * 未注入の場合は元のクエリをそのまま使用する。</p>
     */
    @Autowired(required = false)
    private QueryAnalyzer queryAnalyzer;

    /**
     * コンストラクタ。
     *
     * @param hybridSearchService ハイブリッド検索サービス
     * @param reranker リランカー
     */
    public SemanticSearchTool(
            HybridSearchService hybridSearchService,
            Reranker reranker) {
        this.hybridSearchService = hybridSearchService;
        this.reranker = reranker;
    }

    /**
     * Nablarch知識ベースに対するセマンティック検索を実行する。
     *
     * @param query 検索クエリ（自然言語またはキーワード、日本語・英語対応）
     * @param appType アプリケーション種別フィルタ（web, rest, batch, messaging等）
     * @param module モジュール名フィルタ（nablarch-fw-web等）
     * @param source データソースフィルタ（nablarch-document, github, fintan, javadoc）
     * @param sourceType コンテンツ種別フィルタ（documentation, code, javadoc, config, standard）
     * @param topK 返却する結果数（1-50、デフォルト5）
     * @param mode 検索モード（hybrid, vector, keyword、デフォルトhybrid）
     * @return 検索結果のMarkdownフォーマット文字列
     */
    @Tool(description = "Semantic search over the Nablarch knowledge base. "
            + "Uses hybrid search (BM25 + vector) with Cross-Encoder reranking "
            + "for high-accuracy results. Supports natural language queries in "
            + "Japanese and English. Use this for finding Nablarch APIs, patterns, "
            + "configurations, and troubleshooting information.")
    public String semanticSearch(
            @ToolParam(description = "Search query in natural language or keywords")
            String query,
            @ToolParam(description = "Optional app type filter: web, rest, batch, messaging")
            String appType,
            @ToolParam(description = "Optional module filter: e.g. nablarch-fw-web")
            String module,
            @ToolParam(description = "Optional source filter: nablarch-document, github, fintan, javadoc")
            String source,
            @ToolParam(description = "Optional content type filter: documentation, code, javadoc, config, standard")
            String sourceType,
            @ToolParam(description = "Number of results (1-50, default 5)")
            Integer topK,
            @ToolParam(description = "Search mode: hybrid (default), vector, keyword")
            String mode) {

        if (query == null || query.isBlank()) {
            return "検索クエリを指定してください。";
        }

        int effectiveTopK = (topK != null && topK >= 1 && topK <= 50) ? topK : DEFAULT_TOP_K;
        SearchMode effectiveMode = parseMode(mode);
        SearchFilters filters = new SearchFilters(
                nullIfBlank(appType),
                nullIfBlank(module),
                nullIfBlank(source),
                nullIfBlank(sourceType),
                null
        );

        try {
            return doSearch(query, filters, effectiveTopK, effectiveMode);
        } catch (Exception e) {
            log.error("semantic_search実行中にエラーが発生: {}", e.getMessage(), e);
            return "検索中にエラーが発生しました。search_apiツールをお試しください。";
        }
    }

    /**
     * 検索パイプラインを実行する。
     *
     * @param query クエリ
     * @param filters フィルタ
     * @param topK 返却件数
     * @param mode 検索モード
     * @return Markdown形式の検索結果
     */
    private String doSearch(
            String query, SearchFilters filters, int topK, SearchMode mode) {

        long startTime = System.currentTimeMillis();

        // QueryAnalyzerが注入されている場合はクエリ拡張を行う（将来対応）
        String effectiveQuery = query;
        // TODO: QueryAnalyzer統合（足軽8号のWBS 2.2.14完了後）

        // ハイブリッド検索で候補取得（リランキング用にCANDIDATE_K件取得）
        List<SearchResult> candidates = hybridSearchService.search(
                effectiveQuery, filters, CANDIDATE_K, mode);

        // リランキング
        List<SearchResult> results;
        if (candidates.isEmpty()) {
            results = candidates;
        } else {
            results = reranker.rerank(effectiveQuery, candidates, topK);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return formatResults(query, mode, results, elapsed);
    }

    /**
     * 検索結果をMarkdown形式に整形する。
     *
     * @param query 元のクエリ
     * @param mode 検索モード
     * @param results 検索結果
     * @param elapsedMs 検索時間（ミリ秒）
     * @return Markdown形式の文字列
     */
    String formatResults(
            String query, SearchMode mode,
            List<SearchResult> results, long elapsedMs) {

        if (results.isEmpty()) {
            return "検索結果なし: \"" + query + "\" (モード: " + mode.name().toLowerCase() + ")\n\n"
                    + "ヒント:\n"
                    + "- フィルタ条件を緩和してください（app_type, module等を外す）\n"
                    + "- 別のキーワードや表現を試してください\n"
                    + "- mode=\"keyword\" でキーワード検索を試してください\n"
                    + "- search_api ツールで静的知識ベースを検索してください";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 検索結果: \"").append(query).append("\"\n");
        sb.append("モード: ").append(mode.name().toLowerCase());
        sb.append(" | 結果数: ").append(results.size()).append("件");
        sb.append(" | 検索時間: ").append(elapsedMs).append("ms\n\n---\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("### 結果 ").append(i + 1);
            sb.append(" (スコア: ").append(String.format("%.3f", r.score())).append(")\n");

            Map<String, String> meta = r.metadata();
            if (meta != null && !meta.isEmpty()) {
                sb.append("**ソース**: ");
                sb.append(meta.getOrDefault("source", "不明"));
                String at = meta.get("app_type");
                if (at != null && !at.isEmpty()) {
                    sb.append(" | ").append(at);
                }
                String mod = meta.get("module");
                if (mod != null && !mod.isEmpty()) {
                    sb.append(" | ").append(mod);
                }
                sb.append("\n");
            }

            if (r.sourceUrl() != null && !r.sourceUrl().isBlank()) {
                sb.append("**URL**: ").append(r.sourceUrl()).append("\n");
            }
            sb.append("\n").append(r.content()).append("\n\n---\n\n");
        }

        return sb.toString();
    }

    /**
     * モード文字列をSearchModeに変換する。
     *
     * @param mode モード文字列（hybrid, vector, keyword）
     * @return SearchMode
     */
    private SearchMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return SearchMode.HYBRID;
        }
        return switch (mode.toLowerCase()) {
            case "vector" -> SearchMode.VECTOR;
            case "keyword" -> SearchMode.KEYWORD;
            default -> SearchMode.HYBRID;
        };
    }

    private String nullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }
}

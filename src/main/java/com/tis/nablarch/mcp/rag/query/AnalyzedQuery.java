package com.tis.nablarch.mcp.rag.query;

import com.tis.nablarch.mcp.rag.search.SearchFilters;

import java.util.List;
import java.util.Objects;

/**
 * クエリ解析結果を表すレコード。
 *
 * <p>QueryAnalyzerがユーザークエリを解析した結果を保持する。
 * 元のクエリ、同義語展開後のクエリ、検出言語、抽出エンティティ、
 * 推定フィルタ条件を含む。</p>
 *
 * @param originalQuery 元のユーザークエリ
 * @param expandedQuery 同義語展開後のクエリ（BM25検索で使用）
 * @param language 検出された言語
 * @param entities 抽出されたNablarchドメインエンティティ
 * @param suggestedFilters エンティティから推定した検索フィルタ
 */
public record AnalyzedQuery(
        String originalQuery,
        String expandedQuery,
        QueryLanguage language,
        List<String> entities,
        SearchFilters suggestedFilters
) {

    /**
     * AnalyzedQueryを生成する。
     *
     * @param originalQuery 元のクエリ（必須）
     * @param expandedQuery 展開後のクエリ（必須）
     * @param language 検出言語（必須）
     * @param entities 抽出エンティティ
     * @param suggestedFilters 推定フィルタ
     */
    public AnalyzedQuery {
        Objects.requireNonNull(originalQuery, "originalQueryはnullであってはならない");
        Objects.requireNonNull(expandedQuery, "expandedQueryはnullであってはならない");
        Objects.requireNonNull(language, "languageはnullであってはならない");
        if (entities == null) {
            entities = List.of();
        }
        if (suggestedFilters == null) {
            suggestedFilters = SearchFilters.NONE;
        }
    }
}

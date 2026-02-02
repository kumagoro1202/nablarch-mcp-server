package com.tis.nablarch.mcp.rag.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * メタデータフィルタリングサービス。
 *
 * <p>ハイブリッド検索結果に対するポストフィルタリングとファセット集計を提供する。
 * SQL WHERE句で適用済みの基本フィルタに加え、Java側でのバージョン前方一致・
 * FQCN前方一致・日付範囲フィルタなど、SQLで表現しにくい条件をサポートする。</p>
 */
@Service
public class MetadataFilteringService {

    private static final Logger log = LoggerFactory.getLogger(MetadataFilteringService.class);

    /** ファセット集計対象のメタデータキー一覧 */
    private static final List<String> FACET_KEYS = List.of(
            "source", "source_type", "app_type", "module", "language"
    );

    /**
     * 検索結果に対してメタデータフィルタを適用する。
     *
     * <p>各フィルタ条件がnullの場合、その条件は無視される。
     * 全条件がnullの場合、入力リストがそのまま返却される。</p>
     *
     * @param results 検索結果リスト（nullの場合は空リストを返却）
     * @param filters 拡張フィルタ条件（nullの場合はフィルタなし）
     * @return フィルタ適用後の検索結果リスト
     */
    public List<SearchResult> filter(List<SearchResult> results, ExtendedSearchFilters filters) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        if (filters == null || !filters.hasAnyFilter()) {
            return results;
        }

        List<SearchResult> filtered = results.stream()
                .filter(r -> matchesAllConditions(r, filters))
                .toList();

        log.debug("メタデータフィルタ適用: {} → {} 件", results.size(), filtered.size());
        return filtered;
    }

    /**
     * 検索結果セットからメタデータの分布（ファセット）を集計する。
     *
     * <p>ファセット対象: source, source_type, app_type, module, language。
     * 各キーに対して、値ごとの出現回数をカウントする。</p>
     *
     * @param results 検索結果リスト（nullの場合は空マップを返却）
     * @return ファセット集計結果。キー=メタデータフィールド名、値=値ごとの件数マップ
     */
    public Map<String, Map<String, Long>> computeFacets(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Long>> facets = new LinkedHashMap<>();
        for (String key : FACET_KEYS) {
            Map<String, Long> counts = results.stream()
                    .map(r -> getMetadataValue(r, key))
                    .filter(v -> v != null && !v.isEmpty())
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));
            if (!counts.isEmpty()) {
                facets.put(key, counts);
            }
        }

        log.debug("ファセット集計完了: {} カテゴリ", facets.size());
        return facets;
    }

    /**
     * 検索結果が全てのフィルタ条件にマッチするかを判定する。
     */
    private boolean matchesAllConditions(SearchResult result, ExtendedSearchFilters filters) {
        // baseFilters の完全一致チェック
        if (filters.baseFilters() != null) {
            SearchFilters base = filters.baseFilters();
            if (!matchesExact(result, "app_type", base.appType())) return false;
            if (!matchesExact(result, "module", base.module())) return false;
            if (!matchesExact(result, "source", base.source())) return false;
            if (!matchesExact(result, "source_type", base.sourceType())) return false;
            if (!matchesExact(result, "language", base.language())) return false;
        }

        // version 前方一致
        if (filters.version() != null) {
            String value = getMetadataValue(result, "nablarch_version");
            if (value == null || !value.startsWith(filters.version())) {
                return false;
            }
        }

        // fqcnPrefix 前方一致
        if (filters.fqcnPrefix() != null) {
            String value = getMetadataValue(result, "fqcn");
            if (value == null || !value.startsWith(filters.fqcnPrefix())) {
                return false;
            }
        }

        // dateRange: dateFrom（この時点以降）
        if (filters.dateFrom() != null) {
            Instant updatedAt = parseInstant(getMetadataValue(result, "updated_at"));
            if (updatedAt == null || updatedAt.isBefore(filters.dateFrom())) {
                return false;
            }
        }

        // dateRange: dateTo（この時点以前）
        if (filters.dateTo() != null) {
            Instant updatedAt = parseInstant(getMetadataValue(result, "updated_at"));
            if (updatedAt == null || updatedAt.isAfter(filters.dateTo())) {
                return false;
            }
        }

        return true;
    }

    /**
     * メタデータの値と期待値の完全一致を判定する。
     * 期待値がnullの場合は常にtrue（フィルタ条件なし）。
     */
    private boolean matchesExact(SearchResult result, String key, String expected) {
        if (expected == null) {
            return true;
        }
        String actual = getMetadataValue(result, key);
        return expected.equals(actual);
    }

    /**
     * 検索結果のメタデータから指定キーの値を取得する。
     * メタデータがnullの場合はnullを返す。
     */
    private String getMetadataValue(SearchResult result, String key) {
        if (result.metadata() == null) {
            return null;
        }
        return result.metadata().get(key);
    }

    /**
     * 文字列をInstantにパースする。パース失敗時はnullを返す。
     */
    private Instant parseInstant(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            log.trace("Instantパース失敗: {}", value);
            return null;
        }
    }
}

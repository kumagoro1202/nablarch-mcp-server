package com.tis.nablarch.mcp.rag.search;

import java.time.Instant;

/**
 * 拡張検索フィルタ条件を表すレコード。
 *
 * <p>基本の{@link SearchFilters}に加え、バージョン前方一致・FQCN前方一致・
 * 日付範囲フィルタをサポートする。SQL WHERE句で表現しにくい条件を
 * Java側でポストフィルタリングする際に使用する。</p>
 *
 * <p>各フィールドがnullの場合、その条件は適用されない（条件なし扱い）。</p>
 *
 * @param baseFilters 基本フィルタ条件（appType, module, source, sourceType, language）
 * @param version Nablarchバージョンの前方一致条件（例: "5"は"5u23"にマッチ）
 * @param fqcnPrefix 完全修飾クラス名の前方一致条件（例: "nablarch.fw.web"）
 * @param dateFrom 更新日時範囲の開始（この時点以降）
 * @param dateTo 更新日時範囲の終了（この時点以前）
 */
public record ExtendedSearchFilters(
        SearchFilters baseFilters,
        String version,
        String fqcnPrefix,
        Instant dateFrom,
        Instant dateTo
) {

    /**
     * フィルタ条件なしの定数。baseFiltersも含め全てnull。
     */
    public static final ExtendedSearchFilters NONE =
            new ExtendedSearchFilters(null, null, null, null, null);

    /**
     * いずれかの拡張フィルタ条件が指定されているかを返す。
     *
     * @return 拡張フィルタ条件（version, fqcnPrefix, dateFrom, dateTo）が
     *         1つ以上指定されている場合true
     */
    public boolean hasExtendedFilters() {
        return version != null || fqcnPrefix != null || dateFrom != null || dateTo != null;
    }

    /**
     * baseFiltersまたは拡張フィルタのいずれかが指定されているかを返す。
     *
     * @return いずれかのフィルタ条件が指定されている場合true
     */
    public boolean hasAnyFilter() {
        return hasExtendedFilters() || (baseFilters != null && baseFilters.hasAnyFilter());
    }
}

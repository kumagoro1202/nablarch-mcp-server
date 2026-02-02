package com.tis.nablarch.mcp.rag.search;

/**
 * 検索フィルタ条件を表すレコード。
 *
 * <p>各フィールドがnullの場合、そのフィルタ条件は適用されない。
 * メタデータのJSONBカラムに対してWHERE句を動的構築する際に使用する。</p>
 *
 * @param appType アプリケーション種別（web, rest, batch, messaging等）
 * @param module モジュール名（nablarch-fw-web等）
 * @param source データソース（nablarch-document, github, fintan, javadoc）
 * @param sourceType コンテンツ種別（documentation, code, javadoc, config, standard）
 * @param language 言語（ja, en）
 */
public record SearchFilters(
        String appType,
        String module,
        String source,
        String sourceType,
        String language
) {
    /**
     * フィルタ条件なしの定数。全てのフィールドがnull。
     */
    public static final SearchFilters NONE = new SearchFilters(null, null, null, null, null);

    /**
     * いずれかのフィルタ条件が指定されているかを返す。
     *
     * @return フィルタ条件が1つ以上指定されている場合true
     */
    public boolean hasAnyFilter() {
        return appType != null || module != null || source != null
                || sourceType != null || language != null;
    }
}

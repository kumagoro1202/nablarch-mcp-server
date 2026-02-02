package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SearchFilters} のユニットテスト。
 */
class SearchFiltersTest {

    @Test
    @DisplayName("NONE定数は全フィールドがnull")
    void noneHasAllNullFields() {
        SearchFilters none = SearchFilters.NONE;
        assertAll(
                () -> assertNull(none.appType()),
                () -> assertNull(none.module()),
                () -> assertNull(none.source()),
                () -> assertNull(none.sourceType()),
                () -> assertNull(none.language())
        );
    }

    @Test
    @DisplayName("NONE定数はhasAnyFilterがfalseを返す")
    void noneHasNoFilters() {
        assertFalse(SearchFilters.NONE.hasAnyFilter());
    }

    @Test
    @DisplayName("いずれかのフィールドが設定されるとhasAnyFilterがtrueを返す")
    void hasAnyFilterReturnsTrueWhenSet() {
        assertAll(
                () -> assertTrue(new SearchFilters("web", null, null, null, null).hasAnyFilter()),
                () -> assertTrue(new SearchFilters(null, "mod", null, null, null).hasAnyFilter()),
                () -> assertTrue(new SearchFilters(null, null, "src", null, null).hasAnyFilter()),
                () -> assertTrue(new SearchFilters(null, null, null, "type", null).hasAnyFilter()),
                () -> assertTrue(new SearchFilters(null, null, null, null, "ja").hasAnyFilter())
        );
    }
}

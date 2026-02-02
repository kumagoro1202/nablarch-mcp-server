package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ExtendedSearchFilters} のユニットテスト。
 */
class ExtendedSearchFiltersTest {

    @Test
    @DisplayName("正常系: 全フィールドが正しく保持される")
    void allFieldsPreserved() {
        SearchFilters base = new SearchFilters("web", "nablarch-fw-web", null, null, "ja");
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        ExtendedSearchFilters filters = new ExtendedSearchFilters(base, "6", "nablarch.fw", from, to);

        assertAll(
                () -> assertSame(base, filters.baseFilters()),
                () -> assertEquals("6", filters.version()),
                () -> assertEquals("nablarch.fw", filters.fqcnPrefix()),
                () -> assertEquals(from, filters.dateFrom()),
                () -> assertEquals(to, filters.dateTo())
        );
    }

    @Test
    @DisplayName("正常系: NONE定数は全フィールドがnull")
    void noneConstant() {
        ExtendedSearchFilters none = ExtendedSearchFilters.NONE;
        assertAll(
                () -> assertNull(none.baseFilters()),
                () -> assertNull(none.version()),
                () -> assertNull(none.fqcnPrefix()),
                () -> assertNull(none.dateFrom()),
                () -> assertNull(none.dateTo())
        );
    }

    @Test
    @DisplayName("正常系: hasExtendedFiltersはversion指定時にtrue")
    void hasExtendedFilters_version() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "6", null, null, null);
        assertTrue(filters.hasExtendedFilters());
    }

    @Test
    @DisplayName("正常系: hasExtendedFiltersはfqcnPrefix指定時にtrue")
    void hasExtendedFilters_fqcnPrefix() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(null, null, "nablarch.fw", null, null);
        assertTrue(filters.hasExtendedFilters());
    }

    @Test
    @DisplayName("正常系: hasExtendedFiltersは日付範囲指定時にtrue")
    void hasExtendedFilters_dateRange() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(
                null, null, null, Instant.now(), null);
        assertTrue(filters.hasExtendedFilters());
    }

    @Test
    @DisplayName("正常系: hasExtendedFiltersは拡張条件なしでfalse")
    void hasExtendedFilters_none() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(
                new SearchFilters("web", null, null, null, null),
                null, null, null, null);
        assertFalse(filters.hasExtendedFilters());
    }

    @Test
    @DisplayName("正常系: hasAnyFilterはbaseFiltersのみでもtrue")
    void hasAnyFilter_baseOnly() {
        SearchFilters base = new SearchFilters("web", null, null, null, null);
        ExtendedSearchFilters filters = new ExtendedSearchFilters(base, null, null, null, null);
        assertTrue(filters.hasAnyFilter());
    }

    @Test
    @DisplayName("正常系: hasAnyFilterは拡張条件のみでもtrue")
    void hasAnyFilter_extendedOnly() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(null, "6", null, null, null);
        assertTrue(filters.hasAnyFilter());
    }

    @Test
    @DisplayName("正常系: hasAnyFilterは全条件なしでfalse")
    void hasAnyFilter_none() {
        assertFalse(ExtendedSearchFilters.NONE.hasAnyFilter());
    }

    @Test
    @DisplayName("正常系: baseFiltersがNONEの場合hasAnyFilterはfalse")
    void hasAnyFilter_baseFiltersNone() {
        ExtendedSearchFilters filters = new ExtendedSearchFilters(
                SearchFilters.NONE, null, null, null, null);
        assertFalse(filters.hasAnyFilter());
    }
}

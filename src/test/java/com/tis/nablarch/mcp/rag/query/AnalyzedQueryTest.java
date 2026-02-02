package com.tis.nablarch.mcp.rag.query;

import com.tis.nablarch.mcp.rag.search.SearchFilters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AnalyzedQuery}のテスト。
 *
 * <p>レコードのフィールド検証、null安全性、コンパクトコンストラクタの動作を検証する。</p>
 */
class AnalyzedQueryTest {

    @Test
    void 全フィールドが正しく保持される() {
        SearchFilters filters = new SearchFilters("web", "nablarch-fw-web", null, null, null);
        List<String> entities = List.of("nablarch-fw-web", "ThreadContextHandler");

        AnalyzedQuery query = new AnalyzedQuery(
                "original query",
                "expanded query",
                QueryLanguage.JAPANESE,
                entities,
                filters
        );

        assertEquals("original query", query.originalQuery());
        assertEquals("expanded query", query.expandedQuery());
        assertEquals(QueryLanguage.JAPANESE, query.language());
        assertEquals(entities, query.entities());
        assertEquals(filters, query.suggestedFilters());
    }

    @Test
    void entitiesがnullの場合は空リストになる() {
        AnalyzedQuery query = new AnalyzedQuery(
                "query", "expanded", QueryLanguage.ENGLISH, null, SearchFilters.NONE);
        assertNotNull(query.entities());
        assertTrue(query.entities().isEmpty());
    }

    @Test
    void suggestedFiltersがnullの場合はNONEになる() {
        AnalyzedQuery query = new AnalyzedQuery(
                "query", "expanded", QueryLanguage.ENGLISH, List.of(), null);
        assertEquals(SearchFilters.NONE, query.suggestedFilters());
    }

    @Test
    void originalQueryがnullの場合は例外をスローする() {
        assertThrows(NullPointerException.class, () ->
                new AnalyzedQuery(null, "expanded", QueryLanguage.ENGLISH, List.of(), SearchFilters.NONE));
    }

    @Test
    void expandedQueryがnullの場合は例外をスローする() {
        assertThrows(NullPointerException.class, () ->
                new AnalyzedQuery("query", null, QueryLanguage.ENGLISH, List.of(), SearchFilters.NONE));
    }

    @Test
    void languageがnullの場合は例外をスローする() {
        assertThrows(NullPointerException.class, () ->
                new AnalyzedQuery("query", "expanded", null, List.of(), SearchFilters.NONE));
    }

    @Test
    void recordのequalityが正しく動作する() {
        AnalyzedQuery q1 = new AnalyzedQuery(
                "query", "expanded", QueryLanguage.JAPANESE, List.of("A"), SearchFilters.NONE);
        AnalyzedQuery q2 = new AnalyzedQuery(
                "query", "expanded", QueryLanguage.JAPANESE, List.of("A"), SearchFilters.NONE);
        assertEquals(q1, q2);
        assertEquals(q1.hashCode(), q2.hashCode());
    }

    @Test
    void toStringにフィールド情報が含まれる() {
        AnalyzedQuery query = new AnalyzedQuery(
                "test", "test expanded", QueryLanguage.MIXED, List.of(), SearchFilters.NONE);
        String str = query.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("MIXED"));
    }
}

package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SearchApiTool}.
 */
class SearchApiToolTest {

    private SearchApiTool searchApiTool;

    @BeforeEach
    void setUp() {
        searchApiTool = new SearchApiTool(new NablarchKnowledgeBase());
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void searchApi_withKeyword_returnsResults() {
        String result = searchApiTool.searchApi("UniversalDao", null);
        assertNotNull(result);
        assertTrue(result.contains("UniversalDao"));
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void searchApi_withCategoryFilter_returnsFilteredResults() {
        String result = searchApiTool.searchApi("handler", "web");
        assertNotNull(result);
    }
}

package com.tis.nablarch.mcp.resources;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApiSpecResource}.
 */
class ApiSpecResourceTest {

    private ApiSpecResource apiSpecResource;

    @BeforeEach
    void setUp() {
        apiSpecResource = new ApiSpecResource(new NablarchKnowledgeBase());
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void getApiSpec_withValidModuleAndClass_returnsSpec() {
        String spec = apiSpecResource.getApiSpec("fw", "ExecutionContext");
        assertNotNull(spec);
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void listModules_returnsNonEmptyList() {
        var modules = apiSpecResource.listModules();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
    }
}

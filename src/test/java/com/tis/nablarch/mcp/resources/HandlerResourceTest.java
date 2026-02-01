package com.tis.nablarch.mcp.resources;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HandlerResource}.
 */
class HandlerResourceTest {

    private HandlerResource handlerResource;

    @BeforeEach
    void setUp() {
        handlerResource = new HandlerResource(new NablarchKnowledgeBase());
    }

    @Test
    void listTypes_returnsSupportedTypes() {
        List<String> types = handlerResource.listTypes();
        assertNotNull(types);
        assertEquals(4, types.size());
        assertTrue(types.contains("web"));
        assertTrue(types.contains("rest"));
        assertTrue(types.contains("batch"));
        assertTrue(types.contains("messaging"));
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void getHandlerSpec_withWebType_returnsSpec() {
        String spec = handlerResource.getHandlerSpec("web");
        assertNotNull(spec);
    }

    @Test
    @Disabled("Phase 1 stub - implementation pending")
    void getHandlerQueueTemplate_withWebType_returnsXml() {
        String template = handlerResource.getHandlerQueueTemplate("web");
        assertNotNull(template);
        assertTrue(template.contains("handler"));
    }
}

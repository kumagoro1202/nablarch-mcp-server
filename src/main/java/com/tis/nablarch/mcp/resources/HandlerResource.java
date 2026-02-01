package com.tis.nablarch.mcp.resources;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.stereotype.Component;

/**
 * MCP Resource: nablarch://handler/{type}
 *
 * <p>Provides read-only access to Nablarch handler specifications.
 * Includes handler descriptions, ordering constraints, and configuration
 * examples for each application type (web, REST, batch, messaging).</p>
 */
@Component
public class HandlerResource {

    private final NablarchKnowledgeBase knowledgeBase;

    public HandlerResource(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Retrieve handler specifications for a given application type.
     *
     * @param type the application type (web, rest, batch, messaging)
     * @return handler specifications as Markdown text, including ordering constraints
     */
    public String getHandlerSpec(String type) {
        // TODO: Implement handler spec lookup from knowledge base
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }

    /**
     * Retrieve the standard handler queue template for a given application type.
     *
     * @param type the application type (web, rest, batch, messaging)
     * @return XML handler queue template
     */
    public String getHandlerQueueTemplate(String type) {
        // TODO: Implement handler queue template retrieval
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }

    /**
     * List available handler types.
     *
     * @return list of supported application types
     */
    public java.util.List<String> listTypes() {
        return java.util.List.of("web", "rest", "batch", "messaging");
    }
}

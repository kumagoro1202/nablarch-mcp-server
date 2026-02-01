package com.tis.nablarch.mcp.resources;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.stereotype.Component;

/**
 * MCP Resource: nablarch://api/{module}/{class}
 *
 * <p>Provides read-only access to Nablarch API specifications,
 * including Javadoc references, class descriptions, and method signatures.
 * AI assistants use this resource to understand Nablarch APIs when generating code.</p>
 */
@Component
public class ApiSpecResource {

    private final NablarchKnowledgeBase knowledgeBase;

    public ApiSpecResource(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Retrieve API specification for a given module and class.
     *
     * @param module the Nablarch module name (e.g., "core", "fw", "common")
     * @param className the fully qualified or simple class name
     * @return API specification as Markdown text
     */
    public String getApiSpec(String module, String className) {
        // TODO: Implement API spec lookup from knowledge base
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }

    /**
     * List available API modules.
     *
     * @return list of available module names
     */
    public java.util.List<String> listModules() {
        // TODO: Implement module listing
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }
}

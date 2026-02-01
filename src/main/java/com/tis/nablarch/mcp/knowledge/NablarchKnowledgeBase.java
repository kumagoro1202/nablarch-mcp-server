package com.tis.nablarch.mcp.knowledge;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Nablarch Knowledge Base.
 *
 * <p>Central repository of Nablarch framework knowledge used by
 * MCP tools and resources. Loads and indexes handler catalogs,
 * API patterns, and design guidelines from YAML data files.</p>
 *
 * <p>In Phase 1, knowledge is loaded from static YAML files in
 * {@code classpath:knowledge/}. Future phases may integrate
 * dynamic document crawling and full-text search indexing.</p>
 */
@Component
public class NablarchKnowledgeBase {

    /**
     * Search the knowledge base for entries matching the given keyword.
     *
     * @param keyword  the search keyword
     * @param category optional category filter (null for all categories)
     * @return list of matching entries as formatted strings
     */
    public List<String> search(String keyword, String category) {
        // TODO: Implement search over loaded YAML data
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }

    /**
     * Retrieve handler catalog entries by application type.
     *
     * @param applicationType the application type (web, rest, batch, messaging)
     * @return handler catalog as a structured map
     */
    public Map<String, Object> getHandlerCatalog(String applicationType) {
        // TODO: Implement handler catalog lookup
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }

    /**
     * Retrieve API pattern information by module and class name.
     *
     * @param module    the Nablarch module name
     * @param className the class name
     * @return API specification as formatted text
     */
    public String getApiSpec(String module, String className) {
        // TODO: Implement API spec retrieval
        throw new UnsupportedOperationException("Not yet implemented - Phase 1 stub");
    }
}

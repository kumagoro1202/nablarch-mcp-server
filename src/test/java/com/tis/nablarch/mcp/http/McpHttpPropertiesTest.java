package com.tis.nablarch.mcp.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link McpHttpProperties} のユニットテスト。
 */
class McpHttpPropertiesTest {

    @Test
    @DisplayName("デフォルト値が正しく設定される")
    void defaultValues() {
        McpHttpProperties props = new McpHttpProperties();
        assertFalse(props.isEnabled());
        assertEquals("/mcp", props.getEndpoint());
        assertEquals(Duration.ofMinutes(30), props.getSession().getTimeout());
        assertEquals(100, props.getSession().getMaxSessions());
        assertTrue(props.getCors().isAllowCredentials());
        assertEquals(3600, props.getCors().getMaxAge());
    }

    @Test
    @DisplayName("setter/getterが正しく動作する")
    void setterGetter() {
        McpHttpProperties props = new McpHttpProperties();
        props.setEnabled(true);
        props.setEndpoint("/custom");
        assertTrue(props.isEnabled());
        assertEquals("/custom", props.getEndpoint());
    }

    @Test
    @DisplayName("SessionConfigのデフォルト値")
    void sessionConfigDefaults() {
        McpHttpProperties.SessionConfig session = new McpHttpProperties.SessionConfig();
        assertEquals(Duration.ofMinutes(30), session.getTimeout());
        assertEquals(100, session.getMaxSessions());
        assertEquals(Duration.ofMinutes(5), session.getCleanupInterval());
    }

    @Test
    @DisplayName("CorsConfigのデフォルト値")
    void corsConfigDefaults() {
        McpHttpProperties.CorsConfig cors = new McpHttpProperties.CorsConfig();
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(cors.getAllowedHeaders().contains("Mcp-Session-Id"));
        assertTrue(cors.getExposedHeaders().contains("Mcp-Session-Id"));
    }

    @Test
    @DisplayName("OriginValidationConfigのデフォルト値")
    void originValidationDefaults() {
        McpHttpProperties.OriginValidationConfig origin = new McpHttpProperties.OriginValidationConfig();
        assertTrue(origin.isEnabled());
        assertTrue(origin.isAllowLocalhost());
    }
}

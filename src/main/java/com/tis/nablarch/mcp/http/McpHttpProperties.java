package com.tis.nablarch.mcp.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP HTTPトランスポート設定プロパティ。
 *
 * <p>application-http.yamlの {@code mcp.http} 配下の設定を保持する。</p>
 *
 * @see com.tis.nablarch.mcp.http.config.StreamableHttpTransportConfig
 */
@Component
@ConfigurationProperties(prefix = "mcp.http")
public class McpHttpProperties {

    /**
     * HTTPトランスポートの有効/無効。
     */
    private boolean enabled = false;

    /**
     * MCPエンドポイントパス（デフォルト: /mcp）。
     */
    private String endpoint = "/mcp";

    /**
     * セッション設定。
     */
    private SessionConfig session = new SessionConfig();

    /**
     * CORS設定。
     */
    private CorsConfig cors = new CorsConfig();

    /**
     * Originヘッダ検証設定。
     */
    private OriginValidationConfig originValidation = new OriginValidationConfig();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public SessionConfig getSession() {
        return session;
    }

    public void setSession(SessionConfig session) {
        this.session = session;
    }

    public CorsConfig getCors() {
        return cors;
    }

    public void setCors(CorsConfig cors) {
        this.cors = cors;
    }

    public OriginValidationConfig getOriginValidation() {
        return originValidation;
    }

    public void setOriginValidation(OriginValidationConfig originValidation) {
        this.originValidation = originValidation;
    }

    /**
     * セッション管理設定。
     */
    public static class SessionConfig {

        /**
         * セッションタイムアウト（デフォルト: 30分）。
         */
        private Duration timeout = Duration.ofMinutes(30);

        /**
         * 最大同時セッション数（デフォルト: 100）。
         */
        private int maxSessions = 100;

        /**
         * セッションクリーンアップ間隔（デフォルト: 5分）。
         */
        private Duration cleanupInterval = Duration.ofMinutes(5);

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public int getMaxSessions() {
            return maxSessions;
        }

        public void setMaxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
        }

        public Duration getCleanupInterval() {
            return cleanupInterval;
        }

        public void setCleanupInterval(Duration cleanupInterval) {
            this.cleanupInterval = cleanupInterval;
        }
    }

    /**
     * CORS（Cross-Origin Resource Sharing）設定。
     */
    public static class CorsConfig {

        /**
         * 許可するオリジンのリスト。
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /**
         * 許可するHTTPメソッドのリスト。
         */
        private List<String> allowedMethods = List.of("GET", "POST", "DELETE", "OPTIONS");

        /**
         * 許可するリクエストヘッダのリスト。
         */
        private List<String> allowedHeaders = List.of("Content-Type", "Mcp-Session-Id", "Accept");

        /**
         * クライアントに公開するレスポンスヘッダのリスト。
         */
        private List<String> exposedHeaders = List.of("Mcp-Session-Id");

        /**
         * 認証情報の送信を許可するか。
         */
        private boolean allowCredentials = true;

        /**
         * プリフライトキャッシュの最大時間（秒）。
         */
        private long maxAge = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * Originヘッダ検証設定。
     */
    public static class OriginValidationConfig {

        /**
         * Origin検証の有効/無効（MCP仕様MUST要件によりデフォルト有効）。
         */
        private boolean enabled = true;

        /**
         * localhost系オリジンを許可するか。
         */
        private boolean allowLocalhost = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowLocalhost() {
            return allowLocalhost;
        }

        public void setAllowLocalhost(boolean allowLocalhost) {
            this.allowLocalhost = allowLocalhost;
        }
    }
}

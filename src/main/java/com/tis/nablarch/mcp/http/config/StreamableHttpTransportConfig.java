package com.tis.nablarch.mcp.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tis.nablarch.mcp.http.McpHttpProperties;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * MCP Streamable HTTPトランスポート設定。
 *
 * <p>MCP仕様2025-03-26版のStreamable HTTP Transportを提供する。
 * {@code http}プロファイルでのみ有効化される。</p>
 *
 * <p>このConfigurationはMCP Java SDKの{@link WebMvcStreamableServerTransportProvider}を使用して
 * {@code /mcp}エンドポイントでHTTPトランスポートを提供する。</p>
 *
 * <h2>エンドポイント</h2>
 * <ul>
 *   <li>POST /mcp - JSON-RPCメッセージの送信</li>
 *   <li>GET /mcp - SSEストリームの確立</li>
 *   <li>DELETE /mcp - セッションの終了</li>
 * </ul>
 *
 * @see McpHttpProperties
 * @see WebMvcStreamableServerTransportProvider
 */
@Configuration
@Profile("http")
@ConditionalOnProperty(name = "mcp.http.enabled", havingValue = "true", matchIfMissing = false)
public class StreamableHttpTransportConfig {

    private static final Logger logger = LoggerFactory.getLogger(StreamableHttpTransportConfig.class);

    private final McpHttpProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * コンストラクタ。
     *
     * @param properties HTTP設定プロパティ
     * @param objectMapper JSONシリアライザ
     */
    public StreamableHttpTransportConfig(McpHttpProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * MCP HTTPトランスポートプロバイダを生成する。
     *
     * <p>MCP Java SDKの{@link WebMvcStreamableServerTransportProvider}を使用して
     * Streamable HTTPトランスポートを提供する。</p>
     *
     * @return HTTPトランスポートプロバイダ
     */
    @Bean
    public WebMvcStreamableServerTransportProvider mcpHttpTransportProvider() {
        String endpoint = properties.getEndpoint();
        logger.info("MCP Streamable HTTPトランスポートを初期化: endpoint={}", endpoint);

        return new WebMvcStreamableServerTransportProvider(objectMapper, endpoint);
    }

    /**
     * MCPエンドポイントのルーティング関数を生成する。
     *
     * <p>{@link WebMvcStreamableServerTransportProvider}が提供するルーティング関数を
     * Spring MVCに登録する。これにより、{@code /mcp}エンドポイントで
     * POST/GET/DELETEリクエストを処理できるようになる。</p>
     *
     * @param transportProvider HTTPトランスポートプロバイダ
     * @return ルーティング関数
     */
    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(
            WebMvcStreamableServerTransportProvider transportProvider) {
        logger.info("MCPルーティング関数を登録");
        return transportProvider.getRouterFunction();
    }
}

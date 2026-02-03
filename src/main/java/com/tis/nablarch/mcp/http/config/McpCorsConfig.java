package com.tis.nablarch.mcp.http.config;

import com.tis.nablarch.mcp.http.McpHttpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * MCP HTTPトランスポート用CORS設定。
 *
 * <p>{@code /mcp}エンドポイントに対するCORS（Cross-Origin Resource Sharing）ポリシーを設定する。
 * 設定値は{@code application-http.yaml}の{@code mcp.http.cors}から読み込まれる。</p>
 *
 * <h2>デフォルト設定</h2>
 * <ul>
 *   <li>許可メソッド: GET, POST, DELETE, OPTIONS</li>
 *   <li>許可ヘッダ: Content-Type, Mcp-Session-Id, Accept</li>
 *   <li>公開ヘッダ: Mcp-Session-Id</li>
 *   <li>認証情報: 許可</li>
 *   <li>プリフライトキャッシュ: 3600秒</li>
 * </ul>
 *
 * @see McpHttpProperties.CorsConfig
 */
@Configuration
@Profile("http")
@ConditionalOnProperty(name = "mcp.http.enabled", havingValue = "true", matchIfMissing = false)
public class McpCorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpCorsConfig.class);

    private final McpHttpProperties properties;

    /**
     * コンストラクタ。
     *
     * @param properties HTTP設定プロパティ
     */
    public McpCorsConfig(McpHttpProperties properties) {
        this.properties = properties;
    }

    /**
     * CORSフィルタを生成する。
     *
     * <p>MCPエンドポイント（{@code /mcp}）に対するCORSポリシーを適用する。
     * 許可するオリジンは設定ファイルで指定する。</p>
     *
     * @return CORSフィルタ
     */
    @Bean
    public CorsFilter corsFilter() {
        McpHttpProperties.CorsConfig corsConfig = properties.getCors();

        CorsConfiguration config = new CorsConfiguration();

        // 許可オリジン
        if (corsConfig.getAllowedOrigins() != null && !corsConfig.getAllowedOrigins().isEmpty()) {
            corsConfig.getAllowedOrigins().forEach(config::addAllowedOrigin);
            logger.info("CORS許可オリジン: {}", corsConfig.getAllowedOrigins());
        } else {
            // デフォルトでlocalhost系を許可
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("http://localhost:8080");
            logger.info("CORS許可オリジン: デフォルト (localhost)");
        }

        // 許可メソッド
        corsConfig.getAllowedMethods().forEach(config::addAllowedMethod);

        // 許可ヘッダ
        corsConfig.getAllowedHeaders().forEach(config::addAllowedHeader);

        // 公開ヘッダ（クライアントがアクセス可能なレスポンスヘッダ）
        corsConfig.getExposedHeaders().forEach(config::addExposedHeader);

        // 認証情報の送信許可
        config.setAllowCredentials(corsConfig.isAllowCredentials());

        // プリフライトキャッシュ時間
        config.setMaxAge(corsConfig.getMaxAge());

        // MCPエンドポイントにCORS設定を適用
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        String endpoint = properties.getEndpoint();
        source.registerCorsConfiguration(endpoint, config);
        source.registerCorsConfiguration(endpoint + "/**", config);

        logger.info("CORSフィルタを設定: endpoint={}, methods={}, allowCredentials={}",
                endpoint, corsConfig.getAllowedMethods(), corsConfig.isAllowCredentials());

        return new CorsFilter(source);
    }
}

package com.tis.nablarch.mcp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * テスト用共通設定。
 *
 * <p>コンテキストロードテスト等で必要なビーンを提供する。</p>
 */
@TestConfiguration
public class TestConfig {

    /**
     * テスト用WebClientビーン。
     *
     * <p>FintanIngester等がWebClientを直接注入するため、
     * テストコンテキスト用にダミーのWebClientを提供する。</p>
     *
     * @param builder WebClient.Builder（Spring Boot自動構成）
     * @return WebClientインスタンス
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }
}

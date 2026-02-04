package com.tis.nablarch.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient設定クラス。
 *
 * <p>FintanIngester、OfficialDocsIngester等のHTTPクライアントを必要とする
 * コンポーネント向けにWebClient Beanを提供する。</p>
 *
 * <p>HTTPモード（Servlet）でも動作するよう、spring-webfluxの
 * WebClientをBean登録する。</p>
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient Beanを生成する。
     *
     * @return WebClientインスタンス
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }
}

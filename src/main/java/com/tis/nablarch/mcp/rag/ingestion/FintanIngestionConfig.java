package com.tis.nablarch.mcp.rag.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fintan取込パイプラインの設定。
 *
 * <p>{@code application.yml} の {@code ingestion.fintan} プレフィックスで
 * 設定値をバインドする。</p>
 *
 * <pre>
 * ingestion:
 *   fintan:
 *     base-url: https://fintan.jp/
 *     search-tags:
 *       - Nablarch
 *     batch-size: 10
 *     delay-ms: 1000
 *     max-retries: 3
 *     enabled: true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ingestion.fintan")
public class FintanIngestionConfig {

    /**
     * FintanのベースURL。
     */
    private String baseUrl = "https://fintan.jp/";

    /**
     * 取込対象のタグ一覧。このタグが付いた記事のみ取り込む。
     */
    private List<String> searchTags = List.of("Nablarch");

    /**
     * Embedding処理のバッチサイズ。
     */
    private int batchSize = 10;

    /**
     * 記事取得間のディレイ（ミリ秒）。robots.txt準拠。
     */
    private long delayMs = 1000;

    /**
     * 個別記事取得失敗時の最大リトライ回数。
     */
    private int maxRetries = 3;

    /**
     * 取込パイプラインの有効/無効。
     */
    private boolean enabled = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getSearchTags() {
        return searchTags;
    }

    public void setSearchTags(List<String> searchTags) {
        this.searchTags = searchTags;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

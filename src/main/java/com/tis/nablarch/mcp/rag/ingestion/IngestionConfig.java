package com.tis.nablarch.mcp.rag.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ドキュメント取り込みパイプラインの設定プロパティ。
 *
 * <p>application.yamlの {@code ingestion} 配下の設定を保持する。
 * データソースごとにサブ設定を持つ。</p>
 */
@Component
@ConfigurationProperties(prefix = "ingestion")
public class IngestionConfig {

    private OfficialDocsConfig officialDocs = new OfficialDocsConfig();

    public OfficialDocsConfig getOfficialDocs() {
        return officialDocs;
    }

    public void setOfficialDocs(OfficialDocsConfig officialDocs) {
        this.officialDocs = officialDocs;
    }

    /**
     * Nablarch公式ドキュメント取り込みの設定。
     */
    public static class OfficialDocsConfig {

        /** 公式ドキュメントのベースURL */
        private String baseUrl = "https://nablarch.github.io/docs/LATEST/doc/";

        /** バッチ処理のサイズ（1回のEmbedding API呼び出しで処理するチャンク数） */
        private int batchSize = 20;

        /** クローリング間隔（ミリ秒） */
        private long delayMs = 1000;

        /** HTTP障害時の最大リトライ回数 */
        private int maxRetries = 3;

        /** 取り込み機能の有効/無効 */
        private boolean enabled = true;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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
}

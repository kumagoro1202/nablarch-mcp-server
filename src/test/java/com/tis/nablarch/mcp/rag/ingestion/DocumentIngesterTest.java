package com.tis.nablarch.mcp.rag.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocumentIngester インターフェースのテスト。
 */
class DocumentIngesterTest {

    @Test
    @DisplayName("DocumentIngester の匿名実装が正しく動作する")
    void anonymousImplementation() {
        DocumentIngester ingester = new DocumentIngester() {
            @Override
            public IngestionResult ingestAll() {
                return IngestionResult.success(5);
            }

            @Override
            public IngestionResult ingestIncremental(Instant since) {
                return IngestionResult.success(2);
            }

            @Override
            public String getSourceName() {
                return "test-source";
            }
        };

        assertEquals("test-source", ingester.getSourceName());

        IngestionResult allResult = ingester.ingestAll();
        assertEquals(5, allResult.processedCount());

        IngestionResult incResult = ingester.ingestIncremental(Instant.now());
        assertEquals(2, incResult.processedCount());
    }

    @Test
    @DisplayName("異なるデータソース名を返す実装を複数作れる")
    void multipleImplementations() {
        DocumentIngester docs = new DocumentIngester() {
            @Override
            public IngestionResult ingestAll() { return IngestionResult.empty(); }
            @Override
            public IngestionResult ingestIncremental(Instant since) { return IngestionResult.empty(); }
            @Override
            public String getSourceName() { return "official-docs"; }
        };

        DocumentIngester fintan = new DocumentIngester() {
            @Override
            public IngestionResult ingestAll() { return IngestionResult.empty(); }
            @Override
            public IngestionResult ingestIncremental(Instant since) { return IngestionResult.empty(); }
            @Override
            public String getSourceName() { return "fintan"; }
        };

        assertNotEquals(docs.getSourceName(), fintan.getSourceName());
    }
}

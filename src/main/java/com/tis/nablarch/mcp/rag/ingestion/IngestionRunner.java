package com.tis.nablarch.mcp.rag.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ドキュメント取込の手動実行ランナー。
 *
 * <p>{@code nablarch.mcp.ingestion.run=true} が設定された場合にのみ有効化され、
 * 登録されている全Ingesterの{@link DocumentIngester#ingestAll()}を実行する。
 * 取込完了後にアプリケーションを終了する。</p>
 *
 * <p>実行例:</p>
 * <pre>{@code
 * java -jar nablarch-mcp-server.jar \
 *   --nablarch.mcp.ingestion.enabled=true \
 *   --nablarch.mcp.ingestion.run=true \
 *   --nablarch.mcp.embedding.provider=local
 * }</pre>
 */
@Component
@ConditionalOnProperty(name = "nablarch.mcp.ingestion.run", havingValue = "true")
public class IngestionRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(IngestionRunner.class);

    private final List<DocumentIngester> ingesters;

    public IngestionRunner(List<DocumentIngester> ingesters) {
        this.ingesters = ingesters;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("=== ドキュメント取込を開始 ===");
        logger.info("登録Ingester数: {}", ingesters.size());

        for (DocumentIngester ingester : ingesters) {
            logger.info("--- {} の取込を開始 ---", ingester.getSourceName());
            try {
                IngestionResult result = ingester.ingestAll();
                logger.info("{} 取込完了: processed={}, success={}, errors={}",
                        ingester.getSourceName(),
                        result.processedCount(),
                        result.successCount(),
                        result.errorCount());
                if (result.errorCount() > 0) {
                    result.errors().forEach(e -> logger.warn("  エラー: {}", e));
                }
            } catch (Exception e) {
                logger.error("{} 取込失敗: {}", ingester.getSourceName(), e.getMessage(), e);
            }
        }

        logger.info("=== ドキュメント取込完了。アプリケーションを終了します ===");
        System.exit(0);
    }
}

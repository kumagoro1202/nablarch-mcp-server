package com.tis.nablarch.mcp.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * データベース接続のヘルスインジケータ。
 * pgvector DBへの接続を確認する。接続不可時はDOWNを返す。
 *
 * <p>全体のヘルスステータスにはGracefulHealthStatusAggregatorにより
 * 影響しない（KnowledgeBaseがUPなら全体はUP）。</p>
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthIndicator.class);

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            if (valid) {
                return Health.up()
                        .withDetail("database", conn.getMetaData().getDatabaseProductName())
                        .withDetail("url", conn.getMetaData().getURL())
                        .build();
            }
            return Health.down()
                    .withDetail("reason", "接続は取得できたがvalidation失敗")
                    .build();
        } catch (Exception e) {
            log.debug("DB接続確認失敗（グレースフルデグレード）: {}", e.getMessage());
            return Health.down()
                    .withDetail("reason", "DB接続不可")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

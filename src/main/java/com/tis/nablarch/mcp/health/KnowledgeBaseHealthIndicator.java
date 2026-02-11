package com.tis.nablarch.mcp.health;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 知識ベースのヘルスインジケータ。
 * NablarchKnowledgeBaseのYAMLロード状態を確認する。
 */
@Component
public class KnowledgeBaseHealthIndicator implements HealthIndicator {

    private final NablarchKnowledgeBase knowledgeBase;

    public KnowledgeBaseHealthIndicator(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public Health health() {
        int appTypes = knowledgeBase.getAvailableAppTypes().size();
        int designPatterns = knowledgeBase.getAllDesignPatterns().size();

        if (appTypes == 0) {
            return Health.down()
                    .withDetail("reason", "知識ベースのロードが完了していない")
                    .withDetail("appTypes", appTypes)
                    .build();
        }

        return Health.up()
                .withDetail("appTypes", appTypes)
                .withDetail("designPatterns", designPatterns)
                .build();
    }
}

package com.tis.nablarch.mcp.health;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Embeddingモデルのヘルスインジケータ。
 * プロバイダ設定に基づき、APIキー有無またはONNXモデルファイル存在を確認する。
 */
@Component
public class EmbeddingModelHealthIndicator implements HealthIndicator {

    private final EmbeddingProperties properties;

    public EmbeddingModelHealthIndicator(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        String provider = properties.getProvider();

        if ("local".equals(provider)) {
            return checkLocalModel();
        } else if ("api".equals(provider)) {
            return checkApiConfig();
        }

        return Health.down()
                .withDetail("provider", provider)
                .withDetail("reason", "不明なプロバイダ種別")
                .build();
    }

    private Health checkLocalModel() {
        String modelPath = properties.getLocal().getDocument().getModelPath();
        boolean modelExists = modelPath != null && !modelPath.isEmpty()
                && Files.exists(Path.of(modelPath));

        Health.Builder builder = modelExists ? Health.up() : Health.down();
        builder.withDetail("provider", "local")
                .withDetail("modelName", properties.getLocal().getDocument().getModelName())
                .withDetail("modelPath", modelPath)
                .withDetail("modelFileExists", modelExists);

        if (!modelExists) {
            builder.withDetail("reason", "ONNXモデルファイルが見つからない");
        }

        return builder.build();
    }

    private Health checkApiConfig() {
        String jinaKey = properties.getJina().getApiKey();
        boolean jinaConfigured = jinaKey != null && !jinaKey.isEmpty();

        Health.Builder builder = jinaConfigured ? Health.up() : Health.down();
        builder.withDetail("provider", "api")
                .withDetail("jinaConfigured", jinaConfigured)
                .withDetail("jinaModel", properties.getJina().getModel());

        if (!jinaConfigured) {
            builder.withDetail("reason", "Jina APIキーが設定されていない");
        }

        return builder.build();
    }
}

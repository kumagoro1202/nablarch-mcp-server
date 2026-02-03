package com.tis.nablarch.mcp.embedding;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jina Embeddings v4 クライアント。
 *
 * <p>ドキュメント用Embeddingモデル。3.8Bパラメータ、89言語対応、32Kコンテキスト。
 * OpenAI互換のHTTP APIを呼び出し、1024次元のベクトルを生成する。</p>
 *
 * <h3>有効化条件</h3>
 * <p>{@code nablarch.mcp.embedding.provider=api} の場合に有効化される。
 * ローカルモデル（BGE-M3）への移行により、通常はlocalを推奨する。</p>
 */
@Component
@Qualifier("document")
@ConditionalOnProperty(name = "nablarch.mcp.embedding.provider", havingValue = "api")
public class JinaEmbeddingClient implements EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(JinaEmbeddingClient.class);

    private final WebClient webClient;
    private final EmbeddingProperties.ProviderConfig config;

    /**
     * コンストラクタ。
     *
     * @param properties Embedding設定プロパティ
     */
    public JinaEmbeddingClient(EmbeddingProperties properties) {
        this.config = properties.getJina();
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .build();
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        EmbeddingRequest request = new EmbeddingRequest(
                config.getModel(), texts, config.getDimensions());

        int retries = 0;
        while (true) {
            try {
                EmbeddingResponse response = webClient.post()
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(EmbeddingResponse.class)
                        .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                        .block();

                if (response == null || response.getData() == null) {
                    throw new EmbeddingException("Jina API returned null response");
                }

                logger.debug("Jina API: {} テキストのEmbedding生成完了", texts.size());

                return response.getData().stream()
                        .sorted(Comparator.comparingInt(EmbeddingResponse.EmbeddingData::getIndex))
                        .map(EmbeddingResponse.EmbeddingData::getEmbedding)
                        .collect(Collectors.toList());

            } catch (EmbeddingException e) {
                throw e;
            } catch (Exception e) {
                retries++;
                if (retries > config.getMaxRetries()) {
                    throw new EmbeddingException(
                            "Jina API call failed after " + config.getMaxRetries() + " retries", e);
                }
                logger.warn("Jina API呼び出し失敗（リトライ {}/{}）: {}",
                        retries, config.getMaxRetries(), e.getMessage());
            }
        }
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public int getDimensions() {
        return config.getDimensions();
    }
}

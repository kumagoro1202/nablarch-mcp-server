package com.tis.nablarch.mcp.embedding.local;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BgeM3OnnxEmbeddingClient} のユニットテスト。
 *
 * <p>ONNXモデルが必要なテストは環境変数 {@code EMBEDDING_MODEL_PATH} が設定されている場合のみ実行される。</p>
 */
class BgeM3OnnxEmbeddingClientTest {

    private EmbeddingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EmbeddingProperties();
        properties.setProvider("local");

        EmbeddingProperties.OnnxModelConfig documentConfig = new EmbeddingProperties.OnnxModelConfig();
        documentConfig.setModelName("BAAI/bge-m3");
        documentConfig.setModelPath("/opt/models/bge-m3/model.onnx");
        documentConfig.setTokenizerPath("/opt/models/bge-m3");
        documentConfig.setDimensions(1024);
        documentConfig.setMaxTokens(512);
        documentConfig.setBatchSize(32);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setDocument(documentConfig);
        properties.setLocal(localConfig);
    }

    @Test
    void プロパティ設定が正しく読み込まれること() {
        assertEquals("local", properties.getProvider());
        assertEquals("BAAI/bge-m3", properties.getLocal().getDocument().getModelName());
        assertEquals(1024, properties.getLocal().getDocument().getDimensions());
        assertEquals(512, properties.getLocal().getDocument().getMaxTokens());
        assertEquals(32, properties.getLocal().getDocument().getBatchSize());
    }

    @Test
    void モデル設定のデフォルト値が適用されること() {
        EmbeddingProperties defaultProps = new EmbeddingProperties();
        EmbeddingProperties.OnnxModelConfig config = defaultProps.getLocal().getDocument();

        assertEquals(1024, config.getDimensions());
        assertEquals("", config.getModelName());
        assertEquals("", config.getModelPath());
    }

    /**
     * ONNXモデルを使用した統合テスト。
     *
     * <p>EMBEDDING_MODEL_PATH環境変数が設定されている場合のみ実行。
     * テスト実行前にbge-m3モデルをONNX形式に変換しておく必要がある。</p>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "EMBEDDING_MODEL_PATH", matches = ".+")
    void ONNXモデルでEmbeddingを生成できること() {
        String modelPath = System.getenv("EMBEDDING_MODEL_PATH");

        EmbeddingProperties.OnnxModelConfig config = new EmbeddingProperties.OnnxModelConfig();
        config.setModelName("BAAI/bge-m3");
        config.setModelPath(modelPath + "/bge-m3/model.onnx");
        config.setTokenizerPath(modelPath + "/bge-m3");
        config.setDimensions(1024);
        config.setMaxTokens(512);
        config.setBatchSize(32);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setDocument(config);
        properties.setLocal(localConfig);

        BgeM3OnnxEmbeddingClient client = new BgeM3OnnxEmbeddingClient(properties);
        client.init();

        try {
            float[] embedding = client.embed("Nablarchのハンドラキューについて説明してください。");

            assertNotNull(embedding);
            assertEquals(1024, embedding.length);

            // L2正規化されていることを確認
            float norm = 0;
            for (float v : embedding) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            assertEquals(1.0f, norm, 0.01f);

        } finally {
            client.destroy();
        }
    }

    /**
     * バッチEmbedding生成の統合テスト。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "EMBEDDING_MODEL_PATH", matches = ".+")
    void ONNXモデルでバッチEmbeddingを生成できること() {
        String modelPath = System.getenv("EMBEDDING_MODEL_PATH");

        EmbeddingProperties.OnnxModelConfig config = new EmbeddingProperties.OnnxModelConfig();
        config.setModelName("BAAI/bge-m3");
        config.setModelPath(modelPath + "/bge-m3/model.onnx");
        config.setTokenizerPath(modelPath + "/bge-m3");
        config.setDimensions(1024);
        config.setMaxTokens(512);
        config.setBatchSize(2);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setDocument(config);
        properties.setLocal(localConfig);

        BgeM3OnnxEmbeddingClient client = new BgeM3OnnxEmbeddingClient(properties);
        client.init();

        try {
            List<String> texts = List.of(
                    "Nablarchのハンドラキューとは",
                    "リクエスト処理のフロー",
                    "コンポーネント定義XML"
            );

            List<float[]> embeddings = client.embedBatch(texts);

            assertEquals(3, embeddings.size());

            for (float[] embedding : embeddings) {
                assertEquals(1024, embedding.length);
            }

            // 異なるテキストは異なるベクトルになることを確認
            float similarity = cosineSimilarity(embeddings.get(0), embeddings.get(1));
            assertTrue(similarity < 1.0f);

        } finally {
            client.destroy();
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot; // L2正規化済みなのでドット積 = コサイン類似度
    }
}

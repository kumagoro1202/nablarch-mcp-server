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
 * {@link CodeSageOnnxEmbeddingClient} のユニットテスト。
 *
 * <p>ONNXモデルが必要なテストは環境変数 {@code EMBEDDING_MODEL_PATH} が設定されている場合のみ実行される。</p>
 */
class CodeSageOnnxEmbeddingClientTest {

    private EmbeddingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EmbeddingProperties();
        properties.setProvider("local");

        EmbeddingProperties.OnnxModelConfig codeConfig = new EmbeddingProperties.OnnxModelConfig();
        codeConfig.setModelName("codesage/codesage-small-v2");
        codeConfig.setModelPath("/opt/models/codesage-small-v2/model.onnx");
        codeConfig.setTokenizerPath("/opt/models/codesage-small-v2");
        codeConfig.setDimensions(1024);
        codeConfig.setMaxTokens(512);
        codeConfig.setBatchSize(32);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setCode(codeConfig);
        properties.setLocal(localConfig);
    }

    @Test
    void プロパティ設定が正しく読み込まれること() {
        assertEquals("local", properties.getProvider());
        assertEquals("codesage/codesage-small-v2", properties.getLocal().getCode().getModelName());
        assertEquals(1024, properties.getLocal().getCode().getDimensions());
        assertEquals(512, properties.getLocal().getCode().getMaxTokens());
        assertEquals(32, properties.getLocal().getCode().getBatchSize());
    }

    @Test
    void モデル設定のデフォルト値が適用されること() {
        EmbeddingProperties defaultProps = new EmbeddingProperties();
        EmbeddingProperties.OnnxModelConfig config = defaultProps.getLocal().getCode();

        assertEquals(1024, config.getDimensions());
        assertEquals("", config.getModelName());
        assertEquals("", config.getModelPath());
    }

    /**
     * ONNXモデルを使用した統合テスト（Javaコード）。
     *
     * <p>EMBEDDING_MODEL_PATH環境変数が設定されている場合のみ実行。
     * テスト実行前にcodesage-small-v2モデルをONNX形式に変換しておく必要がある。</p>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "EMBEDDING_MODEL_PATH", matches = ".+")
    void ONNXモデルでJavaコードのEmbeddingを生成できること() {
        String modelPath = System.getenv("EMBEDDING_MODEL_PATH");

        EmbeddingProperties.OnnxModelConfig config = new EmbeddingProperties.OnnxModelConfig();
        config.setModelName("codesage/codesage-small-v2");
        config.setModelPath(modelPath + "/codesage-small-v2/model.onnx");
        config.setTokenizerPath(modelPath + "/codesage-small-v2");
        config.setDimensions(1024);
        config.setMaxTokens(512);
        config.setBatchSize(32);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setCode(config);
        properties.setLocal(localConfig);

        CodeSageOnnxEmbeddingClient client = new CodeSageOnnxEmbeddingClient(properties);
        client.init();

        try {
            String javaCode = """
                @Component
                public class MyHandler implements Handler<HttpRequest, HttpResponse> {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        return new HttpResponse(200);
                    }
                }
                """;

            float[] embedding = client.embed(javaCode);

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
     * バッチEmbedding生成の統合テスト（複数コード）。
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "EMBEDDING_MODEL_PATH", matches = ".+")
    void ONNXモデルでバッチコードEmbeddingを生成できること() {
        String modelPath = System.getenv("EMBEDDING_MODEL_PATH");

        EmbeddingProperties.OnnxModelConfig config = new EmbeddingProperties.OnnxModelConfig();
        config.setModelName("codesage/codesage-small-v2");
        config.setModelPath(modelPath + "/codesage-small-v2/model.onnx");
        config.setTokenizerPath(modelPath + "/codesage-small-v2");
        config.setDimensions(1024);
        config.setMaxTokens(512);
        config.setBatchSize(2);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setCode(config);
        properties.setLocal(localConfig);

        CodeSageOnnxEmbeddingClient client = new CodeSageOnnxEmbeddingClient(properties);
        client.init();

        try {
            List<String> codes = List.of(
                    "public void execute() { System.out.println(\"Hello\"); }",
                    "<component name=\"myAction\" class=\"com.example.MyAction\"/>",
                    "SELECT * FROM users WHERE status = 'ACTIVE'"
            );

            List<float[]> embeddings = client.embedBatch(codes);

            assertEquals(3, embeddings.size());

            for (float[] embedding : embeddings) {
                assertEquals(1024, embedding.length);
            }

            // 異なるコードは異なるベクトルになることを確認
            float similarity = cosineSimilarity(embeddings.get(0), embeddings.get(1));
            assertTrue(similarity < 1.0f);

        } finally {
            client.destroy();
        }
    }

    /**
     * XMLコードのEmbedding生成テスト。
     *
     * <p>NablarchのXML設定ファイルも適切に処理できることを確認。</p>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "EMBEDDING_MODEL_PATH", matches = ".+")
    void ONNXモデルでXML設定のEmbeddingを生成できること() {
        String modelPath = System.getenv("EMBEDDING_MODEL_PATH");

        EmbeddingProperties.OnnxModelConfig config = new EmbeddingProperties.OnnxModelConfig();
        config.setModelName("codesage/codesage-small-v2");
        config.setModelPath(modelPath + "/codesage-small-v2/model.onnx");
        config.setTokenizerPath(modelPath + "/codesage-small-v2");
        config.setDimensions(1024);
        config.setMaxTokens(512);
        config.setBatchSize(32);

        EmbeddingProperties.LocalModelConfig localConfig = new EmbeddingProperties.LocalModelConfig();
        localConfig.setCode(config);
        properties.setLocal(localConfig);

        CodeSageOnnxEmbeddingClient client = new CodeSageOnnxEmbeddingClient(properties);
        client.init();

        try {
            String xmlConfig = """
                <component-configuration xmlns="http://tis.co.jp/nablarch/component-configuration">
                    <component name="httpRequestHandler"
                               class="nablarch.fw.web.handler.HttpRequestHandler">
                        <property name="requestRouter" ref="requestRouter"/>
                    </component>
                </component-configuration>
                """;

            float[] embedding = client.embed(xmlConfig);

            assertNotNull(embedding);
            assertEquals(1024, embedding.length);

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

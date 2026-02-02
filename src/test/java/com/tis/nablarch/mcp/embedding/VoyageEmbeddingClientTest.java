package com.tis.nablarch.mcp.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link VoyageEmbeddingClient} のユニットテスト。
 *
 * <p>MockWebServerを使用してVoyage APIのレスポンスをモックし、
 * クライアントの動作を検証する。</p>
 */
class VoyageEmbeddingClientTest {

    private MockWebServer mockServer;
    private VoyageEmbeddingClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        EmbeddingProperties properties = new EmbeddingProperties();
        EmbeddingProperties.ProviderConfig config = new EmbeddingProperties.ProviderConfig();
        config.setApiKey("test-voyage-key");
        config.setModel("voyage-code-3");
        config.setDimensions(1024);
        config.setBaseUrl(mockServer.url("/v1/embeddings").toString());
        config.setTimeoutSeconds(5);
        config.setMaxRetries(2);
        properties.setVoyage(config);

        client = new VoyageEmbeddingClient(properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void 単一コードのEmbeddingを生成できること() throws Exception {
        float[] expectedEmbedding = new float[]{0.7f, 0.8f, 0.9f};
        enqueueSuccessResponse(List.of(expectedEmbedding));

        float[] result = client.embed("public class HttpResponse {}");

        assertNotNull(result);
        assertArrayEquals(expectedEmbedding, result, 0.001f);

        RecordedRequest request = mockServer.takeRequest();
        assertTrue(request.getHeader("Authorization").contains("Bearer test-voyage-key"));

        EmbeddingRequest sentRequest = objectMapper.readValue(
                request.getBody().readUtf8(), EmbeddingRequest.class);
        assertEquals("voyage-code-3", sentRequest.getModel());
    }

    @Test
    void バッチEmbeddingを生成できること() throws Exception {
        float[] emb1 = new float[]{0.1f, 0.2f};
        float[] emb2 = new float[]{0.3f, 0.4f};
        float[] emb3 = new float[]{0.5f, 0.6f};
        enqueueSuccessResponse(List.of(emb1, emb2, emb3));

        List<float[]> results = client.embedBatch(List.of("code1", "code2", "code3"));

        assertEquals(3, results.size());
        assertArrayEquals(emb1, results.get(0), 0.001f);
        assertArrayEquals(emb3, results.get(2), 0.001f);
    }

    @Test
    void リトライ後に成功すること() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(429).setBody("Rate Limited"));

        float[] expectedEmbedding = new float[]{0.1f};
        enqueueSuccessResponse(List.of(expectedEmbedding));

        float[] result = client.embed("retry test");
        assertArrayEquals(expectedEmbedding, result, 0.001f);
    }

    @Test
    void 最大リトライ超過で例外が発生すること() {
        mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));
        mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("Error"));

        assertThrows(EmbeddingException.class, () -> client.embed("fail test"));
    }

    @Test
    void モデル名と次元数を取得できること() {
        assertEquals("voyage-code-3", client.getModelName());
        assertEquals(1024, client.getDimensions());
    }

    private void enqueueSuccessResponse(List<float[]> embeddings) throws Exception {
        EmbeddingResponse response = new EmbeddingResponse();
        List<EmbeddingResponse.EmbeddingData> data = new java.util.ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            EmbeddingResponse.EmbeddingData d = new EmbeddingResponse.EmbeddingData();
            d.setEmbedding(embeddings.get(i));
            d.setIndex(i);
            data.add(d);
        }
        response.setData(data);
        response.setModel("voyage-code-3");

        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(response)));
    }
}

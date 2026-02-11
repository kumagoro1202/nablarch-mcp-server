package com.tis.nablarch.mcp.health;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingModelHealthIndicatorTest {

    @Test
    void APIモードでキーが設定済みの場合UPを返す() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("api");
        props.getJina().setApiKey("test-key");
        props.getJina().setModel("jina-embeddings-v4");

        var indicator = new EmbeddingModelHealthIndicator(props);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("api", health.getDetails().get("provider"));
        assertEquals(true, health.getDetails().get("jinaConfigured"));
    }

    @Test
    void APIモードでキーが未設定の場合DOWNを返す() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("api");
        props.getJina().setApiKey("");

        var indicator = new EmbeddingModelHealthIndicator(props);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(false, health.getDetails().get("jinaConfigured"));
    }

    @Test
    void ローカルモードでモデルファイルが存在しない場合DOWNを返す() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("local");
        props.getLocal().getDocument().setModelPath("/nonexistent/model.onnx");
        props.getLocal().getDocument().setModelName("bge-m3");

        var indicator = new EmbeddingModelHealthIndicator(props);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("local", health.getDetails().get("provider"));
        assertEquals(false, health.getDetails().get("modelFileExists"));
    }

    @Test
    void 不明なプロバイダの場合DOWNを返す() {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setProvider("unknown");

        var indicator = new EmbeddingModelHealthIndicator(props);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("reason"));
    }
}

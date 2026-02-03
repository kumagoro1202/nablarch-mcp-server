package com.tis.nablarch.mcp.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Streamable HTTPトランスポートの統合テスト。
 *
 * <p>WBS 3.3.10 - HTTPプロファイルでのサーバー動作を検証する。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("http")
@AutoConfigureMockMvc
class StreamableHttpTransportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private McpHttpProperties properties;

    @Nested
    @DisplayName("コンテキスト起動テスト")
    class ContextLoadTest {

        @Test
        @DisplayName("HTTPプロファイルでアプリケーションコンテキストが起動する")
        void contextLoads() {
            assertNotNull(mockMvc);
            assertNotNull(properties);
        }

        @Test
        @DisplayName("McpHttpPropertiesが正しくバインドされる")
        void propertiesAreBound() {
            assertTrue(properties.isEnabled());
            assertEquals("/mcp", properties.getEndpoint());
        }

        @Test
        @DisplayName("セッション設定が正しくバインドされる")
        void sessionConfigIsBound() {
            McpHttpProperties.SessionConfig session = properties.getSession();
            assertNotNull(session);
            assertEquals(Duration.ofMinutes(30), session.getTimeout());
            assertEquals(100, session.getMaxSessions());
        }

        @Test
        @DisplayName("CORS設定が正しくバインドされる")
        void corsConfigIsBound() {
            McpHttpProperties.CorsConfig cors = properties.getCors();
            assertNotNull(cors);
            assertTrue(cors.getAllowedOrigins().contains("http://localhost:3000"));
            assertTrue(cors.isAllowCredentials());
        }
    }

    @Nested
    @DisplayName("/mcp エンドポイントテスト")
    class McpEndpointTest {

        @Test
        @DisplayName("POST /mcp - JSON-RPCリクエストを受け付ける")
        void postMcpAcceptsJsonRpcRequest() throws Exception {
            String jsonRpcRequest = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}
                """;

            mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRpcRequest))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OPTIONS /mcp - CORSプリフライト")
        void optionsMcpHandlesCorsPreFlight() throws Exception {
            mockMvc.perform(options("/mcp")
                    .header("Origin", "http://localhost:3000")
                    .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"));
        }
    }

    @Nested
    @DisplayName("CORS設定テスト")
    class CorsTest {

        @Test
        @DisplayName("許可オリジンからのリクエストはCORSヘッダが付与される")
        void allowedOriginGetsCorsHeaders() throws Exception {
            mockMvc.perform(post("/mcp")
                    .header("Origin", "http://localhost:3000")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        }
    }

    @Nested
    @DisplayName("エラーケーステスト")
    class ErrorCaseTest {

        @Test
        @DisplayName("不正なJSONでエラーレスポンス")
        void invalidJsonReturnsError() throws Exception {
            mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid }"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("空のボディでエラー")
        void emptyBodyReturnsError() throws Exception {
            mockMvc.perform(post("/mcp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
                    .andExpect(status().isBadRequest());
        }
    }
}

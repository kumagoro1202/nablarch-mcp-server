package com.tis.nablarch.mcp.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Actuatorエンドポイントの統合テスト。
 * HTTPプロファイルでActuatorエンドポイントが正しく応答することを検証する。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"http", "test"})
@AutoConfigureMockMvc
class ActuatorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthエンドポイントが応答する() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void infoエンドポイントが応答する() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsエンドポイントが応答する() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    void 未公開エンドポイントにアクセスできない() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }

    @Test
    void prometheusエンドポイントは未公開() throws Exception {
        // prometheusはexposure.includeに含めていないので404
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isNotFound());
    }
}

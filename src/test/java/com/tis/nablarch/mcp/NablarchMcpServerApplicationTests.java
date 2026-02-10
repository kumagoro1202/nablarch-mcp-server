package com.tis.nablarch.mcp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for application context loading.
 */
@Disabled("MCPトランスポートプロバイダがクラスパスにないため、Phase 3まで無効化")
@SpringBootTest
@ActiveProfiles("test")
class NablarchMcpServerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully.
    }
}

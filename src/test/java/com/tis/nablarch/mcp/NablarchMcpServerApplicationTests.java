package com.tis.nablarch.mcp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for application context loading.
 *
 * <p>現在MCPトランスポートプロバイダがクラスパスにないため無効化。
 * Phase 2でHTTP/SSEトランスポートが追加されたら再有効化する。</p>
 */
@Disabled("MCPトランスポートプロバイダがクラスパスにないため、Phase 2まで無効化")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class NablarchMcpServerApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully.
    }
}

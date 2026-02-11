package com.tis.nablarch.mcp.health;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.DesignPatternEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseHealthIndicatorTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    @Test
    void 知識ベースがロード済みの場合UPを返す() {
        when(knowledgeBase.getAvailableAppTypes()).thenReturn(Set.of("web", "rest", "batch"));
        when(knowledgeBase.getAllDesignPatterns()).thenReturn(List.of(new DesignPatternEntry()));

        var indicator = new KnowledgeBaseHealthIndicator(knowledgeBase);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(3, health.getDetails().get("appTypes"));
        assertEquals(1, health.getDetails().get("designPatterns"));
    }

    @Test
    void 知識ベースが未ロードの場合DOWNを返す() {
        when(knowledgeBase.getAvailableAppTypes()).thenReturn(Set.of());

        var indicator = new KnowledgeBaseHealthIndicator(knowledgeBase);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("reason"));
    }
}

package com.tis.nablarch.mcp.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GracefulHealthStatusAggregatorTest {

    private final GracefulHealthStatusAggregator aggregator = new GracefulHealthStatusAggregator();

    @Test
    void いずれかがUPなら全体はUP() {
        Status result = aggregator.getAggregateStatus(
                Set.of(Status.UP, Status.DOWN, Status.OUT_OF_SERVICE));
        assertEquals(Status.UP, result);
    }

    @Test
    void 全てUPなら全体はUP() {
        Status result = aggregator.getAggregateStatus(Set.of(Status.UP));
        assertEquals(Status.UP, result);
    }

    @Test
    void 全てDOWNなら全体はDOWN() {
        Status result = aggregator.getAggregateStatus(Set.of(Status.DOWN));
        assertEquals(Status.DOWN, result);
    }

    @Test
    void UPとUNKNOWNの場合全体はUP() {
        Status result = aggregator.getAggregateStatus(
                Set.of(Status.UP, Status.UNKNOWN));
        assertEquals(Status.UP, result);
    }

    @Test
    void DOWNとOUT_OF_SERVICEの場合全体はDOWN() {
        Status result = aggregator.getAggregateStatus(
                Set.of(Status.DOWN, Status.OUT_OF_SERVICE));
        assertEquals(Status.DOWN, result);
    }
}

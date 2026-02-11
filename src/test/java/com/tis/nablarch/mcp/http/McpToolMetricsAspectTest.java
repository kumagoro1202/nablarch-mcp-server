package com.tis.nablarch.mcp.http;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link McpToolMetricsAspect} の単体テスト。
 */
class McpToolMetricsAspectTest {

    private MeterRegistry registry;
    private McpToolMetricsAspect aspect;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        aspect = new McpToolMetricsAspect(registry);
    }

    @Test
    void 正常実行時に呼び出しカウンタとタイマーが記録される() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("sampleTool", "sample_tool");
        when(joinPoint.proceed()).thenReturn("result");

        Object result = aspect.measureToolExecution(joinPoint);

        assertEquals("result", result);
        assertEquals(1.0, registry.counter("mcp.tool.invocations", "tool", "sample_tool").count());
        assertNotNull(registry.timer("mcp.tool.duration", "tool", "sample_tool"));
        assertTrue(registry.timer("mcp.tool.duration", "tool", "sample_tool").count() > 0);
    }

    @Test
    void 例外発生時にエラーカウンタが記録される() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("failingTool", "failing_tool");
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("test error"));

        assertThrows(IllegalStateException.class, () -> aspect.measureToolExecution(joinPoint));

        assertEquals(1.0, registry.counter("mcp.tool.invocations", "tool", "failing_tool").count());
        assertEquals(1.0, registry.counter("mcp.tool.errors", "tool", "failing_tool",
                "exception", "IllegalStateException").count());
        assertTrue(registry.timer("mcp.tool.duration", "tool", "failing_tool").count() > 0);
    }

    @Test
    void 複数回呼び出しでカウンタが累積する() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("multiTool", "multi_tool");
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.measureToolExecution(joinPoint);
        aspect.measureToolExecution(joinPoint);
        aspect.measureToolExecution(joinPoint);

        assertEquals(3.0, registry.counter("mcp.tool.invocations", "tool", "multi_tool").count());
        assertEquals(3, registry.timer("mcp.tool.duration", "tool", "multi_tool").count());
    }

    @Test
    void Tool名が未指定の場合メソッド名がフォールバックされる() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("fallbackMethod", "");
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.measureToolExecution(joinPoint);

        assertEquals(1.0, registry.counter("mcp.tool.invocations", "tool", "fallbackMethod").count());
    }

    // テスト用のモックメソッド（@Toolアノテーション付き）
    @Tool(name = "sample_tool", description = "test")
    public String sampleTool() { return ""; }

    @Tool(name = "failing_tool", description = "test")
    public String failingTool() { return ""; }

    @Tool(name = "multi_tool", description = "test")
    public String multiTool() { return ""; }

    @Tool(name = "", description = "test")
    public String fallbackMethod() { return ""; }

    private ProceedingJoinPoint mockJoinPoint(String methodName, String toolName) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = this.getClass().getMethod(methodName);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(methodName);

        return joinPoint;
    }
}

package com.tis.nablarch.mcp.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link McpToolLoggingAspect} の単体テスト。
 */
class McpToolLoggingAspectTest {

    private McpToolLoggingAspect aspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    void setUp() {
        aspect = new McpToolLoggingAspect();
        aspectLogger = (Logger) LoggerFactory.getLogger(McpToolLoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        aspectLogger.detachAppender(listAppender);
        MDC.clear();
    }

    @Test
    void 正常実行時にINFOログが記録される() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("logTestTool",
                new String[]{"keyword"}, new Object[]{"nablarch"});
        when(joinPoint.proceed()).thenReturn("search result");

        MDC.put(CorrelationIdFilter.MDC_KEY, "test-request-123");
        aspect.logToolExecution(joinPoint);

        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(Level.INFO, event.getLevel());
        String msg = event.getFormattedMessage();
        assertTrue(msg.contains("log_test_tool"), "ツール名を含む: " + msg);
        assertTrue(msg.contains("test-request-123"), "リクエストIDを含む: " + msg);
        assertTrue(msg.contains("nablarch"), "パラメータを含む: " + msg);
        assertTrue(msg.contains("responseSize="), "レスポンスサイズを含む: " + msg);
        assertTrue(msg.contains("durationMs="), "処理時間を含む: " + msg);
    }

    @Test
    void 例外発生時にERRORログが記録される() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("logErrorTool",
                new String[]{"query"}, new Object[]{"test"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

        assertThrows(RuntimeException.class, () -> aspect.logToolExecution(joinPoint));

        assertEquals(1, listAppender.list.size());
        ILoggingEvent event = listAppender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("test error"));
    }

    @Test
    void 長いパラメータは切り詰められる() throws Throwable {
        String longParam = "a".repeat(200);
        ProceedingJoinPoint joinPoint = mockJoinPoint("logTruncateTool",
                new String[]{"data"}, new Object[]{longParam});
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.logToolExecution(joinPoint);

        String msg = listAppender.list.get(0).getFormattedMessage();
        assertTrue(msg.contains("...(truncated)"), "切り詰めマーカーを含む: " + msg);
        assertFalse(msg.contains(longParam), "元の長い文字列を含まない");
    }

    @Test
    void requestIdがnullでもログが記録される() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("logNoIdTool",
                new String[]{"q"}, new Object[]{"test"});
        when(joinPoint.proceed()).thenReturn("ok");

        // MDC未設定
        aspect.logToolExecution(joinPoint);

        assertEquals(1, listAppender.list.size());
        assertTrue(listAppender.list.get(0).getFormattedMessage().contains("requestId=null"));
    }

    // テスト用の@Toolアノテーション付きメソッド
    @Tool(name = "log_test_tool", description = "test")
    public String logTestTool(String keyword) { return ""; }

    @Tool(name = "log_error_tool", description = "test")
    public String logErrorTool(String query) { return ""; }

    @Tool(name = "log_truncate_tool", description = "test")
    public String logTruncateTool(String data) { return ""; }

    @Tool(name = "log_no_id_tool", description = "test")
    public String logNoIdTool(String q) { return ""; }

    private ProceedingJoinPoint mockJoinPoint(String methodName,
            String[] paramNames, Object[] args) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        // メソッド名からパラメータ型を推定（全てString）
        Class<?>[] paramTypes = new Class[args.length];
        java.util.Arrays.fill(paramTypes, String.class);
        Method method = this.getClass().getMethod(methodName, paramTypes);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }
}

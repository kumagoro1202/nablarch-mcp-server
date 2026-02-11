package com.tis.nablarch.mcp.http;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * MCP Tool呼び出しのメトリクスを記録するAOPアスペクト。
 *
 * <p>HTTPプロファイルでのみ有効。以下のメトリクスを記録する:</p>
 * <ul>
 *   <li>{@code mcp.tool.invocations} - Tool呼び出し回数（Counter）</li>
 *   <li>{@code mcp.tool.duration} - Tool処理時間（Timer）</li>
 *   <li>{@code mcp.tool.errors} - Toolエラー回数（Counter）</li>
 * </ul>
 */
@Aspect
@Component
@Profile("http")
public class McpToolMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(McpToolMetricsAspect.class);

    private final MeterRegistry meterRegistry;

    /**
     * コンストラクタ。
     *
     * @param meterRegistry Micrometerレジストリ
     */
    public McpToolMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * {@link Tool}アノテーション付きメソッドの実行をインターセプトし、メトリクスを記録する。
     *
     * @param joinPoint ジョインポイント
     * @return メソッドの戻り値
     * @throws Throwable メソッド実行時の例外
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object measureToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = resolveToolName(joinPoint);

        Counter.builder("mcp.tool.invocations")
                .tag("tool", toolName)
                .description("MCP Tool呼び出し回数")
                .register(meterRegistry)
                .increment();

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Object result = joinPoint.proceed();
            sample.stop(Timer.builder("mcp.tool.duration")
                    .tag("tool", toolName)
                    .description("MCP Tool処理時間")
                    .register(meterRegistry));
            return result;
        } catch (Throwable ex) {
            sample.stop(Timer.builder("mcp.tool.duration")
                    .tag("tool", toolName)
                    .description("MCP Tool処理時間")
                    .register(meterRegistry));

            Counter.builder("mcp.tool.errors")
                    .tag("tool", toolName)
                    .tag("exception", ex.getClass().getSimpleName())
                    .description("MCP Toolエラー回数")
                    .register(meterRegistry)
                    .increment();

            throw ex;
        }
    }

    /**
     * {@link Tool}アノテーションからツール名を解決する。
     * name属性が未指定の場合はメソッド名をフォールバックとして使用する。
     */
    private String resolveToolName(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Tool toolAnnotation = signature.getMethod().getAnnotation(Tool.class);
            if (toolAnnotation != null && !toolAnnotation.name().isEmpty()) {
                return toolAnnotation.name();
            }
        } catch (Exception e) {
            log.warn("Tool名の解決に失敗: {}", e.getMessage());
        }
        return joinPoint.getSignature().getName();
    }
}

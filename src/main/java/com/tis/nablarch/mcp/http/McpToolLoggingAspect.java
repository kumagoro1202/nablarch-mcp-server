package com.tis.nablarch.mcp.http;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * MCP Tool呼び出しの構造化ログを記録するAOPアスペクト。
 *
 * <p>HTTPプロファイルでのみ有効。JSON構造化ログ（logback-spring.xml）と
 * 相関ID（{@link CorrelationIdFilter}）と連携して、Tool呼び出しの
 * トレーサビリティを提供する。</p>
 *
 * <p>セキュリティ: パラメータ値は100文字で切り詰め、レスポンス本体はログしない（サイズのみ）。</p>
 */
@Aspect
@Component
@Profile("http")
public class McpToolLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(McpToolLoggingAspect.class);
    private static final int MAX_PARAM_LENGTH = 100;

    /**
     * {@link Tool}アノテーション付きメソッドの実行をインターセプトし、構造化ログを記録する。
     *
     * @param joinPoint ジョインポイント
     * @return メソッドの戻り値
     * @throws Throwable メソッド実行時の例外
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object logToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = resolveToolName(joinPoint);
        String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
        String sanitizedParams = sanitizeParams(joinPoint);

        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            int responseSize = estimateSize(result);

            log.info("MCP Tool実行完了: tool={}, requestId={}, params={}, responseSize={}, durationMs={}",
                    toolName, requestId, sanitizedParams, responseSize, durationMs);

            return result;
        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            log.error("MCP Tool実行エラー: tool={}, requestId={}, params={}, error={}, durationMs={}",
                    toolName, requestId, sanitizedParams, ex.getMessage(), durationMs);

            throw ex;
        }
    }

    /**
     * {@link Tool}アノテーションからツール名を解決する。
     */
    private String resolveToolName(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Tool toolAnnotation = signature.getMethod().getAnnotation(Tool.class);
            if (toolAnnotation != null && !toolAnnotation.name().isEmpty()) {
                return toolAnnotation.name();
            }
        } catch (Exception e) {
            // フォールバック
        }
        return joinPoint.getSignature().getName();
    }

    /**
     * パラメータをサニタイズして文字列に変換する。
     * 各パラメータは最大100文字に切り詰められる。
     */
    private String sanitizeParams(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (paramNames == null || args == null || args.length == 0) {
                return "{}";
            }

            var sb = new StringBuilder("{");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                String name = (paramNames.length > i) ? paramNames[i] : "arg" + i;
                String value = truncate(String.valueOf(args[i]));
                sb.append(name).append("=").append(value);
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "{error: sanitization failed}";
        }
    }

    /**
     * 文字列を最大長で切り詰める。
     */
    private String truncate(String value) {
        if (value == null) return "null";
        if (value.length() <= MAX_PARAM_LENGTH) return value;
        return value.substring(0, MAX_PARAM_LENGTH) + "...(truncated)";
    }

    /**
     * レスポンスのバイトサイズを推定する。
     */
    private int estimateSize(Object result) {
        if (result == null) return 0;
        return result.toString().getBytes(StandardCharsets.UTF_8).length;
    }
}

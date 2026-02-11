package com.tis.nablarch.mcp.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * リクエスト相関IDフィルタ。
 *
 * <p>各HTTPリクエストに一意のリクエストIDを付与し、
 * MDC経由で構造化ログに出力する。
 * リクエストヘッダ {@code X-Request-Id} があればそれを使用し、
 * なければUUIDを自動生成する。</p>
 *
 * <p>HTTPプロファイルでのみ有効。STDIOモードでは不要。</p>
 */
@Component
@Profile("http")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Request-Id";
    static final String MDC_KEY = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = extractOrGenerateRequestId(request);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            // セキュリティ: 長すぎる値や制御文字を防止
            String sanitized = headerValue.strip();
            if (sanitized.length() > MAX_REQUEST_ID_LENGTH) {
                sanitized = sanitized.substring(0, MAX_REQUEST_ID_LENGTH);
            }
            // 英数字・ハイフン・アンダースコアのみ許可
            if (sanitized.matches("[a-zA-Z0-9\\-_]+")) {
                return sanitized;
            }
        }
        return UUID.randomUUID().toString();
    }
}

package com.tis.nablarch.mcp.http;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CorrelationIdFilter} の単体テスト。
 */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void ヘッダなしの場合UUIDが自動生成される() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String responseId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseId);
        // UUID形式: 8-4-4-4-12
        assertTrue(responseId.matches("[a-f0-9\\-]{36}"), "UUID形式でない: " + responseId);
        // フィルタ完了後にMDCがクリアされている
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void ヘッダ指定時はその値が使用される() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "custom-request-123");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals("custom-request-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void 長すぎるリクエストIDは切り詰められる() throws Exception {
        var request = new MockHttpServletRequest();
        String longId = "a".repeat(100);
        request.addHeader(CorrelationIdFilter.HEADER_NAME, longId);
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String responseId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertEquals(64, responseId.length());
    }

    @Test
    void 不正文字を含むリクエストIDは新規生成される() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "bad<script>id");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String responseId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotEquals("bad<script>id", responseId);
        // 代わりにUUIDが生成される
        assertTrue(responseId.matches("[a-f0-9\\-]{36}"));
    }

    @Test
    void 空白のみのヘッダは新規生成される() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   ");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        String responseId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertTrue(responseId.matches("[a-f0-9\\-]{36}"));
    }

    @Test
    void フィルタチェーン内でMDCにrequestIdが設定されている() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "test-id-456");
        var response = new MockHttpServletResponse();

        // フィルタチェーン内でMDCを検証
        final String[] capturedMdcValue = {null};
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req,
                    jakarta.servlet.ServletResponse res) {
                capturedMdcValue[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
            }
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals("test-id-456", capturedMdcValue[0]);
        // フィルタ完了後はクリア
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}

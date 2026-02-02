package com.tis.nablarch.mcp.rag.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SearchResult} のユニットテスト。
 */
class SearchResultTest {

    @Test
    @DisplayName("正常系: 全フィールドが正しく保持される")
    void allFieldsPreserved() {
        Map<String, String> metadata = Map.of("source", "github", "app_type", "web");
        SearchResult result = new SearchResult("id1", "内容", 0.95, metadata, "https://example.com");

        assertAll(
                () -> assertEquals("id1", result.id()),
                () -> assertEquals("内容", result.content()),
                () -> assertEquals(0.95, result.score()),
                () -> assertEquals("github", result.metadata().get("source")),
                () -> assertEquals("https://example.com", result.sourceUrl())
        );
    }

    @Test
    @DisplayName("正常系: metadataがnullの場合はnullが保持される")
    void nullMetadataAllowed() {
        SearchResult result = new SearchResult("id1", "内容", 0.5, null, null);
        assertNull(result.metadata());
    }

    @Test
    @DisplayName("正常系: metadataが不変コピーになる")
    void metadataIsImmutableCopy() {
        Map<String, String> original = new HashMap<>();
        original.put("key", "value");
        SearchResult result = new SearchResult("id1", "内容", 0.5, original, null);

        assertThrows(UnsupportedOperationException.class,
                () -> result.metadata().put("new", "value"));
    }

    @Test
    @DisplayName("異常系: idがnullの場合はNullPointerException")
    void nullIdThrows() {
        assertThrows(NullPointerException.class,
                () -> new SearchResult(null, "内容", 0.5, null, null));
    }

    @Test
    @DisplayName("異常系: contentがnullの場合はNullPointerException")
    void nullContentThrows() {
        assertThrows(NullPointerException.class,
                () -> new SearchResult("id1", null, 0.5, null, null));
    }
}

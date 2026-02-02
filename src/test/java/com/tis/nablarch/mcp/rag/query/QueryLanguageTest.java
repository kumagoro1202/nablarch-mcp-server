package com.tis.nablarch.mcp.rag.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link QueryLanguage}のテスト。
 *
 * <p>enum値の存在と値一覧を検証する。</p>
 */
class QueryLanguageTest {

    @Test
    void JAPANESE値が存在する() {
        assertNotNull(QueryLanguage.JAPANESE);
        assertEquals("JAPANESE", QueryLanguage.JAPANESE.name());
    }

    @Test
    void ENGLISH値が存在する() {
        assertNotNull(QueryLanguage.ENGLISH);
        assertEquals("ENGLISH", QueryLanguage.ENGLISH.name());
    }

    @Test
    void MIXED値が存在する() {
        assertNotNull(QueryLanguage.MIXED);
        assertEquals("MIXED", QueryLanguage.MIXED.name());
    }

    @Test
    void enum値は3つである() {
        assertEquals(3, QueryLanguage.values().length);
    }

    @Test
    void valueOfで正しく変換できる() {
        assertEquals(QueryLanguage.JAPANESE, QueryLanguage.valueOf("JAPANESE"));
        assertEquals(QueryLanguage.ENGLISH, QueryLanguage.valueOf("ENGLISH"));
        assertEquals(QueryLanguage.MIXED, QueryLanguage.valueOf("MIXED"));
    }

    @Test
    void 不正な値でvalueOfは例外をスローする() {
        assertThrows(IllegalArgumentException.class, () ->
                QueryLanguage.valueOf("INVALID"));
    }
}

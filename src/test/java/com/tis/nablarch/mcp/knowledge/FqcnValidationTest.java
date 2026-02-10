package com.tis.nablarch.mcp.knowledge;

import com.tis.nablarch.mcp.knowledge.model.HandlerCatalogEntry;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 知識YAML内のFQCN（完全修飾クラス名）の妥当性を自動検証するテスト。
 *
 * <p>handler-catalog.yamlに記載されたFQCNが正しいJava FQCNフォーマットであること、
 * nablarchパッケージに属すること、重複がないことを検証する。</p>
 */
@DisplayName("FQCN自動検証テスト")
class FqcnValidationTest {

    /**
     * Java FQCNのパターン: パッケージ.クラス名（ネストクラス$含む）。
     */
    private static final Pattern FQCN_PATTERN =
            Pattern.compile("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*\\.[A-Z][A-Za-z0-9]*(\\$[A-Z][A-Za-z0-9]*)*$");

    private static Map<String, HandlerCatalogEntry> catalog;

    @BeforeAll
    static void loadCatalog() throws Exception {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try (InputStream is = FqcnValidationTest.class.getResourceAsStream("/knowledge/handler-catalog.yaml")) {
            assertNotNull(is, "handler-catalog.yaml がクラスパスに存在すること");
            catalog = yaml.readValue(is, new TypeReference<>() {});
        }
    }

    /**
     * 全アプリタイプ×全ハンドラのFQCNエントリをパラメータ化テストのソースとして提供。
     */
    static Stream<FqcnEntry> allFqcnEntries() {
        List<FqcnEntry> entries = new ArrayList<>();
        for (Map.Entry<String, HandlerCatalogEntry> e : catalog.entrySet()) {
            String appType = e.getKey();
            HandlerCatalogEntry catalogEntry = e.getValue();
            if (catalogEntry.handlers == null) continue;
            for (HandlerEntry h : catalogEntry.handlers) {
                entries.add(new FqcnEntry(appType, h.name, h.fqcn));
            }
        }
        return entries.stream();
    }

    @ParameterizedTest(name = "[{0}] {1}: {2}")
    @MethodSource("allFqcnEntries")
    @DisplayName("FQCNがJava命名規約に準拠していること")
    void fqcnMatchesJavaConvention(FqcnEntry entry) {
        assertNotNull(entry.fqcn(), entry.name() + " のFQCNがnullでないこと");
        assertFalse(entry.fqcn().isBlank(), entry.name() + " のFQCNが空でないこと");
        assertTrue(FQCN_PATTERN.matcher(entry.fqcn()).matches(),
                entry.name() + " のFQCN '" + entry.fqcn() + "' がJava FQCNフォーマットに準拠すること");
    }

    @ParameterizedTest(name = "[{0}] {1}: {2}")
    @MethodSource("allFqcnEntries")
    @DisplayName("FQCNがnablarchパッケージに属すること")
    void fqcnBelongsToNablarchPackage(FqcnEntry entry) {
        assertTrue(entry.fqcn().startsWith("nablarch."),
                entry.name() + " のFQCN '" + entry.fqcn() + "' がnablarchパッケージに属すること");
    }

    @Test
    @DisplayName("全アプリタイプに1つ以上のハンドラが定義されていること")
    void eachAppTypeHasHandlers() {
        assertFalse(catalog.isEmpty(), "カタログが空でないこと");
        for (Map.Entry<String, HandlerCatalogEntry> e : catalog.entrySet()) {
            assertNotNull(e.getValue().handlers,
                    e.getKey() + " のhandlersがnullでないこと");
            assertFalse(e.getValue().handlers.isEmpty(),
                    e.getKey() + " に1つ以上のハンドラが定義されていること");
        }
    }

    @Test
    @DisplayName("ハンドラ名がアプリタイプ内で一意であること")
    void handlerNamesUniqueWithinAppType() {
        for (Map.Entry<String, HandlerCatalogEntry> e : catalog.entrySet()) {
            String appType = e.getKey();
            if (e.getValue().handlers == null) continue;

            Set<String> names = new HashSet<>();
            for (HandlerEntry h : e.getValue().handlers) {
                assertTrue(names.add(h.name),
                        appType + " 内でハンドラ名 '" + h.name + "' が重複していないこと");
            }
        }
    }

    @Test
    @DisplayName("orderフィールドが正の連番であること")
    void orderFieldsArePositiveSequence() {
        for (Map.Entry<String, HandlerCatalogEntry> e : catalog.entrySet()) {
            if (e.getValue().handlers == null) continue;
            int expectedOrder = 1;
            for (HandlerEntry h : e.getValue().handlers) {
                assertEquals(expectedOrder, h.order,
                        e.getKey() + " のハンドラ '" + h.name + "' のorderが" + expectedOrder + "であること");
                expectedOrder++;
            }
        }
    }

    /**
     * パラメータ化テスト用のFQCNエントリレコード。
     */
    record FqcnEntry(String appType, String name, String fqcn) {
        @Override
        public String toString() {
            return appType + "/" + name;
        }
    }
}

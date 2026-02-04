package com.tis.nablarch.mcp.codegen;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCodeGeneratorTest {

    @Mock
    private NablarchKnowledgeBase knowledgeBase;

    private DefaultCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DefaultCodeGenerator(knowledgeBase);
        when(knowledgeBase.search(anyString(), anyString())).thenReturn(List.of());
    }

    @Nested
    class Actionテスト {
        @Test
        void Web用アクションを生成できる() {
            GenerationResult result = generator.generate("action", "Product", "web", Map.of());
            assertEquals(1, result.files().size());
            assertTrue(result.files().get(0).content().contains("ProductAction"));
        }

        @Test
        void REST用アクションを生成できる() {
            GenerationResult result = generator.generate("action", "Product", "rest", Map.of());
            assertTrue(result.files().get(0).content().contains("@GET"));
        }

        @Test
        void Batch用アクションを生成できる() {
            GenerationResult result = generator.generate("action", "Product", "batch", Map.of());
            assertTrue(result.files().get(0).content().contains("BatchAction"));
        }

        @Test
        void Messaging用アクションを生成できる() {
            GenerationResult result = generator.generate("action", "Product", "messaging", Map.of());
            assertTrue(result.files().get(0).content().contains("MessagingAction"));
        }
    }

    @Nested
    class 他タイプテスト {
        @Test
        void フォームを生成できる() {
            GenerationResult result = generator.generate("form", "Product", "web", Map.of());
            assertEquals("ProductForm.java", result.files().get(0).fileName());
        }

        @Test
        void SQL定義ファイルを生成できる() {
            GenerationResult result = generator.generate("sql", "Product", "web", Map.of());
            assertTrue(result.files().get(0).content().contains("FIND_ALL"));
        }

        @Test
        void エンティティを生成できる() {
            GenerationResult result = generator.generate("entity", "Product", "web", Map.of());
            assertTrue(result.files().get(0).content().contains("@Entity"));
        }

        @Test
        void ハンドラを生成できる() {
            GenerationResult result = generator.generate("handler", "AuditLog", "web", Map.of());
            assertTrue(result.files().get(0).content().contains("Handler<I, O>"));
        }

        @Test
        void インターセプタを生成できる() {
            GenerationResult result = generator.generate("interceptor", "AuditLog", "web", Map.of());
            assertEquals(2, result.files().size());
        }
    }
}

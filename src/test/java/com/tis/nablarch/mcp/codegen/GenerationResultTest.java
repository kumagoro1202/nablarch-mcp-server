package com.tis.nablarch.mcp.codegen;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GenerationResultTest {

    @Test
    void コンストラクタとアクセサが正しく動作する() {
        GenerationResult.GeneratedFile file = new GenerationResult.GeneratedFile(
                "path", "ProductAction.java", "content", "java");
        GenerationResult result = new GenerationResult(
                "action", "Product", "web", List.of(file),
                List.of("規約"), List.of("dep"), List.of());
        assertEquals("action", result.type());
        assertEquals(1, result.files().size());
    }

    @Test
    void 同じ内容のrecordはequalsがtrue() {
        GenerationResult r1 = new GenerationResult("form", "Product", "web", List.of(), List.of(), List.of(), List.of());
        GenerationResult r2 = new GenerationResult("form", "Product", "web", List.of(), List.of(), List.of(), List.of());
        assertEquals(r1, r2);
    }
}

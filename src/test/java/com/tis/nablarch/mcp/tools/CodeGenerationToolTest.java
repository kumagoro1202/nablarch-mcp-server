package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.codegen.CodeGenerator;
import com.tis.nablarch.mcp.codegen.GenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeGenerationToolTest {

    @Mock
    private CodeGenerator codeGenerator;

    private CodeGenerationTool tool;

    @BeforeEach
    void setUp() {
        tool = new CodeGenerationTool(codeGenerator);
    }

    @Nested
    class 入力バリデーションテスト {
        @ParameterizedTest
        @NullAndEmptySource
        void typeがnullまたは空の場合エラー(String type) {
            String result = tool.generateCode(type, "Product", "web", null);
            assertTrue(result.contains("生成対象タイプを指定"));
            verifyNoInteractions(codeGenerator);
        }

        @ParameterizedTest
        @NullAndEmptySource
        void nameがnullまたは空の場合エラー(String name) {
            String result = tool.generateCode("action", name, "web", null);
            assertTrue(result.contains("名前を指定"));
        }

        @Test
        void 不正なtypeはエラー() {
            String result = tool.generateCode("invalid", "Product", "web", null);
            assertTrue(result.contains("不正な生成対象タイプ"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"action", "form", "sql", "entity", "handler", "interceptor"})
        void 有効なtypeは受け付ける(String type) {
            when(codeGenerator.generate(eq(type), anyString(), anyString(), any()))
                    .thenReturn(new GenerationResult(type, "P", "web", List.of(), List.of(), List.of(), List.of()));
            tool.generateCode(type, "Product", "web", null);
            verify(codeGenerator).generate(eq(type), anyString(), anyString(), any());
        }
    }

    @Nested
    class 出力フォーマットテスト {
        @Test
        void Markdown形式でフォーマットされる() {
            when(codeGenerator.generate(anyString(), anyString(), anyString(), any()))
                    .thenReturn(new GenerationResult("action", "Product", "web",
                            List.of(new GenerationResult.GeneratedFile("p", "f.java", "code", "java")),
                            List.of("規約"), List.of("dep"), List.of()));
            String result = tool.generateCode("action", "Product", "web", null);
            assertTrue(result.contains("## 生成結果: Product"));
        }
    }

    @Nested
    class エラーハンドリングテスト {
        @Test
        void 例外発生時はエラーメッセージ() {
            when(codeGenerator.generate(anyString(), anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("error"));
            String result = tool.generateCode("action", "Product", "web", null);
            assertTrue(result.contains("エラーが発生"));
        }
    }
}

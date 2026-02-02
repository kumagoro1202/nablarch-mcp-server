package com.tis.nablarch.mcp.db.repository;

import com.tis.nablarch.mcp.db.entity.CodeChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CodeChunkRepository} のユニットテスト。
 *
 * <p>H2データベースを使用したCRUD操作およびメタデータ検索のテスト。
 * ベクトル類似度検索はpgvectorが必要なため、統合テストで別途テストする。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
class CodeChunkRepositoryTest {

    @Autowired
    private CodeChunkRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 保存と取得ができること() {
        CodeChunk chunk = createCodeChunk(
                "public class HttpResponse {}", "nablarch-fw-web",
                "src/main/java/nablarch/fw/web/HttpResponse.java");

        CodeChunk saved = repository.save(chunk);
        assertNotNull(saved.getId());

        Optional<CodeChunk> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("public class HttpResponse {}", found.get().getContent());
    }

    @Test
    void リポジトリ名で検索できること() {
        repository.save(createCodeChunk("code1", "nablarch-fw-web", "src/A.java"));
        repository.save(createCodeChunk("code2", "nablarch-fw-web", "src/B.java"));
        repository.save(createCodeChunk("code3", "nablarch-core", "src/C.java"));

        List<CodeChunk> results = repository.findByRepo("nablarch-fw-web");
        assertEquals(2, results.size());
    }

    @Test
    void モジュール名で検索できること() {
        CodeChunk chunk1 = createCodeChunk("code1", "nablarch-fw-web", "src/A.java");
        chunk1.setModule("nablarch-fw-web");
        repository.save(chunk1);

        CodeChunk chunk2 = createCodeChunk("code2", "nablarch-core", "src/B.java");
        chunk2.setModule("nablarch-core-repository");
        repository.save(chunk2);

        List<CodeChunk> results = repository.findByModule("nablarch-fw-web");
        assertEquals(1, results.size());
    }

    @Test
    void チャンク種別で検索できること() {
        CodeChunk chunk1 = createCodeChunk("class body", "repo", "A.java");
        chunk1.setChunkType("class");
        repository.save(chunk1);

        CodeChunk chunk2 = createCodeChunk("method body", "repo", "B.java");
        chunk2.setChunkType("method");
        repository.save(chunk2);

        List<CodeChunk> results = repository.findByChunkType("class");
        assertEquals(1, results.size());
        assertEquals("class body", results.get(0).getContent());
    }

    @Test
    void FQCNで検索できること() {
        CodeChunk chunk = createCodeChunk("code", "repo", "A.java");
        chunk.setFqcn("nablarch.fw.web.HttpResponse");
        repository.save(chunk);

        List<CodeChunk> results = repository.findByFqcn("nablarch.fw.web.HttpResponse");
        assertEquals(1, results.size());
    }

    @Test
    void 作成日時が自動設定されること() {
        CodeChunk chunk = createCodeChunk("test", "repo", "test.java");
        CodeChunk saved = repository.save(chunk);
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void 削除ができること() {
        CodeChunk saved = repository.save(createCodeChunk("to delete", "repo", "del.java"));
        Long id = saved.getId();

        repository.deleteById(id);
        assertFalse(repository.findById(id).isPresent());
    }

    private CodeChunk createCodeChunk(String content, String repo, String filePath) {
        CodeChunk chunk = new CodeChunk();
        chunk.setContent(content);
        chunk.setRepo(repo);
        chunk.setFilePath(filePath);
        chunk.setLanguage("java");
        return chunk;
    }
}

package com.tis.nablarch.mcp.db.repository;

import com.tis.nablarch.mcp.db.entity.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DocumentChunkRepository} のユニットテスト。
 *
 * <p>H2データベースを使用したCRUD操作およびメタデータ検索のテスト。
 * ベクトル類似度検索はpgvectorが必要なため、{@code @Tag("integration")} 付きの
 * 統合テストで別途テストする。</p>
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=always"
})
class DocumentChunkRepositoryTest {

    @Autowired
    private DocumentChunkRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void 保存と取得ができること() {
        DocumentChunk chunk = createDocumentChunk(
                "Nablarchのハンドラキューは...", "nablarch-document", "documentation",
                "nablarch-fw-web", "web");

        DocumentChunk saved = repository.save(chunk);
        assertNotNull(saved.getId());

        Optional<DocumentChunk> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Nablarchのハンドラキューは...", found.get().getContent());
        assertEquals("nablarch-document", found.get().getSource());
    }

    @Test
    void ソース種別で検索できること() {
        repository.save(createDocumentChunk("doc1", "nablarch-document", "documentation", null, null));
        repository.save(createDocumentChunk("doc2", "nablarch-document", "documentation", null, null));
        repository.save(createDocumentChunk("doc3", "github", "code", null, null));

        List<DocumentChunk> results = repository.findBySource("nablarch-document");
        assertEquals(2, results.size());
    }

    @Test
    void モジュール名で検索できること() {
        repository.save(createDocumentChunk("doc1", "nablarch-document", "documentation",
                "nablarch-fw-web", null));
        repository.save(createDocumentChunk("doc2", "nablarch-document", "documentation",
                "nablarch-core-repository", null));

        List<DocumentChunk> results = repository.findByModule("nablarch-fw-web");
        assertEquals(1, results.size());
        assertEquals("doc1", results.get(0).getContent());
    }

    @Test
    void アプリタイプで検索できること() {
        repository.save(createDocumentChunk("web doc", "nablarch-document", "documentation", null, "web"));
        repository.save(createDocumentChunk("batch doc", "nablarch-document", "documentation", null, "batch"));

        List<DocumentChunk> results = repository.findByAppType("web");
        assertEquals(1, results.size());
        assertEquals("web doc", results.get(0).getContent());
    }

    @Test
    void FQCNで検索できること() {
        DocumentChunk chunk = createDocumentChunk("handler doc", "javadoc", "javadoc", null, null);
        chunk.setFqcn("nablarch.fw.web.HttpResponse");
        repository.save(chunk);

        List<DocumentChunk> results = repository.findByFqcn("nablarch.fw.web.HttpResponse");
        assertEquals(1, results.size());
    }

    @Test
    void 更新日時が自動設定されること() {
        DocumentChunk chunk = createDocumentChunk("test", "nablarch-document", "documentation", null, null);
        DocumentChunk saved = repository.save(chunk);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void 削除ができること() {
        DocumentChunk saved = repository.save(
                createDocumentChunk("to delete", "nablarch-document", "documentation", null, null));
        Long id = saved.getId();

        repository.deleteById(id);
        assertFalse(repository.findById(id).isPresent());
    }

    private DocumentChunk createDocumentChunk(String content, String source, String sourceType,
                                               String module, String appType) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setContent(content);
        chunk.setSource(source);
        chunk.setSourceType(sourceType);
        chunk.setModule(module);
        chunk.setAppType(appType);
        chunk.setNablarchVersion("6u3");
        return chunk;
    }
}

package com.tis.nablarch.mcp.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * コードチャンクエンティティ。
 *
 * <p>Nablarchソースコード（Java, XML, SQL等）のテキストチャンクとメタデータを格納する。</p>
 *
 * <p>embeddingカラム（pgvector vector(1024)型）はJPA標準マッピングの対象外とし、
 * ネイティブクエリ経由で操作する。</p>
 */
@Entity
@Table(name = "code_chunks")
public class CodeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** コードチャンクテキスト本文 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** リポジトリ名 */
    @Column(nullable = false, length = 100)
    private String repo;

    /** ファイルパス */
    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    /** 完全修飾クラス名 */
    @Column(length = 300)
    private String fqcn;

    /** チャンク種別（class, method, config, test） */
    @Column(name = "chunk_type", length = 20)
    private String chunkType;

    /** プログラミング言語（java, xml, sql, properties） */
    @Column(length = 10)
    private String language;

    /** Mavenモジュール名 */
    @Column(length = 100)
    private String module;

    /** 作成日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CodeChunk() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getter / Setter ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFqcn() {
        return fqcn;
    }

    public void setFqcn(String fqcn) {
        this.fqcn = fqcn;
    }

    public String getChunkType() {
        return chunkType;
    }

    public void setChunkType(String chunkType) {
        this.chunkType = chunkType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

package com.tis.nablarch.mcp.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * ドキュメントチャンクエンティティ。
 *
 * <p>Nablarchドキュメント（公式ドキュメント、Javadoc、設定標準等）の
 * テキストチャンクとメタデータを格納する。</p>
 *
 * <p>embeddingカラム（pgvector vector(1024)型）はJPA標準マッピングの対象外とし、
 * ネイティブクエリ経由で操作する。</p>
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** チャンクテキスト本文 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** ソース種別（nablarch-document, github, fintan, javadoc） */
    @Column(nullable = false, length = 50)
    private String source;

    /** コンテンツ種別（documentation, code, javadoc, config, standard） */
    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    /** Mavenモジュール名（例: nablarch-core-repository） */
    @Column(length = 100)
    private String module;

    /** アプリタイプ（web, rest, batch, messaging, common） */
    @Column(name = "app_type", length = 20)
    private String appType;

    /** 言語（ja, en） */
    @Column(length = 5)
    private String language;

    /** 完全修飾クラス名 */
    @Column(length = 300)
    private String fqcn;

    /** ソースURL */
    @Column(columnDefinition = "TEXT")
    private String url;

    /** リポジトリ内ファイルパス */
    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    /** Nablarchバージョン */
    @Column(name = "nablarch_version", length = 10)
    private String nablarchVersion = "6u3";

    /** 作成日時 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新日時 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DocumentChunk() {
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFqcn() {
        return fqcn;
    }

    public void setFqcn(String fqcn) {
        this.fqcn = fqcn;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getNablarchVersion() {
        return nablarchVersion;
    }

    public void setNablarchVersion(String nablarchVersion) {
        this.nablarchVersion = nablarchVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

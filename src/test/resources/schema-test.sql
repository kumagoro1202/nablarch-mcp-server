-- H2互換テスト用スキーマ
-- pgvectorのvector型はH2に存在しないため、embeddingカラムを除外

CREATE TABLE IF NOT EXISTS document_chunks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    content         CLOB            NOT NULL,
    source          VARCHAR(50)     NOT NULL,
    source_type     VARCHAR(20)     NOT NULL,
    module          VARCHAR(100),
    app_type        VARCHAR(20),
    language        VARCHAR(5),
    fqcn            VARCHAR(300),
    url             CLOB,
    file_path       CLOB,
    nablarch_version VARCHAR(10)    DEFAULT '6u3',
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS code_chunks (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    content         CLOB            NOT NULL,
    repo            VARCHAR(100)    NOT NULL,
    file_path       CLOB            NOT NULL,
    fqcn            VARCHAR(300),
    chunk_type      VARCHAR(20),
    language        VARCHAR(10),
    module          VARCHAR(100),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

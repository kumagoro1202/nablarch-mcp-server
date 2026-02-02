-- V1: ベクトルDBテーブル作成
-- pgvector拡張はdocker-entrypoint-initdb.dで作成済み

-- ドキュメントチャンクテーブル
CREATE TABLE document_chunks (
    id              BIGSERIAL       PRIMARY KEY,
    content         TEXT            NOT NULL,
    embedding       vector(1024),
    source          VARCHAR(50)     NOT NULL,
    source_type     VARCHAR(20)     NOT NULL,
    module          VARCHAR(100),
    app_type        VARCHAR(20),
    language        VARCHAR(5),
    fqcn            VARCHAR(300),
    url             TEXT,
    file_path       TEXT,
    section_hierarchy TEXT[],
    nablarch_version VARCHAR(10)    DEFAULT '6u3',
    created_at      TIMESTAMP       DEFAULT NOW(),
    updated_at      TIMESTAMP       DEFAULT NOW()
);

-- コードチャンクテーブル
CREATE TABLE code_chunks (
    id              BIGSERIAL       PRIMARY KEY,
    content         TEXT            NOT NULL,
    embedding       vector(1024),
    repo            VARCHAR(100)    NOT NULL,
    file_path       TEXT            NOT NULL,
    fqcn            VARCHAR(300),
    chunk_type      VARCHAR(20),
    language        VARCHAR(10),
    module          VARCHAR(100),
    created_at      TIMESTAMP       DEFAULT NOW()
);

-- ベクトルインデックス（IVFFlat, コサイン類似度）
CREATE INDEX idx_doc_chunks_embedding ON document_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX idx_code_chunks_embedding ON code_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Full Text Search インデックス（ハイブリッド検索のBM25部分）
CREATE INDEX idx_doc_chunks_content_fts ON document_chunks
    USING gin(to_tsvector('english', content));

CREATE INDEX idx_code_chunks_content_fts ON code_chunks
    USING gin(to_tsvector('english', content));

-- メタデータフィルタリング用インデックス（document_chunks）
CREATE INDEX idx_doc_chunks_source ON document_chunks (source);
CREATE INDEX idx_doc_chunks_app_type ON document_chunks (app_type);
CREATE INDEX idx_doc_chunks_module ON document_chunks (module);
CREATE INDEX idx_doc_chunks_fqcn ON document_chunks (fqcn);

-- メタデータフィルタリング用インデックス（code_chunks）
CREATE INDEX idx_code_chunks_repo ON code_chunks (repo);
CREATE INDEX idx_code_chunks_chunk_type ON code_chunks (chunk_type);
CREATE INDEX idx_code_chunks_module ON code_chunks (module);
CREATE INDEX idx_code_chunks_fqcn ON code_chunks (fqcn);

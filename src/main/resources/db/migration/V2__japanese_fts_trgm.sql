-- V2: 日本語全文検索対応（pg_trgm）
-- 'english' FTSインデックスは日本語テキストに非対応。
-- pg_trgm（trigram）のGINインデックスに置き換え、ILIKE検索で日本語を正しく検索可能にする。

-- pg_trgm拡張を有効化
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 既存のFTSインデックスを削除
DROP INDEX IF EXISTS idx_doc_chunks_content_fts;
DROP INDEX IF EXISTS idx_code_chunks_content_fts;

-- pg_trgm GINインデックスを作成（ILIKE/LIKE検索を高速化）
CREATE INDEX idx_doc_chunks_content_trgm ON document_chunks
    USING gin(content gin_trgm_ops);

CREATE INDEX idx_code_chunks_content_trgm ON code_chunks
    USING gin(content gin_trgm_ops);

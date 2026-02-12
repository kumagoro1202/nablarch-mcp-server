-- pgvector拡張の有効化
CREATE EXTENSION IF NOT EXISTS vector;

-- pg_trgm拡張の有効化（日本語全文検索用）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

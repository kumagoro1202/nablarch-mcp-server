# ベクトルDBスキーマ設計書 (WBS 2.1.1)

## 概要

Phase 2 RAGエンジンのデータ層として、PostgreSQL + pgvectorを使用する。
ドキュメントチャンクとコードチャンクを別テーブルで管理し、
それぞれ異なるEmbeddingモデル（Jina v4 / Voyage-code-3）で生成した
1024次元ベクトルを格納する。

## テーブル設計

### document_chunks テーブル

Nablarchドキュメント（公式ドキュメント、Javadoc、設定標準等）のチャンクを格納する。

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PRIMARY KEY | チャンクID |
| content | TEXT | NOT NULL | チャンクテキスト |
| embedding | vector(1024) | — | Jina v4 Embeddingベクトル |
| source | VARCHAR(50) | NOT NULL | ソース種別（nablarch-document, github, fintan, javadoc） |
| source_type | VARCHAR(20) | NOT NULL | コンテンツ種別（documentation, code, javadoc, config, standard） |
| module | VARCHAR(100) | — | Mavenモジュール名（例: nablarch-core-repository） |
| app_type | VARCHAR(20) | — | アプリタイプ（web, rest, batch, messaging, common） |
| language | VARCHAR(5) | — | 言語（ja, en） |
| fqcn | VARCHAR(300) | — | 完全修飾クラス名 |
| url | TEXT | — | ソースURL |
| file_path | TEXT | — | リポジトリ内ファイルパス |
| section_hierarchy | TEXT[] | — | セクション階層 |
| nablarch_version | VARCHAR(10) | DEFAULT '6u3' | Nablarchバージョン |
| created_at | TIMESTAMP | DEFAULT NOW() | 作成日時 |
| updated_at | TIMESTAMP | DEFAULT NOW() | 更新日時 |

### code_chunks テーブル

Nablarchソースコード（Java, XML, SQL等）のチャンクを格納する。

| カラム | 型 | 制約 | 説明 |
|--------|-----|------|------|
| id | BIGSERIAL | PRIMARY KEY | チャンクID |
| content | TEXT | NOT NULL | コードチャンクテキスト |
| embedding | vector(1024) | — | Voyage-code-3 Embeddingベクトル |
| repo | VARCHAR(100) | NOT NULL | リポジトリ名 |
| file_path | TEXT | NOT NULL | ファイルパス |
| fqcn | VARCHAR(300) | — | 完全修飾クラス名 |
| chunk_type | VARCHAR(20) | — | チャンク種別（class, method, config, test） |
| language | VARCHAR(10) | — | プログラミング言語（java, xml, sql, properties） |
| module | VARCHAR(100) | — | Mavenモジュール名 |
| created_at | TIMESTAMP | DEFAULT NOW() | 作成日時 |

## インデックス設計

### ベクトルインデックス（IVFFlat）

```sql
-- document_chunks: コサイン類似度ベクトル検索
CREATE INDEX idx_doc_chunks_embedding ON document_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- code_chunks: コサイン類似度ベクトル検索
CREATE INDEX idx_code_chunks_embedding ON code_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
```

IVFFlatを選択した理由:
- 初期データ量（~40,000チャンク）ではIVFFlatで十分な性能
- HNSWはメモリ消費が大きく、Phase 2の規模では過剰
- 将来的にHNSWへの移行は容易

### Full Text Search (FTS) インデックス

```sql
-- ハイブリッド検索のBM25部分を担う
CREATE INDEX idx_doc_chunks_content_fts ON document_chunks
    USING gin(to_tsvector('english', content));

CREATE INDEX idx_code_chunks_content_fts ON code_chunks
    USING gin(to_tsvector('english', content));
```

### メタデータフィルタリング用インデックス

```sql
-- document_chunks
CREATE INDEX idx_doc_chunks_source ON document_chunks (source);
CREATE INDEX idx_doc_chunks_app_type ON document_chunks (app_type);
CREATE INDEX idx_doc_chunks_module ON document_chunks (module);
CREATE INDEX idx_doc_chunks_fqcn ON document_chunks (fqcn);

-- code_chunks
CREATE INDEX idx_code_chunks_repo ON code_chunks (repo);
CREATE INDEX idx_code_chunks_chunk_type ON code_chunks (chunk_type);
CREATE INDEX idx_code_chunks_module ON code_chunks (module);
CREATE INDEX idx_code_chunks_fqcn ON code_chunks (fqcn);
```

## クエリパターン

### ベクトル類似度検索

```sql
SELECT id, content, 1 - (embedding <=> :queryVector) AS similarity
FROM document_chunks
WHERE app_type = :appType
ORDER BY embedding <=> :queryVector
LIMIT :topK;
```

### ハイブリッド検索（BM25 + ベクトル）

```sql
WITH vector_results AS (
    SELECT id, content, 1 - (embedding <=> :queryVector) AS vector_score
    FROM document_chunks
    ORDER BY embedding <=> :queryVector
    LIMIT 50
),
fts_results AS (
    SELECT id, content, ts_rank(to_tsvector('english', content),
           plainto_tsquery('english', :keyword)) AS fts_score
    FROM document_chunks
    WHERE to_tsvector('english', content) @@ plainto_tsquery('english', :keyword)
    LIMIT 50
)
SELECT COALESCE(v.id, f.id) AS id,
       COALESCE(v.content, f.content) AS content,
       COALESCE(v.vector_score, 0) * 0.7 + COALESCE(f.fts_score, 0) * 0.3 AS combined_score
FROM vector_results v
FULL OUTER JOIN fts_results f ON v.id = f.id
ORDER BY combined_score DESC
LIMIT :topK;
```

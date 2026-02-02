# ハイブリッド検索設計書

> **WBS番号**: 2.1.4
> **ステータス**: 設計完了
> **作成日**: 2026-02-02
> **作成者**: ashigaru8 (subtask_056)
> **関連文書**: architecture.md §4.6, ADR-001, wbs.md Phase 2
> **依存タスク**: WBS 2.1.1（ベクトルDBスキーマ設計）

---

## 目次

1. [概要](#1-概要)
2. [設計方針](#2-設計方針)
3. [BM25検索（キーワード検索）](#3-bm25検索キーワード検索)
4. [ベクトル検索（セマンティック検索）](#4-ベクトル検索セマンティック検索)
5. [Reciprocal Rank Fusion (RRF)](#5-reciprocal-rank-fusion-rrf)
6. [重み付けハイブリッドスコアリング](#6-重み付けハイブリッドスコアリング)
7. [検索フロー](#7-検索フロー)
8. [メタデータフィルタリング](#8-メタデータフィルタリング)
9. [パフォーマンス設計](#9-パフォーマンス設計)
10. [インターフェース設計](#10-インターフェース設計)
11. [エラーハンドリング](#11-エラーハンドリング)
12. [Phase 1からの移行](#12-phase-1からの移行)

---

## 1. 概要

### 1.1 目的

本設計書は、Nablarch MCPサーバー Phase 2 における**ハイブリッド検索**の設計を定義する。
ハイブリッド検索は、BM25キーワード検索とベクトルセマンティック検索を組み合わせ、
Reciprocal Rank Fusion (RRF) で統合することにより、単一手法を上回る検索精度を実現する。

### 1.2 背景

Phase 1 では `NablarchKnowledgeBase.java` が大文字小文字無視の部分文字列マッチングによる
キーワード検索を提供している。この方式は以下の限界がある：

- **同義語の非対応**: 「データベースアクセス」で「universal-dao」が見つからない
- **意味的類似性の欠如**: 「REST APIの認証設定」で認証ハンドラの設定例が検索できない
- **日本語/英語混在クエリの低精度**: Nablarchは日本語ドキュメントと英語APIが混在

ハイブリッド検索により、キーワードの正確性（BM25）とセマンティックな理解（ベクトル検索）を
両立し、ADR-001で決定したRAG-enhanced MCPアーキテクチャの中核を実現する。

### 1.3 スコープ

| 対象 | 内容 |
|------|------|
| 検索対象テーブル | `document_chunks`, `code_chunks` |
| 検索アルゴリズム | BM25 (PostgreSQL FTS), ベクトルコサイン類似度 (pgvector) |
| 統合方式 | Reciprocal Rank Fusion (RRF) + 重み付けスコアリング |
| フィルタリング | app_type, module, source, source_type |
| パフォーマンス目標 | 100-300ms（リランキング前） |

---

## 2. 設計方針

### 2.1 アーキテクチャ原則

1. **並列実行**: BM25とベクトル検索は独立して並列実行し、レイテンシを最小化
2. **モード切替**: hybrid / vector / keyword の3モードをサポート（デフォルト: hybrid）
3. **パラメータ調整可能**: RRFのk値、重み付けα値を設定ファイルで変更可能
4. **グレースフルデグレード**: 一方の検索が失敗した場合、もう一方の結果のみで応答
5. **テーブル横断検索**: document_chunks と code_chunks を統合して検索

### 2.2 技術スタック

| コンポーネント | 技術 | 備考 |
|--------------|------|------|
| BM25検索 | PostgreSQL 16 Full Text Search | to_tsvector / to_tsquery |
| ベクトル検索 | pgvector コサイン類似度 | 1024次元ベクトル |
| インデックス | GIN (FTS) + ivfflat/HNSW (vector) | |
| 実行環境 | Spring Boot 3.4.x | CompletableFuture並列実行 |
| Embedding | Jina v4 (ドキュメント) / Voyage-code-3 (コード) | 1024次元 |

---

## 3. BM25検索（キーワード検索）

### 3.1 PostgreSQL Full Text Searchの利用

BM25検索には PostgreSQL の組み込み Full Text Search (FTS) 機能を使用する。
FTS は tf-idf ベースのランキング関数 `ts_rank_cd()` を提供し、BM25に近い精度を実現する。

### 3.2 テキスト検索設定

```sql
-- 日本語テキスト検索設定（pg_bigmまたはpgroonga拡張使用）
-- デフォルトの 'japanese' 設定は形態素解析ベース

-- document_chunks テーブルのFTSインデックス
CREATE INDEX idx_document_chunks_fts
  ON document_chunks
  USING GIN (to_tsvector('japanese', content));

-- code_chunks テーブルのFTSインデックス
CREATE INDEX idx_code_chunks_fts
  ON code_chunks
  USING GIN (to_tsvector('japanese', content));
```

### 3.3 日本語テキスト検索の考慮事項

Nablarchドキュメントは日本語と英語が混在するため、以下の対応が必要：

| 課題 | 対策 |
|------|------|
| 日本語形態素解析 | PostgreSQL `japanese` 辞書 + pg_bigm（2-gram）のフォールバック |
| 英語技術用語 | 英語部分は `english` 辞書で処理（ハイブリッド辞書設定） |
| FQCN検索 | ドット区切りをトークン化（`nablarch.fw.Handler` → `nablarch`, `fw`, `Handler`） |
| キャメルケース | キャメルケース分割（`HandlerQueueManager` → `Handler`, `Queue`, `Manager`） |

### 3.4 BM25検索クエリ

```sql
-- BM25検索（document_chunks）
SELECT
    dc.id,
    dc.content,
    ts_rank_cd(
        to_tsvector('japanese', dc.content),
        to_tsquery('japanese', :query),
        32  -- normalization: rank / (rank + 1)
    ) AS bm25_score,
    dc.metadata,
    dc.source_url
FROM document_chunks dc
WHERE to_tsvector('japanese', dc.content) @@ to_tsquery('japanese', :query)
  AND (:app_type IS NULL OR dc.metadata->>'app_type' = :app_type)
  AND (:module IS NULL OR dc.metadata->>'module' = :module)
  AND (:source IS NULL OR dc.metadata->>'source' = :source)
ORDER BY bm25_score DESC
LIMIT :top_k;
```

### 3.5 クエリ前処理

ユーザークエリをFTSクエリに変換する前処理を行う：

```
入力: "REST API認証付きハンドラキュー構成"

前処理ステップ:
1. 形態素解析: ["REST", "API", "認証", "付き", "ハンドラ", "キュー", "構成"]
2. ストップワード除去: ["REST", "API", "認証", "ハンドラ", "キュー", "構成"]
3. tsquery構築: 'REST' & 'API' & '認証' & 'ハンドラ' & 'キュー' & '構成'
4. OR拡張（オプション）: ('REST' | 'RESTful') & 'API' & ('認証' | 'authentication') ...
```

### 3.6 スコア正規化

BM25スコアは0〜∞の範囲を取るため、[0, 1]に正規化する：

```
normalized_bm25 = bm25_score / (bm25_score + 1)
```

`ts_rank_cd` の normalization パラメータ 32 によりこの正規化が自動適用される。

---

## 4. ベクトル検索（セマンティック検索）

### 4.1 Embeddingモデル

| 用途 | モデル | 次元数 | コンテキスト長 |
|------|--------|--------|--------------|
| ドキュメント検索 | Jina embeddings-v4 | 1024 | 32Kトークン |
| コード検索 | Voyage-code-3 | 1024 | - |

クエリのEmbeddingは検索対象テーブルに応じてモデルを切り替える：
- `document_chunks` → Jina v4
- `code_chunks` → Voyage-code-3
- 両方検索時 → 各テーブルに対応するモデルで別々にEmbedding生成

### 4.2 コサイン類似度検索クエリ

```sql
-- ベクトル検索（document_chunks）
SELECT
    dc.id,
    dc.content,
    1 - (dc.embedding <=> :query_vector) AS vector_score,
    dc.metadata,
    dc.source_url
FROM document_chunks dc
WHERE (:app_type IS NULL OR dc.metadata->>'app_type' = :app_type)
  AND (:module IS NULL OR dc.metadata->>'module' = :module)
  AND (:source IS NULL OR dc.metadata->>'source' = :source)
ORDER BY dc.embedding <=> :query_vector
LIMIT :top_k;
```

**注意**: `<=>` はpgvectorのコサイン距離演算子。`1 - distance` でコサイン類似度に変換。

### 4.3 インデックス戦略

| インデックスタイプ | 特性 | 適用条件 |
|------------------|------|---------|
| **ivfflat** | 構築高速、精度は`nprobes`依存 | チャンク数 < 100K |
| **HNSW** | 高精度（recall > 99%）、構築低速 | 本番環境推奨 |

```sql
-- ivfflat インデックス（初期構築用）
CREATE INDEX idx_document_chunks_embedding_ivfflat
  ON document_chunks
  USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);

-- HNSW インデックス（本番用、チャンク数に応じて切替）
CREATE INDEX idx_document_chunks_embedding_hnsw
  ON document_chunks
  USING hnsw (embedding vector_cosine_ops)
  WITH (m = 16, ef_construction = 64);
```

Nablarchの推定チャンク数（約4万件）ではivfflatで十分な精度が得られるが、
本番環境ではHNSWへの移行を推奨する。

### 4.4 スコア正規化

コサイン類似度は[-1, 1]の範囲だが、正規化済みベクトルでは[0, 1]となる。
Embeddingモデルの出力は通常正規化済みのため、追加の正規化は不要。

---

## 5. Reciprocal Rank Fusion (RRF)

### 5.1 アルゴリズム定義

RRFは複数の検索結果ランキングを統合する手法である。各結果に対し、
各ランキングでの順位の逆数を合計してスコアとする。

```
RRF_score(d) = Σ_{r ∈ R} 1 / (k + rank_r(d))
```

- `d`: ドキュメント（チャンク）
- `R`: ランキングの集合（BM25ランキング, ベクトルランキング）
- `rank_r(d)`: ランキング`r`におけるドキュメント`d`の順位（1始まり）
- `k`: スムージングパラメータ（デフォルト: 60）

### 5.2 kパラメータ

| k値 | 特性 |
|-----|------|
| k=1 | 上位のランク差が大きくなる（トップヘビー） |
| k=60 | 標準値。上位と下位のスコア差が穏やか |
| k=100 | ランク差がさらに平坦化される |

**デフォルト k=60** を採用（原論文 Cormack et al., 2009 の推奨値）。

### 5.3 統合例

```
BM25結果:      [doc_A(rank=1), doc_B(rank=2), doc_C(rank=3), doc_D(rank=4)]
Vector結果:    [doc_C(rank=1), doc_A(rank=2), doc_E(rank=3), doc_B(rank=4)]

RRF計算（k=60）:
  doc_A: 1/(60+1) + 1/(60+2) = 0.01639 + 0.01613 = 0.03252
  doc_B: 1/(60+2) + 1/(60+4) = 0.01613 + 0.01563 = 0.03176
  doc_C: 1/(60+3) + 1/(60+1) = 0.01587 + 0.01639 = 0.03226
  doc_D: 1/(60+4) + 0         = 0.01563 + 0        = 0.01563
  doc_E: 0        + 1/(60+3)  = 0        + 0.01587 = 0.01587

RRF順位: doc_A > doc_C > doc_B > doc_E > doc_D
```

### 5.4 片方にしか存在しない結果の扱い

BM25またはベクトル検索の一方にのみ存在する結果は、もう一方のスコアを0として計算する。
これにより、両方の検索で上位に出現する結果が優先される。

---

## 6. 重み付けハイブリッドスコアリング

### 6.1 RRFとの組み合わせ

RRFに加え、正規化されたスコアの重み付け平均も併用可能とする。
ユースケースに応じて切り替えられる設計とする。

```
hybrid_score(d) = α * normalized_bm25(d) + (1 - α) * vector_score(d)
```

### 6.2 重み付けパラメータ α

| パラメータ | 値 | 説明 |
|----------|-----|------|
| α = 0.0 | ベクトル検索のみ | セマンティック検索に完全依存 |
| α = 0.3 | **デフォルト** | ベクトル重視（Nablarchはセマンティック理解が重要） |
| α = 0.5 | 均等 | BM25とベクトルを等しく重視 |
| α = 0.7 | BM25重視 | キーワード正確性を重視 |
| α = 1.0 | BM25のみ | キーワード検索に完全依存 |

**デフォルト α=0.3 の根拠**:
- Nablarchの検索クエリは「ハンドラキューの設定方法」「Universal DAOの使い方」のように
  概念的な質問が多く、セマンティック理解の重要度が高い
- 一方でFQCN検索（`nablarch.fw.Handler`）のようなキーワード完全一致が必要なケースもあるため、
  BM25の比重を0.3として一定の寄与を確保

### 6.3 統合方式の選択

| 統合方式 | 特性 | 推奨ユースケース |
|---------|------|----------------|
| **RRF** | スコアスケール非依存、ロバスト | デフォルト（一般的な検索） |
| **重み付け平均** | スコアの大小が反映される | チューニング済み環境 |

**デフォルトはRRFを使用**。重み付け平均は検索品質評価（WBS 2.1.7）の結果に基づき、
RRFを上回る場合に切り替えを検討する。

---

## 7. 検索フロー

### 7.1 全体フロー図

```
                        ┌──────────────┐
                        │  ユーザークエリ │
                        │  + フィルタ条件 │
                        └──────┬───────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  クエリ前処理         │
                    │  - 言語検出           │
                    │  - トークン化         │
                    │  - ストップワード除去  │
                    └──────────┬──────────┘
                               │
                    ┌──────────┴──────────┐
                    │                     │
                    ▼                     ▼
          ┌──────────────────┐  ┌──────────────────┐
          │  BM25検索         │  │  Embedding生成    │
          │  (PostgreSQL FTS) │  │  (Jina v4 /      │
          │                   │  │   Voyage-code-3)  │
          │  Top-50取得       │  │                   │
          └────────┬─────────┘  └────────┬─────────┘
                   │                     │
                   │                     ▼
                   │            ┌──────────────────┐
                   │            │  ベクトル検索      │
                   │            │  (pgvector cosine)│
                   │            │  Top-50取得       │
                   │            └────────┬─────────┘
                   │                     │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │  RRF / 重み付け統合   │
                   │  BM25結果 + Vector結果│
                   │  → 統合スコア計算     │
                   │  → Top-K選出         │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │  結果返却             │
                   │  List<SearchResult>   │
                   │  (→ リランキングへ)    │
                   └─────────────────────┘
```

### 7.2 並列実行設計

BM25検索とEmbedding生成+ベクトル検索は独立しているため、`CompletableFuture`で並列実行する。

```java
// HybridSearchService.java（疑似コード）
public List<SearchResult> search(String query, SearchFilters filters, int topK, SearchMode mode) {

    if (mode == SearchMode.KEYWORD) {
        return bm25SearchService.search(query, filters, topK);
    }
    if (mode == SearchMode.VECTOR) {
        float[] embedding = embeddingService.embed(query);
        return vectorSearchService.search(embedding, filters, topK);
    }

    // HYBRID mode: 並列実行
    int candidateK = 50; // RRF統合前の候補数

    CompletableFuture<List<SearchResult>> bm25Future =
        CompletableFuture.supplyAsync(() ->
            bm25SearchService.search(query, filters, candidateK));

    CompletableFuture<List<SearchResult>> vectorFuture =
        CompletableFuture.supplyAsync(() -> {
            float[] embedding = embeddingService.embed(query);
            return vectorSearchService.search(embedding, filters, candidateK);
        });

    List<SearchResult> bm25Results = bm25Future.join();
    List<SearchResult> vectorResults = vectorFuture.join();

    return rrfMerge(bm25Results, vectorResults, topK);
}
```

### 7.3 モード別動作

| モード | BM25 | Vector | RRF統合 | 用途 |
|--------|------|--------|---------|------|
| `HYBRID` | ○ | ○ | ○ | デフォルト。最高精度 |
| `KEYWORD` | ○ | × | × | FQCN検索、exact match重視 |
| `VECTOR` | × | ○ | × | セマンティック検索のみ |

---

## 8. メタデータフィルタリング

### 8.1 フィルタ条件

architecture.md §4.3 で定義されたメタデータスキーマに基づき、以下のフィルタをサポートする：

| フィルタ名 | 型 | 例 | 説明 |
|-----------|-----|-----|------|
| `app_type` | String | `"web"`, `"rest"`, `"batch"` | アプリケーション種別 |
| `module` | String | `"nablarch-core-repository"` | モジュール名 |
| `source` | String | `"nablarch-document"`, `"github"` | データソース |
| `source_type` | String | `"documentation"`, `"code"` | コンテンツ種別 |
| `language` | String | `"ja"`, `"en"` | 言語 |

### 8.2 WHERE句動的構築

フィルタ条件はオプショナルであり、指定されたもののみWHERE句に追加する。

```java
// SearchFilters.java
public record SearchFilters(
    String appType,
    String module,
    String source,
    String sourceType,
    String language
) {}

// WHERE句動的構築（BM25検索の場合）
private String buildWhereClause(SearchFilters filters) {
    StringBuilder where = new StringBuilder();
    where.append("WHERE to_tsvector('japanese', content) @@ to_tsquery('japanese', :query)");

    if (filters.appType() != null) {
        where.append(" AND metadata->>'app_type' = :app_type");
    }
    if (filters.module() != null) {
        where.append(" AND metadata->>'module' = :module");
    }
    if (filters.source() != null) {
        where.append(" AND metadata->>'source' = :source");
    }
    if (filters.sourceType() != null) {
        where.append(" AND metadata->>'source_type' = :source_type");
    }
    if (filters.language() != null) {
        where.append(" AND metadata->>'language' = :language");
    }
    return where.toString();
}
```

### 8.3 フィルタのインデックス対応

高頻度に使用されるフィルタに対してJSONBインデックスを作成する：

```sql
-- app_type フィルタ用インデックス
CREATE INDEX idx_document_chunks_app_type
  ON document_chunks ((metadata->>'app_type'));

-- source フィルタ用インデックス
CREATE INDEX idx_document_chunks_source
  ON document_chunks ((metadata->>'source'));

-- 複合インデックス（app_type + source_type）
CREATE INDEX idx_document_chunks_app_source
  ON document_chunks ((metadata->>'app_type'), (metadata->>'source_type'));
```

---

## 9. パフォーマンス設計

### 9.1 レイテンシバジェット

全体の検索パイプライン（リランキング含む）のレイテンシ目標は300ms。
ハイブリッド検索はリランキング前のステージであり、200ms以内を目標とする。

| ステージ | 目標レイテンシ | 備考 |
|---------|-------------|------|
| クエリ前処理 | 5ms | トークン化・ストップワード除去 |
| Embedding生成 | 50-100ms | 外部API呼び出し（Jina v4） |
| BM25検索 | 10-30ms | PostgreSQL FTS（インデックス使用） |
| ベクトル検索 | 20-50ms | pgvector（インデックス使用） |
| RRF統合 | 5ms | インメモリ計算 |
| **合計（並列実行）** | **60-155ms** | BM25とVector+Embeddingが並列 |

**ボトルネック**: Embedding生成の外部API呼び出し（50-100ms）が支配的。

### 9.2 ボトルネック対策

| 対策 | 効果 | 実装フェーズ |
|------|------|------------|
| Embeddingキャッシュ | 同一クエリの再計算回避 | Phase 2初期 |
| コネクションプール | DB接続のオーバーヘッド削減 | Phase 2初期 |
| プリペアドステートメント | SQL解析コスト削減 | Phase 2初期 |
| 結果キャッシュ（Caffeine） | 頻出クエリの応答高速化 | Phase 2後半 |
| pgvectorscale（StreamingDiskANN） | 大規模時のベクトル検索高速化 | Phase 4 |

### 9.3 Top-K候補数の設計

RRF統合の入力として各検索から取得する候補数（candidateK）は、
最終的なTop-K（デフォルト5-10件）を確保するために余裕を持たせる。

| candidateK | 精度 | レイテンシ |
|-----------|------|----------|
| 20 | 低い（片方にしかない結果を見逃す） | 高速 |
| **50** | **十分（デフォルト）** | **標準** |
| 100 | 高い（冗長な結果も取得） | やや遅い |

---

## 10. インターフェース設計

### 10.1 HybridSearchService

```java
package com.tis.nablarch.mcp.rag.search;

/**
 * ハイブリッド検索サービス。
 * BM25キーワード検索とベクトルセマンティック検索をRRFで統合する。
 */
public interface HybridSearchService {

    /**
     * ハイブリッド検索を実行する。
     *
     * @param query ユーザークエリ
     * @param filters メタデータフィルタ条件（nullの場合フィルタなし）
     * @param topK 返却する結果数（デフォルト: 10）
     * @param mode 検索モード（HYBRID, KEYWORD, VECTOR）
     * @return 検索結果リスト（スコア降順）
     */
    List<SearchResult> search(String query, SearchFilters filters, int topK, SearchMode mode);
}
```

### 10.2 SearchResult

```java
package com.tis.nablarch.mcp.rag.search;

/**
 * 検索結果を表すDTO。
 */
public record SearchResult(
    String id,
    String content,
    double score,
    Map<String, String> metadata,
    String sourceUrl
) {}
```

### 10.3 SearchMode

```java
package com.tis.nablarch.mcp.rag.search;

/**
 * 検索モード。
 */
public enum SearchMode {
    /** BM25 + ベクトル検索のハイブリッド（デフォルト） */
    HYBRID,
    /** BM25キーワード検索のみ */
    KEYWORD,
    /** ベクトルセマンティック検索のみ */
    VECTOR
}
```

### 10.4 SearchFilters

```java
package com.tis.nablarch.mcp.rag.search;

/**
 * 検索フィルタ条件。
 * nullのフィールドはフィルタリングしない。
 */
public record SearchFilters(
    String appType,
    String module,
    String source,
    String sourceType,
    String language
) {
    /** フィルタなし */
    public static final SearchFilters NONE = new SearchFilters(null, null, null, null, null);
}
```

### 10.5 設定パラメータ

```yaml
# application.yml
nablarch:
  rag:
    search:
      # ハイブリッド検索設定
      hybrid:
        rrf-k: 60                # RRFスムージングパラメータ
        alpha: 0.3               # BM25重み（0.0=Vector only, 1.0=BM25 only）
        candidate-k: 50          # RRF統合前の候補数
        default-top-k: 10        # デフォルト結果数
        merge-strategy: rrf      # rrf | weighted_average
        timeout-ms: 200          # 検索タイムアウト（ms）

      # BM25検索設定
      bm25:
        language: japanese        # PostgreSQL FTS辞書
        normalization: 32         # ts_rank_cd正規化フラグ

      # ベクトル検索設定
      vector:
        index-type: ivfflat      # ivfflat | hnsw
        nprobes: 10              # ivfflatのプローブ数
        ef-search: 40            # HNSWの検索時ef値
```

---

## 11. エラーハンドリング

### 11.1 グレースフルデグレード

ハイブリッド検索の一方が失敗した場合、もう一方の結果のみで応答する。

| 障害パターン | 動作 | ログレベル |
|------------|------|----------|
| BM25検索失敗 | ベクトル検索結果のみ返却 | WARN |
| ベクトル検索失敗 | BM25検索結果のみ返却 | WARN |
| Embedding API失敗 | BM25検索結果のみ返却（= KEYWORDモード） | WARN |
| 両方失敗 | 空リスト返却 + エラーログ | ERROR |
| タイムアウト | 完了した方の結果のみ返却 | WARN |

### 11.2 タイムアウト制御

```java
// 並列実行のタイムアウト制御
try {
    CompletableFuture.allOf(bm25Future, vectorFuture)
        .get(timeoutMs, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
    // タイムアウト時は完了した方の結果を使用
    log.warn("ハイブリッド検索タイムアウト: {}ms", timeoutMs);
}

List<SearchResult> bm25Results = bm25Future.isDone()
    ? bm25Future.get() : Collections.emptyList();
List<SearchResult> vectorResults = vectorFuture.isDone()
    ? vectorFuture.get() : Collections.emptyList();
```

---

## 12. Phase 1からの移行

### 12.1 移行方針

Phase 1 の `NablarchKnowledgeBase.search()` は Phase 2 で以下のように移行する：

| Phase 1 | Phase 2 | 変更内容 |
|---------|---------|---------|
| `NablarchKnowledgeBase.search()` | `HybridSearchService.search()` | 検索ロジックをRAGパイプラインに委譲 |
| インメモリ部分文字列マッチ | BM25 + ベクトル検索 | PostgreSQL FTS + pgvector |
| `SearchApiTool.searchApi()` | `SemanticSearchTool.semanticSearch()` | 新Tool追加（search_apiは残す） |

### 12.2 後方互換性

- `search_api` Tool は Phase 2 でも維持する（Phase 1 の静的検索として）
- `semantic_search` Tool を新規追加し、RAGパイプラインを使用する検索を提供
- 将来的に `search_api` は deprecated とし、`semantic_search` に統合する

---

## 付録

### A. 参考文献

| 文献 | 内容 |
|------|------|
| Cormack et al., 2009 | Reciprocal Rank Fusion の原論文 |
| pgvector公式ドキュメント | コサイン類似度、インデックス設定 |
| PostgreSQL FTS公式ドキュメント | to_tsvector, to_tsquery, ts_rank_cd |
| arXiv:2505.03275 | RAG-MCPツール選択最適化の実証論文 |

### B. 関連WBSタスク

| WBS | タスク | 本設計との関係 |
|-----|-------|-------------|
| 2.1.1 | ベクトルDBスキーマ設計 | テーブル定義の前提条件 |
| 2.1.5 | リランキング設計 | 本設計の検索結果を入力とする |
| 2.1.6 | semantic_search Tool設計 | 本設計の検索機能をMCP Toolとして公開 |
| 2.2.10 | BM25検索実装 | 本設計のBM25部分を実装 |
| 2.2.11 | ベクトル検索実装 | 本設計のベクトル検索部分を実装 |
| 2.2.12 | ハイブリッド検索実装 | 本設計全体を実装 |

# リランキング設計書

> **WBS番号**: 2.1.5
> **ステータス**: 設計完了
> **作成日**: 2026-02-02
> **作成者**: ashigaru8 (subtask_056)
> **関連文書**: 11_hybrid-search.md, architecture.md §4.6, ADR-001
> **依存タスク**: WBS 2.1.4（ハイブリッド検索設計）

---

## 目次

1. [概要](#1-概要)
2. [Cross-Encoderモデル選定](#2-cross-encoderモデル選定)
3. [リランキングパイプライン](#3-リランキングパイプライン)
4. [API呼び出し設計](#4-api呼び出し設計)
5. [スコア正規化](#5-スコア正規化)
6. [フォールバック設計](#6-フォールバック設計)
7. [パフォーマンス設計](#7-パフォーマンス設計)
8. [インターフェース設計](#8-インターフェース設計)
9. [設定パラメータ](#9-設定パラメータ)

---

## 1. 概要

### 1.1 目的

本設計書は、ハイブリッド検索結果に対する**Cross-Encoderリランキング**の設計を定義する。
リランキングはハイブリッド検索（WBS 2.1.4）の後段に位置し、クエリと各候補ドキュメントの
ペアを直接評価することで、検索精度をさらに向上させる。

### 1.2 Bi-Encoder vs Cross-Encoder

| 特性 | Bi-Encoder（ベクトル検索） | Cross-Encoder（リランキング） |
|------|--------------------------|------------------------------|
| 処理方式 | クエリとドキュメントを独立にエンコード | クエリ+ドキュメントを連結してエンコード |
| 精度 | 中（独立表現の内積） | 高（トークン間の直接注意機構） |
| 速度 | 高速（事前計算可能） | 低速（ペアごとに推論必要） |
| 適用場面 | 大量候補からの絞り込み（1段目） | 少数候補の精密順位付け（2段目） |

Cross-Encoderは全候補に適用すると計算コストが高いため、
ハイブリッド検索で絞り込んだTop-50候補に対してのみ適用する。

### 1.3 パイプライン位置

```
ユーザークエリ
    │
    ▼
ハイブリッド検索（WBS 2.1.4）
    │ Top-50候補
    ▼
★ リランキング（本設計書） ★
    │ Top-K結果（5-10件）
    ▼
結果整形・返却
```

---

## 2. Cross-Encoderモデル選定

### 2.1 候補モデル比較

| 項目 | Jina Reranker v2 | Cohere Reranker v3 |
|------|-------------------|---------------------|
| 多言語対応 | 100言語以上 | 100言語以上 |
| 日本語精度 | 高い（多言語MTEB上位） | 高い |
| 最大入力長 | 8192トークン | 4096トークン |
| バッチ処理 | ○（1リクエスト複数ペア） | ○ |
| 価格 | $0.02/1000検索 | $1.00/1000検索 |
| レイテンシ | 50-80ms（50候補） | 80-120ms（50候補） |
| SLA | 99.9% | 99.9% |
| セルフホスト | ○（Jina AI Cloud or ローカル） | × |

### 2.2 選定結果

**Jina Reranker v2 を第一選択とする。**

選定理由：
1. **コスト効率**: Cohereの50分の1の価格
2. **長文コンテキスト**: 8192トークンでNablarchの長いドキュメントチャンクに対応
3. **低レイテンシ**: 50候補で50-80ms（目標100ms以内を十分達成）
4. **セルフホスト可能**: 将来的にローカル実行に移行しレイテンシ・コスト削減が可能
5. **Jina統一**: Embeddingモデル（Jina v4）と同一ベンダーでAPIキー管理を統一

**Cohere Reranker v3 をフォールバックとして設定可能とする。**

---

## 3. リランキングパイプライン

### 3.1 処理フロー

```
┌────────────────────────────────────────────────────┐
│            リランキングパイプライン                    │
│                                                     │
│  入力: ハイブリッド検索結果（Top-50）                  │
│     │                                               │
│     ▼                                               │
│  ┌──────────────────────────────────┐              │
│  │ 1. 入力検証                       │              │
│  │    - 候補数チェック（0件なら即返却） │              │
│  │    - 重複排除（IDベース）          │              │
│  └──────────────┬───────────────────┘              │
│                 ▼                                    │
│  ┌──────────────────────────────────┐              │
│  │ 2. ペア構築                       │              │
│  │    - (query, doc_content) ペアを   │              │
│  │      候補数分構築                  │              │
│  │    - content切り詰め（8192トークン  │              │
│  │      上限に収まるよう）             │              │
│  └──────────────┬───────────────────┘              │
│                 ▼                                    │
│  ┌──────────────────────────────────┐              │
│  │ 3. Cross-Encoder API呼び出し      │              │
│  │    - バッチリクエスト（1回で全ペア） │              │
│  │    - タイムアウト: 100ms           │              │
│  └──────────────┬───────────────────┘              │
│                 ▼                                    │
│  ┌──────────────────────────────────┐              │
│  │ 4. スコア正規化                   │              │
│  │    - sigmoid適用（ロジットをprob化）│              │
│  │    - [0, 1] 範囲に変換            │              │
│  └──────────────┬───────────────────┘              │
│                 ▼                                    │
│  ┌──────────────────────────────────┐              │
│  │ 5. 結果選出                       │              │
│  │    - スコア降順ソート              │              │
│  │    - Top-K選出（デフォルト: 10）    │              │
│  │    - スコア閾値フィルタ（任意）     │              │
│  └──────────────┬───────────────────┘              │
│                 ▼                                    │
│  出力: リランキング済み結果（Top-K）                   │
└────────────────────────────────────────────────────┘
```

### 3.2 候補数の制御

| パラメータ | 値 | 説明 |
|----------|-----|------|
| 入力候補数（maxCandidates） | 50 | ハイブリッド検索からの入力上限 |
| 出力件数（topK） | 5-10 | 最終的にクライアントに返す件数 |
| スコア閾値（minScore） | 0.1 | この値未満の結果は除外（オプション） |

---

## 4. API呼び出し設計

### 4.1 Jina Reranker v2 APIリクエスト

```
POST https://api.jina.ai/v1/rerank
Content-Type: application/json
Authorization: Bearer {JINA_API_KEY}

{
    "model": "jina-reranker-v2-base-multilingual",
    "query": "REST APIのハンドラキュー構成",
    "documents": [
        "Nablarchのハンドラキューは、リクエスト処理の...",
        "RESTfulウェブサービスの認証には...",
        ...
    ],
    "top_n": 10,
    "return_documents": false
}
```

### 4.2 APIレスポンス

```json
{
    "model": "jina-reranker-v2-base-multilingual",
    "usage": {
        "total_tokens": 1234
    },
    "results": [
        {
            "index": 0,
            "relevance_score": 0.95
        },
        {
            "index": 3,
            "relevance_score": 0.87
        },
        ...
    ]
}
```

### 4.3 HTTPクライアント設計

```java
// CrossEncoderRerankerClient.java（疑似コード）
@Component
public class JinaRerankerClient implements RerankerClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    /**
     * リランキングAPIを呼び出す。
     *
     * @param query ユーザークエリ
     * @param documents 候補ドキュメントのコンテンツリスト
     * @param topN 返却する上位件数
     * @return リランキングスコア（index→score のマップ）
     */
    public List<RerankResult> rerank(String query, List<String> documents, int topN) {
        var request = new RerankRequest(model, query, documents, topN, false);

        var response = restClient.post()
            .uri("https://api.jina.ai/v1/rerank")
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .retrieve()
            .body(RerankResponse.class);

        return response.results();
    }
}
```

### 4.4 Cohere Rerankerフォールバック

```
POST https://api.cohere.ai/v1/rerank
Content-Type: application/json
Authorization: bearer {COHERE_API_KEY}

{
    "model": "rerank-multilingual-v3.0",
    "query": "REST APIのハンドラキュー構成",
    "documents": [...],
    "top_n": 10,
    "return_documents": false
}
```

レスポンス形式はJinaと類似のため、共通インターフェース `RerankerClient` で抽象化する。

---

## 5. スコア正規化

### 5.1 Cross-Encoderスコアの性質

Jina Reranker v2のスコアはモデルのロジット出力であり、範囲は固定されていない。
`relevance_score`として返却されるが、値域はモデルに依存する。

### 5.2 正規化方式

`relevance_score` を [0, 1] の確率値に変換するため、sigmoid関数を適用する：

```
normalized_score = 1 / (1 + exp(-relevance_score))
```

ただし、Jina Reranker v2 の `relevance_score` は既に [0, 1] 正規化済みのため、
追加の正規化は不要。Cohere Reranker も同様に正規化済みスコアを返す。

### 5.3 ハイブリッド検索スコアとの結合

リランキング後のスコアは、ハイブリッド検索スコアとリランキングスコアを結合する：

```
final_score = β * rerank_score + (1 - β) * hybrid_score
```

| パラメータ | 値 | 説明 |
|----------|-----|------|
| β = 1.0 | **デフォルト** | リランキングスコアのみ使用（Cross-Encoderの精度を信頼） |
| β = 0.7 | オプション | リランキング重視だがハイブリッドスコアも考慮 |

デフォルトではリランキングスコアのみ（β=1.0）を使用する。
Cross-Encoderはクエリ-ドキュメントペアを直接評価するため、
Bi-Encoderベースのハイブリッドスコアよりも精度が高いことが一般的である。

---

## 6. フォールバック設計

### 6.1 フォールバック戦略

| 障害パターン | フォールバック動作 | ログレベル |
|------------|------------------|----------|
| Jina API接続エラー | Cohere APIにフォールバック | WARN |
| Cohere APIも失敗 | ハイブリッド検索のRRFスコアのみで結果返却 | WARN |
| API応答タイムアウト（100ms超） | RRFスコアのみで結果返却 | WARN |
| レート制限（429） | 指数バックオフ → 失敗ならRRFスコア | WARN |
| 不正レスポンス（parse失敗） | RRFスコアのみで結果返却 | ERROR |

### 6.2 フォールバックフロー

```
リランキングリクエスト
    │
    ▼
Jina Reranker API
    │
    ├── 成功 → リランキング結果を返却
    │
    └── 失敗 → Cohere Reranker API（設定されている場合）
                    │
                    ├── 成功 → リランキング結果を返却
                    │
                    └── 失敗 → ハイブリッド検索のRRFスコアで返却
                               （リランキングスキップ）
```

### 6.3 サーキットブレーカー

連続失敗時にAPIコールを一時停止し、システム負荷を軽減する。

| パラメータ | 値 | 説明 |
|----------|-----|------|
| failureThreshold | 5 | 連続失敗でサーキットオープン |
| waitDuration | 30秒 | オープン状態の維持時間 |
| halfOpenMaxCalls | 3 | ハーフオープン時の試行回数 |

```java
// サーキットブレーカー設定（Spring Cloud CircuitBreaker / Resilience4j）
@CircuitBreaker(name = "reranker", fallbackMethod = "fallbackToRrfScore")
public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK) {
    // Jina Reranker API呼び出し
}

private List<SearchResult> fallbackToRrfScore(
        String query, List<SearchResult> candidates, int topK, Throwable t) {
    log.warn("リランキングフォールバック: {}", t.getMessage());
    return candidates.stream()
        .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
        .limit(topK)
        .toList();
}
```

---

## 7. パフォーマンス設計

### 7.1 レイテンシバジェット

全体パイプライン: 300ms

| ステージ | バジェット | 備考 |
|---------|----------|------|
| ハイブリッド検索 | 200ms | WBS 2.1.4参照 |
| **リランキング** | **100ms** | 本設計書のスコープ |
| 合計 | 300ms | |

### 7.2 リランキング内のレイテンシ内訳

| 処理 | 目標 | 備考 |
|------|------|------|
| ペア構築 | 2ms | インメモリ処理 |
| API呼び出し（ネットワーク） | 10-20ms | RTT |
| API推論（サーバーサイド） | 40-60ms | 50候補バッチ処理 |
| レスポンス処理 | 2ms | JSON parse + ソート |
| **合計** | **54-84ms** | 目標100ms以内を達成 |

### 7.3 候補数とレイテンシの関係

| 候補数 | Jina推論時間（推定） | 備考 |
|--------|-------------------|------|
| 10 | 15-20ms | 高速だが精度低下リスク |
| 25 | 30-40ms | 軽量バランス |
| **50** | **50-70ms** | **デフォルト（精度とレイテンシのバランス）** |
| 100 | 100-140ms | 目標超過リスク |

### 7.4 最適化戦略

| 対策 | 効果 | 実装フェーズ |
|------|------|------------|
| コンテンツ切り詰め（先頭2048トークン） | API推論時間削減 | Phase 2初期 |
| リランキングキャッシュ | 同一クエリの再計算回避 | Phase 2後半 |
| セルフホスト（Jina Rerankerローカル実行） | ネットワークRTT削除 | Phase 4 |

---

## 8. インターフェース設計

### 8.1 RerankerService

```java
package com.tis.nablarch.mcp.rag.rerank;

/**
 * リランキングサービスインターフェース。
 * ハイブリッド検索結果をCross-Encoderで再順位付けする。
 */
public interface RerankerService {

    /**
     * 検索結果をリランキングする。
     *
     * @param query ユーザークエリ
     * @param candidates ハイブリッド検索結果（Top-N候補）
     * @param topK 返却する上位件数
     * @return リランキング済みの検索結果（スコア降順）
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK);
}
```

### 8.2 RerankerClient

```java
package com.tis.nablarch.mcp.rag.rerank;

/**
 * リランキングAPIクライアントインターフェース。
 * Jina / Cohere等のプロバイダを差し替え可能にする。
 */
public interface RerankerClient {

    /**
     * 外部リランキングAPIを呼び出す。
     *
     * @param query クエリ文字列
     * @param documents ドキュメントコンテンツのリスト
     * @param topN 上位N件を返却
     * @return リランキング結果（index + score）
     */
    List<RerankResult> rerank(String query, List<String> documents, int topN);
}
```

### 8.3 RerankResult

```java
package com.tis.nablarch.mcp.rag.rerank;

/**
 * リランキングAPI応答の1件分。
 *
 * @param index 入力ドキュメントリストでのインデックス
 * @param relevanceScore 関連度スコア [0, 1]
 */
public record RerankResult(
    int index,
    double relevanceScore
) {}
```

---

## 9. 設定パラメータ

```yaml
# application.yml
nablarch:
  rag:
    rerank:
      enabled: true                    # リランキングの有効/無効
      max-candidates: 50               # リランキング対象の最大候補数
      default-top-k: 10               # デフォルト出力件数
      min-score: 0.1                   # スコア閾値（これ未満は除外）
      timeout-ms: 100                  # APIタイムアウト（ms）
      score-combination:
        beta: 1.0                      # rerank_score重み（1.0=rerankのみ）

      # プロバイダ設定
      primary-provider: jina
      fallback-provider: cohere        # null = フォールバックなし

      jina:
        api-key: ${JINA_API_KEY}
        model: jina-reranker-v2-base-multilingual
        endpoint: https://api.jina.ai/v1/rerank

      cohere:
        api-key: ${COHERE_API_KEY}
        model: rerank-multilingual-v3.0
        endpoint: https://api.cohere.ai/v1/rerank

      # サーキットブレーカー設定
      circuit-breaker:
        failure-threshold: 5
        wait-duration-seconds: 30
        half-open-max-calls: 3
```

---

## 付録

### A. 関連WBSタスク

| WBS | タスク | 本設計との関係 |
|-----|-------|-------------|
| 2.1.4 | ハイブリッド検索設計 | リランキングの入力を提供 |
| 2.1.6 | semantic_search Tool設計 | リランキングを含むパイプラインをToolとして公開 |
| 2.2.13 | Cross-Encoder リランキング実装 | 本設計を実装 |
| 2.3.6 | 統合テスト: リランキング | リランキング精度テスト |

### B. Jina Reranker v2 API仕様

| 項目 | 値 |
|------|-----|
| エンドポイント | `https://api.jina.ai/v1/rerank` |
| メソッド | POST |
| 認証 | Bearer Token |
| 入力上限 | 1000ドキュメント/リクエスト |
| トークン上限 | 8192トークン/ドキュメント |
| レート制限 | 500 RPM (Free), 2000 RPM (Pro) |

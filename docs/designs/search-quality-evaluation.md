# 検索品質評価設計書

> **WBS番号**: 2.1.7
> **ステータス**: 設計完了
> **作成日**: 2026-02-02
> **作成者**: ashigaru8 (subtask_056)
> **関連文書**: hybrid-search.md, reranking.md, semantic-search-tool.md
> **依存タスク**: WBS 2.1.6（semantic_search Tool設計）

---

## 目次

1. [概要](#1-概要)
2. [評価メトリクス](#2-評価メトリクス)
3. [評価データセット仕様](#3-評価データセット仕様)
4. [評価データセット（50件）](#4-評価データセット50件)
5. [A/Bテスト方式](#5-abテスト方式)
6. [自動評価スクリプト設計](#6-自動評価スクリプト設計)
7. [評価レポートフォーマット](#7-評価レポートフォーマット)
8. [チューニング指針](#8-チューニング指針)

---

## 1. 概要

### 1.1 目的

本設計書は、Phase 2 RAG検索パイプラインの**検索品質**を定量的に評価するための
メトリクス、データセット、テスト方式、自動評価スクリプトを定義する。

### 1.2 評価対象

| 検索方式 | 説明 | 比較コード |
|---------|------|----------|
| BM25 only | PostgreSQL FTS のみ | `A` |
| Vector only | pgvector コサイン類似度のみ | `B` |
| Hybrid (RRF) | BM25 + Vector + RRF統合 | `C` |
| Hybrid + Rerank | BM25 + Vector + RRF + Cross-Encoder | `D` |
| Phase 1 Baseline | NablarchKnowledgeBase.search() | `E` |

### 1.3 品質目標

| メトリクス | 目標値 | Phase 1推定値 |
|----------|--------|-------------|
| MRR | ≥ 0.70 | 0.35-0.45 |
| Recall@5 | ≥ 0.80 | 0.40-0.50 |
| NDCG@5 | ≥ 0.70 | 0.30-0.40 |

---

## 2. 評価メトリクス

### 2.1 MRR（Mean Reciprocal Rank）

最初の正解ドキュメントの順位の逆数の平均。

```
MRR = (1/N) * Σ_{i=1}^{N} 1/rank_i
```

- `N`: 評価クエリ数
- `rank_i`: i番目のクエリにおける最初の正解ドキュメントの順位
- 正解が見つからない場合: `1/rank_i = 0`

**例**:
| クエリ | 最初の正解順位 | Reciprocal Rank |
|-------|-------------|-----------------|
| Q1 | 1位 | 1/1 = 1.000 |
| Q2 | 3位 | 1/3 = 0.333 |
| Q3 | 2位 | 1/2 = 0.500 |
| Q4 | なし | 0 |
| **MRR** | | **(1+0.333+0.5+0)/4 = 0.458** |

### 2.2 Recall@K

上位K件に含まれる正解ドキュメントの割合。

```
Recall@K = |relevant ∩ top-K| / |relevant|
```

- `relevant`: 正解ドキュメントの集合
- `top-K`: 検索結果の上位K件

**K=5をメイン指標とする。** 理由：semantic_search Toolのデフォルト返却数（10件）の半分で
十分な正解が含まれていることを確認する。

### 2.3 NDCG@K（Normalized Discounted Cumulative Gain）

順位を考慮した関連度評価。上位に関連度の高いドキュメントが来るほど高スコア。

```
DCG@K = Σ_{i=1}^{K} (2^{rel_i} - 1) / log₂(i + 1)

NDCG@K = DCG@K / IDCG@K
```

- `rel_i`: i番目の結果の関連度（0, 1, 2, 3）
- `IDCG@K`: 理想的な順序でのDCG@K

**関連度レベル**:

| レベル | 値 | 定義 |
|-------|-----|------|
| Perfect | 3 | クエリに対する直接的な正解。そのまま回答に使える |
| Highly Relevant | 2 | 強く関連する。回答の重要な根拠となる |
| Marginally Relevant | 1 | 部分的に関連する。補足情報として有用 |
| Not Relevant | 0 | 無関係 |

### 2.4 補助メトリクス

| メトリクス | 定義 | 用途 |
|----------|------|------|
| Precision@K | top-K中の正解数/K | 結果の精度 |
| Latency P50/P95/P99 | 検索レイテンシの分位数 | パフォーマンス評価 |
| Coverage | 結果が1件以上返るクエリの割合 | 検索カバレッジ |

---

## 3. 評価データセット仕様

### 3.1 データセットフォーマット

```yaml
# evaluation-dataset.yaml
dataset:
  version: "1.0"
  created: "2026-02-XX"
  total_queries: 50

queries:
  - id: "Q001"
    query: "RESTful Webサービスのハンドラキュー構成"
    category: "handler_queue"
    expected_docs:
      - doc_id: "doc_rest_handler_queue_001"
        relevance: 3   # Perfect
        description: "REST用ハンドラキュー設定ガイド"
      - doc_id: "doc_handler_queue_general_001"
        relevance: 2   # Highly Relevant
        description: "ハンドラキュー共通設計パターン"
      - doc_id: "doc_jaxrs_setup_001"
        relevance: 1   # Marginally Relevant
        description: "JAX-RSセットアップガイド"
    metadata:
      language: "ja"
      query_type: "documentation"
      difficulty: "medium"
```

### 3.2 カテゴリ配分

| カテゴリ | 件数 | 説明 |
|---------|------|------|
| handler_queue | 10 | ハンドラキュー構成・設計・制約に関するクエリ |
| api_usage | 10 | API使い方（DAO、Validation、Repository等） |
| design_pattern | 10 | 設計パターン（アーキテクチャ、ハンドラ設計等） |
| troubleshooting | 10 | トラブルシューティング（エラー解決、設定不備等） |
| configuration | 10 | 設定方法（XML設定、プロパティ設定等） |
| **合計** | **50** | |

### 3.3 クエリの多様性要件

各カテゴリ内で以下の多様性を確保する：

| 軸 | バリエーション |
|-----|------------|
| 言語 | 日本語(7), 英語(2), 混在(1) / カテゴリ |
| 具体度 | 具体的(4), 抽象的(3), FQCN指定(3) / カテゴリ |
| クエリ長 | 短(3語以下): 3, 中(4-8語): 4, 長(9語以上): 3 / カテゴリ |

---

## 4. 評価データセット（50件）

### 4.1 handler_queue カテゴリ（10件）

| ID | クエリ | 難易度 | 言語 |
|----|-------|--------|------|
| Q001 | RESTful Webサービスのハンドラキュー構成 | medium | ja |
| Q002 | バッチアプリケーションのハンドラキューに必要なハンドラ | medium | ja |
| Q003 | handler queue order constraints for web application | medium | en |
| Q004 | ThreadContextHandler の配置ルール | easy | ja |
| Q005 | nablarch.fw.handler.GlobalErrorHandler | easy | mixed |
| Q006 | メッセージング処理のリトライハンドラ設定 | hard | ja |
| Q007 | ハンドラキューにカスタムハンドラを追加する方法 | medium | ja |
| Q008 | Jakarta Batch のハンドラキューとWebの違い | hard | ja |
| Q009 | HttpAccessLogHandler configuration | medium | en |
| Q010 | ハンドラの実行順序が正しくない場合のデバッグ方法 | hard | ja |

### 4.2 api_usage カテゴリ（10件）

| ID | クエリ | 難易度 | 言語 |
|----|-------|--------|------|
| Q011 | Universal DAOでのデータベース検索方法 | easy | ja |
| Q012 | BeanValidation でカスタムバリデータを作成する | medium | ja |
| Q013 | SystemRepository コンポーネント定義と取得 | medium | ja |
| Q014 | nablarch.common.dao.UniversalDao usage | easy | en |
| Q015 | HTTPリクエストのバリデーション実装パターン | medium | ja |
| Q016 | ファイルダウンロードの実装方法（StreamResponse） | hard | ja |
| Q017 | nablarch.fw.web.HttpResponse のステータスコード設定 | easy | mixed |
| Q018 | メッセージ管理（多言語対応メッセージの定義と取得） | hard | ja |
| Q019 | 排他制御（楽観ロック）の実装 | medium | ja |
| Q020 | code management using CodeManager | medium | en |

### 4.3 design_pattern カテゴリ（10件）

| ID | クエリ | 難易度 | 言語 |
|----|-------|--------|------|
| Q021 | Nablarchのアクションクラス設計パターン | easy | ja |
| Q022 | ドメインバリデーション戦略の設計 | medium | ja |
| Q023 | form and entity mapping pattern | medium | en |
| Q024 | マルチプロセス設計（バッチの並列実行） | hard | ja |
| Q025 | 業務アクションとライブラリの責務分離 | medium | ja |
| Q026 | Nablarchアプリケーションのレイヤ構造 | easy | ja |
| Q027 | 共通処理をハンドラで実装する設計パターン | medium | ja |
| Q028 | テスタブルなアクション設計（依存性の注入） | hard | ja |
| Q029 | nablarch error handling design pattern | medium | en |
| Q030 | RESTful APIのレスポンス設計（エラーレスポンス形式） | medium | ja |

### 4.4 troubleshooting カテゴリ（10件）

| ID | クエリ | 難易度 | 言語 |
|----|-------|--------|------|
| Q031 | SystemRepository初期化エラーの対処法 | easy | ja |
| Q032 | ハンドラキューでNullPointerExceptionが発生する | medium | ja |
| Q033 | DBコネクション枯渇の原因と対策 | hard | ja |
| Q034 | web-component-configuration.xml が読み込まれない | medium | ja |
| Q035 | ClassNotFoundException nablarch component | medium | mixed |
| Q036 | バッチ処理が途中で停止する原因の調査方法 | hard | ja |
| Q037 | バリデーションエラーメッセージが表示されない | medium | ja |
| Q038 | application log output not working | medium | en |
| Q039 | セッションストアの設定不備によるエラー | medium | ja |
| Q040 | マルチスレッド環境でのリソースリーク検出 | hard | ja |

### 4.5 configuration カテゴリ（10件）

| ID | クエリ | 難易度 | 言語 |
|----|-------|--------|------|
| Q041 | データベース接続設定（env.config） | easy | ja |
| Q042 | ログ出力設定（log.properties のカスタマイズ） | medium | ja |
| Q043 | nablarch XML configuration for REST service | medium | en |
| Q044 | メッセージングの受信キュー設定 | medium | ja |
| Q045 | 開発環境と本番環境のプロファイル切り替え | hard | ja |
| Q046 | HTTPセッションのタイムアウト設定 | easy | ja |
| Q047 | nablarch.fw.web.servlet.NablarchServletContextListener設定 | easy | mixed |
| Q048 | テスト用のコンポーネント定義オーバーライド方法 | hard | ja |
| Q049 | batch application configuration setup | medium | en |
| Q050 | CSRFトークン検証の有効化設定 | medium | ja |

---

## 5. A/Bテスト方式

### 5.1 テスト構成

全50クエリを5つの検索方式で実行し、メトリクスを比較する。

```
評価データセット（50クエリ）
    │
    ├──→ [A] BM25 only        → MRR, Recall@5, NDCG@5
    ├──→ [B] Vector only      → MRR, Recall@5, NDCG@5
    ├──→ [C] Hybrid (RRF)     → MRR, Recall@5, NDCG@5
    ├──→ [D] Hybrid + Rerank  → MRR, Recall@5, NDCG@5
    └──→ [E] Phase 1 Baseline → MRR, Recall@5, NDCG@5
```

### 5.2 制御変数

テスト間で以下のパラメータを固定する：

| パラメータ | 値 | 理由 |
|----------|-----|------|
| top_k | 10 | デフォルト設定 |
| candidateK（RRF入力） | 50 | 設計書デフォルト |
| RRF k | 60 | 設計書デフォルト |
| α（重み付け） | 0.3 | 設計書デフォルト |
| リランキング候補数 | 50 | 設計書デフォルト |

### 5.3 パラメータ感度分析

A/Bテスト後、最良方式に対してパラメータ感度分析を実施する。

| パラメータ | テスト範囲 | ステップ |
|----------|----------|---------|
| RRF k | 10, 30, 60, 100, 200 | 5値 |
| α | 0.0, 0.1, 0.3, 0.5, 0.7, 1.0 | 6値 |
| candidateK | 20, 50, 100 | 3値 |
| リランキング有無 | on / off | 2値 |

### 5.4 統計的有意性検定

方式間の差が統計的に有意かを検証するため、以下の検定を適用する：

- **対応ありt検定**: 同一クエリセットに対する2方式のメトリクス比較
- **有意水準**: α = 0.05
- **効果量**: Cohen's d ≥ 0.3 を有意な改善とみなす

---

## 6. 自動評価スクリプト設計

### 6.1 評価フレームワーク

```
src/test/java/com/tis/nablarch/mcp/rag/evaluation/
├── SearchEvaluationRunner.java       ← 評価実行メイン
├── MetricsCalculator.java            ← メトリクス計算
├── EvaluationDataset.java            ← データセット読み込み
├── EvaluationReport.java             ← レポート生成
└── model/
    ├── EvaluationQuery.java          ← クエリ + 正解データ
    ├── EvaluationResult.java         ← 1クエリの評価結果
    └── EvaluationSummary.java        ← 全体サマリ
```

### 6.2 SearchEvaluationRunner

```java
package com.tis.nablarch.mcp.rag.evaluation;

/**
 * 検索品質評価ランナー。
 * 評価データセットに対して各検索方式を実行し、メトリクスを計算する。
 */
@Tag("evaluation")
public class SearchEvaluationRunner {

    /**
     * 全検索方式のA/Bテストを実行する。
     */
    @Test
    void runFullEvaluation() {
        EvaluationDataset dataset = EvaluationDataset.load(
            "classpath:evaluation/evaluation-dataset.yaml");

        Map<String, EvaluationSummary> results = new LinkedHashMap<>();

        // [A] BM25 only
        results.put("BM25", evaluate(dataset, SearchMode.KEYWORD, false));

        // [B] Vector only
        results.put("Vector", evaluate(dataset, SearchMode.VECTOR, false));

        // [C] Hybrid (RRF)
        results.put("Hybrid", evaluate(dataset, SearchMode.HYBRID, false));

        // [D] Hybrid + Rerank
        results.put("Hybrid+Rerank", evaluate(dataset, SearchMode.HYBRID, true));

        // レポート生成
        EvaluationReport.generate(results, "docs/search-quality-report.md");
    }

    private EvaluationSummary evaluate(
            EvaluationDataset dataset, SearchMode mode, boolean rerank) {

        List<EvaluationResult> queryResults = new ArrayList<>();

        for (EvaluationQuery query : dataset.queries()) {
            List<SearchResult> searchResults;
            long startTime = System.nanoTime();

            if (rerank) {
                searchResults = ragPipeline.search(
                    query.queryText(), SearchFilters.NONE, 10, mode);
            } else {
                searchResults = hybridSearchService.search(
                    query.queryText(), SearchFilters.NONE, 10, mode);
            }

            long latencyMs = (System.nanoTime() - startTime) / 1_000_000;

            queryResults.add(new EvaluationResult(
                query, searchResults, latencyMs));
        }

        return MetricsCalculator.summarize(queryResults);
    }
}
```

### 6.3 MetricsCalculator

```java
package com.tis.nablarch.mcp.rag.evaluation;

/**
 * 検索品質メトリクス計算。
 */
public class MetricsCalculator {

    /**
     * MRR（Mean Reciprocal Rank）を計算する。
     */
    public static double calculateMRR(List<EvaluationResult> results) {
        double sum = 0.0;
        for (EvaluationResult r : results) {
            int rank = findFirstRelevantRank(r.searchResults(), r.query().expectedDocs());
            if (rank > 0) {
                sum += 1.0 / rank;
            }
        }
        return sum / results.size();
    }

    /**
     * Recall@K を計算する。
     */
    public static double calculateRecallAtK(List<EvaluationResult> results, int k) {
        double sum = 0.0;
        for (EvaluationResult r : results) {
            Set<String> topKIds = r.searchResults().stream()
                .limit(k)
                .map(SearchResult::id)
                .collect(Collectors.toSet());
            Set<String> relevantIds = r.query().expectedDocs().stream()
                .filter(d -> d.relevance() >= 2)  // Highly Relevant以上
                .map(ExpectedDoc::docId)
                .collect(Collectors.toSet());
            if (!relevantIds.isEmpty()) {
                long hits = relevantIds.stream()
                    .filter(topKIds::contains).count();
                sum += (double) hits / relevantIds.size();
            }
        }
        return sum / results.size();
    }

    /**
     * NDCG@K を計算する。
     */
    public static double calculateNDCGAtK(List<EvaluationResult> results, int k) {
        double sum = 0.0;
        for (EvaluationResult r : results) {
            double dcg = calculateDCG(r.searchResults(), r.query().expectedDocs(), k);
            double idcg = calculateIDCG(r.query().expectedDocs(), k);
            if (idcg > 0) {
                sum += dcg / idcg;
            }
        }
        return sum / results.size();
    }

    private static double calculateDCG(
            List<SearchResult> results,
            List<ExpectedDoc> expectedDocs, int k) {
        Map<String, Integer> relevanceMap = expectedDocs.stream()
            .collect(Collectors.toMap(ExpectedDoc::docId, ExpectedDoc::relevance));

        double dcg = 0.0;
        for (int i = 0; i < Math.min(results.size(), k); i++) {
            int rel = relevanceMap.getOrDefault(results.get(i).id(), 0);
            dcg += (Math.pow(2, rel) - 1) / (Math.log(i + 2) / Math.log(2));
        }
        return dcg;
    }

    private static double calculateIDCG(List<ExpectedDoc> expectedDocs, int k) {
        List<Integer> sortedRels = expectedDocs.stream()
            .map(ExpectedDoc::relevance)
            .sorted(Comparator.reverseOrder())
            .limit(k)
            .toList();

        double idcg = 0.0;
        for (int i = 0; i < sortedRels.size(); i++) {
            idcg += (Math.pow(2, sortedRels.get(i)) - 1) / (Math.log(i + 2) / Math.log(2));
        }
        return idcg;
    }

    /**
     * 全メトリクスのサマリを生成する。
     */
    public static EvaluationSummary summarize(List<EvaluationResult> results) {
        return new EvaluationSummary(
            calculateMRR(results),
            calculateRecallAtK(results, 5),
            calculateNDCGAtK(results, 5),
            calculateRecallAtK(results, 10),
            calculateNDCGAtK(results, 10),
            calculateLatencyStats(results)
        );
    }
}
```

### 6.4 実行方法

```bash
# 評価テスト実行（@Tag("evaluation") でフィルタ）
./mvnw test -Dgroups='evaluation'

# レポート出力先
# docs/search-quality-report.md
```

---

## 7. 評価レポートフォーマット

### 7.1 レポート構造

```markdown
# 検索品質評価レポート

## 概要
- 評価日: YYYY-MM-DD
- データセット: 50クエリ（5カテゴリ × 10件）
- 検索方式: 5パターン

## メトリクス比較

| 方式 | MRR | Recall@5 | NDCG@5 | Recall@10 | NDCG@10 | P50(ms) | P95(ms) |
|------|-----|----------|--------|-----------|---------|---------|---------|
| [A] BM25 only | 0.XX | 0.XX | 0.XX | 0.XX | 0.XX | XX | XX |
| [B] Vector only | 0.XX | 0.XX | 0.XX | 0.XX | 0.XX | XX | XX |
| [C] Hybrid | 0.XX | 0.XX | 0.XX | 0.XX | 0.XX | XX | XX |
| [D] Hybrid+Rerank | 0.XX | 0.XX | 0.XX | 0.XX | 0.XX | XX | XX |
| [E] Phase 1 | 0.XX | 0.XX | 0.XX | 0.XX | 0.XX | XX | XX |
| **目標** | **≥0.70** | **≥0.80** | **≥0.70** | — | — | **≤200** | **≤300** |

## カテゴリ別分析

（カテゴリ別のメトリクス内訳テーブル）

## 失敗分析

（目標未達のクエリの詳細分析）

## 推奨事項

（チューニング提案）
```

---

## 8. チューニング指針

### 8.1 メトリクス別チューニング

| 問題 | 考えられる原因 | チューニング |
|------|-------------|------------|
| MRR < 0.70 | 正解が上位に来ない | α値調整（BM25/Vector重み）、リランキング強化 |
| Recall@5 < 0.80 | 正解が結果に含まれない | candidateK増加、クエリ拡張強化 |
| NDCG@5 < 0.70 | 順序が適切でない | リランキングモデル変更、RRF k値調整 |
| レイテンシ > 300ms | 検索処理が遅い | Embeddingキャッシュ、インデックス最適化 |

### 8.2 反復改善プロセス

```
1. ベースライン評価（全5方式）
    │
    ▼
2. 最良方式の特定
    │
    ▼
3. パラメータ感度分析
    │
    ▼
4. パラメータ調整
    │
    ▼
5. 再評価 → 目標達成まで 3-4 を繰り返す
    │
    ▼
6. 最終レポート生成
```

---

## 付録

### A. 関連WBSタスク

| WBS | タスク | 本設計との関係 |
|-----|-------|-------------|
| 2.1.4 | ハイブリッド検索設計 | 評価対象の検索方式 C, D |
| 2.1.5 | リランキング設計 | 評価対象の検索方式 D |
| 2.1.6 | semantic_search Tool設計 | 評価対象のツール |
| 2.3.8 | RAG検索品質評価 | **本設計書を実行** |
| 2.4.3 | 検索品質評価レポート | 本設計の出力レポート |

### B. 評価データセットメンテナンス

- 初期バージョン（v1.0）は本設計書で定義した50件
- ドキュメント取り込み完了後、expected_doc_ids を実際のチャンクIDにマッピング
- 新しいドキュメントソース追加時にクエリを追加し、データセットをバージョンアップ
- 目標: Phase 2完了時に100件以上の評価データセットを構築

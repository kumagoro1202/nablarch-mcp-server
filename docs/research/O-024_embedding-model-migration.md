# O-024: Embeddingモデル無償化移行調査レポート

> **ステータス**: 完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru8 (subtask_068 / cmd_033)
> **関連文書**: architecture.md §4.4, rag-pipeline-spec.md §2.5, database-schema.md, ADR-001

---

## 目次

1. [エグゼクティブサマリ](#1-エグゼクティブサマリ)
2. [現行アーキテクチャの分析](#2-現行アーキテクチャの分析)
3. [無償OSSモデル候補一覧](#3-無償ossモデル候補一覧)
4. [ドキュメント用モデル詳細比較](#4-ドキュメント用モデル詳細比較)
5. [コード用モデル詳細比較](#5-コード用モデル詳細比較)
6. [切り替えに必要な改修範囲](#6-切り替えに必要な改修範囲)
7. [推奨案](#7-推奨案)
8. [切り替え手順概要](#8-切り替え手順概要)
9. [リスクと対策](#9-リスクと対策)
10. [参考文献](#10-参考文献)

---

## 1. エグゼクティブサマリ

### 1.1 調査目的

現行のNablarch MCPサーバーRAGパイプラインでは、有償Embedding API（Jina embeddings-v4、Voyage-code-3）を使用している。本調査は、無償のローカル実行可能なOSSモデルへの切り替え可能性を評価し、推奨案を提示するものである。

### 1.2 結論

| 用途 | 現行モデル | 推奨移行先 | 次元数 | スキーマ変更 |
|------|-----------|-----------|--------|------------|
| ドキュメント用 | Jina embeddings-v4（有償API） | **BAAI/bge-m3** | 1024 | **不要** |
| コード用 | Voyage-code-3（有償API） | **CodeSage-small-v2** | 1024 | **不要** |

**移行の最大の利点**:
- API利用料金が**ゼロ**になる（現行は従量課金）
- ネットワーク依存が解消され、**オフライン実行**が可能になる
- 推論レイテンシの削減（ローカル実行によりネットワーク遅延を排除）
- pgvectorスキーマ変更**不要**（両推奨モデルとも1024次元出力）

**トレードオフ**:
- サーバーのメモリ消費量が増加（モデルをメモリにロードする必要がある）
- 初回起動時のモデルロード時間が発生
- 一部ベンチマークでは有償モデルにわずかに劣る可能性がある

### 1.3 殿への確認事項（要対応）

1. **推奨案の承認**: BGE-M3（ドキュメント用）+ CodeSage-small-v2（コード用）の組み合わせでよいか
2. **GPU環境の有無**: サーバー環境にGPUがあればより高性能なモデル（Qwen3-Embedding-4B等）も選択可能。CPU前提か
3. **既存ベクトルデータの扱い**: 全チャンクの再Embedding生成が必要。許容できるダウンタイムの目安

---

## 2. 現行アーキテクチャの分析

### 2.1 現行Embeddingモデル

| 項目 | Jina embeddings-v4（ドキュメント用） | Voyage-code-3（コード用） |
|------|--------------------------------------|--------------------------|
| パラメータ数 | 3.8B | 非公開 |
| 次元数 | 1024 | 1024 |
| 多言語対応 | 89言語 | あり |
| コンテキスト長 | 32Kトークン | 非公開 |
| API | `https://api.jina.ai/v1/embeddings` | `https://api.voyageai.com/v1/embeddings` |
| ライセンス | OSSモデルだがAPIは有償 | 商用（APIのみ） |
| 課金 | 従量課金（トークン数ベース） | 従量課金（トークン数ベース） |
| CoIRスコア | — | 77.33 |

### 2.2 現行実装の構造

Embeddingクライアントは`EmbeddingClient`インターフェースで適切に抽象化されている。

```
src/main/java/com/tis/nablarch/mcp/embedding/
├── EmbeddingClient.java          # インターフェース（4メソッド）
├── JinaEmbeddingClient.java      # Jina v4実装（@Component）
├── VoyageEmbeddingClient.java    # Voyage-code-3実装（@Component）
├── EmbeddingRequest.java         # APIリクエストDTO
├── EmbeddingResponse.java        # APIレスポンスDTO
├── EmbeddingException.java       # 例外クラス
└── config/
    └── EmbeddingProperties.java  # 設定プロパティ（@ConfigurationProperties）
```

**EmbeddingClientインターフェース**:

```java
public interface EmbeddingClient {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    String getModelName();
    int getDimensions();
}
```

**設計上の優位点**（移行に有利な点）:
- インターフェースが適切に定義されており、新しい実装クラスを追加するだけで切り替え可能
- `@Qualifier`アノテーションでドキュメント用/コード用を区別する設計が既に存在
- `EmbeddingProperties.ProviderConfig`が`baseUrl`、`model`、`dimensions`等を外部設定化済み

### 2.3 pgvectorスキーマ

```sql
-- document_chunks テーブル
embedding   vector(1024)  -- Jina v4の次元数

-- code_chunks テーブル
embedding   vector(1024)  -- Voyage-code-3の次元数
```

**重要**: 両テーブルとも`vector(1024)`で統一されている。1024次元出力のモデルを選べばスキーマ変更不要。

### 2.4 現行のAPI呼び出しフロー

```
テキスト入力
    → EmbeddingClient.embed() / embedBatch()
    → WebClient HTTP POST（OpenAI互換API）
    → 外部APIサーバー（Jina / Voyage）
    → レスポンスからfloat[]を抽出
    → pgvectorに格納
```

移行後はHTTP呼び出しが**ローカル推論**に置き換わる。

---

## 3. 無償OSSモデル候補一覧

### 3.1 ドキュメント用Embeddingモデル候補

| # | モデル | パラメータ数 | 次元数 | 日本語 | ライセンス | MTEB多言語 | 特記事項 |
|---|--------|------------|--------|--------|-----------|-----------|---------|
| 1 | **BAAI/bge-m3** | 568M | 1024 | ◎ | MIT | 63.0 | Dense/Sparse/Multi-vector対応、8192トークン |
| 2 | **Qwen3-Embedding-0.6B** | 600M | 32-1024 | ◎ | Apache 2.0 | — | Matryoshka対応、100+言語 |
| 3 | **Qwen3-Embedding-4B** | 4B | 32-4096 | ◎ | Apache 2.0 | — | 高品質だがGPU推奨 |
| 4 | **Qwen3-Embedding-8B** | 8B | 32-4096 | ◎ | Apache 2.0 | 70.58 (#1) | MTEB多言語1位、GPU必須 |
| 5 | **intfloat/multilingual-e5-large-instruct** | 560M | 1024 | ○ | MIT | ~61.8 | Mistralベース、安定 |
| 6 | **Nomic Embed Text V2 (MoE)** | ~300M | 256-768 | ○ | Apache 2.0 | — | MoE構造、軽量 |
| 7 | **EmbeddingGemma-300M** | 300M | 128-768 | ○ | Apache 2.0 | — | Google DeepMind、超軽量 |
| 8 | **cl-nagoya/ruri-v3-310m** | 310M | — | ◎◎ | — | JMTEB 75.85 | 日本語特化、JMTEB上位 |

### 3.2 コード用Embeddingモデル候補

| # | モデル | パラメータ数 | 次元数 | Java対応 | ライセンス | 特記事項 |
|---|--------|------------|--------|----------|-----------|---------|
| 1 | **codesage/codesage-small-v2** | 130M | 1024 | ◎（明示的） | OSS | Matryoshka対応、9言語 |
| 2 | **codesage/codesage-base-v2** | 356M | 1024 | ◎（明示的） | OSS | 高精度、Matryoshka対応 |
| 3 | **nomic-ai/CodeRankEmbed** | 137M | 768 | ◎ | Apache 2.0 | 軽量SOTA、8192トークン |
| 4 | **nomic-ai/nomic-embed-code** | 7B | — | ◎ | Apache 2.0 | CodeSearchNet SOTA、GPU必須 |
| 5 | **Qwen3-Embedding-0.6B** | 600M | 32-1024 | ○ | Apache 2.0 | 汎用だがコードも対応 |
| 6 | **codesage/codesage-large-v2** | 1.3B | 2048 | ◎（明示的） | OSS | 最高精度、GPU推奨 |

---

## 4. ドキュメント用モデル詳細比較

### 4.1 BAAI/bge-m3（推奨第1位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 568M |
| 次元数 | **1024**（現行と同一） |
| コンテキスト長 | 8192トークン |
| 多言語対応 | 100+言語 |
| 日本語性能 | MIRACL-ja: 0.91, JMTEB: 中上位 |
| メモリ使用量 | ~1.06 GB（fp16） |
| ライセンス | MIT |
| GPU | 推奨だがCPUでも動作可能 |
| ONNX対応 | HuggingFace Optimumで変換可能 |
| 検索方式 | Dense / Sparse / Multi-vector 同時対応 |

**推奨理由**:
1. **次元数1024**: pgvectorスキーマ変更不要（`vector(1024)`そのまま）
2. **日本語性能**: Nablarchドキュメントは日本語が主。MIRACL日本語検索で0.91と高スコア
3. **ハイブリッド検索との親和性**: Dense + Sparseを1モデルで生成でき、現行のBM25+Vectorハイブリッド検索と相性が良い
4. **メモリ効率**: ~1GBで動作可能。CPU推論もサポート
5. **MITライセンス**: 商用利用に制約なし
6. **成熟度**: 広く採用されており、コミュニティサポートが充実

**注意点**:
- 日本語特化モデル（ruri、sarashina-embedding）にはJMTEBで劣る
- Jina v4の32Kコンテキストと比較すると8192トークンに制限される（ただし現行チャンキングは512トークンなので問題なし）

### 4.2 Qwen3-Embedding-0.6B（推奨第2位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 600M |
| 次元数 | 32-1024（Matryoshka） |
| コンテキスト長 | 32Kトークン |
| 多言語対応 | 100+言語 |
| ライセンス | Apache 2.0 |
| GPU | 推奨だがCPUでも動作可能 |

**推奨理由**:
1. 1024次元出力をサポート（Matryoshka Learning）→ スキーマ変更不要
2. MTEB多言語リーダーボードでQwen3シリーズは1位（8Bモデル）。0.6Bは軽量かつ高品質
3. Apache 2.0ライセンス
4. 32Kコンテキストで現行Jina v4と同等

**注意点**:
- 2025年後半リリースのため、コミュニティ実績がBGE-M3ほど豊富ではない
- ONNX変換の実績が限定的

### 4.3 cl-nagoya/ruri-v3-310m（推奨第3位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 310M |
| 次元数 | — |
| 日本語性能 | JMTEB: 75.85（上位） |
| ライセンス | オープン |
| GPU | CPUでも軽量に動作 |

**推奨理由**:
1. JMTEB（日本語Embedding評価ベンチマーク）で75.85と上位
2. 日本語ドキュメント検索に最適化
3. 軽量（310M）でCPU推論が実用的

**注意点**:
- 次元数が1024でない場合、pgvectorスキーマ変更が必要
- 英語コンテンツ（Javadoc等）の品質が多言語モデルに劣る可能性
- 日本語特化のため、将来的に英語ドキュメントを追加する場合の拡張性に制限

---

## 5. コード用モデル詳細比較

### 5.1 codesage/codesage-small-v2（推奨第1位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 130M |
| 次元数 | **1024**（現行と同一） |
| 対応言語 | Java, Python, Go, C, C#, JS, TS, PHP, Ruby |
| ライセンス | OSS |
| GPU | CPUで十分実用的 |
| Matryoshka | 対応（次元数を柔軟に変更可能） |
| 学習データ | The Stack V2（デュアルコンシステンシーフィルタリング） |

**推奨理由**:
1. **次元数1024**: pgvectorスキーマ変更不要
2. **Javaの明示的サポート**: 9言語にJavaが含まれ、Java/XMLコードの埋め込みに最適化
3. **軽量（130M）**: CPUのみで高速推論可能。メモリ消費が少ない
4. **V2の品質改善**: V1から10%以上の品質改善（Code2Code検索）
5. **Matryoshka対応**: 必要に応じて次元数を調整可能
6. **SentenceTransformers対応**: HuggingFace/SentenceTransformersから直接利用可能

**注意点**:
- XMLの明示的サポートはないが、テキストとして処理可能
- Voyage-code-3のCoIR 77.33には劣る可能性

### 5.2 nomic-ai/CodeRankEmbed（推奨第2位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 137M |
| 次元数 | 768 |
| 対応言語 | Python, Java, Ruby, PHP, JS, Go |
| コンテキスト長 | 8192トークン |
| ライセンス | Apache 2.0 |
| メモリ | ~521MB |

**推奨理由**:
1. CodeSearchNetベンチマークでSOTA（モデルサイズ対比）
2. Apache 2.0ライセンス
3. 軽量でデプロイが容易

**注意点**:
- **次元数768**: `vector(1024)`から`vector(768)`へのスキーマ変更が必要
- 再Embeddingだけでなく、DBマイグレーションも必要

### 5.3 codesage/codesage-base-v2（推奨第3位）

| 項目 | 値 |
|------|-----|
| パラメータ数 | 356M |
| 次元数 | **1024** |
| 対応言語 | Java含む9言語 |
| ライセンス | OSS |

**推奨理由**:
1. 次元数1024でスキーマ変更不要
2. Smallより高精度（パラメータ数2.7倍）
3. Java明示的サポート

**注意点**:
- Smallの約3倍のメモリが必要
- CPU推論は可能だが、Smallと比較すると遅い

---

## 6. 切り替えに必要な改修範囲

### 6.1 改修概要

| 改修対象 | 影響度 | 内容 |
|---------|--------|------|
| **新規: LocalEmbeddingClient** | 中 | ONNX Runtime/DJLを使ったローカル推論クライアントの実装 |
| **変更: EmbeddingProperties** | 小 | ローカルモデル用のプロパティ追加（モデルパス等） |
| **変更: application.yaml** | 小 | ローカルモデル設定の追加 |
| **変更: build.gradle** | 小 | ONNX Runtime/DJL依存関係の追加 |
| **既存維持: EmbeddingClient** | なし | インターフェースは変更不要 |
| **既存維持: pgvectorスキーマ** | なし | 1024次元モデルを選択すれば変更不要 |
| **既存維持: RAGパイプライン** | なし | HybridSearchService等は変更不要 |
| **データ: 再Embedding生成** | 大 | 全チャンク（約4万件）の再Embedding生成が必要 |

### 6.2 EmbeddingClientインターフェースの互換性

現行インターフェースの4メソッドに対する互換性:

| メソッド | 互換性 | 備考 |
|---------|--------|------|
| `embed(String text)` | ◎ | そのまま実装可能 |
| `embedBatch(List<String> texts)` | ◎ | バッチ推論で対応 |
| `getModelName()` | ◎ | モデル名を返すだけ |
| `getDimensions()` | ◎ | 1024を返す |

### 6.3 Java + ONNX Runtimeでのローカル推論

Spring AIがONNX Transformer Embeddingsの公式サポートを提供している。

**必要な依存関係**:

```groovy
// Spring AI ONNX Transformer Embedding
implementation("org.springframework.ai:spring-ai-starter-model-transformers")

// または手動で
implementation("com.microsoft.onnxruntime:onnxruntime:1.x")
implementation("ai.djl.huggingface:tokenizers:0.x")
```

**推論フロー**:

```
テキスト入力
    → HuggingFaceTokenizer（トークナイズ）
    → OrtSession.run()（ONNX推論）
    → Mean Pooling + L2正規化
    → float[1024]
```

**モデルの準備手順**:

```bash
# HuggingFaceモデルをONNX形式に変換
pip install optimum
optimum-cli export onnx --model BAAI/bge-m3 ./bge-m3-onnx/
optimum-cli export onnx --model codesage/codesage-small-v2 ./codesage-small-v2-onnx/
```

### 6.4 新規クラスの設計概要

```
embedding/
├── EmbeddingClient.java               # 既存（変更なし）
├── JinaEmbeddingClient.java           # 既存（廃止予定だが残しておく）
├── VoyageEmbeddingClient.java         # 既存（廃止予定だが残しておく）
├── local/
│   ├── OnnxEmbeddingClient.java       # 新規: ONNX Runtimeベースの実装
│   └── OnnxModelConfig.java           # 新規: モデルパス等の設定
├── EmbeddingRequest.java              # 既存（変更なし）
├── EmbeddingResponse.java             # 既存（変更なし）
├── EmbeddingException.java            # 既存（変更なし）
└── config/
    └── EmbeddingProperties.java       # 既存（local設定の追加）
```

### 6.5 pgvectorスキーマへの影響

推奨案（BGE-M3 + CodeSage-small-v2）を採用した場合:

| テーブル | 現行 | 移行後 | スキーマ変更 |
|---------|------|--------|------------|
| document_chunks | `vector(1024)` | `vector(1024)` | **不要** |
| code_chunks | `vector(1024)` | `vector(1024)` | **不要** |
| ivfflatインデックス | lists=100 | lists=100 | **再構築が必要**（データが変わるため） |

### 6.6 既存取込パイプラインへの影響

| コンポーネント | 影響 | 備考 |
|---------------|------|------|
| OfficialDocsIngester | **なし** | EmbeddingClientインターフェース経由のため |
| FintanIngester | **なし** | 同上 |
| ChunkingService | **なし** | Embedding生成とは独立 |
| HybridSearchService | **なし** | 検索クエリのEmbedding生成もEmbeddingClient経由 |
| VectorSearchService | **なし** | pgvectorクエリは次元数に依存しない |
| CrossEncoderReranker | **なし** | リランキングはEmbeddingとは独立 |

---

## 7. 推奨案

### 7.1 推奨構成（CPU環境想定）

```
┌──────────────────────────────────────────────────────────────┐
│              移行後のEmbeddingアーキテクチャ                      │
│                                                               │
│  ┌──────────────────────────┐ ┌────────────────────────────┐ │
│  │ ドキュメント用            │ │ コード用                    │ │
│  │ BAAI/bge-m3              │ │ codesage/codesage-small-v2  │ │
│  │ 568M, 1024次元, MIT      │ │ 130M, 1024次元, OSS        │ │
│  │ ~1GB メモリ              │ │ ~260MB メモリ              │ │
│  │ 100+言語, 日本語◎        │ │ Java明示的対応             │ │
│  └────────────┬─────────────┘ └─────────────┬──────────────┘ │
│               │                              │                │
│  ┌────────────▼──────────────────────────────▼──────────────┐ │
│  │              ONNX Runtime (ローカル推論)                    │ │
│  │   • CPU推論（GPUなしで動作）                                │ │
│  │   • Spring AI TransformersEmbeddingModel 互換              │ │
│  └──────────────────────────┬───────────────────────────────┘ │
│                             │                                  │
│  ┌──────────────────────────▼──────────────────────────────┐  │
│  │              PostgreSQL + pgvector                        │  │
│  │   vector(1024) — スキーマ変更なし                          │  │
│  └────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### 7.2 推奨理由のサマリ

| 評価軸 | BGE-M3（ドキュメント） | CodeSage-small-v2（コード） |
|--------|----------------------|---------------------------|
| **次元数** | 1024（スキーマ変更不要） | 1024（スキーマ変更不要） |
| **日本語** | ◎（MIRACL-ja: 0.91） | —（コード用なので不要） |
| **Java対応** | — | ◎（9言語に明示的に含む） |
| **メモリ** | ~1GB（fp16） | ~260MB |
| **CPU推論** | 可能（やや遅い） | 高速（軽量） |
| **ライセンス** | MIT | OSS |
| **コスト** | 無償 | 無償 |
| **コミュニティ** | 大規模、多数の採用実績 | Amazon Science、ICLR 2024 |

### 7.3 代替案（GPU環境がある場合）

GPU環境（VRAM 8GB以上）が利用可能な場合は、より高性能な構成も可能:

| 用途 | 推奨モデル | 改善点 |
|------|-----------|--------|
| ドキュメント用 | Qwen3-Embedding-4B | MTEB多言語スコア大幅向上 |
| コード用 | codesage/codesage-base-v2 | Code2Codeで10%+精度向上 |

---

## 8. 切り替え手順概要

### Phase A: 準備（コード変更なし）

1. ONNXモデルファイルの準備
   - BGE-M3をONNX形式に変換
   - CodeSage-small-v2をONNX形式に変換
   - モデルファイルをリポジトリ外のストレージに配置（サイズが大きいためGit管理対象外）
2. ベンチマーク環境の構築
   - 現行モデルと新モデルで同一クエリセットの品質比較

### Phase B: 実装

1. `OnnxEmbeddingClient`の実装（`EmbeddingClient`インターフェース実装）
2. `EmbeddingProperties`にローカルモデル設定を追加
3. `build.gradle`にONNX Runtime / DJL依存関係を追加
4. プロファイル切り替え（`local` / `api`）で旧・新クライアントを選択可能にする
5. ユニットテスト・統合テストの追加

### Phase C: データ移行

1. 全チャンクの再Embedding生成（バッチ処理）
   - document_chunks: 約5,000件 × BGE-M3
   - code_chunks: 約20,000ファイル × CodeSage-small-v2
2. ivfflatインデックスの再構築
3. VACUUM ANALYZE実行

### Phase D: 検証

1. 検索品質評価（Phase 2の50クエリ評価データセットを使用）
2. MRR / Recall@5 / NDCG@5の比較
3. レイテンシ計測（API呼び出し vs ローカル推論）
4. メモリ使用量の監視

### 推定データ移行時間

| 項目 | チャンク数 | 推定速度（CPU） | 推定時間 |
|------|-----------|---------------|---------|
| document_chunks（BGE-M3） | ~5,000 | ~50件/秒 | ~2分 |
| code_chunks（CodeSage） | ~20,000 | ~100件/秒 | ~3分 |
| インデックス再構築 | — | — | ~1分 |
| **合計** | | | **~6分** |

---

## 9. リスクと対策

### 9.1 品質低下リスク

| リスク | 影響度 | 対策 |
|--------|--------|------|
| ドキュメント検索品質の低下 | 中 | Phase 2の評価フレームワークで定量比較。目標MRR≥0.70を維持 |
| コード検索品質の低下 | 中 | CodeSearchNetベンチマークで定量比較 |
| 日本語検索の劣化 | 低 | BGE-M3は日本語に強い。JMTEB評価で確認 |

### 9.2 運用リスク

| リスク | 影響度 | 対策 |
|--------|--------|------|
| メモリ不足 | 中 | 合計~1.3GB。サーバースペック確認。fp16/量子化で削減可能 |
| 初回起動遅延 | 低 | モデルロードに10-30秒。アプリケーション起動時に非同期ロード |
| ONNXモデル更新の手間 | 低 | Dockerイメージにモデルを同梱。バージョン管理 |

### 9.3 互換性リスク

| リスク | 影響度 | 対策 |
|--------|--------|------|
| 旧Embeddingとの非互換 | 高 | 全チャンクの再Embedding必須。新旧モデルの並行運用期間を設ける |
| ONNX変換時の精度差 | 低 | fp32で変換し、元モデルとのコサイン類似度で検証 |

---

## 10. 参考文献

### モデル情報

| モデル | URL |
|--------|-----|
| BAAI/bge-m3 | https://huggingface.co/BAAI/bge-m3 |
| Qwen3-Embedding | https://github.com/QwenLM/Qwen3-Embedding |
| codesage/codesage-small-v2 | https://huggingface.co/codesage/codesage-small-v2 |
| nomic-ai/CodeRankEmbed | https://huggingface.co/nomic-ai/CodeRankEmbed |
| nomic-ai/nomic-embed-code | https://huggingface.co/nomic-ai/nomic-embed-code |
| cl-nagoya/ruri-v3-310m | JMTEB leaderboard |
| EmbeddingGemma-300M | Google DeepMind |

### ベンチマーク

| ベンチマーク | URL |
|------------|-----|
| MTEB Leaderboard | https://huggingface.co/spaces/mteb/leaderboard |
| JMTEB (日本語) | https://github.com/sbintuitions/JMTEB |
| CodeSearchNet | CoRNStack論文 (ICLR 2025) |

### Java統合

| 技術 | URL |
|------|-----|
| Spring AI ONNX Embeddings | https://docs.spring.io/spring-ai/reference/api/embeddings/onnx.html |
| ONNX Runtime Java | https://onnxruntime.ai/ |
| DJL HuggingFace Tokenizer | https://docs.djl.ai/ |

### 内部ドキュメント

| ドキュメント | パス |
|------------|------|
| アーキテクチャ設計書 §4.4 | docs/architecture.md |
| RAGパイプライン仕様書 §2.5 | docs/rag-pipeline-spec.md |
| DBスキーマドキュメント | docs/database-schema.md |
| 検索品質評価レポート | docs/search-quality-report.md |
| ADR-001 | docs/decisions/ADR-001_rag-enhanced-architecture.md |

---

> **本レポートの結論**: 無償OSSモデルへの移行は**技術的に実現可能**であり、**pgvectorスキーマ変更なし**で実施できる。推奨構成（BGE-M3 + CodeSage-small-v2）はCPU環境で動作し、ランニングコストをゼロにできる。殿の判断を仰ぐ。

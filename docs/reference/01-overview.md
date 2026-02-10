# Nablarch MCP Server — プロジェクト概要

> **対象読者**: 開発者（プロジェクトの全体像を理解したい人）
> **前提知識**: Java, MCPの基本概念
> **概要**: プロジェクトビジョン、対象ユーザー、機能概要、ロードマップ

---

## 目次

1. [プロジェクトの目的・ビジョン](#1-プロジェクトの目的ビジョン)
2. [RAG-enhanced MCPサーバーとは](#2-rag-enhanced-mcpサーバーとは)
3. [対象ユーザー](#3-対象ユーザー)
4. [主要機能の概要](#4-主要機能の概要)
5. [技術スタック](#5-技術スタック)
6. [プロジェクトロードマップ](#6-プロジェクトロードマップ)

---

## 1. プロジェクトの目的・ビジョン

### 解決すべき課題

Nablarchは、TIS株式会社が開発したミッションクリティカルシステム向けJavaアプリケーション開発・実行基盤である。金融・公共・製造業など大規模基幹システムの構築実績がある一方で、以下の深刻な情報不足問題を抱えている。

| 指標 | Nablarch | 参考: Spring Boot |
|------|----------|------------------|
| GitHub Stars | 42 | 約76,000 |
| Qiita記事数 | 14 | 数万件 |
| Zenn記事数 | ほぼ0 | 数千件 |
| Stack Overflow | ほぼ0 | 大量 |
| 公開求人数 | ほぼ0 | 1,130件（レバテック） |

開発者からは以下の声が上がっている。

- **「圧倒的、初心者殺しな情報のなさ。リファレンスがあるが使い方がわからない」**
- 「お世辞にもわかりやすいフレームワークではない」
- 「公式の『未経験者でもすぐに開発を始められる』は嘘だと思っている」

この情報不足は、開発者の学習コストを大幅に増大させ、生産性の低下を招いている。

### ビジョン

**Nablarch開発者がAIコーディングツールを最大限活用できるようにする。**

AIコーディングツール（Claude Code、GitHub Copilot、Cursor等）は急速に普及しているが、Nablarch固有の知識（ハンドラキューアーキテクチャ、XML設定方式、独自のDB/トランザクション管理等）を持たないため、Nablarch開発では効果が限定的である。

本プロジェクトは、Nablarchの公式ドキュメント・GitHub上の113リポジトリ・Javadoc・開発標準をAIが理解・活用できる形で提供するMCPサーバーを構築し、**AIの知識不足をインフラレベルで解消する**ことを目指す。

### 期待される効果

| 効果 | 説明 |
|------|------|
| **学習コスト削減** | AIがNablarchの概念・パターンをリアルタイムで解説 |
| **コード生成精度向上** | ハンドラキュー構成・アクションクラス等をNablarch規約に準拠して生成 |
| **トラブルシューティング高速化** | エラーメッセージからNablarch固有の原因を特定 |
| **設計品質向上** | ベストプラクティス・アンチパターンをAIが参照しながら設計支援 |
| **開発者体験の改善** | 「情報がない」問題をAIが補完し、Nablarch開発のハードルを大幅に下げる |

---

## 2. RAG-enhanced MCPサーバーとは

### MCP（Model Context Protocol）の概要

MCPは、Anthropicが2024年11月に発表したAIアプリケーションと外部システムを接続するためのオープン標準プロトコルである。Linux FoundationのAgentic AI Foundationに参画し、OpenAI・Google DeepMindなど主要AIプロバイダーにも採用されている。

```
┌─────────────────────────────────────────┐
│          AIコーディングツール               │
│   (Claude Code / Copilot / Cursor)       │
│  ┌──────────┐ ┌──────────┐              │
│  │MCP Client│ │MCP Client│ ...           │
│  └─────┬────┘ └─────┬────┘              │
└────────┼────────────┼───────────────────┘
         │            │  JSON-RPC 2.0
    ┌────▼────┐  ┌────▼────┐
    │MCP Server│  │MCP Server│
    │(Local)   │  │(Remote)  │
    └─────────┘  └─────────┘
```

MCPサーバーは3つのプリミティブを通じてAIに機能を提供する。

| プリミティブ | 制御主体 | 用途 | 例 |
|---|---|---|---|
| **Tools** | AIモデル | AIが呼び出す実行可能な関数 | API検索、ハンドラキュー検証、コード生成 |
| **Resources** | アプリケーション | 読み取り専用のデータソース | API仕様書、ハンドラカタログ、設計パターン |
| **Prompts** | ユーザー | 再利用可能なテンプレート | アプリ作成ガイド、トラブルシューティング手順 |

### RAG（Retrieval-Augmented Generation）の概要

RAGは、事前にインデックス化した外部知識をベクトル検索で取得し、LLMのコンテキストに注入する技術である。

```
ユーザーの質問
    │
    ▼
[Embedding] → [ベクトル検索] → [関連文書取得] → [コンテキスト構築] → [LLM生成]
                    │
              ┌─────▼─────┐
              │ ベクトルDB  │
              │ (Nablarch  │
              │  知識ベース) │
              └───────────┘
```

Nablarchの大量のドキュメント・コード・設定ファイルから、ユーザーの質問に最も関連する情報を高精度に検索できる。

### 両者を統合する意義 — 「AIが知って使う」

RAGとMCPは代替関係ではなく、**補完関係**にある。

| 技術 | 役割 | 比喩 |
|------|------|------|
| **RAG** | AIが「知る」ための仕組み | 図書館の蔵書検索 |
| **MCP** | AIが「使う」ための仕組み | 図書館の窓口サービス |
| **RAG+MCP** | AIが「知って使う」仕組み | 蔵書検索付き総合窓口 |

本プロジェクトでは、MCPサーバーの内部にRAGエンジンを組み込む**「RAG-enhanced MCP」**アーキテクチャを採用する。

```
┌──────────────────────────────────────────────────────┐
│          Nablarch MCPサーバー（RAG-enhanced）           │
│                                                       │
│  ┌─────────────────────────────────────────────┐     │
│  │              MCP Protocol Layer              │     │
│  │  Tools │ Resources │ Prompts                 │     │
│  └────────────────────┬────────────────────────┘     │
│                        │                              │
│  ┌─────────────────────▼────────────────────────┐     │
│  │              RAGエンジン（内蔵）                │     │
│  │  セマンティック検索 │ ハイブリッド検索 │ リランキング│     │
│  └─────────────────────┬────────────────────────┘     │
│                        │                              │
│  ┌─────────────────────▼────────────────────────┐     │
│  │             Nablarch知識ベース                 │     │
│  │  公式Docs │ GitHub 113リポ │ Javadoc │ Fintan  │     │
│  └──────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────┘
```

この統合により以下が実現する。

- **MCP単体では困難な大量知識の高精度検索**: RAGのセマンティック検索が解決
- **RAG単体では困難なAIツールとの標準的な統合**: MCPの標準プロトコルが解決
- **学術的にも実証済み**: RAG-MCP論文（arXiv:2505.03275）によると、RAGによるMCPツール選択でプロンプトトークン75%削減・精度3倍向上

---

## 3. 対象ユーザー

### 主要ターゲット

#### Nablarch開発者（初級〜エキスパート）

| レベル | 想定する課題 | 本サーバーの提供価値 |
|--------|------------|-------------------|
| **初級** | ハンドラキュー等の概念が理解しにくい。情報が少ない | 対話的な学習支援、段階的ガイド |
| **中級** | 設計パターンやベストプラクティスの情報が見つけにくい | セマンティック検索による関連情報の即時取得 |
| **上級** | バージョンアップ時の変更点把握、高度な設定最適化 | API変更検索、マイグレーション支援 |
| **エキスパート** | 新規メンバーへの教育コスト | AIを通じた知識の民主化 |

#### AIコーディングツール利用者

- **Claude Desktop**: GUIベースでNablarch知識を対話的に利用
- **Claude Code**: CLIベースでNablarch開発をAI支援
- **VS Code + GitHub Copilot**: エディタ内でNablarchコード補完を高精度化
- **Cursor**: AIファーストIDEでNablarch開発
- **JetBrains IDE + MCP対応プラグイン**: IntelliJ IDEA等から利用

#### Nablarchプロジェクトのリーダー・アーキテクト

- ハンドラキュー構成の妥当性検証
- コードレビュー支援（Nablarch規約準拠チェック）
- 新規プロジェクトの設計支援

---

## 4. 主要機能の概要

### ユースケース一覧

| # | ユースケース | カテゴリ | 概要 |
|---|-------------|---------|------|
| 1 | **ハンドラキュー構成の自動設計** | 設計支援 | アプリ種別・要件からハンドラキューXML構成を生成・検証 |
| 2 | **Nablarchコード生成** | コード生成 | アクションクラス、フォームBean、SQL定義等をNablarch規約に準拠して生成 |
| 3 | **トラブルシューティング** | 問題解決 | エラーメッセージからNablarch固有の原因を特定し、解決策を提示 |
| 4 | **学習支援** | 教育 | 初学者向けの段階的ガイド、概念説明、チュートリアル |
| 5 | **コードレビュー** | 品質保証 | Nablarch規約・パターンへの準拠チェック、アンチパターン検出 |
| 6 | **マイグレーション支援** | 保守 | Nablarch 5→6等のバージョンアップ時の変更点把握・影響分析 |
| 7 | **テスト生成** | テスト | Nablarch独自のExcelベーステスティングフレームワーク連携 |

### 提供予定のTools

| ツール名 | 説明 |
|---------|------|
| `semantic_search` | Nablarch知識ベースのセマンティック検索 |
| `search_api` | Nablarch APIのキーワード検索 |
| `generate_handler` | ハンドラキュー構成XMLの生成 |
| `generate_action` | 業務アクションクラスの生成 |
| `generate_form` | フォームBean/Entityの生成 |
| `generate_sql` | SQL定義ファイルの生成 |
| `generate_test` | JUnitテストコードの生成 |
| `validate_handler_queue` | ハンドラキュー構成の妥当性検証 |

### 提供予定のResources

| リソースURI | 説明 |
|------------|------|
| `nablarch://api/{module}/{class}` | API Javadocリファレンス |
| `nablarch://handler/{type}` | ハンドラ仕様（Web/Batch/REST等） |
| `nablarch://pattern/{name}` | 設計パターン・ベストプラクティス |
| `nablarch://antipattern/{name}` | アンチパターン集 |
| `nablarch://config/{type}` | 標準ハンドラキュー構成テンプレート |
| `nablarch://library/{name}` | ライブラリ仕様（DB、ログ、ファイル等） |
| `nablarch://guide/{topic}` | 開発ガイド |
| `nablarch://example/{type}` | 実装例・サンプルコード |
| `nablarch://version` | Nablarch最新バージョン情報 |

### 提供予定のPrompts

| プロンプト名 | 説明 |
|-------------|------|
| `create-web-app` | Webアプリケーション新規作成ガイド |
| `create-rest-api` | RESTful API構築ガイド |
| `create-batch` | バッチアプリケーション構築ガイド |
| `setup-handler-queue` | ハンドラキュー設計支援 |
| `review-code` | Nablarchコードレビュー |
| `troubleshoot` | トラブルシューティング支援 |

---

## 5. 技術スタック

### 確定済みの技術選定

| コンポーネント | 技術 | 選定理由 |
|---|---|---|
| **言語** | Java 17+ | Nablarchとの一貫性、MCP Java SDK要件 |
| **フレームワーク** | Spring Boot 3.4.x | MCP Boot Starter活用、エコシステムの充実 |
| **MCP SDK** | [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) 0.17.x | 公式SDK、Spring AI共同メンテナンス |
| **ビルドツール** | Gradle (Kotlin DSL) | モダンなビルド管理 |
| **テスト** | JUnit 5 + Spring Test | 標準的なテストフレームワーク |

### 想定する追加技術（RAG基盤）

| コンポーネント | 候補技術 | 備考 |
|---|---|---|
| **ベクトルDB** | PostgreSQL + pgvector | Nablarch（RDBMS中心）との親和性、コスト効率 |
| **Embeddingモデル（ドキュメント）** | Jina embeddings-v4 | 日本語89言語対応、OSS、32Kコンテキスト |
| **Embeddingモデル（コード）** | Voyage-code-3 | CoIRベンチマーク最高水準、Java対応 |
| **検索方式** | ハイブリッド検索（BM25 + ベクトル検索） | キーワードとセマンティックの両立 |
| **リランキング** | Cross-Encoder | 検索精度の向上 |

### トランスポート

| トランスポート | 用途 | 対応フェーズ |
|---|---|---|
| **STDIO** | ローカル開発（Claude Desktop、Claude Code向け） | Phase 1 |
| **Streamable HTTP** | リモート/チーム共有 | Phase 3 |

### データソース

本サーバーの知識ベースは以下のNablarch関連情報源から構築する。

| データソース | 内容 | 想定規模 |
|---|---|---|
| **Nablarch公式ドキュメント** | アーキテクチャ、API仕様、開発ガイド | 数百ページ |
| **GitHub nablarch organization** | 113リポジトリのソースコード | 数万ファイル |
| **Javadoc** | 全APIリファレンス | 全モジュール |
| **Fintanコンテンツ** | 学習教材、開発標準、事例 | 数十コンテンツ |
| **開発標準** | コーディング規約、設計書テンプレート | 複数ドキュメント |

### 技術選定の経緯

本プロジェクトでは当初、Nablarchフレームワーク自体でMCPサーバーを実装する可能性も調査した（詳細は実装可能性調査レポートを参照）。調査の結果、以下の理由からSpring Boot実装に確定した。

- NablarchにはSSE/リアクティブ/ノンブロッキングのネイティブサポートがなく、MCPプロトコルのStreamable HTTPトランスポート実装が困難
- MCPサーバーのトランスポート層にNablarchを使う技術的必要性がない
- **Nablarchの知識・ツールをMCPサーバーのコンテンツとして提供すること**に価値がある（フレームワーク自体をトランスポート層に使う必要はない）

---

## 6. プロジェクトロードマップ

### Phase 1: 基盤構築（MVP）

MCPサーバーの最小構成を構築し、基本的な動作を検証する。

- MCPサーバー基盤（STDIOトランスポート）
- 基本的なResources（API仕様、ハンドラ一覧）
- 基本的なTools（`search_api`、`validate_handler_queue`）
- Claude Desktop / Claude Codeでの動作検証

### Phase 2: RAGエンジン統合

セマンティック検索によるNablarch知識の高精度検索を実現する。

- pgvector + Embeddingパイプラインの構築
- Nablarchドキュメントのインデックス化
- `semantic_search` ツールの実装
- ハイブリッド検索 + リランキングの導入
- 検索精度の評価・チューニング

### Phase 3: コード生成・ツール拡充

AIによるNablarchコード生成の精度を向上させる。

- `generate_*` ツール群の実装
- コードEmbedding（GitHub 113リポジトリ）
- Promptsの実装
- テスト生成機能
- Streamable HTTPトランスポートの追加

### Phase 4: 高度化・エンタープライズ対応

チーム利用・エンタープライズ対応の機能を整備する。

- Fintanコンテンツ統合
- 自動更新パイプライン（GitHub Webhookによる増分更新）
- OAuth 2.0認証
- コンテナ化・CI/CDパイプライン
- IDE統合モジュール（VS Code拡張等）

---

## 参考資料

### MCP関連

- [MCP公式サイト](https://modelcontextprotocol.io/)
- [MCP仕様書（2025-03-26）](https://modelcontextprotocol.io/specification/2025-03-26)
- [MCP Java SDK（GitHub）](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP ドキュメント](https://docs.spring.io/spring-ai-mcp/reference/mcp.html)
- [MCP Java Server Guide](https://modelcontextprotocol.io/sdk/java/mcp-server)

### Nablarch関連

- [Nablarch公式ドキュメント](https://nablarch.github.io/docs/LATEST/doc/)
- [Nablarch GitHub Organization](https://github.com/nablarch)
- [Nablarch アーキテクチャ概要](https://nablarch.github.io/docs/LATEST/doc/application_framework/application_framework/nablarch/architecture.html)
- [Fintan（TIS技術共有サイト）](https://fintan.jp/)

---

*本文書は計画段階のものであり、実装の進捗に伴い内容が変更される可能性がある。*

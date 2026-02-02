# Nablarch MCP Server

**RAG強化型** [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) サーバー。
[Nablarch](https://nablarch.github.io/) フレームワークの知識をAIコーディングツールに提供する。

## 概要

Nablarchはミッションクリティカルシステム向けのJavaアプリケーションフレームワークだが、メインストリームのフレームワークと比較してコミュニティリソースが著しく不足している。本MCPサーバーは **RAG（Retrieval-Augmented Generation）** と **MCP** を組み合わせることで、AIツールにNablarchの深い知識を提供する。

| 技術 | 役割 |
|---|---|
| **RAG** | Nablarchのドキュメント・コード・Javadocに対するセマンティック検索 |
| **MCP** | 知識をTools・Resources・Promptsとして公開する標準プロトコル |
| **RAG + MCP** | AIツールがNablarchの知識を「知って使う」ことを高精度で実現 |

### 提供機能

- **Tools**（10個）: セマンティック検索、ハンドラキュー設計、コード生成、設定XML検証、API検索、テスト生成、トラブルシューティング、マイグレーション分析、パターン推奨、ハンドラキュー最適化
- **Resources**（8 URIパターン）: ハンドラカタログ、APIリファレンス、設計パターン、学習ガイド、サンプルコード、設定テンプレート、アンチパターン、バージョン情報
- **Prompts**（6テンプレート）: Webアプリ作成、REST API作成、バッチ作成、ハンドラキュー設計、コードレビュー、トラブルシューティング
- **RAGパイプライン**: Nablarch公式ドキュメント・GitHub 113リポジトリ・Javadoc・Fintanコンテンツに対するハイブリッド検索（BM25 + ベクトル類似度）

## アーキテクチャ

```
┌──────────────────────────────────────────────────────────────────┐
│                     AIコーディングツール                            │
│   Claude Code  |  Cursor  |  Copilot  |  VS Code                 │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │                      MCP Client                           │   │
│   └──────────────────────────┬───────────────────────────────┘   │
└──────────────────────────────┼───────────────────────────────────┘
                               │ JSON-RPC 2.0
                               │ STDIO（ローカル）/ Streamable HTTP（リモート）
┌──────────────────────────────▼───────────────────────────────────┐
│              Nablarch MCP Server（Spring Boot）                    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   MCP Protocol Layer                         │ │
│  │   Tools (10)  |  Resources (8種)  |  Prompts (6)            │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │                                    │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │                   RAGエンジン（内蔵）                         │ │
│  │   セマンティック検索  |  ハイブリッド検索  |  リランキング       │ │
│  │   Doc Embedder (Jina v4)  |  Code Embedder (Voyage-code-3) │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │                                    │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │              PostgreSQL + pgvector                           │ │
│  │   Docs Index  |  Code Index  |  Javadoc Index  |  Config    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

## 動作要件

- Java 17 以上
- Maven 3.9.x（Maven Wrapper同梱）
- PostgreSQL 16+ with pgvector拡張（RAG機能、Phase 2以降）

## クイックスタート

### ビルド

```bash
./mvnw package
```

### 実行（STDIOモード）

```bash
./mvnw spring-boot:run
```

### Claude Code での設定

MCP設定ファイル（`.claude/mcp.json`）に以下を追加:

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": ["-jar", "target/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"]
    }
  }
}
```

### Docker で実行（Phase 4）

```bash
docker compose up
```

## 技術スタック

| コンポーネント | 技術 | 選定理由 |
|---|---|---|
| 言語 | Java 17+ | Nablarchエコシステムとの一貫性 |
| フレームワーク | Spring Boot 3.4.x | MCP Boot Starterサポート |
| MCP SDK | [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) 0.17.x | 公式SDK、Spring AI統合 |
| ビルド | Maven 3.9.x | Nablarchエコシステムとの整合性 |
| テスト | JUnit 5 + Spring Test | 標準的なJavaテスト |
| ベクトルDB | PostgreSQL + pgvector | SQL + ベクトル検索統合、コスト効率 |
| ドキュメントEmbedding | Jina embeddings-v4 | 89言語対応、32Kコンテキスト、OSS |
| コードEmbedding | Voyage-code-3 | Java/XMLに最高水準のEmbedding |
| リランキング | Cross-Encoder | ハイブリッド検索の精度向上 |
| トランスポート | STDIO / Streamable HTTP | ローカル開発 + リモートチーム共有 |

## 知識ソース

RAGパイプラインは以下のNablarch関連情報源をインデックス化する:

| ソース | 内容 | 規模 |
|---|---|---|
| [Nablarch公式ドキュメント](https://nablarch.github.io/) | アーキテクチャ、API仕様、開発ガイド | 数百ページ |
| [GitHub nablarch org](https://github.com/nablarch) | 113リポジトリ（ソースコード） | 数万ファイル |
| Javadoc | 全APIリファレンス | 全モジュール |
| [Fintan](https://fintan.jp/) | 学習教材、開発標準 | 数十コンテンツ |

## プロジェクト状況

**現在**: 計画・設計段階。プロジェクトスケルトンとスタブ実装。

### Phase 1: MCP基盤 + 静的知識
- [x] プロジェクト構造
- [x] MCP SDK統合
- [x] Toolスタブ（search_api, validate_handler_queue）
- [x] Resourceスタブ（API仕様、ハンドラ仕様）
- [x] Knowledge Base構造
- [ ] Tool実装
- [ ] Resource実装
- [ ] Prompt実装
- [ ] MCP Inspectorテスト

### Phase 2: RAGエンジン統合
- [ ] pgvectorセットアップ + スキーマ定義
- [ ] ドキュメント取込みパイプライン
- [ ] デュアルEmbedding（Jina v4 + Voyage-code-3）
- [ ] `semantic_search` ツール
- [ ] ハイブリッド検索（BM25 + ベクトル検索）
- [ ] リランキング（Cross-Encoder）
- [ ] 検索品質評価

### Phase 3: ツール拡充 + コード生成
- [ ] `design_handler_queue` ツール（RAG連携）
- [ ] `generate_code` ツール（RAG連携）
- [ ] `generate_test` ツール
- [ ] `troubleshoot` ツール
- [ ] `analyze_migration` ツール
- [ ] Streamable HTTPトランスポート
- [ ] 全Promptテンプレート

### Phase 4: 本番デプロイ
- [ ] Docker Composeデプロイ
- [ ] OAuth 2.0認証
- [ ] 自動更新パイプライン（GitHub Webhook）
- [ ] モニタリング・ログ
- [ ] IDE統合モジュール

## ドキュメント

| ドキュメント | 内容 |
|---|---|
| [docs/overview.md](docs/overview.md) | プロジェクトビジョン、対象ユーザー、機能概要 |
| [docs/architecture.md](docs/architecture.md) | RAG強化型アーキテクチャ設計、コンポーネント図、データモデル |
| [docs/use-cases.md](docs/use-cases.md) | 12ユースケース（シーケンス図・入出力例付き） |
| [docs/user-guide.md](docs/user-guide.md) | セットアップ、設定、利用方法ガイド |
| [docs/decisions/](docs/decisions/) | Architecture Decision Records（ADR） |
| [docs/research/](docs/research/) | 調査レポート・分析資料 |

## 関連リソース

- [MCP仕様](https://spec.modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Nablarch公式ドキュメント](https://nablarch.github.io/)
- [Nablarch GitHub](https://github.com/nablarch)
- [pgvector](https://github.com/pgvector/pgvector)

## ライセンス

[Apache License 2.0](LICENSE)

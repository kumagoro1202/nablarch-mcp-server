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

**現在**: Phase 1〜3完了 + Phase 4-1（品質基盤・オブザーバビリティ）完了。Phase 4-2〜4-4未着手。

### 実装状況サマリ

| 項目 | 状況 |
|---|---|
| **実装フェーズ** | Phase 4-1完了（品質基盤12タスク） |
| **Tools** | 10個実装・登録済み |
| **Resources** | 8 URIパターン実装・登録済み |
| **Prompts** | 6テンプレート実装済み |
| **テスト** | 1,027テスト以上（0失敗、14スキップ=ONNXモデル・Phase 1 stub） |
| **品質** | Checkstyle 0違反、SpotBugs 0件、CI/CD導入済み |
| **オブザーバビリティ** | Actuator + 3 HealthIndicator + 構造化ログ + 相関ID + Micrometer/Prometheus |
| **知識データ** | 17 YAMLファイル（カバレッジ85%） |
| **Agent Skills** | 6件（.claude/skills/配下） |

### Phase 1: MCP基盤 + 静的知識（✅ 完了）
- [x] プロジェクト構造
- [x] MCP SDK統合
- [x] Toolスタブ（search_api, validate_handler_queue）
- [x] Resourceスタブ（API仕様、ハンドラ仕様）
- [x] Knowledge Base構造
- [x] Tool実装（10個）
- [x] Resource実装（8 URIパターン）
- [x] Prompt実装（6テンプレート）
- [x] MCP Inspectorテスト

### Phase 2: RAGエンジン統合（✅ 完了）
- [x] pgvectorセットアップ + スキーマ定義
- [x] ドキュメント取込みパイプライン
- [x] デュアルEmbedding（Jina v4 + Voyage-code-3）
- [x] `semantic_search` ツール
- [x] ハイブリッド検索（BM25 + ベクトル検索）
- [x] リランキング（Cross-Encoder）
- [x] 検索品質評価

### Phase 3: ツール拡充 + コード生成（✅ 完了）
- [x] `design_handler_queue` ツール（RAG連携）
- [x] `generate_code` ツール（RAG連携）
- [x] `generate_test` ツール
- [x] `troubleshoot` ツール
- [x] `analyze_migration` ツール
- [x] Streamable HTTPトランスポート
- [x] 全Promptテンプレート

### 品質改善実施済み（cmd_066）

**P0改善（7件、ブロッカー対応）**:
- DB認証情報の環境変数化（セキュリティ）
- HTTP Origin検証のデフォルト有効化（MCP仕様MUST要件準拠）
- FQCN誤り5件修正（情報精度向上）
- version-info.yaml全面更新（最新環境情報反映）
- Tool未登録2件追加（MigrationAnalysisTool, TestGenerationTool）
- ResourceProvider未登録6件追加（Api, Pattern, Example, Config, Antipattern, Version）
- CI/CD導入（GitHub Actions）

**P1改善（7件、重要改善）**:
- isError:true対応（MCP仕様準拠）
- エラーメッセージ内部情報露出防止
- @ToolParam required=false追加（16パラメータ）
- DesignHandlerQueueTool二重管理解消（約90行削減）
- SetupHandlerQueuePromptアプリタイプ拡張（messagingテンプレート追加）
- TroubleshootTool Markdownテーブルtypo修正
- FQCN自動検証テスト導入（131テストケース追加）

### 知識データ拡充（cmd_078）

- 知識YAMLファイル: 10→17ファイルに拡充（カバレッジ30%→85%）
- 追加領域: データバインド、バリデーション、ログ、メール、メッセージ、セキュリティ、ユーティリティ

### Agent Skills導入（cmd_079）

- `.claude/skills/`配下に6件のエージェントスキルを追加
- Nablarch API設計ガイド、コンポーネントXML設定ガイド、エラーハンドリングガイド、ハンドラキュー設計ガイド、マイグレーションガイド、テスト戦略ガイド

### Phase 4-1: 品質基盤・オブザーバビリティ（✅ 完了）
- [x] Spring Boot Actuator（health/info/metrics/prometheus）
- [x] カスタムHealthIndicator 3件 + GracefulHealthStatusAggregator
- [x] 構造化ログ（JSON） + リクエスト相関ID
- [x] Micrometer + MCP Tool固有メトリクス（Counter/Timer/Error）
- [x] Prometheus連携（/actuator/prometheus）
- [x] MCP JSON-RPCリクエスト/レスポンスログ
- [x] エラーハンドリング統一 + SpotBugs/Checkstyle警告ゼロ化
- [x] pgvector統合テストCI対応
- [x] Tool名snake_case統一

### Phase 4-2〜4-4: 未着手
- [ ] Docker Composeデプロイ（Phase 4-2）
- [ ] OAuth 2.0認証（Phase 4-3）
- [ ] 自動更新パイプライン / GitHub Webhook（Phase 4-4）

### 既知の課題

- Phase 4-2〜4-4未着手（コンテナ化、セキュリティ、データパイプライン拡充）

## ドキュメント

**[docs/INDEX.md](docs/INDEX.md)** — 読者別ガイド付きのドキュメント総合案内

| カテゴリ | 内容 |
|---|---|
| [guides/](docs/guides/) | 🟢 **利用者向け** — セットアップ、使い方、Streamable HTTP設定 |
| [reference/](docs/reference/) | 🔵 **開発者向け** — アーキテクチャ、API仕様、RAGパイプライン、DBスキーマ |
| [designs/](docs/designs/) | 📐 設計書（Phase 1〜3、全23本） |
| [articles/](docs/articles/) | 📚 連載記事シリーズ（全17記事の専門家育成カリキュラム） |
| [decisions/](docs/decisions/) | 📋 ADR（アーキテクチャ決定記録） |
| [research/](docs/research/) | 📊 調査レポート・分析資料 |

## 関連リソース

- [MCP仕様](https://spec.modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Nablarch公式ドキュメント](https://nablarch.github.io/)
- [Nablarch GitHub](https://github.com/nablarch)
- [pgvector](https://github.com/pgvector/pgvector)

## ライセンス

[Apache License 2.0](LICENSE)

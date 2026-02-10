# Nablarch MCP Server ドキュメントガイド

> **最終更新**: 2026-02-10

---

## 読者別ガイド

### 🟢 利用者（MCPサーバーをClaude等から使いたい人）

MCPサーバーをセットアップし、AIツールから使えるようにしたい方向け。

| 順序 | ドキュメント | 重要度 | 概要 |
|:---:|-------------|:---:|------|
| ① | [セットアップガイド](guides/01-setup.md) | **必読** | インストール、ビルド、動作確認 |
| ② | [ユーザーガイド](guides/02-user-guide.md) | **必読** | AIツールでの設定方法、プロンプト例、FAQ |
| ③ | [Streamable HTTP設定](guides/03-streamable-http.md) | 推奨 | リモート接続の設定（チーム利用時） |

### 🔵 開発者（MCPサーバーを拡張・カスタマイズしたい人）

アーキテクチャを理解し、新しいTool/Resource/Promptを追加したい方向け。

| 順序 | ドキュメント | 重要度 | 概要 |
|:---:|-------------|:---:|------|
| ① | [プロジェクト概要](reference/01-overview.md) | **必読** | ビジョン、対象ユーザー、機能概要 |
| ② | [アーキテクチャ設計](reference/02-architecture.md) | **必読** | RAG強化型アーキテクチャ、コンポーネント構成 |
| ③ | [ユースケース集](reference/03-use-cases.md) | **必読** | 12のユースケースとシーケンス図 |
| ④ | [API仕様書](reference/06-api-specification.md) | **必読** | Tools/Resources/PromptsのAPI仕様 |
| ⑤ | [RAGパイプライン仕様](reference/04-rag-pipeline-spec.md) | 推奨 | 検索パイプラインの技術仕様 |
| ⑥ | [DBスキーマ](reference/05-database-schema.md) | 推奨 | pgvectorスキーマ定義 |
| ⑦ | [Tool API仕様](reference/api/tool-api-specification.md) | 参考 | 10 Toolsの詳細仕様 |
| ⑧ | [Resource URI仕様](reference/api/resource-uri-specification.md) | 参考 | 8 URIパターンの仕様 |

### 🟡 コントリビューター（PRを出したい人）

開発環境を構築し、設計書を読んでから実装に取りかかりたい方向け。

| 順序 | ドキュメント | 重要度 | 概要 |
|:---:|-------------|:---:|------|
| ① | [セットアップガイド](guides/01-setup.md) | **必読** | 開発環境の構築 |
| ② | [アーキテクチャ設計](reference/02-architecture.md) | **必読** | 全体設計の理解 |
| ③ | [設計書一覧](#設計書designs) | **必読** | 担当箇所の詳細設計 |
| ④ | [WBS](project/wbs.md) | 推奨 | タスクの依存関係を確認 |
| ⑤ | [進捗管理表](project/progress.md) | 推奨 | 現在の進捗状況 |

### 📚 学習者（体系的に学びたい人）

連載記事シリーズで基礎から体系的に学びたい方向け。

| ドキュメント | 概要 |
|-------------|------|
| [連載記事INDEX](articles/INDEX.md) | 全17記事の専門家育成シリーズ（約8〜10時間） |
| [初心者向けアーキテクチャ解説](reference/07-architecture-beginners.md) | MCPとRAGの基本概念をやさしく解説 |

---

## ディレクトリ構成

```
docs/
├── INDEX.md                 ← 📍 このファイル
├── guides/                  🟢 利用者向けガイド
│   ├── 01-setup.md            セットアップガイド
│   ├── 02-user-guide.md       ユーザーガイド
│   └── 03-streamable-http.md  Streamable HTTP設定ガイド
├── reference/               🔵 開発者向けリファレンス
│   ├── 01-overview.md         プロジェクト概要
│   ├── 02-architecture.md     アーキテクチャ設計
│   ├── 03-use-cases.md        ユースケース集
│   ├── 04-rag-pipeline-spec.md RAGパイプライン仕様
│   ├── 05-database-schema.md  DBスキーマ
│   ├── 06-api-specification.md API仕様書
│   ├── 07-architecture-beginners.md 初心者向けアーキテクチャ解説
│   └── api/                   API詳細仕様
│       ├── tool-api-specification.md
│       └── resource-uri-specification.md
├── designs/                 📐 設計書（Phase 1〜3）
│   └── 01〜23の設計書
├── articles/                📚 連載記事シリーズ（全17記事）
│   └── INDEX.md（記事一覧・学習パス）
├── decisions/               📋 ADR（アーキテクチャ決定記録）
│   └── ADR-001
├── research/                📊 調査レポート
│   └── O-023, O-024
├── project/                 📈 プロジェクト管理
│   ├── wbs.md                 WBS
│   ├── progress.md            進捗管理表
│   └── search-quality-report.md 検索品質レポート
├── test-results/            🧪 テスト結果
│   ├── claude-integration-test.md
│   ├── mcp-inspector-test.md
│   ├── e2e-test-results.md
│   └── use-case-test-results.md
└── checklists/              ✅ WBSチェックリスト
    └── WBS-*.md
```

---

## 設計書（designs/）

Phase 1〜3の詳細設計書（WBS番号順）。

| # | 設計書 | Phase | 内容 |
|---|--------|-------|------|
| 01 | [Spring Boot基盤設計](designs/01_spring-boot-foundation.md) | Phase 1 | プロジェクト構造、依存関係 |
| 02 | [MCP SDK統合設計](designs/02_mcp-sdk-integration.md) | Phase 1 | MCP Java SDKの統合方式 |
| 03 | [静的知識ベース設計](designs/03_knowledge-base.md) | Phase 1 | YAML知識ファイルの構造 |
| 04 | [search_api Tool設計](designs/04_tool-search-api.md) | Phase 1 | API検索Tool |
| 05 | [validate_config Tool設計](designs/05_tool-validate-config.md) | Phase 1 | 設定検証Tool |
| 06 | [Resource URI設計](designs/06_resource-uri-design.md) | Phase 1 | URIパターン設計 |
| 07 | [Promptテンプレート設計](designs/07_prompt-templates.md) | Phase 1 | 6テンプレートの設計 |
| 08 | [ベクトルDBスキーマ設計](designs/08_vector-db-schema.md) | Phase 2 | pgvectorスキーマ |
| 09 | [Embeddingパイプライン設計](designs/09_embedding-pipeline.md) | Phase 2 | Embedding取込処理 |
| 10 | [チャンキング戦略設計](designs/10_chunking-strategy.md) | Phase 2 | ドキュメント分割戦略 |
| 11 | [ハイブリッド検索設計](designs/11_hybrid-search.md) | Phase 2 | BM25+ベクトル検索 |
| 12 | [リランキング設計](designs/12_reranking.md) | Phase 2 | Cross-Encoderリランキング |
| 13 | [semantic_search Tool設計](designs/13_semantic-search-tool.md) | Phase 2 | セマンティック検索Tool |
| 14 | [検索品質評価設計](designs/14_search-quality-evaluation.md) | Phase 2 | 品質評価フレームワーク |
| 15 | [design_handler_queue Tool設計](designs/15_tool-design-handler-queue.md) | Phase 3 | ハンドラキュー設計Tool |
| 16 | [generate_code Tool設計](designs/16_tool-generate-code.md) | Phase 3 | コード生成Tool |
| 17 | [generate_test Tool設計](designs/17_tool-generate-test.md) | Phase 3 | テスト生成Tool |
| 18 | [troubleshoot Tool設計](designs/18_tool-troubleshoot.md) | Phase 3 | トラブルシューティングTool |
| 19 | [analyze_migration Tool設計](designs/19_tool-analyze-migration.md) | Phase 3 | マイグレーション分析Tool |
| 20 | [recommend_pattern Tool設計](designs/20_tool-recommend-pattern.md) | Phase 3 | パターン推奨Tool |
| 21 | [optimize_handler_queue Tool設計](designs/21_tool-optimize-handler-queue.md) | Phase 3 | ハンドラキュー最適化Tool |
| 22 | [Phase 3 Resource URI拡張](designs/22_resource-uri-phase3.md) | Phase 3 | URI拡張設計 |
| 23 | [Streamable HTTPトランスポート](designs/23_streamable-http-transport.md) | Phase 3 | HTTPトランスポート設計 |

---

## ADR（アーキテクチャ決定記録）

| ADR | タイトル |
|-----|---------|
| [ADR-001](decisions/ADR-001_rag-enhanced-architecture.md) | RAG-Enhanced アーキテクチャの採用 |

---

## 調査レポート

| レポート | 内容 |
|----------|------|
| [O-023](research/O-023_nablarch_rag_mcp_analysis.md) | RAG×MCP関連性分析レポート |
| [O-024](research/O-024_embedding-model-migration.md) | Embeddingモデル移行調査 |

---

## テスト結果

| レポート | 内容 |
|----------|------|
| [Claude統合テスト](test-results/claude-integration-test.md) | Claude Code統合テスト結果 |
| [MCP Inspector](test-results/mcp-inspector-test.md) | MCP Inspector検証結果 |
| [E2Eテスト](test-results/e2e-test-results.md) | エンドツーエンドテスト結果 |
| [ユースケーステスト](test-results/use-case-test-results.md) | ユースケースベーステスト結果 |

---

## 連載記事の位置づけ

`articles/` ディレクトリには、全17記事の **専門家育成シリーズ** が格納されている。
これは実用ガイドではなく、**体系的な技術解説記事** として位置づけられている。

- ゼロからNablarch MCP Serverの専門家になるための段階的カリキュラム
- 想定学習時間: 約8〜10時間
- 詳細は [articles/INDEX.md](articles/INDEX.md) を参照

---

## クイックリンク

- **GitHub**: [nablarch-mcp-server](https://github.com/kumagoro1202/nablarch-mcp-server)
- **現在の進捗**: Phase 1-3 完了 / Phase 4 未着手
- **技術スタック**: Spring Boot 3.x + MCP Java SDK 0.17.x + PostgreSQL 16 + pgvector

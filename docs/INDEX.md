# Nablarch MCP Server ドキュメント INDEX

> **最終更新**: 2026-02-04
> **プロジェクト**: nablarch-mcp-server

---

## 読者別ガイド

### 初めての方（プロジェクト概要を知りたい）

1. [01-overview.md](01-overview.md) — プロジェクトのビジョンと目的
2. [02-architecture.md](02-architecture.md) — 技術アーキテクチャの全体像

### 開発者（機能詳細を理解したい）

1. [03-use-cases.md](03-use-cases.md) — 12のユースケースとシーケンス図
2. [04-rag-pipeline-spec.md](04-rag-pipeline-spec.md) — RAGパイプラインの技術仕様
3. [05-database-schema.md](05-database-schema.md) — ベクトルDBスキーマ定義
4. [06-api-specification.md](06-api-specification.md) — MCP API仕様（Tools/Resources/Prompts）

### 利用者（セットアップして使いたい）

1. [07-setup-guide.md](07-setup-guide.md) — インストール・ビルド手順
2. [08-user-guide.md](08-user-guide.md) — AIツールでの設定方法と使い方

### プロジェクト管理者

1. [10-wbs.md](10-wbs.md) — Work Breakdown Structure
2. [11-progress.md](11-progress.md) — 進捗管理表

---

## ドキュメント一覧

### 主要ドキュメント

| # | ドキュメント | 概要 |
|---|-------------|------|
| 01 | [01-overview.md](01-overview.md) | プロジェクトの目的・ビジョン、対象ユーザー、機能概要、技術スタック、ロードマップ |
| 02 | [02-architecture.md](02-architecture.md) | RAG強化MCPサーバーのアーキテクチャ設計、コンポーネント構成、デプロイメント |
| 03 | [03-use-cases.md](03-use-cases.md) | ハンドラキュー設計、API検索、コード生成など12のユースケースとシーケンス図 |
| 04 | [04-rag-pipeline-spec.md](04-rag-pipeline-spec.md) | ドキュメント取込・検索・リランキングパイプラインの技術仕様 |
| 05 | [05-database-schema.md](05-database-schema.md) | PostgreSQL + pgvectorのスキーマ定義、インデックス、マイグレーション |
| 06 | [06-api-specification.md](06-api-specification.md) | MCP Tools/Resources/Promptsの仕様、JSON-RPCインターフェース |
| 07 | [07-setup-guide.md](07-setup-guide.md) | 前提条件、ビルド手順、Claude Desktop/Claude Code設定方法 |
| 08 | [08-user-guide.md](08-user-guide.md) | 各AIツールでの設定、プロンプト例、高度な使い方、FAQ |
| 09 | [09-search-quality-report.md](09-search-quality-report.md) | RAG検索パイプラインの品質評価結果、リランキング効果分析 |
| 10 | [10-wbs.md](10-wbs.md) | Phase 1〜4のWBS、依存関係図、クリティカルパス、リスク管理 |
| 11 | [11-progress.md](11-progress.md) | Phase別タスク進捗、PR履歴、完了状況 |

---

## サブディレクトリ

### designs/

Phase 1〜3の詳細設計書（WBS番号順にナンバリング済み）

| # | 設計書 | Phase | 内容 |
|---|--------|-------|------|
| 01 | [01_spring-boot-foundation.md](designs/01_spring-boot-foundation.md) | Phase 1 | Spring Boot基盤設計 |
| 02 | [02_mcp-sdk-integration.md](designs/02_mcp-sdk-integration.md) | Phase 1 | MCP SDK統合設計 |
| 03 | [03_knowledge-base.md](designs/03_knowledge-base.md) | Phase 1 | 静的知識ベース設計 |
| 04 | [04_tool-search-api.md](designs/04_tool-search-api.md) | Phase 1 | search_api Tool設計 |
| 05 | [05_tool-validate-config.md](designs/05_tool-validate-config.md) | Phase 1 | validate_config Tool設計 |
| 06 | [06_resource-uri-design.md](designs/06_resource-uri-design.md) | Phase 1 | Resource URI設計 |
| 07 | [07_prompt-templates.md](designs/07_prompt-templates.md) | Phase 1 | Promptテンプレート設計 |
| 08 | [08_vector-db-schema.md](designs/08_vector-db-schema.md) | Phase 2 | ベクトルDBスキーマ設計 |
| 09 | [09_embedding-pipeline.md](designs/09_embedding-pipeline.md) | Phase 2 | Embeddingパイプライン設計 |
| 10 | [10_chunking-strategy.md](designs/10_chunking-strategy.md) | Phase 2 | チャンキング戦略設計 |
| 11 | [11_hybrid-search.md](designs/11_hybrid-search.md) | Phase 2 | ハイブリッド検索設計 |
| 12 | [12_reranking.md](designs/12_reranking.md) | Phase 2 | リランキング設計 |
| 13 | [13_semantic-search-tool.md](designs/13_semantic-search-tool.md) | Phase 2 | semantic_search Tool設計 |
| 14 | [14_search-quality-evaluation.md](designs/14_search-quality-evaluation.md) | Phase 2 | 検索品質評価設計 |
| 15 | [15_tool-design-handler-queue.md](designs/15_tool-design-handler-queue.md) | Phase 3 | design_handler_queue Tool設計 |
| 16 | [16_tool-generate-code.md](designs/16_tool-generate-code.md) | Phase 3 | generate_code Tool設計 |
| 17 | [17_tool-generate-test.md](designs/17_tool-generate-test.md) | Phase 3 | generate_test Tool設計 |
| 18 | [18_tool-troubleshoot.md](designs/18_tool-troubleshoot.md) | Phase 3 | troubleshoot Tool設計 |
| 19 | [19_tool-analyze-migration.md](designs/19_tool-analyze-migration.md) | Phase 3 | analyze_migration Tool設計 |
| 20 | [20_tool-recommend-pattern.md](designs/20_tool-recommend-pattern.md) | Phase 3 | recommend_pattern Tool設計 |
| 21 | [21_tool-optimize-handler-queue.md](designs/21_tool-optimize-handler-queue.md) | Phase 3 | optimize_handler_queue Tool設計 |
| 22 | [22_resource-uri-phase3.md](designs/22_resource-uri-phase3.md) | Phase 3 | Phase 3 Resource URI拡張設計 |
| 23 | [23_streamable-http-transport.md](designs/23_streamable-http-transport.md) | Phase 3 | Streamable HTTPトランスポート設計 |

### decisions/

アーキテクチャ決定記録（ADR）

| ADR | タイトル |
|-----|---------|
| [ADR-001](decisions/ADR-001_rag-enhanced-architecture.md) | RAG-Enhanced アーキテクチャの採用 |

### research/

調査・分析レポート

| レポート | 内容 |
|----------|------|
| [O-023](research/O-023_nablarch_rag_mcp_analysis.md) | RAG×MCP関連性分析レポート |
| [O-024](research/O-024_embedding-model-migration.md) | Embeddingモデル移行調査 |

### checklists/

WBSタスク完了チェックリスト（WBS番号でソート）

### test-results/

テスト結果レポート

| レポート | 内容 |
|----------|------|
| [claude-integration-test.md](test-results/claude-integration-test.md) | Claude Code統合テスト結果 |
| [mcp-inspector-test.md](test-results/mcp-inspector-test.md) | MCP Inspector検証結果 |

---

## クイックリンク

- **GitHub**: [nablarch-mcp-server](https://github.com/kumagoro1202/nablarch-mcp-server)
- **現在の進捗**: Phase 1-2 完了 / Phase 3 進行中（76%）
- **技術スタック**: Spring Boot 3.x + MCP Java SDK 0.17.x + PostgreSQL 16 + pgvector

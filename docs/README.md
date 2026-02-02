# nablarch-mcp-server ドキュメント

## 全体設計文書

- [プロジェクト概要](overview.md) - ビジョン、対象ユーザー、機能概要
- [アーキテクチャ設計書](architecture.md) - RAG-enhanced MCPサーバーの技術設計
- [ユースケース集](use-cases.md) - 12のユースケースとシーケンス図
- [ユーザーガイド](user-guide.md) - 設定方法、プロンプト例、FAQ

## 設計文書（designs/）

Phase 1-2の全設計書（WBS番号順）:

| # | 設計書 | Phase | WBS |
|---|--------|-------|-----|
| 01 | [Spring Boot基盤設計](designs/01_spring-boot-foundation.md) | Phase 1 | 1.1.1 |
| 02 | [MCP SDK統合設計](designs/02_mcp-sdk-integration.md) | Phase 1 | 1.1.2 |
| 03 | [静的知識ベース設計](designs/03_knowledge-base.md) | Phase 1 | 1.1.3 |
| 04 | [search_api Tool設計](designs/04_tool-search-api.md) | Phase 1 | 1.1.4 |
| 05 | [validate_config Tool設計](designs/05_tool-validate-config.md) | Phase 1 | 1.1.5 |
| 06 | [Resource URI設計](designs/06_resource-uri-design.md) | Phase 1 | 1.1.6 |
| 07 | [Promptテンプレート設計](designs/07_prompt-templates.md) | Phase 1 | 1.1.7 |
| 08 | [ベクトルDBスキーマ設計](designs/08_vector-db-schema.md) | Phase 2 | 2.1.1 |
| 09 | [Embeddingパイプライン設計](designs/09_embedding-pipeline.md) | Phase 2 | 2.1.2 |
| 10 | [チャンキング戦略設計](designs/10_chunking-strategy.md) | Phase 2 | 2.1.3 |
| 11 | [ハイブリッド検索設計](designs/11_hybrid-search.md) | Phase 2 | 2.1.4 |
| 12 | [リランキング設計](designs/12_reranking.md) | Phase 2 | 2.1.5 |
| 13 | [semantic_search Tool設計](designs/13_semantic-search-tool.md) | Phase 2 | 2.1.6 |
| 14 | [検索品質評価設計](designs/14_search-quality-evaluation.md) | Phase 2 | 2.1.7 |

## 意思決定記録（ADR）

- [ADR-001: RAG-Enhanced アーキテクチャの採用](decisions/ADR-001_rag-enhanced-architecture.md)

## 調査資料

- [O-023: RAG×MCP関連性分析](research/O-023_nablarch_rag_mcp_analysis.md)

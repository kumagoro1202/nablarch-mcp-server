# Phase 1 MVP 進捗管理表

> **プロジェクト**: nablarch-mcp-server
> **フェーズ**: Phase 1 — MVP実装
> **最終更新**: 2026-02-02

## Wave 1: 基盤構築

| WBS | タスク名 | 担当 | 状態 | PR | 備考 |
|-----|---------|------|------|-----|------|
| 1.1.1 | Spring Boot基盤設計 | Batch A (ashigaru2) | 完了 | — | docs/design/spring-boot-foundation.md |
| 1.1.2 | MCP SDK統合設計 | Batch A (ashigaru2) | 完了 | — | docs/design/mcp-sdk-integration.md |
| 1.1.3 | ナレッジベース設計 | Batch B | 完了 | — | docs/design/knowledge-base.md |
| 1.1.4 | search_api Tool API設計 | Batch C (ashigaru2) | 完了 | — | docs/design/tool-search-api.md |
| 1.1.5 | validate_config Tool API設計 | Batch C (ashigaru2) | 完了 | — | docs/design/tool-validate-config.md |
| 1.2.1 | Spring Bootプロジェクト構築 | Batch A (ashigaru2) | 完了 | — | build.gradle.kts更新、パッケージ整備 |
| 1.2.2 | MCP SDK統合・STDIOトランスポート | Batch A (ashigaru2) | 完了 | — | Resource/Prompt登録、STDIO動作確認済 |
| 1.2.3 | ナレッジベース基盤実装 | Batch B | 完了 | — | 知識YAML 7本作成済 |
| 1.2.4 | SearchApiTool実装 | Batch C (ashigaru2) | 完了 | — | NablarchKnowledgeBase.search()連携 |
| 1.2.5 | ValidateHandlerQueueTool実装 | Batch C (ashigaru2) | 完了 | — | XML解析+ハンドラキュー検証 |

## Wave 2: コア機能実装

| WBS | タスク名 | 担当 | 状態 | PR | 備考 |
|-----|---------|------|------|-----|------|
| 1.3.1 | SearchApiToolテスト | Batch C (ashigaru2) | 完了 | — | Mockitoベース単体テスト 7件 |
| 1.3.2 | ValidateHandlerQueueToolテスト | Batch C (ashigaru2) | 完了 | — | Mockitoベース単体テスト 11件 |
| 1.3.3 | handler/{app_type} Resource実装 | — | 未着手 | — | |
| 1.3.4 | guide/{topic} Resource実装 | — | 未着手 | — | |
| 1.3.5 | Promptテンプレート実装 (6種) | — | 未着手 | — | |
| 1.3.6 | ハンドラカタログYAML作成 | — | 未着手 | — | |
| 1.3.7 | APIパターンYAML作成 | — | 未着手 | — | |
| 1.3.8 | 設計ガイドラインYAML作成 | — | 未着手 | — | |

## Wave 3: テスト・品質

| WBS | タスク名 | 担当 | 状態 | PR | 備考 |
|-----|---------|------|------|-----|------|
| 1.4.1 | Tool単体テスト | — | 未着手 | — | |
| 1.4.2 | Resource単体テスト | — | 未着手 | — | |
| 1.4.3 | Prompt単体テスト | — | 未着手 | — | |
| 1.4.4 | KnowledgeBase単体テスト | — | 未着手 | — | |
| 1.4.5 | 統合テスト（STDIO） | — | 未着手 | — | |
| 1.4.6 | MCP Inspectorテスト | — | 未着手 | — | |
| 1.4.7 | Claude Codeエンドツーエンドテスト | — | 未着手 | — | |
| 1.4.8 | カバレッジ確認・品質ゲート | — | 未着手 | — | |
| 1.4.9 | ドキュメント整備 | — | 未着手 | — | |
| 1.4.10 | リリースビルド・タグ | — | 未着手 | — | |

## 凡例

- **未着手**: まだ作業を開始していない
- **進行中**: 作業中
- **完了**: 完了基準チェックリストを全てパス
- **ブロック**: 依存タスクの完了待ち

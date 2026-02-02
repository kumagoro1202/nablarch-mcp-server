# WBS 1.1.7 — Promptテンプレート設計（全6種）

## 概要

MCP Promptとして6種のテンプレートを設計・実装する。各PromptはNablarchの知識YAMLファイルを読み込み、Markdown形式のガイダンスを生成する。

## Prompt一覧

| # | Prompt名 | クラス | 引数 | 知識ファイル |
|---|---------|--------|------|------------|
| 1 | setup-handler-queue | SetupHandlerQueuePrompt | app_type | handler-catalog, handler-constraints, config-templates |
| 2 | create-action | CreateActionPrompt | app_type, action_name | api-patterns |
| 3 | review-config | ReviewConfigPrompt | config_xml | handler-constraints, handler-catalog, error-catalog |
| 4 | explain-handler | ExplainHandlerPrompt | handler_name | handler-catalog, handler-constraints |
| 5 | migration-guide | MigrationGuidePrompt | from_version, to_version | module-catalog |
| 6 | best-practices | BestPracticesPrompt | topic | design-patterns, api-patterns |

## 設計方針

### 共通パターン
- `@Component` + `@PostConstruct` でYAMLを初期化時に読み込み
- `execute(Map<String, String> arguments)` メソッドでPromptを実行
- 引数バリデーション: null/blank/不正値に対して `IllegalArgumentException`
- 出力: `McpSchema.GetPromptResult` にMarkdown形式の `TextContent` を格納

### 1. setup-handler-queue
- **目的**: 指定アプリタイプのハンドラキュー構成ガイド
- **入力**: `app_type` (web/rest/batch/messaging)
- **出力**: ハンドラ一覧テーブル + 順序制約 + XMLテンプレート

### 2. create-action
- **目的**: アクションクラスのスケルトンコード生成ガイド
- **入力**: `app_type` + `action_name`
- **出力**: 推奨パターン + コード例 + 命名規則

### 3. review-config
- **目的**: XML設定ファイルのレビュー観点提示
- **入力**: `config_xml` (XML文字列)
- **出力**: レビュー対象表示 + 制約チェックポイント + 問題パターン + 確認事項

### 4. explain-handler
- **目的**: 特定ハンドラの詳細説明
- **入力**: `handler_name`
- **出力**: 基本情報テーブル + 使用アプリタイプ + 制約情報
- **特記**: 大文字小文字を無視したマッチング、未知ハンドラへの適切なメッセージ

### 5. migration-guide
- **目的**: バージョン間移行ガイド
- **入力**: `from_version`, `to_version`
- **出力**: モジュール一覧 + 主要クラス + 依存関係 + 移行手順

### 6. best-practices
- **目的**: トピック別ベストプラクティス
- **入力**: `topic` (handler-queue/action/validation/database/testing)
- **出力**: 設計パターン + 推奨実装パターン + 一般的な注意事項
- **特記**: トピックからカテゴリへのマッピングテーブルで知識を選択

## McpServerConfig統合

`nablarchPrompts()` メソッドに6つのPrompt Beanを注入し、`promptSpec()` ヘルパーで `Function<Map<String, String>, GetPromptResult>` 型のメソッド参照を渡す。

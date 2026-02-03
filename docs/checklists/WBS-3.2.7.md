# WBS 3.2.7 optimize_handler_queue Tool実装 完了基準チェックリスト

> **作成日**: 2026-02-03
> **作成者**: ashigaru5
> **関連設計書**: docs/designs/21_tool-optimize-handler-queue.md

## 実装成果物

### ソースコード

- [x] `OptimizeHandlerQueueTool.java` - Tool本体
  - [x] `@Tool`アノテーション付与
  - [x] `@ToolParam`アノテーション付与（currentXml, appType, concern）
  - [x] XML解析機能（HandlerEntry抽出）
  - [x] app_type自動推定機能
  - [x] 3観点の最適化ルール実装
    - [x] 正確性（COR-001〜002）
    - [x] セキュリティ（SEC-001〜003, SEC-005）
    - [x] パフォーマンス（PERF-001〜002, PERF-005）
  - [x] Before/After XML生成
  - [x] Markdown形式出力
  - [x] 入力バリデーション
  - [x] エラーハンドリング

### 設定

- [x] `McpServerConfig.java` - Tool登録
  - [x] import文追加
  - [x] nablarchTools Beanに追加

## 機能要件

| 要件ID | 要件 | 状態 |
|--------|------|------|
| REQ-1 | 既存ハンドラキューXML解析 | 実装済 |
| REQ-2 | app_type自動推定 | 実装済 |
| REQ-3 | 正確性観点分析 | 実装済 |
| REQ-4 | セキュリティ観点分析 | 実装済 |
| REQ-5 | パフォーマンス観点分析 | 実装済 |
| REQ-6 | Before/After XML生成 | 実装済 |
| REQ-7 | 重大度別サマリ | 実装済 |

## 設計書との整合性

| 設計項目 | 設計書記載 | 実装状態 |
|---------|----------|---------|
| Tool名 | optimize_handler_queue | 実装済（OptimizeHandlerQueueTool） |
| パッケージ | com.tis.nablarch.mcp.tools | 実装済 |
| 入力パラメータ | current_xml, app_type, concern | 実装済 |
| 出力形式 | Markdown | 実装済 |
| 正確性ルール | COR-001〜004 | COR-001〜002実装済 |
| セキュリティルール | SEC-001〜005 | SEC-001〜003, SEC-005実装済 |
| パフォーマンスルール | PERF-001〜005 | PERF-001〜002, PERF-005実装済 |

## 最適化ルール詳細

### 正確性（Correctness）

| ルールID | ルール名 | 重大度 | 実装状態 |
|---------|---------|-------|---------|
| COR-001 | 必須ハンドラ欠落 | high | 実装済 |
| COR-002 | 順序制約違反 | high | 実装済 |
| COR-003 | 外殻/内殻配置違反 | medium | 未実装（Phase 2+） |
| COR-004 | 非互換ハンドラ同居 | medium | 未実装（Phase 2+） |

### セキュリティ（Security）

| ルールID | ルール名 | 重大度 | 実装状態 |
|---------|---------|-------|---------|
| SEC-001 | SecureHandler未設定 | high | 実装済 |
| SEC-002 | CSRF対策未設定 | high | 実装済 |
| SEC-003 | セッションストア未設定 | medium | 実装済 |
| SEC-004 | 認証ハンドラ配置不適 | medium | 未実装（Phase 2+） |
| SEC-005 | 本番不要ハンドラ残存 | medium | 実装済 |

### パフォーマンス（Performance）

| ルールID | ルール名 | 重大度 | 実装状態 |
|---------|---------|-------|---------|
| PERF-001 | 不要ハンドラの除去 | medium | 実装済 |
| PERF-002 | 重複ハンドラ | medium | 実装済 |
| PERF-003 | 軽量ハンドラの後方配置 | low | 未実装（Phase 2+） |
| PERF-004 | 条件付き適用推奨 | low | 未実装（Phase 2+） |
| PERF-005 | ログハンドラの非同期化推奨 | low | 実装済 |

## 対応ユースケース

| UC | ユースケース名 | 対応状態 |
|----|--------------|---------|
| UC10 | ハンドラキュー最適化 | 対応済（メインTool） |

## 備考

- ビルド環境の制約（JDK未インストール）によりローカルビルド確認は未実施
- CI/CDでのビルド確認を推奨
- 一部ルール（COR-003, COR-004, SEC-004, PERF-003, PERF-004）はPhase 2+で追加予定

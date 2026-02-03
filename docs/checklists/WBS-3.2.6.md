# WBS 3.2.6 recommend_pattern Tool実装 完了基準チェックリスト

> **作成日**: 2026-02-03
> **作成者**: ashigaru5
> **関連設計書**: docs/designs/20_tool-recommend-pattern.md

## 実装成果物

### ソースコード

- [x] `RecommendPatternTool.java` - Tool本体（442行）
  - [x] `@Tool`アノテーション付与
  - [x] `@ToolParam`アノテーション付与（requirement, appType, constraints, maxResults）
  - [x] 4フェーズ処理フロー実装
    - [x] Phase 1: 要件分析（analyzeRequirement）
    - [x] Phase 2: 候補収集（getAllDesignPatterns）
    - [x] Phase 3: スコアリング（scorePattern）
    - [x] Phase 4: ランキング生成（generateRanking）
  - [x] スコアリングモデル実装
    - [x] S1: キーワード一致度（重み0.40）
    - [x] S2: カテゴリ一致度（重み0.25）
    - [x] S3: app_type適合度（重み0.20）
    - [x] S4: 制約一致度（重み0.15）
  - [x] Markdown形式出力
  - [x] 入力バリデーション（10文字未満、app_type不正）
  - [x] エラーハンドリング

- [x] `NablarchKnowledgeBase.java` - ヘルパーメソッド追加
  - [x] `getAllDesignPatterns()`: 全パターン取得
  - [x] `getDesignPatternEntry(String name)`: 名前指定取得

### 設定

- [x] `McpServerConfig.java` - Tool登録
  - [x] import文追加
  - [x] nablarchTools Beanに追加

## 機能要件

| 要件ID | 要件 | 状態 |
|--------|------|------|
| REQ-1 | 自然言語要件からパターン推薦 | 実装済 |
| REQ-2 | 11種Nablarchデザインパターン対応 | 実装済 |
| REQ-3 | スコア付きランキング出力 | 実装済 |
| REQ-4 | app_typeフィルタリング | 実装済 |
| REQ-5 | 制約条件対応 | 実装済 |
| REQ-6 | Resource URI連携 | 実装済 |

## 設計書との整合性

| 設計項目 | 設計書記載 | 実装状態 |
|---------|----------|---------|
| Tool名 | recommend_pattern | 実装済（RecommendPatternTool） |
| パッケージ | com.tis.nablarch.mcp.tools | 実装済 |
| 入力パラメータ | requirement, app_type, constraints, max_results | 実装済 |
| 出力形式 | Markdown | 実装済 |
| スコアリング重み | S1:0.40, S2:0.25, S3:0.20, S4:0.15 | 実装済 |
| 最小スコア閾値 | 0.20 | 実装済 |
| デフォルトmax_results | 3 | 実装済 |

## 対応ユースケース

| UC | ユースケース名 | 対応状態 |
|----|--------------|---------|
| UC8 | 設計パターン推奨 | 対応済（メインTool） |

## 備考

- ビルド環境の制約（JDK未インストール）によりローカルビルド確認は未実施
- CI/CDでのビルド確認を推奨

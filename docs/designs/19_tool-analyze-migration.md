# analyze_migration Tool 詳細設計書

> **WBS番号**: 3.1.5
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru4 (subtask_063)
> **対応ユースケース**: UC9（Nablarchバージョンアップ支援）

## 1. 概要

`analyze_migration` ToolはNablarch 5から6への移行において、既存コードの非推奨API使用を検出し、移行影響を分析して修正提案を生成する。

## 2. Tool定義

| 項目 | 値 |
|------|-----|
| Tool名 | `analyze_migration` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `MigrationAnalysisTool` |
| 説明 | Nablarchバージョン移行時のコード影響を分析し、修正提案を生成 |

## 3. 入力スキーマ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| code_snippet | string | ○ | - | 分析対象のコード |
| source_version | string | × | "5" | 移行元バージョン |
| target_version | string | × | "6" | 移行先バージョン |
| analysis_scope | string | × | "full" | 分析範囲 |

## 4. 出力スキーマ

- migration_issues: 検出された移行問題リスト
- summary: 問題サマリ（自動修正可能/手動修正必要）
- recommendations: 優先順位付き推奨事項
- metadata: 処理メタデータ

## 5. 移行パターンDB設計

### 5.1 破壊的変更パターン

| ID | カテゴリ | パターン | 自動修正 |
|----|---------|---------|---------|
| BC-001 | namespace | javax.servlet → jakarta.servlet | ○ |
| BC-002 | namespace | javax.persistence → jakarta.persistence | ○ |
| BC-003 | api_removal | DbAccessSupport | × |
| BC-005 | api_change | SqlResultSet + search() | × |
| BC-006 | dependency | javax.servlet-api | ○ |
| BC-007 | dependency | nablarch-bom 5.x | ○ |

### 5.2 パターンマッチング優先順位
1. 完全一致パターン
2. コンテキスト付きパターン
3. 正規表現パターン
4. 非推奨警告

## 6. コード解析ロジック

### 6.1 処理フロー
1. コードタイプ検出（Java/XML/POM）
2. パターンマッチング
3. 影響分析
4. RAG補完（手動修正必要時）

### 6.2 コードタイプ検出
- JAVA: package, import, class定義
- XML: component, list, property
- POM: project, dependency
- PROPERTIES: key=value形式

## 7. RAG連携フロー

検索トリガー:
- 手動修正が必要な問題が検出された場合
- パターンDBに詳細情報がない場合

クエリ生成:
1. 移行ガイド検索
2. 問題タイプ別詳細検索

## 8. 修正提案生成

| カテゴリ | 自動修正 | 工数 |
|---------|---------|------|
| namespace | ○ | trivial |
| dependency | ○ | trivial |
| api_change | △ | moderate |
| api_removal | × | major |
| config | × | moderate |

## 9. MCP Tool登録

```java
@Tool(name = "analyze_migration")
public class MigrationAnalysisTool {
    @Tool.Execute
    public MigrationAnalysisResult execute(MigrationAnalysisRequest request) {
        // 実装
    }
}
```

## 10. エラーハンドリング

| エラーケース | 対応 |
|-------------|------|
| code_snippet が空 | 400 Bad Request |
| サポート外バージョン | 400 + サポート範囲情報 |
| コードタイプ判定失敗 | デフォルト(Java)で処理 |
| RAG検索失敗 | 基本分析のみ + warning |

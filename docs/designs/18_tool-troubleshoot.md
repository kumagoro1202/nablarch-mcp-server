# troubleshoot Tool 詳細設計書

> **WBS番号**: 3.1.4
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru4 (subtask_063)
> **対応ユースケース**: UC5（トラブルシューティング支援）

## 1. 概要

`troubleshoot` ToolはNablarch固有のエラーメッセージ・スタックトレースを解析し、RAGから検索した解決策を優先順位付けして提供する。

## 2. Tool定義

| 項目 | 値 |
|------|-----|
| Tool名 | `troubleshoot` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `TroubleshootTool` |
| 説明 | Nablarch固有のエラーメッセージ・スタックトレースを解析し、解決策を提供 |

## 3. 入力スキーマ

| パラメータ | 型 | 必須 | 説明 |
|----------|-----|------|------|
| error_message | string | ○ | エラーメッセージ本文 |
| stack_trace | string | × | スタックトレース全文 |
| error_code | string | × | Nablarchエラーコード |
| environment | object | × | 環境情報 |

## 4. 出力スキーマ

- error_analysis: エラー分類結果
- solutions: 優先順位付き解決策リスト
- related_resources: 関連ドキュメント
- metadata: 処理メタデータ

## 5. エラー解析ロジック

### 5.1 処理フロー
1. エラー分類（ErrorClassifier）
2. スタックトレース解析（StackTraceAnalyzer）
3. コンテキスト構築（ContextBuilder）
4. RAG検索
5. 解決策ランキング

### 5.2 エラーカテゴリ
- initialization: 初期化エラー
- database: DB関連エラー
- validation: バリデーションエラー
- handler: ハンドラエラー
- configuration: 設定エラー

## 6. RAG検索フロー

最大3つのクエリを生成：
1. メインクエリ: エラーメッセージ + カテゴリ
2. コンポーネントコンテキスト
3. コード参照

## 7. 解決策優先順位付け

スコアリングモデル:
- 0.40 × rag_relevance_score
- 0.30 × pattern_match_score
- 0.15 × environment_match_score
- 0.15 × source_authority_score

## 8. Phase 1 Promptとの連携

| 機能 | Phase 1 Prompt | Phase 3 Tool |
|------|---------------|--------------|
| ユーザーインタラクション | ○ | × |
| エラー解析 | △ | ○ |
| RAG検索 | × | ○ |

## 9. MCP Tool登録

```java
@Tool(name = "troubleshoot")
public class TroubleshootTool {
    @Tool.Execute
    public TroubleshootResult execute(TroubleshootRequest request) {
        // 実装
    }
}
```

## 10. エラーハンドリング

| エラーケース | 対応 |
|-------------|------|
| error_message が空 | 400 Bad Request |
| RAG検索タイムアウト | 部分結果 + warning |
| パターンマッチなし | 汎用解決策提示 |

# WBS 2.1.5 — リランキング設計 完了基準チェックリスト

**タスクID**: WBS 2.1.5
**成果物**: docs/designs/reranking.md
**担当**: ashigaru8 (subtask_056)

## 完了基準

- [ ] Cross-Encoder API呼び出し設計が記述されている
  - [ ] Jina Reranker / Cohere Reranker の比較・選定
  - [ ] API呼び出しフロー（リクエスト/レスポンス形式）
- [ ] リランキングパイプラインが設計されている
  - [ ] ハイブリッド検索結果(Top-50) → Cross-Encoder → Top-K(5-10)
  - [ ] スコア正規化方式
- [ ] フォールバック設計が記述されている
  - [ ] API障害時はRRFスコアのみで応答
- [ ] レイテンシバジェット配分が定義されている
  - [ ] 検索200ms + リランキング100ms = 300ms
- [ ] hybrid-search.md との整合性が確認されている

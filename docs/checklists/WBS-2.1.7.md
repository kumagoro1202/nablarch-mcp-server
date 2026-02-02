# WBS 2.1.7 — 検索品質評価設計 完了基準チェックリスト

**タスクID**: WBS 2.1.7
**成果物**: docs/designs/14_search-quality-evaluation.md
**担当**: ashigaru8 (subtask_056)

## 完了基準

- [ ] 評価メトリクスが定義されている
  - [ ] MRR (Mean Reciprocal Rank): 目標 0.7以上
  - [ ] Recall@5: 目標 0.8以上
  - [ ] NDCG@5: 目標 0.7以上
- [ ] 評価データセット仕様が定義されている
  - [ ] 50件以上のNablarch固有クエリ
  - [ ] カテゴリ: ハンドラキュー(10), API使い方(10), 設計パターン(10), トラブルシューティング(10), 設定方法(10)
- [ ] 評価データセットフォーマットが定義されている
  - [ ] query, expected_doc_ids, relevance_score
- [ ] A/Bテスト方式が設計されている
  - [ ] BM25 only vs Vector only vs Hybrid vs Hybrid+Rerank
- [ ] 自動評価スクリプト設計が記述されている

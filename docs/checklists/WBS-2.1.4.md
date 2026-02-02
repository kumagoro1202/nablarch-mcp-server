# WBS 2.1.4 — ハイブリッド検索設計 完了基準チェックリスト

**タスクID**: WBS 2.1.4
**成果物**: docs/designs/11_hybrid-search.md
**担当**: ashigaru8 (subtask_056)

## 完了基準

- [ ] BM25検索（PostgreSQL FTS）の設計が記述されている
  - [ ] to_tsvector / to_tsquery の使用方法
  - [ ] tf-idf重み付け（ts_rank_cd）の説明
  - [ ] 日本語テキスト検索の考慮事項
- [ ] ベクトル検索（pgvectorコサイン類似度）の設計が記述されている
  - [ ] コサイン類似度クエリの定義
  - [ ] インデックス戦略（ivfflat / HNSW）
- [ ] Reciprocal Rank Fusion (RRF) の設計が記述されている
  - [ ] 数式: score = Σ 1/(k + rank_i) where k=60
  - [ ] BM25/Vector結果の統合フロー
- [ ] 重み付け方式の設計が記述されている
  - [ ] α*BM25 + (1-α)*Vector, α=0.3 (デフォルト)
  - [ ] パラメータチューニング方針
- [ ] 検索フロー図が含まれている
  - [ ] query → 並列検索 → RRF統合 → Top-K
- [ ] メタデータフィルタリングが設計されている
  - [ ] app_type, module, source条件
  - [ ] WHERE句動的構築方式
- [ ] パフォーマンス要件が定義されている
  - [ ] 目標: 100-300ms
  - [ ] ボトルネック分析と対策
- [ ] architecture.md §4.6 との整合性が確認されている
- [ ] ADR-001 の決定事項との整合性が確認されている

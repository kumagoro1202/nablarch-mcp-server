# WBS 2.3.8 — RAG検索品質評価 完了基準チェックリスト

**タスクID**: WBS 2.3.8
**成果物**: 品質評価テスト + 評価データセット + メトリクス計算ユーティリティ
**担当**: ashigaru8 (subtask_069)

## 完了基準

### 評価データセット
- [ ] evaluation-queries.yaml に50件以上のクエリを定義
- [ ] 5カテゴリ（handler_queue, api_usage, design_pattern, troubleshooting, configuration）
- [ ] 日本語/英語/混在クエリを含む

### メトリクス計算
- [ ] EvaluationMetrics: MRR計算
- [ ] EvaluationMetrics: Recall@K計算
- [ ] EvaluationMetrics: NDCG@K計算
- [ ] EvaluationMetricsTest: 正確性検証 + エッジケース

### 品質評価テスト
- [ ] SearchQualityEvaluationTest: MRR計算テスト
- [ ] SearchQualityEvaluationTest: Recall@5テスト
- [ ] SearchQualityEvaluationTest: Recall@10テスト
- [ ] SearchQualityEvaluationTest: NDCG@5テスト
- [ ] SearchQualityEvaluationTest: リランキング効果検証
- [ ] SearchQualityEvaluationTest: 検索モード別比較

### データセットローダー
- [ ] QueryEvaluationDataset: YAML読込
- [ ] EvaluationQuery record定義

### 品質
- [ ] Javadocが全て日本語
- [ ] mainに直接コミットしていない
- [ ] テスト全パス

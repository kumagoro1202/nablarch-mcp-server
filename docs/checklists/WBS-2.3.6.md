# WBS 2.3.6: 統合テスト — リランキング

## チェックリスト

### RerankingIntegrationTest
- [x] リランキング効果検証（特定文書が上位に移動）
- [x] Top-N制御（候補50件 → rerank → topN=5で5件）
- [x] Reranker API失敗時のフォールバック（元のHybridSearch順序維持）
- [x] 空候補のリランキング（空リスト → 空リスト、API呼出なし）
- [x] パイプライン全段テスト（BM25+Vector → RRF → Rerank → 最終結果）

### 品質
- [x] Javadocは日本語で記述
- [x] 新規テスト全通過

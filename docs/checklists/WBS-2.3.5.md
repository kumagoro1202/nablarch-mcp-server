# WBS 2.3.5: 統合テスト — ハイブリッド検索

## チェックリスト

### HybridSearchIntegrationTest
- [x] HYBRIDモード統合フロー: BM25 5件 + Vector 5件（重複2件）→ RRF統合
- [x] 重複結果のスコアが非重複より高いことの検証
- [x] 結果がスコア降順であることの検証
- [x] テストクエリセット（ハンドラキュー/REST API/バッチ処理）
- [x] フィルタ適用統合テスト（appType="web"がBM25/Vector両方に渡される）
- [x] グレースフルデグレード: BM25タイムアウト → Vector結果のみ
- [x] グレースフルデグレード: Vector例外 → BM25結果のみ
- [x] 空結果統合テスト: 両方空 → 空リスト

### SearchQualityTest
- [x] RRFスコア計算精度検証（k=60、小数6桁精度）
- [x] 重複マージ精度（同一IDのRRF合算）
- [x] topK切り捨て精度（50件 → topK=5 で5件）

### 品質
- [x] Javadocは日本語で記述
- [x] 新規テスト全通過

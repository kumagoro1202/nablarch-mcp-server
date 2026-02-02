# WBS 2.3.7: semantic_search 統合テスト

## 概要
SemanticSearchToolのE2E統合テスト。
RAGパイプライン全段（QueryAnalyzer → HybridSearch → Rerank → 結果整形）の統合動作を検証する。

## チェックリスト

### SemanticSearchIntegrationTest
- [ ] 全段パイプライン統合テスト（日本語クエリ）
- [ ] 英語クエリ統合テスト
- [ ] フィルタ付き検索
- [ ] QueryAnalyzer未注入時のフォールバック
- [ ] Reranker失敗時のフォールバック
- [ ] 検索結果ゼロ
- [ ] topK パラメータ

### SemanticSearchMcpRegistrationTest
- [ ] Tool登録確認（semantic_search がMCPに登録）
- [ ] Toolの入力スキーマ検証
- [ ] Tool説明文確認

### SemanticSearchOutputFormatTest
- [ ] Markdown構造（番号、スコア、コンテンツ、URL）
- [ ] メタデータ表示
- [ ] 特殊文字処理

### 環境整備
- [ ] プリ既存バグ修正（FintanIngester @Qualifier, OfficialDocsIngesterTest）
- [ ] TestConfig.java（WebClientビーン提供）
- [ ] application-test.yaml（rerank設定追加）

## 依存
- SemanticSearchTool (WBS 2.2.15)
- HybridSearchService (WBS 2.2.12)
- CrossEncoderReranker (WBS 2.2.13)
- QueryAnalyzer (WBS 2.2.14)

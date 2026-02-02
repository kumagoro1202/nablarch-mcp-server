# WBS 2.3.7: semantic_search 統合テスト

## 概要
SemanticSearchToolのE2E統合テスト。
RAGパイプライン全段（QueryAnalyzer → HybridSearch → Rerank → 結果整形）の統合動作を検証する。

## チェックリスト

### SemanticSearchIntegrationTest
- [x] 全段パイプライン統合テスト（日本語クエリ）
- [x] 英語クエリ統合テスト
- [x] フィルタ付き検索
- [x] Reranker失敗時のフォールバック
- [x] 検索結果ゼロ
- [x] topK パラメータ
- [x] keywordモード（BM25のみ使用）

### SemanticSearchMcpRegistrationTest
- [x] Tool登録確認（semanticSearch がMCPに登録）
- [x] Toolの入力スキーマ検証（query, appType, mode）
- [x] Tool説明文確認（Nablarch + search 含む英語説明文）
- [x] 既存ツール共存確認（searchApi + semanticSearch）

### SemanticSearchOutputFormatTest
- [x] Markdown構造（ヘッダ、番号、スコア、コンテンツ、URL）
- [x] メタデータ表示（source, app_type, module）
- [x] 特殊文字処理
- [x] エッジケース（0件、1件、大量件、空メタデータ、null値）

### 環境整備
- [x] プリ既存バグ修正（FintanIngester @Qualifier, OfficialDocsIngesterTest）
- [x] TestConfig.java（WebClientビーン提供）
- [x] application-test.yaml（rerank設定追加）
- [x] SemanticSearchTool queryAnalyzer型修正（Object → QueryAnalyzer）
- [x] ToolCallback API修正（getToolDefinition().name()/description()/inputSchema()）
- [x] OfficialDocsIngester @MockitoBean差し替え

### ビルド結果
- [x] 全453テスト実行
- [x] 新規テスト25件全通過
- [x] 既知の失敗2件のみ（BM25SearchServiceTest, NablarchMcpServerApplicationTests）

## 依存
- SemanticSearchTool (WBS 2.2.15)
- HybridSearchService (WBS 2.2.12)
- CrossEncoderReranker (WBS 2.2.13)
- QueryAnalyzer (WBS 2.2.14)

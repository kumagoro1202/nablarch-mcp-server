# WBS 2.2.15: semantic_search Tool 実装

## 概要
MCP Tool として `semantic_search` を登録し、RAGパイプライン全体を統合する。
クエリ → [QueryAnalyzer(optional)] → HybridSearch → Rerank → 結果整形

## チェックリスト

### 実装
- [ ] SemanticSearchTool @Service
  - [ ] @Tool("semantic_search") + @Description でMCP Tool登録
  - [ ] DI: HybridSearchService, Reranker
  - [ ] オプショナルDI: QueryAnalyzer (@Autowired(required=false))
  - [ ] 入力パラメータ: query(必須), filters(optional), topK(default=5), mode(default=HYBRID)
  - [ ] 処理フロー:
    1. QueryAnalyzer あれば → クエリ拡張
    2. HybridSearchService.search() で候補取得
    3. Reranker.rerank() でリランキング
    4. 結果をMarkdown形式に整形して返却
  - [ ] Markdown出力形式（スコア、内容スニペット、メタデータ、ソースURL）
  - [ ] SearchFilters変換: Tool入力 → SearchFilters record
  - [ ] エラーハンドリング: 検索失敗時は "検索中にエラーが発生しました" メッセージ

### McpServerConfig
- [ ] nablarchTools に SemanticSearchTool 追加

### テスト
- [ ] SemanticSearchToolTest
  - [ ] 正常検索フロー（モック: HybridSearch, Reranker）
  - [ ] QueryAnalyzer未注入時の動作
  - [ ] フィルタ変換
  - [ ] エラーハンドリング
  - [ ] Markdown出力形式

## 依存
- HybridSearchService (WBS 2.2.12)
- Reranker / CrossEncoderReranker (WBS 2.2.13)
- QueryAnalyzer (WBS 2.2.14) ※オプショナル、足軽8号が並行実装中
- SearchApiTool パターン参照

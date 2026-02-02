# WBS 2.2.12 ハイブリッド検索実装 (HybridSearchService) 完了基準チェックリスト

- [ ] HybridSearchService.java が com.tis.nablarch.mcp.rag.search パッケージに存在する
- [ ] BM25SearchService と VectorSearchService をDI注入している
- [ ] search(query, filters, topK, mode) メソッドが実装されている
- [ ] HYBRID モード: CompletableFuture で並列実行している
- [ ] KEYWORD モード: BM25SearchService.search() のみ実行
- [ ] VECTOR モード: VectorSearchService.search() のみ実行
- [ ] RRF計算: 1/(k+rank) でスコア統合、k=60（デフォルト）
- [ ] 候補数: 内部で50件ずつ取得 → RRF統合 → topK件返却
- [ ] グレースフルデグレード: 一方が例外の場合もう一方の結果で応答
- [ ] 両方失敗時は空リスト + エラーログ
- [ ] HybridSearchServiceTest が存在し通過する
- [ ] テスト: HYBRID モード（RRFスコア検証）
- [ ] テスト: KEYWORD モード
- [ ] テスト: VECTOR モード
- [ ] テスト: グレースフルデグレード（BM25失敗時）
- [ ] テスト: グレースフルデグレード（Vector失敗時）
- [ ] テスト: 空結果ケース

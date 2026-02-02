# WBS 2.2.11 ベクトル検索実装 (VectorSearchService) 完了基準チェックリスト

- [ ] VectorSearchService.java が com.tis.nablarch.mcp.rag.search パッケージに存在する
- [ ] NamedParameterJdbcTemplate を使用して柔軟なSQL構築をしている
- [ ] EmbeddingClient.embed() でクエリをベクトルに変換している
- [ ] float[] → "[0.1,0.2,...]" 形式のベクトル文字列変換がある
- [ ] document_chunks テーブルへのコサイン類似度検索が実装されている
- [ ] code_chunks テーブルへのコサイン類似度検索が実装されている
- [ ] document_chunks は Jina v4、code_chunks は Voyage-code-3 を使用している
- [ ] 2テーブルの結果をスコア降順でマージしている
- [ ] SearchFilters によるメタデータフィルタリングが動的WHERE句で実装されている
- [ ] topK制限が正しく適用されている
- [ ] query null/空白のバリデーションがある
- [ ] VectorSearchServiceTest が存在し通過する
- [ ] テスト: 正常検索（モック）
- [ ] テスト: フィルタ付き検索
- [ ] テスト: 空結果
- [ ] テスト: バリデーション
- [ ] テスト: 2テーブル統合

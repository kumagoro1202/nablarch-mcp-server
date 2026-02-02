# WBS 2.2.6 Embedding統合: Jina v4 完了基準チェックリスト

- [ ] EmbeddingClient インターフェースが定義されている
- [ ] EmbeddingRequest / EmbeddingResponse DTOが存在する
- [ ] EmbeddingProperties 設定クラスが存在する
- [ ] JinaEmbeddingClient.java が存在する
- [ ] embed() で単一テキストのEmbedding生成ができる
- [ ] embedBatch() で複数テキストの一括Embedding生成ができる
- [ ] リトライ機構が実装されている
- [ ] タイムアウト設定が反映されている
- [ ] JinaEmbeddingClientTest が存在し通過する（MockWebServer使用）

# WBS 2.3.4: 統合テスト — 取込パイプライン

## テストクラス

### IngestionPipelineIntegrationTest（パイプラインフロー検証）

- [x] OfficialDocsIngester全件取込フロー（HTML取得→パース→チャンキング→Embedding→格納）
- [x] FintanIngester全件取込フロー（Markdown取得→パース→チャンキング→Embedding→格納）
- [x] OfficialDocsIngester増分取込（since指定、全URL処理）
- [x] FintanIngester増分取込（since指定、新規URLのみ処理）
- [x] 障害隔離テスト（1件目成功、2件目失敗、3件目成功 → 2件のみ格納）
- [x] バッチサイズ制御（30チャンク、バッチサイズ10 → embedBatch 3回呼出）
- [x] OfficialDocs空データソース（URL一覧空 → IngestionResult(0,0,0,[])）
- [x] Fintan空データソース（記事URL一覧空 → IngestionResult(0,0,0,[])）
- [x] OfficialDocs無効化状態 → 空結果
- [x] Fintan無効化状態 → 空結果

### IngestionDataFlowTest（データ変換整合性検証）

- [x] HTML→ParsedDocument→DocumentChunkDto変換整合性（タイトル・URL・コンテンツ保持）
- [x] HTMLからセクション単位でParsedDocument生成
- [x] HTMLパース結果にsource_urlメタデータ含有
- [x] Markdown→ParsedDocument→DocumentChunkDto変換整合性
- [x] Markdownから見出し単位でParsedDocument生成
- [x] HTMLメタデータ伝搬（source, source_type, language → チャンクまで）
- [x] Markdownメタデータ伝搬（source, source_type, content_type → チャンクまで）
- [x] チャンクインデックスと総チャンク数の正確性
- [x] HTMLチャンクの最小サイズ検証（50文字以上）
- [x] Markdownチャンクの最小サイズ検証（50文字以上）
- [x] 大きなコンテンツの複数チャンク分割
- [x] 短いコンテンツの単一チャンク保持

### テストデータ

- [x] sample-official-doc.html（Nablarch公式Docs風HTML）
- [x] sample-fintan-article.md（Fintanブログ風Markdown）

## 検証項目

- [x] 既存テストを壊していないこと
- [x] 新規テスト全パス

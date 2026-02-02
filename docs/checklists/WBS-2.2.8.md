# WBS 2.2.8: ドキュメント取り込みパイプライン（公式Docs）

## チェックリスト

### 設計・インターフェース
- [x] DocumentIngester インターフェース作成（ingestAll, ingestIncremental, getSourceName）
- [x] IngestionResult レコード作成（processedCount, successCount, errorCount, errors）

### 設定
- [x] IngestionConfig @ConfigurationProperties 作成
- [x] application-test.yaml に ingestion 設定追加

### OfficialDocsIngester 実装
- [x] @Service, DocumentIngester 実装
- [x] コンストラクタDI（HtmlDocumentParser, ChunkingService, EmbeddingClient, DocumentChunkRepository）
- [x] クローリング: インデックスページからURL一覧取得（WebClient）
- [x] robots.txt準拠のディレイ（デフォルト1秒間隔）
- [x] パース: HtmlDocumentParser.parse() 呼び出し
- [x] チャンキング: ChunkingService.chunk() 呼び出し
- [x] Embedding: EmbeddingClient.embedBatch() 呼び出し
- [x] 格納: DocumentChunkRepository.save() + updateEmbedding()
- [x] 個別ドキュメント障害隔離（1ページ失敗しても他は続行）
- [x] リトライ: 指数バックオフ3回（HTTP 429/5xx）
- [x] 増分取込（ingestIncremental）対応
- [x] バッチサイズ制御

### テスト
- [x] OfficialDocsIngesterTest: 正常取込フロー
- [x] OfficialDocsIngesterTest: 個別ドキュメント障害隔離
- [x] OfficialDocsIngesterTest: 増分取込
- [x] OfficialDocsIngesterTest: 空結果ケース
- [x] OfficialDocsIngesterTest: バッチサイズ制御
- [x] IngestionResultTest: 正常値・エラー集計
- [x] DocumentIngester IF テスト

### 品質
- [x] Javadocは日本語で記述
- [x] ./gradlew test 新規テスト全通過（既存の2件失敗はmain由来で変更前から存在）
- [x] mainブランチ未接触

### 納品
- [ ] git commit + push
- [ ] PR作成
- [ ] ashigaru6_report.yaml 報告
- [ ] 家老にsend-keys通知

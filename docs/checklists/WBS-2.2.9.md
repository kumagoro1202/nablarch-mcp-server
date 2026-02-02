# WBS 2.2.9 — Fintan取込パイプライン 完了基準チェックリスト

**タスクID**: WBS 2.2.9
**成果物**: FintanIngester.java + テスト
**担当**: ashigaru8 (subtask_060)

## 完了基準

### インターフェース・DTO
- [ ] DocumentIngester インターフェース作成（足軽6号と同一設計）
  - [ ] ingestAll() → IngestionResult
  - [ ] ingestIncremental(Instant since) → IngestionResult
  - [ ] getSourceName() → String
- [ ] IngestionResult DTO作成
  - [ ] processedCount, successCount, errorCount, errors

### FintanIngester 実装
- [ ] @Service, DocumentIngester 実装
- [ ] コンストラクタDI: MarkdownDocumentParser, HtmlDocumentParser, ChunkingService, EmbeddingClient, DocumentChunkRepository
- [ ] 記事URL一覧取得（WebClient）
- [ ] 各記事取得＋パース（HTML→HtmlDocumentParser, Markdown→MarkdownDocumentParser）
- [ ] チャンキング: ChunkingService.chunk()
- [ ] Embedding: EmbeddingClient.embedBatch()
- [ ] 格納: DocumentChunkRepository.save() + updateEmbedding()
- [ ] メタデータ: source="fintan", source_type="documentation"
- [ ] エラーハンドリング: 個別記事障害隔離、リトライ3回
- [ ] ディレイ: 1秒間隔（robots.txt準拠）
- [ ] 増分取込 (ingestIncremental) 対応

### 設定
- [ ] FintanIngestionConfig (@ConfigurationProperties)
  - [ ] base-url, search-tags, batch-size, delay-ms, max-retries, enabled

### テスト
- [ ] FintanIngesterTest
  - [ ] 正常取込フロー
  - [ ] HTML記事とMarkdown記事の判定
  - [ ] 個別記事障害隔離
  - [ ] 増分取込
- [ ] FintanIngestionConfigTest
  - [ ] 設定値バインド検証

### 品質
- [ ] Javadocが全て日本語で記述されている
- [ ] ./gradlew test が通る（既存テスト含む）

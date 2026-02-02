# Embeddingパイプライン設計書

> **WBS**: 2.1.2
> **ステータス**: 完了
> **作成日**: 2026-02-02
> **関連**: ADR-001, architecture.md §4

---

## 1. パイプライン概要

Nablarch知識ベースの構築・更新を行うデータ処理パイプライン。
4種のデータソースからドキュメントを取得し、パース→チャンキング→Embedding→格納の4段階で処理する。

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Embeddingパイプライン全体像                        │
│                                                                     │
│  ┌──────────────┐   ┌──────────┐   ┌───────────┐   ┌───────────┐  │
│  │ データソース   │→ │ パーサー   │→ │ チャンキング │→ │ Embedding │  │
│  │ (4種)        │   │ (4種)     │   │ (6タイプ)   │   │ (2モデル)  │  │
│  └──────────────┘   └──────────┘   └───────────┘   └─────┬─────┘  │
│                                                           │        │
│                                                    ┌──────▼──────┐ │
│                                                    │   格納       │ │
│                                                    │ PostgreSQL   │ │
│                                                    │ + pgvector   │ │
│                                                    └─────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

## 2. データソース定義

### 2.1 4種のデータソース

| # | データソース | 形式 | 取得方法 | 推定規模 | 更新頻度 |
|---|---|---|---|---|---|
| DS-1 | Nablarch公式ドキュメント | HTML | WebFetch / クローリング | 数百ページ | バージョンリリース時 |
| DS-2 | Fintan技術記事 | Markdown / HTML | WebFetch | 数十記事 | 不定期 |
| DS-3 | GitHubリポジトリ | Java / XML | gh API / git clone | 113リポジトリ | コミット単位 |
| DS-4 | XML設定ファイル | XML | ローカルファイル / gh API | サンプルプロジェクト内 | プロジェクト更新時 |

### 2.2 各データソースの詳細

#### DS-1: Nablarch公式ドキュメント

```
URL: https://nablarch.github.io/docs/LATEST/doc/
構造: Sphinx生成HTML、セクション階層あり
言語: 日本語
特徴:
  - h1/h2/h3 による階層構造
  - コードブロック（Java, XML）を含む
  - 内部リンクによるページ間参照
  - バージョン別のディレクトリ構造 (/LATEST/, /5u23/ 等)
取得戦略:
  - サイトマップまたはインデックスページからURL一覧を取得
  - 各ページをHTTPで取得し、HTMLパーサーでセクション抽出
```

#### DS-2: Fintan技術記事

```
URL: https://fintan.jp/ (Nablarch関連記事)
構造: Markdown / HTML（CMS生成）
言語: 日本語
特徴:
  - ブログ形式の技術記事
  - コードサンプル、図表を含む
  - Nablarchタグでフィルタリング可能
取得戦略:
  - Nablarchタグ付き記事一覧をWebSearchで取得
  - 各記事をWebFetchで取得
  - Markdownパーサーで構造抽出
```

#### DS-3: GitHubリポジトリ

```
組織: https://github.com/nablarch (113リポジトリ)
対象ファイル:
  - Java: src/main/java/**/*.java（Handler実装、Action、Entity等）
  - Javadoc: クラス/メソッドレベルのドキュメントコメント
  - XML: src/main/resources/**/*.xml（コンポーネント定義）
取得戦略:
  - gh api でリポジトリ一覧取得
  - 主要リポジトリ（nablarch-fw-*, nablarch-core-*）を優先
  - ファイルツリーAPIでJava/XMLファイルを列挙
  - コンテンツAPIで個別ファイル取得
```

#### DS-4: XML設定ファイル

```
ソース: サンプルプロジェクト、公式ドキュメント内の設定例
対象:
  - ハンドラキュー定義 (handler-queue.xml)
  - コンポーネント定義 (component-definition.xml)
  - 環境設定 (env.config)
取得戦略:
  - サンプルプロジェクトのXMLファイルをgh APIで取得
  - 公式ドキュメントのコードブロックからXML例を抽出（DS-1のパース結果から）
```

## 3. パイプラインフロー

### 3.1 段階別処理フロー

```
┌──────────────────────────────────────────────────────────────────┐
│ Stage 1: データ取得 (Ingestion)                                   │
│                                                                   │
│  DS-1 ──→ RawDocument { url, rawContent, contentType=HTML }      │
│  DS-2 ──→ RawDocument { url, rawContent, contentType=MARKDOWN }  │
│  DS-3 ──→ RawDocument { filePath, rawContent, contentType=JAVA } │
│           RawDocument { filePath, rawContent, contentType=XML }   │
│  DS-4 ──→ RawDocument { filePath, rawContent, contentType=XML }  │
└──────────────────────────┬───────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ Stage 2: パース (Parsing)                                        │
│                                                                   │
│  HtmlDocumentParser     ──→ List<ParsedDocument>                 │
│  MarkdownDocumentParser ──→ List<ParsedDocument>                 │
│  JavaSourceParser       ──→ List<ParsedDocument>                 │
│  XmlConfigParser        ──→ List<ParsedDocument>                 │
│                                                                   │
│  ParsedDocument: { content, metadata, sourceUrl, contentType }   │
└──────────────────────────┬───────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ Stage 3: チャンキング (Chunking)                                  │
│                                                                   │
│  ChunkingService.chunk(ParsedDocument) → List<DocumentChunkDto>  │
│                                                                   │
│  6コンテンツタイプ別ストラテジー:                                    │
│  HTML     → セクション単位 (512 tokens, 128 overlap)              │
│  Markdown → 見出し単位 (512 tokens, 128 overlap)                 │
│  Javadoc  → クラス/メソッド単位 (256 tokens)                      │
│  Java     → メソッド単位 (256-512 tokens)                        │
│  XML      → 要素単位 (256 tokens)                                │
│  Text     → 段落単位 (512 tokens, 128 overlap)                   │
│                                                                   │
│  DocumentChunkDto: { content, metadata, chunkIndex, totalChunks } │
└──────────────────────────┬───────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ Stage 4: Embedding                                                │
│                                                                   │
│  ドキュメント系 (HTML/Markdown/Text):                              │
│    Jina embeddings-v4 → float[1024]                              │
│                                                                   │
│  コード系 (Java/Javadoc/XML):                                     │
│    Voyage-code-3 → float[1024]                                   │
│                                                                   │
│  EmbeddingRouter: contentType に応じてモデルを自動選択             │
└──────────────────────────┬───────────────────────────────────────┘
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│ Stage 5: 格納 (Storage)                                           │
│                                                                   │
│  ドキュメント系 → document_chunks テーブル                         │
│    id, content, embedding(vector(1024)), metadata(jsonb),        │
│    source_url, chunk_type, created_at, updated_at                │
│                                                                   │
│  コード系 → code_chunks テーブル                                   │
│    id, content, embedding(vector(1024)), metadata(jsonb),        │
│    file_path, class_name, created_at, updated_at                 │
│                                                                   │
│  PostgreSQL 16 + pgvector (ivfflat / HNSW インデックス)           │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 データソース別の完全フロー

#### DS-1: Nablarch公式ドキュメント → document_chunks

```
取得: WebFetch(url) → HTML
パース: HtmlDocumentParser.parse(html, url) → List<ParsedDocument>
  - セクション分割（h2/h3見出し）
  - コードブロック保持
  - メタデータ: {source: "nablarch-document", section_hierarchy: [...]}
チャンキング: ChunkingService.chunk(doc) → List<DocumentChunkDto>
  - 最大512トークン、128トークンオーバーラップ
Embedding: JinaEmbeddingClient.embed(chunk.content) → float[1024]
格納: DocumentChunkRepository.save(DocumentChunk)
```

#### DS-2: Fintan記事 → document_chunks

```
取得: WebFetch(url) → HTML/Markdown
パース: MarkdownDocumentParser.parse(content, url) → List<ParsedDocument>
  - 見出し区切り
  - コードフェンス保持
  - メタデータ: {source: "fintan", category: "..."}
チャンキング: ChunkingService.chunk(doc)
  - 最大512トークン、128トークンオーバーラップ
Embedding: JinaEmbeddingClient.embed(chunk.content)
格納: DocumentChunkRepository.save(DocumentChunk)
```

#### DS-3: GitHubリポジトリ → code_chunks

```
取得: gh api repos/nablarch/{repo}/contents/{path} → Java/XML
パース（Java）: JavaSourceParser.parse(javaContent, filePath) → List<ParsedDocument>
  - クラス/メソッド/Javadoc分割
  - メタデータ: {source: "github", fqcn: "...", file_path: "..."}
パース（XML）: XmlConfigParser.parse(xmlContent, filePath) → List<ParsedDocument>
  - 要素単位分割
  - メタデータ: {source: "github", file_path: "...", element_type: "..."}
チャンキング: ChunkingService.chunk(doc)
  - Java: メソッド単位、256-512トークン
  - XML: 要素単位、256トークン
Embedding: VoyageEmbeddingClient.embed(chunk.content)
格納: CodeChunkRepository.save(CodeChunk)
```

## 4. バッチ処理設計

### 4.1 処理モード

| モード | 説明 | トリガー | 処理範囲 |
|---|---|---|---|
| FULL | 全データソースの全件取り込み | 初回構築、スキーマ変更後 | 全ドキュメント |
| INCREMENTAL | 前回以降の差分のみ取り込み | 定期実行（日次/週次） | 変更分のみ |
| TARGETED | 指定データソース/URLのみ | 手動実行、Webhook | 指定範囲のみ |

### 4.2 増分更新メカニズム

```
【増分更新フロー】

1. 変更検出
   - DS-1/DS-2: HTTPレスポンスの Last-Modified / ETag を保存・比較
   - DS-3: git log --since={last_run} でコミット差分取得
   - DS-4: ファイルタイムスタンプ比較

2. 差分処理
   - 新規: パース→チャンキング→Embedding→INSERT
   - 更新: 既存チャンク削除→パース→チャンキング→Embedding→INSERT
   - 削除: 対象source_urlのチャンクをDELETE

3. 変更管理テーブル
   ingestion_log:
     id, source_url, last_modified, etag, status, processed_at, chunk_count
```

### 4.3 並列処理設計

```
【並列処理アーキテクチャ】

ExecutorService (固定スレッドプール: 4)
  ├── Thread-1: DS-1 処理 (HTML → document_chunks)
  ├── Thread-2: DS-2 処理 (Markdown → document_chunks)
  ├── Thread-3: DS-3 処理 (Java → code_chunks)
  └── Thread-4: DS-3 処理 (XML → code_chunks)

各スレッドは独立したデータソースを処理するため、
テーブルロックの競合なし（document_chunks / code_chunks は分離）。

Embedding API呼び出しはバッチ化（1リクエストで最大32テキスト）。
レート制限: Jina API 500 RPM, Voyage API 300 RPM
```

## 5. エラーハンドリング・リトライ戦略

### 5.1 エラー分類

| エラー種別 | 例 | 対応 |
|---|---|---|
| 一時的エラー | HTTP 429/503、タイムアウト、API レート制限 | 指数バックオフリトライ |
| 永続的エラー | HTTP 404、パース不能、不正なコンテンツ | スキップ＋ログ記録 |
| システムエラー | DB接続断、メモリ不足 | パイプライン停止＋アラート |

### 5.2 リトライ戦略

```java
// 指数バックオフ設定
RetryConfig:
  maxRetries: 3
  initialDelay: 1000ms
  multiplier: 2.0
  maxDelay: 30000ms
  retryableExceptions:
    - HttpTimeoutException
    - HttpServerErrorException (5xx)
    - TooManyRequestsException (429)
```

### 5.3 障害回復

```
【障害回復フロー】

1. 個別ドキュメント障害
   - 該当ドキュメントをスキップ
   - error_log テーブルに記録（url, error_type, message, timestamp）
   - 残りのドキュメントは処理続行

2. Embedding API障害
   - リトライ上限到達後、未処理チャンクをpending_embeddingsキューに登録
   - 次回バッチ実行時にpendingキューから優先処理

3. DB障害
   - トランザクション単位でロールバック
   - パイプライン停止、管理者通知
   - 復旧後、ingestion_log のステータスから再開ポイントを特定

4. 部分的成功の保証
   - 各ドキュメントは独立したトランザクションで処理
   - 1ドキュメントの失敗が他に波及しない
   - バッチ全体のサマリ（成功数/失敗数/スキップ数）をログ出力
```

## 6. パイプラインのJavaクラス構成

```
com.tis.nablarch.mcp.rag
├── parser/
│   ├── DocumentParser.java          # パーサーインターフェース
│   ├── ParsedDocument.java          # パース結果DTO
│   ├── HtmlDocumentParser.java      # DS-1用
│   ├── MarkdownDocumentParser.java  # DS-2用
│   ├── JavaSourceParser.java        # DS-3用（Javaソース）
│   └── XmlConfigParser.java         # DS-3/DS-4用
├── chunking/
│   ├── ChunkingService.java         # チャンキングエンジン
│   ├── DocumentChunkDto.java        # チャンク結果DTO
│   └── ContentType.java             # コンテンツタイプenum
├── embedding/
│   ├── EmbeddingRouter.java         # モデル自動選択
│   ├── JinaEmbeddingClient.java     # Jina v4クライアント
│   └── VoyageEmbeddingClient.java   # Voyage-code-3クライアント
├── ingestion/
│   ├── IngestionPipeline.java       # パイプラインオーケストレーター
│   ├── OfficialDocsIngester.java    # DS-1取り込み
│   ├── FintanIngester.java          # DS-2取り込み
│   ├── GitHubCodeIngester.java      # DS-3取り込み
│   └── IngestionLog.java            # 取り込み履歴
└── config/
    └── RagConfig.java               # RAG設定（チャンクサイズ、APIキー等）
```

## 7. 設定パラメータ

```yaml
# application.yml (RAG関連設定)
nablarch.rag:
  chunking:
    default-max-tokens: 512
    default-overlap-tokens: 128
    code-max-tokens: 256
  embedding:
    jina:
      api-url: "https://api.jina.ai/v1/embeddings"
      model: "jina-embeddings-v4"
      dimensions: 1024
      batch-size: 32
    voyage:
      api-url: "https://api.voyageai.com/v1/embeddings"
      model: "voyage-code-3"
      dimensions: 1024
      batch-size: 32
  ingestion:
    mode: INCREMENTAL
    thread-pool-size: 4
    retry:
      max-retries: 3
      initial-delay-ms: 1000
      multiplier: 2.0
      max-delay-ms: 30000
  datasources:
    official-docs:
      base-url: "https://nablarch.github.io/docs/LATEST/doc/"
      enabled: true
    fintan:
      search-tag: "Nablarch"
      enabled: true
    github:
      org: "nablarch"
      priority-repos:
        - "nablarch-fw-web"
        - "nablarch-fw-web-extension"
        - "nablarch-core-repository"
        - "nablarch-core-validation"
        - "nablarch-core-transaction"
      enabled: true
```

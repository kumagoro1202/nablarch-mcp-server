# WBS 2.2.13: Cross-Encoder リランキング実装

## 概要
Jina Reranker v2 (jina-reranker-v2-base-multilingual) を HTTP API で呼び出し、
ハイブリッド検索結果をCross-Encoderで再順位付けする。

## チェックリスト

### インターフェース
- [ ] Reranker インターフェース作成
  - `List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK)`

### 設定
- [ ] RerankProperties (@ConfigurationProperties)
  - api-key, model, timeout-ms, top-k

### 実装
- [ ] CrossEncoderReranker @Service
  - [ ] WebClient で Jina Reranker API 呼び出し (POST /v1/rerank)
  - [ ] リクエストDTO: model, query, documents, top_n, return_documents
  - [ ] レスポンスDTO: results[].index, results[].relevance_score
  - [ ] relevance_score でスコア置換・ソート
  - [ ] フォールバック: API失敗時は元のスコア順をそのまま返却
  - [ ] タイムアウト: 3秒

### テスト
- [ ] CrossEncoderRerankerTest
  - [ ] 正常リランキング（MockWebServer）
  - [ ] スコア置換・ソート検証
  - [ ] API失敗時フォールバック（元のスコア順維持）
  - [ ] 空候補リスト
  - [ ] topK制限

### 設定ファイル
- [ ] application.yaml にリランキング設定追加

## 依存
- SearchResult (com.tis.nablarch.mcp.rag.search)
- WebClient (spring-boot-starter-webflux) ※既存依存

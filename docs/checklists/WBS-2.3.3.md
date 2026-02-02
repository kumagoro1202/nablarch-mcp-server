# WBS 2.3.3 ユニットテスト: Embeddingクライアント 完了基準チェックリスト

> Wave 1で実装済み（足軽7号が143テスト中に含めて実装済み）

## テストファイル

| ファイル | テスト数 | パス |
|---------|---------|------|
| JinaEmbeddingClientTest.java | 5件 | src/test/java/com/tis/nablarch/mcp/embedding/ |
| VoyageEmbeddingClientTest.java | 5件 | src/test/java/com/tis/nablarch/mcp/embedding/ |
| **合計** | **10件** | |

## チェックリスト

### JinaEmbeddingClient（5件）

- [x] 単一テキストのEmbeddingを生成できること
- [x] バッチEmbeddingを生成できること
- [x] リトライ後に成功すること
- [x] 最大リトライ超過で例外が発生すること
- [x] モデル名と次元数を取得できること

### VoyageEmbeddingClient（5件）

- [x] 単一コードのEmbeddingを生成できること
- [x] バッチEmbeddingを生成できること
- [x] リトライ後に成功すること
- [x] 最大リトライ超過で例外が発生すること
- [x] モデル名と次元数を取得できること

## テスト対象の機能カバレッジ

| 機能 | Jina | Voyage | 備考 |
|------|------|--------|------|
| 単一Embedding生成 | [x] | [x] | 基本機能 |
| バッチEmbedding生成 | [x] | [x] | 複数テキスト一括処理 |
| リトライ成功 | [x] | [x] | 一時障害からの回復 |
| リトライ上限超過 | [x] | [x] | 最大リトライ後の例外発生 |
| モデル情報取得 | [x] | [x] | モデル名・次元数 |

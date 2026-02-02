# WBS 2.3.2 ユニットテスト: チャンキングエンジン 完了基準チェックリスト

> Wave 1で実装済み（足軽6号・7号の作業に含まれていた）

## テストファイル

| ファイル | テスト数 | パス |
|---------|---------|------|
| ChunkingServiceTest.java | 19件 | src/test/java/com/tis/nablarch/mcp/rag/chunking/ |

## チェックリスト

### コンテンツタイプ別チャンキング（8件）

- [x] chunk_HTML_短いコンテンツは1チャンク
- [x] chunk_HTML_長いコンテンツは複数チャンク
- [x] chunk_HTML_メタデータ引き継ぎ
- [x] chunk_MARKDOWN_短いコンテンツ
- [x] chunk_JAVADOC_1ドキュメント1チャンク
- [x] chunk_JAVA_短いメソッドは1チャンク
- [x] chunk_XML_1要素1チャンク
- [x] chunk_TEXT_短いテキスト

### 分割ロジック（3件）

- [x] chunk_TEXT_長いテキストは複数チャンク
- [x] chunk_チャンクインデックスが連番
- [x] chunk_短いコンテンツでもMinChars以上なら返す

### 異常系（1件）

- [x] chunk_異常系_documentがnull

### トークン→文字数変換（3件）

- [x] tokensToChars_英語テキスト
- [x] tokensToChars_日本語テキスト
- [x] tokensToChars_混合テキスト

### 日本語比率計算（4件）

- [x] calculateJapaneseRatio_全日本語
- [x] calculateJapaneseRatio_全英語
- [x] calculateJapaneseRatio_空文字
- [x] calculateJapaneseRatio_null

# WBS 2.2.4 — ドキュメントパーサー実装

## チェックリスト

- [x] DocumentParser インターフェース作成
- [x] ParsedDocument DTO作成
- [x] ContentType enum作成
- [x] HtmlDocumentParser 実装 + テスト
- [x] MarkdownDocumentParser 実装 + テスト
- [x] JavaSourceParser 実装 + テスト
- [x] XmlConfigParser 実装 + テスト
- [x] build.gradle.kts に Jsoup 依存追加
- [x] テストデータ配置 (src/test/resources/testdata/)
- [x] ./gradlew test 通過

## テスト結果

- HtmlDocumentParserTest: 10テスト PASS
- MarkdownDocumentParserTest: 10テスト PASS
- JavaSourceParserTest: 11テスト PASS
- XmlConfigParserTest: 12テスト PASS

## 完了日時

2026-02-02

# WBS 2.2.5 — チャンキングエンジン実装

## チェックリスト

- [x] DocumentChunkDto record作成
- [x] ChunkingService 実装（6コンテンツタイプ対応）
- [x] トークンカウント近似実装（日本語/英語比率に応じた動的計算）
- [x] オーバーラップ処理実装（段落/文/改行境界での分割）
- [x] メタデータ引き継ぎ（content_type, chunk_index, total_chunks追加）
- [x] ChunkingServiceTest（20テスト: HTML/Markdown/Javadoc/Java/XML/Text + 共通 + ユーティリティ）
- [x] ./gradlew clean build 通過

## 完了日時

2026-02-02

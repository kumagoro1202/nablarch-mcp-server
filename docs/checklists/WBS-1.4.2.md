# WBS 1.4.2 API仕様書 完了基準チェックリスト

## 基本構成
- [ ] docs/api-specification.md が存在する
- [ ] サーバー情報セクションがある（名前、バージョン、capabilities）
- [ ] MCP Protocol準拠の仕様記述

## Tools仕様
- [ ] search_api: 入力パラメータ定義がある
- [ ] search_api: 出力形式の説明がある
- [ ] search_api: 使用例がある
- [ ] validate_handler_queue: 入力パラメータ定義がある
- [ ] validate_handler_queue: 出力形式の説明がある
- [ ] validate_handler_queue: 使用例がある

## Resources仕様
- [ ] handler/{app_type}: URI形式の説明がある
- [ ] handler/{app_type}: サポートタイプ一覧（6種）がある
- [ ] handler/{app_type}: レスポンス形式の説明がある
- [ ] guide/{topic}: URI形式の説明がある
- [ ] guide/{topic}: サポートトピック一覧（6種）がある
- [ ] guide/{topic}: レスポンス形式の説明がある

## Prompts仕様
- [ ] 全6種のPrompt名と説明がある
- [ ] 各Promptの引数定義（名前、説明、必須）がある
- [ ] 各Promptの期待出力形式の説明がある
- [ ] 使用例がある

## データソース
- [ ] 7種の知識YAMLファイルの一覧がある
- [ ] 各ファイルの内容概要がある

## エラーハンドリング
- [ ] バリデーションエラーの説明がある
- [ ] 不正入力時の挙動説明がある

## 制限事項
- [ ] Phase 1の制限が記載されている
- [ ] Phase 2ロードマップへの言及がある

## 品質
- [ ] Markdown構文が正しい
- [ ] テーブル・コードブロックの書式が正しい

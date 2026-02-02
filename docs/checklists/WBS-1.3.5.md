# WBS 1.3.5 MCP Inspector統合テスト 完了基準チェックリスト

## テスト実行
- [ ] FIFOパイプ経由のSTDIO JSON-RPCテスト方式で実行
- [ ] テスト結果が /tmp/mcp-all-output.jsonl に保存されている

## initialize
- [ ] サーバー情報（名前、バージョン）が正しい
- [ ] capabilities（tools, resources, prompts）が返却される

## Tools テスト
- [ ] tools/list で2ツールが返却される
- [ ] search_api 正常系（キーワード検索で結果あり）
- [ ] search_api 異常系（存在しないキーワードで結果なし）
- [ ] validate_handler_queue（XML検証で検証結果が返却される）

## Resources テスト
- [ ] resources/list で12リソースが返却される
- [ ] resources/read handler/{app_type} でMarkdownが返却される
- [ ] resources/read guide/{topic} でMarkdownが返却される

## Prompts テスト
- [ ] prompts/list で6プロンプトが返却される
- [ ] prompts/get setup-handler-queue が正常動作
- [ ] prompts/get explain-handler が正常動作
- [ ] prompts/get create-action が正常動作
- [ ] prompts/get review-config が正常動作
- [ ] prompts/get migration-guide が正常動作
- [ ] prompts/get best-practices が正常動作

## ドキュメント
- [ ] docs/test-results/mcp-inspector-test.md が存在する
- [ ] テスト環境情報が記載されている
- [ ] テスト一覧表がある
- [ ] 各テストの詳細（リクエスト/レスポンス要約）がある
- [ ] テスト結果サマリがある

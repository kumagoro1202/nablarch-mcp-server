# WBS 1.4.1 セットアップガイド 完了基準チェックリスト

## 基本構成
- [ ] docs/setup-guide.md が存在する
- [ ] 前提条件セクションがある（Java 17+, Gradle, Claude Desktop/Code）
- [ ] インストール手順セクションがある

## インストール手順
- [ ] リポジトリクローン手順がある
- [ ] ビルドコマンド（`./gradlew bootJar`）が記載されている
- [ ] JARファイルの配置先説明がある

## クライアント設定
- [ ] Claude Desktop設定（claude_desktop_config.json）の例がある
- [ ] Claude Code設定（.mcp.json）の例がある
- [ ] STDIO transport設定が記載されている

## 動作確認
- [ ] MCP Inspectorでの確認手順がある
- [ ] 簡単なテストコマンド例がある

## 機能一覧
- [ ] Tools一覧（2種: search_api, validate_handler_queue）が記載されている
- [ ] Resources一覧（12種: handler 6 + guide 6）が記載されている
- [ ] Prompts一覧（6種）が記載されている
- [ ] 各機能の使用例がある

## トラブルシューティング
- [ ] よくあるエラーと対処法がある
- [ ] ログ確認方法がある

## 品質
- [ ] Markdown構文が正しい
- [ ] コマンド例がコードブロックで囲まれている
- [ ] JSON設定例がコードブロックで囲まれている

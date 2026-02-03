# WBS 3.1.3 — generate_test Tool設計 完了基準チェックリスト

**タスクID**: WBS 3.1.3
**成果物**: docs/designs/17_tool-generate-test.md
**担当**: ashigaru3 (subtask_062)
**対応ユースケース**: UC7（テスト自動生成）

## 完了基準

- [ ] Tool定義が明記されている
  - [ ] Tool名: `generate_test`
  - [ ] パッケージ・クラス名が定義されている
  - [ ] Tool説明文（@Tool description）が定義されている
- [ ] 入力スキーマが定義されている（JSON Schema）
  - [ ] target_class（string, 必須 — テスト対象FQCN）
  - [ ] test_type（string, 必須 — unit/request-response/batch/messaging）
  - [ ] format（string, 任意 — junit5/nablarch-excel）
  - [ ] options（object, 任意 — 生成オプション）
- [ ] 出力スキーマが定義されている
  - [ ] テストコード（Java）の構造
  - [ ] Excelテストデータの構造（シート定義）
  - [ ] 適用された規約・パターン一覧
- [ ] テストタイプ別生成ロジックが設計されている
  - [ ] ユニットテスト生成ロジック
  - [ ] リクエスト単体テスト生成ロジック
  - [ ] バッチテスト生成ロジック
  - [ ] メッセージングテスト生成ロジック
- [ ] Nablarchテストフレームワーク対応設計が含まれている
  - [ ] JUnit5 + Nablarch Testing Frameworkの統合方針
  - [ ] テストスーパークラスの選択ロジック
  - [ ] SimpleDbAndHttpFwTestSupport等の使い分け
- [ ] Excelテストデータ生成ロジックが設計されている
  - [ ] シート構造定義（testShots, expectedStatus, setupTable等）
  - [ ] テストデータ生成パターン
  - [ ] 正常系・異常系のデータ生成方針
- [ ] RAG連携フローが記述されている
  - [ ] テストパターン検索フロー
  - [ ] テスト規約検索フロー
  - [ ] RAGフォールバック方針
- [ ] モック・スタブ生成方針が設計されている
- [ ] テストデータ設計方針が記述されている
- [ ] MCP Tool登録パターン（@Tool, @ToolParam）が設計されている
- [ ] エラーハンドリング方針が定義されている
- [ ] 設定パラメータが定義されている
- [ ] 既存設計書（01〜14）のフォーマットに準拠している
- [ ] 全ドキュメントが日本語で記述されている

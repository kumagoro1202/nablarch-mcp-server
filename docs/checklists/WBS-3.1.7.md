# WBS 3.1.7 — optimize_handler_queue Tool設計 完了基準チェックリスト

**タスクID**: WBS 3.1.7
**成果物**: docs/designs/21_tool-optimize-handler-queue.md
**担当**: ashigaru5 (subtask_064)

## 完了基準

- [ ] Tool定義（名前、パッケージ、クラス名、説明）が記述されている
- [ ] 入力スキーマが定義されている
  - [ ] current_xml(string, 必須), app_type(string?), concern(string?)
- [ ] 出力スキーマが定義されている
  - [ ] 最適化提案リスト（id, concern, severity, type, handler, reason, suggested_fix）
  - [ ] 最適化後XML
- [ ] XML解析ロジック（HandlerQueueXmlParser共有）が設計されている
- [ ] app_type自動推定ロジックが設計されている
- [ ] 3観点の最適化ルールが設計されている
  - [ ] 正確性観点（COR-001～004）
  - [ ] セキュリティ観点（SEC-001～005）
  - [ ] パフォーマンス観点（PERF-001～005）
- [ ] RAG連携フロー（ベストプラクティス・アンチパターン検索）が設計されている
- [ ] Before/After XML生成ロジックが設計されている
- [ ] design_handler_queue Toolとの役割分担が明記されている
- [ ] validate_handler_queue Toolとの役割分担が明記されている
- [ ] MCP Tool登録パターン（@Tool annotation）が設計されている
- [ ] エラーハンドリング方針が定義されている
- [ ] 既存Tool（validate_config, design_handler_queue）との整合性が確認されている
- [ ] 設定パラメータが定義されている

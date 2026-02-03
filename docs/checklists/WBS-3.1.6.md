# WBS 3.1.6 — recommend_pattern Tool設計 完了基準チェックリスト

**タスクID**: WBS 3.1.6
**成果物**: docs/designs/20_tool-recommend-pattern.md
**担当**: ashigaru5 (subtask_064)

## 完了基準

- [ ] Tool定義（名前、パッケージ、クラス名、説明）が記述されている
- [ ] 入力スキーマが定義されている
  - [ ] requirement(string, 必須), app_type(string?), constraints(array[string]?), max_results(int?)
- [ ] 出力スキーマが定義されている
  - [ ] パターンランキング（score, name, rationale, code_example, trade_offs, resource_uri）
- [ ] パターンマッチングロジックが設計されている
  - [ ] 要件分析フェーズ
  - [ ] 候補収集フェーズ
  - [ ] スコアリングフェーズ
  - [ ] ランキング生成フェーズ
- [ ] RAG検索+スコアリング設計が記述されている
  - [ ] Phase 1: キーワード一致、カテゴリ一致、app_type適合、制約一致
  - [ ] Phase 2+: セマンティック類似度の追加
- [ ] design-patterns.yaml（11パターン）との連携が設計されている
- [ ] Resource URI（nablarch://pattern/{name}）との関係が明記されている
- [ ] パターン推薦の根拠提示方針が定義されている
- [ ] 複数パターン候補のランキング方式が設計されている
- [ ] MCP Tool登録パターン（@Tool annotation）が設計されている
- [ ] エラーハンドリング方針が定義されている
- [ ] 既存Tool（search_api, validate_config, design_handler_queue）との整合性が確認されている

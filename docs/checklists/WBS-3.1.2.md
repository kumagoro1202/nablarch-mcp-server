# WBS 3.1.2 — generate_code Tool設計 完了基準チェックリスト

**タスクID**: WBS 3.1.2
**成果物**: docs/designs/16_tool-generate-code.md
**担当**: ashigaru2 (subtask_061)

## 完了基準

- [ ] Tool定義（名前、説明、パッケージ、クラス名）が記述されている
- [ ] 入力スキーマが定義されている
  - [ ] type(string, enum: action/form/sql/entity/handler/interceptor)
  - [ ] name(string, 必須)
  - [ ] app_type(string?, enum: web/rest/batch/messaging)
  - [ ] specifications(object?, タイプ固有パラメータ)
- [ ] 出力スキーマが定義されている
  - [ ] files[{path, content, language}]
  - [ ] conventions_applied[string]
  - [ ] dependencies[string]
- [ ] コード生成テンプレート設計（6種）が記述されている
  - [ ] action テンプレート（Web/REST/Batch各パターン）
  - [ ] form テンプレート（BeanValidation付き）
  - [ ] sql テンプレート（SQL定義ファイル）
  - [ ] entity テンプレート（@Entity, @Table, @Column）
  - [ ] handler テンプレート（Handler<I,O>実装）
  - [ ] interceptor テンプレート（Interceptor実装）
- [ ] RAG連携フローが設計されている
  - [ ] コーディング規約検索
  - [ ] APIパターン検索
  - [ ] Phase 1知識ベースからのフォールバック
- [ ] テンプレートエンジン選定・設計が記述されている
- [ ] アプリタイプ別の生成ロジックが設計されている
  - [ ] Web（JSP + アクション）
  - [ ] REST（JAX-RS + JSON）
  - [ ] Batch（BatchAction + DataReader）
  - [ ] Messaging（MessagingAction）
- [ ] 生成コードの品質保証方針が記述されている
- [ ] エラーハンドリング方針が記述されている
- [ ] MCP Tool登録パターン（@Tool annotation）のクラス設計がある
- [ ] architecture.md / use-cases.md（UC3, UC12）との整合性が確認されている
- [ ] 既存設計書（04, 05, 13）のフォーマットに準拠している
- [ ] 全文が日本語で記述されている

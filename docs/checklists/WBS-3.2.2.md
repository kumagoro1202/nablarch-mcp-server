# WBS 3.2.2 generate_code Tool実装 完了基準チェックリスト

## 概要
- **WBS番号**: 3.2.2
- **タスク**: generate_code MCP Tool実装
- **担当**: ashigaru2 (subtask_069)
- **ステータス**: 実装完了

## 完了基準

### 1. 実装ファイル
- [x] `CodeGenerationTool.java` — MCP Tool本体（@Tool/@ToolParam アノテーション）
- [x] `CodeGenerator.java` — コード生成エンジンインターフェース
- [x] `DefaultCodeGenerator.java` — コード生成エンジン実装
- [x] `GenerationResult.java` — 生成結果DTO（record）
- [x] `NamingConventionHelper.java` — 命名規約ユーティリティ

### 2. MCP Tool登録
- [x] `McpServerConfig.java` に `CodeGenerationTool` を登録

### 3. 機能要件
- [x] 6種のコード生成対応: action / form / sql / entity / handler / interceptor
- [x] 4種のアプリタイプ対応: web / rest / batch / messaging
- [x] 知識ベース連携（NablarchKnowledgeBase）による規約検索
- [x] 入力バリデーション（type, name, appType）
- [x] specifications JSONパース
- [x] Markdown形式の出力フォーマット

### 4. コード品質
- [x] Javadocコメント（日本語）
- [x] エラーハンドリング（フォールバック規約適用）
- [x] ロギング（slf4j）
- [x] 既存Toolパターン準拠

### 5. ビルド確認
- [ ] `./mvnw compile` 成功 ※JDK未インストールのため未確認（CI/CDで検証要）

### 6. PR作成
- [ ] feature/3.2.2-impl-generate-code ブランチにコミット
- [ ] PRタイトル: "feat: WBS 3.2.2 generate_code Tool実装"
- [ ] PR説明は日本語

## 生成されるコードテンプレート

| タイプ | app_type | 出力 |
|--------|----------|------|
| action | web | doメソッド + @InjectForm/@OnError |
| action | rest | JAX-RSアノテーション + EntityResponse |
| action | batch | BatchAction<Entity> + DataReader |
| action | messaging | MessagingAction |
| form | 全タイプ | BeanValidation付きForm |
| sql | 全タイプ | Nablarch SQL定義（FIND_ALL, FIND_BY_ID） |
| entity | 全タイプ | @Entity/@Table/@Column/@Id/@Version |
| handler | 全タイプ | Handler<I,O>実装 |
| interceptor | 全タイプ | アノテーション + Interceptor実装（2ファイル） |

## 備考
- 設計書: docs/designs/16_tool-generate-code.md（PR #29）
- テンプレートエンジンはJava Text Blocksを使用（JMustache依存なし）
- ユニットテストは別タスク（WBS 3.3.2）で実施

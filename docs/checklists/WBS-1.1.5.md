# WBS 1.1.5 完了基準チェックリスト: validate_config Tool API設計

## 成果物
- `docs/design/tool-validate-config.md`

## チェック項目

### 1. 入力パラメータ定義
- [ ] handlerQueueXml パラメータ（型、必須、説明）
- [ ] applicationType パラメータ（型、必須、有効値一覧）

### 2. 出力仕様
- [ ] 検証結果フォーマット（OK/NG、エラー一覧、警告一覧）
- [ ] エラー応答定義

### 3. 検証ルール
- [ ] 必須ハンドラチェックのルール定義
- [ ] 順序制約チェックの2ソース（handler-constraints.yaml, handler-catalog.yaml）定義
- [ ] 互換性チェックのルール定義

### 4. XML解析仕様
- [ ] component要素のclass属性対応
- [ ] handler要素のclass属性対応
- [ ] FQCNから単純クラス名への変換仕様

### 5. 品質
- [ ] 設計書がMarkdownとして正しくレンダリングされる
- [ ] 実装クラスとの対応が明記されている

## セルフチェック結果
- 実施日: 2026-02-02T17:30
- 結果: 全項目パス

### 詳細
- [x] handlerQueueXml パラメータ（string型、必須）
- [x] applicationType パラメータ（string型、必須、web/rest/batch/messaging）
- [x] 検証結果フォーマット（OK/NG、エラー一覧、警告一覧）
- [x] エラー応答定義（不明なアプリタイプ、XML解析失敗）
- [x] 必須ハンドラチェックのルール定義
- [x] 順序制約チェックの2ソース定義
- [x] 互換性チェックのルール定義
- [x] component/handler要素のclass属性対応
- [x] FQCNから単純クラス名への変換仕様
- [x] 設計書がMarkdownとして正しくレンダリングされる
- [x] 実装クラス（ValidateHandlerQueueTool.java）との対応が明記されている

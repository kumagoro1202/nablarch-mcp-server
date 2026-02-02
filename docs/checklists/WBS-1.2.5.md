# WBS 1.2.5 完了基準チェックリスト: ValidateHandlerQueueTool実装

## 成果物
- `src/main/java/com/tis/nablarch/mcp/tools/ValidateHandlerQueueTool.java`

## チェック項目

### 1. @Toolアノテーション
- [ ] description が英語で記述されている（MCP仕様準拠）
- [ ] @ToolParam で各パラメータのdescriptionが定義されている

### 2. 入力バリデーション
- [ ] null/空白XMLでエラーメッセージを返す
- [ ] null/空白applicationTypeでエラーメッセージを返す

### 3. XML解析
- [ ] class属性のFQCNから単純クラス名を抽出
- [ ] 複数ハンドラを順序通りに抽出
- [ ] class属性なしXMLでエラーメッセージを返す

### 4. 検証委譲
- [ ] NablarchKnowledgeBase.validateHandlerQueue() への委譲が正しい

### 5. Javadoc
- [ ] クラスJavadocが日本語（cmd_027）
- [ ] メソッドJavadocが日本語
- [ ] パラメータ・戻り値のJavadocが日本語

### 6. 品質
- [ ] コンパイルが通る
- [ ] tool-validate-config.md の設計に準拠

## セルフチェック結果
- 実施日: 2026-02-02T17:30
- 結果: 全項目パス

### 詳細
- [x] @Tool description 英語
- [x] @ToolParam で handlerQueueXml, applicationType のdescription定義
- [x] null/空白XMLで「ハンドラキューXMLを指定してください。」を返す
- [x] null/空白applicationTypeで「アプリケーションタイプを指定してください」を返す
- [x] CLASS_ATTR_PATTERN正規表現でFQCNからクラス名を抽出
- [x] 複数ハンドラの順序保持
- [x] class属性なしXMLで抽出失敗メッセージ
- [x] NablarchKnowledgeBase.validateHandlerQueue(appType, handlerNames) への委譲
- [x] クラス・メソッド・パラメータのJavadocが日本語
- [x] コンパイル通過確認済
- [x] tool-validate-config.md の設計に準拠

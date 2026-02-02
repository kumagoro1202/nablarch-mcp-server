# WBS 1.3.2 完了基準チェックリスト: ValidateHandlerQueueToolテスト

## 成果物
- `src/test/java/com/tis/nablarch/mcp/tools/ValidateHandlerQueueToolTest.java`

## チェック項目

### 1. 入力バリデーションテスト
- [ ] null XMLでエラーメッセージ
- [ ] 空白XMLでエラーメッセージ
- [ ] null applicationTypeでエラーメッセージ
- [ ] 空 applicationTypeでエラーメッセージ
- [ ] class属性なしXMLで抽出失敗メッセージ

### 2. XML解析テスト
- [ ] FQCNから単純クラス名抽出
- [ ] ドットなし単純名のそのまま返却
- [ ] 複数ハンドラの順序保持
- [ ] 空XMLで空リスト

### 3. 検証委譲テスト
- [ ] 正常XMLでKnowledgeBase.validateHandlerQueue()に正しく委譲
- [ ] 検証エラー結果の正しい返却

### 4. テスト手法
- [ ] Mockito @Mock による NablarchKnowledgeBase のモック
- [ ] verifyNoInteractions でバリデーション時にKBが呼ばれないことを確認

### 5. 品質
- [ ] 全テストがパスする
- [ ] Javadocが日本語（cmd_027）

## セルフチェック結果
- 実施日: 2026-02-02T17:30
- 結果: 全項目パス

### 詳細
- [x] null XMLテスト
- [x] 空白XMLテスト
- [x] null applicationTypeテスト
- [x] 空 applicationTypeテスト
- [x] class属性なしXMLテスト
- [x] FQCN→単純クラス名抽出テスト
- [x] ドットなし単純名テスト
- [x] 複数ハンドラ順序保持テスト
- [x] 空XML→空リストテスト
- [x] 正常XML検証委譲テスト
- [x] 検証エラー結果返却テスト
- [x] Mockito @Mock + @ExtendWith(MockitoExtension.class)
- [x] verifyNoInteractions 確認
- [x] 全11テスト パス
- [x] Javadoc日本語

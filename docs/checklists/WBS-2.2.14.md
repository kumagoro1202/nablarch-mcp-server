# WBS 2.2.14 — クエリ解析・拡張エンジン 完了基準チェックリスト

**タスクID**: WBS 2.2.14
**成果物**: QueryAnalyzer.java + AnalyzedQuery + テスト
**担当**: ashigaru8 (subtask_063)

## 完了基準

### 言語検出
- [ ] 日本語クエリの検出（QueryLanguage.JAPANESE）
- [ ] 英語クエリの検出（QueryLanguage.ENGLISH）
- [ ] 日英混在クエリの検出（QueryLanguage.MIXED）
- [ ] Unicode範囲によるJapanese比率計算

### エンティティ抽出
- [ ] FQCNパターン（nablarch.xxx.yyy.ZzzClass）
- [ ] ハンドラ名（XxxHandler）
- [ ] モジュール名（nablarch-fw-web等）
- [ ] 設定ファイルパターン（*.xml, component-configuration等）
- [ ] 抽出結果→SearchFiltersヒントへの変換

### クエリリフォーミュレーション
- [ ] Nablarch固有同義語マップ（双方向）
- [ ] 同義語展開後のexpandedQuery生成
- [ ] 将来のYAML外部化を見据えた設計

### テスト
- [ ] QueryAnalyzerTest: 言語検出、FQCN抽出、ハンドラ名抽出、モジュール名抽出、同義語展開、複合クエリ、空/null
- [ ] AnalyzedQueryTest: recordフィールド検証
- [ ] QueryLanguageTest: enum値検証
- [ ] ./gradlew test --tests "com.tis.nablarch.mcp.rag.query.*" 通過

### 品質
- [ ] Javadocが全て日本語
- [ ] mainに直接コミットしていない

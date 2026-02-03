# WBS 3.3.8 追加Resources（6種）ユニットテスト - 完了チェックリスト

## 概要
- **WBS ID**: 3.3.8
- **タスクID**: subtask_077
- **前提**: WBS 3.2.8-3.2.13 実装完了（PR #38）
- **ブランチ**: feature/3.3.8-test-resources-phase3

## テスト対象

| # | クラス名 | URIパターン | 出力形式 |
|---|---------|------------|---------|
| 1 | ApiResourceProvider | api/{module}/{class} | JSON |
| 2 | PatternResourceProvider | pattern/{name} | Markdown |
| 3 | ExampleResourceProvider | example/{type} | JSON |
| 4 | ConfigResourceProvider | config/{name} | XML |
| 5 | AntipatternResourceProvider | antipattern/{name} | Markdown |
| 6 | VersionResourceProvider | version | JSON |

## 完了基準チェックリスト

### テストクラス作成
- [x] ApiResourceProviderTest.java 作成
- [x] PatternResourceProviderTest.java 作成
- [x] ExampleResourceProviderTest.java 作成
- [x] ConfigResourceProviderTest.java 作成
- [x] AntipatternResourceProviderTest.java 作成
- [x] VersionResourceProviderTest.java 作成

### テストケース網羅性

#### ApiResourceProviderTest
- [x] getModuleList 正常系テスト（JSON形式、必須フィールド）
- [x] getClassList 正常系テスト（有効なモジュールキー）
- [x] getClassList 異常系テスト（存在しないモジュール→エラー）
- [x] getClassDetail 異常系テスト（存在しないクラス→エラー）
- [x] getValidModuleKeys テスト（非空、プレフィックスなし）

#### PatternResourceProviderTest
- [x] getPatternList 正常系テスト（Markdown形式、テーブル形式）
- [x] getPatternDetail 正常系テスト（有効なパターン名）
- [x] getPatternDetail 異常系テスト（存在しないパターン→エラー）
- [x] ソース帰属情報の検証

#### ExampleResourceProviderTest
- [x] getExampleList 正常系テスト（JSON形式、4タイプ含む）
- [x] getExampleDetail 正常系テスト（各タイプの詳細）
- [x] getExampleDetail 異常系テスト（存在しないタイプ→エラー）
- [x] getValidExampleTypes テスト（4種類確認）

#### ConfigResourceProviderTest
- [x] getTemplateList 正常系テスト（Markdown形式、テーブル形式）
- [x] getTemplate 正常系テスト（XML形式、ヘッダコメント）
- [x] getTemplate 異常系テスト（存在しないテンプレート→エラー）
- [x] XML設定内容の検証（web.xml要素）

#### AntipatternResourceProviderTest
- [x] getAntipatternList 正常系テスト（Markdown形式、テーブル形式）
- [x] getAntipatternDetail 正常系テスト（全7種のアンチパターン）
- [x] getAntipatternDetail 異常系テスト（存在しないアンチパターン→エラー）
- [x] コード例（悪い例・良い例）の検証
- [x] getValidAntipatternNames テスト（7種類確認）

#### VersionResourceProviderTest
- [x] getVersionInfo 正常系テスト（JSON形式）
- [x] フレームワーク名・最新バージョンの検証
- [x] サポートバージョン一覧の検証
- [x] プラットフォーム情報の検証（サーバ、DB）
- [x] BOM情報の検証
- [x] モジュール一覧の検証
- [x] リンク情報の検証

### テスト基盤
- [x] JUnit 5 使用
- [x] @BeforeEach による初期化
- [x] @ParameterizedTest による複数ケーステスト
- [x] 既存テストパターン（GuideResourceProviderTest）に準拠

### ドキュメント
- [x] 全Javadocを日本語で記述
- [x] 本チェックリスト作成

## 成果物一覧

```
src/test/java/com/tis/nablarch/mcp/resources/
├── ApiResourceProviderTest.java      （新規）
├── PatternResourceProviderTest.java  （新規）
├── ExampleResourceProviderTest.java  （新規）
├── ConfigResourceProviderTest.java   （新規）
├── AntipatternResourceProviderTest.java （新規）
├── VersionResourceProviderTest.java  （新規）
├── GuideResourceProviderTest.java    （既存）
├── HandlerResourceProviderTest.java  （既存）
├── ApiSpecResourceTest.java          （既存）
└── HandlerResourceTest.java          （既存）

docs/checklists/
└── WBS-3.3.8.md                      （本ファイル）
```

## 備考
- テスト実行はCI/CDで検証（JDK環境依存のため）
- Phase 3 Resourcesの6クラスすべてのユニットテストを網羅

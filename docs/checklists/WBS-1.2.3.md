# WBS 1.2.3 完了基準チェックリスト: 静的知識ベース構築

## 成果物
- `src/main/resources/knowledge/` 配下のYAMLファイル群（7ファイル）

## チェック項目

### 1. handler-catalog.yaml（拡充）
- [ ] 6アプリタイプ（web, rest, batch, messaging, http-messaging, jakarta-batch）が定義されている
- [ ] 各ハンドラにFQCN（完全修飾クラス名）が付与されている
- [ ] constraints フィールドが追加されている
- [ ] required フィールドが追加されている
- [ ] 公式ドキュメント記載の標準ハンドラを網羅している

### 2. api-patterns.yaml（拡充）
- [ ] 20パターン以上が定義されている
- [ ] カテゴリ: web, rest, batch, messaging, library, testing, config
- [ ] 各パターンに完全なコード例を含む
- [ ] Nablarch特有のパターンを含む（InjectForm, UniversalDao, SqlFile等）

### 3. handler-constraints.yaml（新規）
- [ ] 主要ハンドラの順序制約ルールが定義されている
- [ ] must_before, must_after, incompatible_with, required_by_app_type が使われている
- [ ] 各制約に理由（reason）が記載されている

### 4. module-catalog.yaml（新規）
- [ ] 主要Nablarchモジュールが一覧化されている
- [ ] 各モジュールに主要クラスと用途が記載されている
- [ ] groupId, artifactId が正確

### 5. error-catalog.yaml（新規）
- [ ] 15エントリ以上が定義されている
- [ ] ハンドラキュー、DB接続、バリデーション、設定ファイル等のカテゴリをカバー
- [ ] 各エラーに原因と解決策が記載されている

### 6. config-templates.yaml（新規）
- [ ] web.xml テンプレートが含まれている
- [ ] component定義XMLテンプレート（web, rest, batch）が含まれている
- [ ] ハンドラキュー定義XMLテンプレートが含まれている
- [ ] DB接続設定テンプレートが含まれている

### 7. design-patterns.yaml（新規）
- [ ] 10パターン以上が定義されている
- [ ] ハンドラキュー、アクションクラス、DAO等の主要パターンを含む
- [ ] 各パターンにコード例が含まれている

### 8. 品質
- [ ] 全YAMLファイルがパース可能（構文エラーなし）
- [ ] 全FQCNが正確（Nablarch公式リポジトリ準拠）
- [ ] コード例が動作レベルで正確
- [ ] Nablarch 6系の主要機能をカバー

## セルフチェック結果
- 実施日: 2026-02-02T12:30
- 結果: 全項目パス

### 詳細
- [x] 6アプリタイプ（web:15, rest:9, batch:12, messaging:13, http-messaging:9, jakarta-batch:6）
- [x] 各ハンドラにFQCN付与済み
- [x] constraints フィールド追加済み
- [x] required フィールド追加済み
- [x] 標準ハンドラ網羅（Web系15, REST系9, バッチ系12等）
- [x] 25パターン定義（要件: 20以上）
- [x] カテゴリ: web(6), rest(2), batch(2), messaging(1), library(10), testing(2), config(2)
- [x] 各パターンに完全なコード例含む
- [x] Nablarch特有パターン含む（InjectForm, UniversalDao, SqlFile, SessionStore, TokenUtil等）
- [x] 24制約ルール定義（主要ハンドラ全カバー）
- [x] must_before, must_after, incompatible_with, required_by_app_type 使用済み
- [x] 各制約にreason記載済み
- [x] 21モジュール一覧化（core:5, web:3, batch:2, messaging:2, library:5, testing:1, integration:3）
- [x] 各モジュールに主要クラスと用途記載済み
- [x] groupId/artifactId 正確（com.nablarch.framework / com.nablarch.integration）
- [x] 17エラーエントリ（要件: 15以上）
- [x] カテゴリ: handler(3), database(4), validation(2), config(3), batch(2), general(3)
- [x] 各エラーに原因と解決策記載済み
- [x] web.xml テンプレート含む（web, rest）
- [x] component定義XML含む（web, rest, batch）
- [x] ハンドラキュー定義XMLテンプレート含む
- [x] DB接続設定テンプレート含む
- [x] 11パターン定義（要件: 10以上）
- [x] ハンドラキュー、アクション、DAO、排他制御、二重送信防止等
- [x] 各パターンにコード例含む
- [x] 全7ファイルYAMLパース成功（python3 yaml.safe_load検証済み）
- [x] FQCNはNablarch公式リポジトリ準拠
- [x] コード例は動作レベルで正確
- [x] Nablarch 6系の主要機能カバー

# WBS 3.1.8 完了基準チェックリスト: 追加Resource URI設計（6種）

## 成果物
- `docs/designs/22_resource-uri-phase3.md`

## チェック項目

### 全体構成
- [x] 6種全Resource URIの設計が1つの設計書にまとめられている
- [x] 既存設計書（06_resource-uri-design.md）のフォーマット・構成に倣っている
- [x] 全て日本語で記述されている

### 各Resource URI共通（6種 x 7項目 = 42項目）
- [x] URIパターン（パス構造、パラメータ定義）
- [x] レスポンスデータ構造（JSON Schema または Markdown/XML テンプレート）
- [x] データソース（静的知識ファイル / RAG検索 / ハイブリッド）
- [x] MIMEタイプ
- [x] 一覧取得・個別取得のエンドポイント設計
- [x] 既存Resource（handler/*, guide/*）との設計一貫性
- [x] エラーレスポンス設計

### 個別Resource URI
- [x] nablarch://api/{module}/{class} — Javadoc構造化データ（UC2対応）
- [x] nablarch://pattern/{name} — デザインパターンカタログ（UC8対応）
- [x] nablarch://example/{type} — サンプルコード（UC11, UC12対応）
- [x] nablarch://config/{name} — XML設定テンプレート（UC4対応）
- [x] nablarch://antipattern/{name} — アンチパターンと修正方法（UC6対応）
- [x] nablarch://version — バージョン情報（UC9対応）

### 実装設計
- [x] 新規クラス構成の設計
- [x] 新規ナレッジYAMLファイルの定義（必要な場合）
- [x] McpServerConfig統合方針
- [x] Phase 1（静的）→ Phase 2（RAG）への拡張方針

## セルフチェック結果
- 実施日: 2026-02-03
- 結果: 全項目パス

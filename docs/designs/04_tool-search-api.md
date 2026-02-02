# WBS 1.1.4: search_api Tool API設計

> **作成日**: 2026-02-02
> **作成者**: ashigaru2 (subtask_046)

## 概要

Nablarch知識ベースからAPIパターン・モジュール・ハンドラ・設計パターン・エラー情報を横断検索するMCPツール。

## API仕様

### 入力パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|------|------|------|
| keyword | string | ✅ | 検索キーワード（クラス名、メソッド名、概念） |
| category | string | - | カテゴリフィルタ: handler, library, web, batch, rest, messaging, error, module |

### 出力

フォーマット済みテキスト（Markdown形式）。検索結果を以下の形式で返す:

```
検索結果: "keyword" (カテゴリ: category)
件数: N件

[APIパターン] action-class (web) — Webアプリケーションの標準アクションクラスパターン | FQCN: nablarch.fw.web.HttpResponse
[モジュール] nablarch-fw-web (web) — Nablarch Webフレームワーク
[ハンドラ] HttpResponseHandler — ハンドラの処理結果からHTTPレスポンスを構築する | FQCN: nablarch.fw.web.handler.HttpResponseHandler | 必須: はい
```

### エラー応答

| 条件 | 応答 |
|------|------|
| keyword が空またはnull | "検索キーワードを指定してください" |
| 結果0件 | "検索結果なし: {keyword}" |

## 実装クラス

- **SearchApiTool.java** (`@Tool` アノテーション付き)
- 内部で `NablarchKnowledgeBase.search(keyword, category)` を呼び出し
- 結果リストをMarkdown形式に整形して返却

## 検索対象

| カテゴリ | 検索フィールド |
|---------|--------------|
| APIパターン | name, description, fqcn, category |
| モジュール | name, description, artifactId, keyClasses.fqcn |
| ハンドラ | name, description, fqcn |
| 設計パターン | name, description, problem, category |
| エラー | id, errorMessage, cause, category |

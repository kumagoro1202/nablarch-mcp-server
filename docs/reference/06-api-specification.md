# Nablarch MCP Server API仕様書

> **対象読者**: 開発者（API仕様を参照したい人）
> **前提知識**: MCP, JSON-RPCの基本知識
> **概要**: MCP Tools/Resources/PromptsのAPI仕様、入出力フォーマット

---

## サーバー情報

| 項目 | 値 |
|------|-----|
| サーバー名 | `nablarch-mcp-server` |
| バージョン | `0.1.0` |
| プロトコル | MCP (Model Context Protocol) over JSON-RPC 2.0 |
| トランスポート | STDIO（標準入出力） |
| サーバータイプ | SYNC |

### Capabilities

```json
{
  "tools": { "listChanged": false },
  "resources": { "listChanged": false },
  "prompts": { "listChanged": false }
}
```

Phase 1では全capabilities が静的（`listChanged: false`）です。

---

## Tools仕様

### search_api

Nablarchの知識ベースをキーワード検索し、APIパターン・モジュール・ハンドラ・設計パターン・エラー情報を返します。

#### Tool定義

```json
{
  "name": "search_api",
  "description": "Search the Nablarch API documentation for classes, methods, and patterns. Use this when you need to find Nablarch APIs for code generation.",
  "inputSchema": {
    "type": "object",
    "properties": {
      "keyword": {
        "type": "string",
        "description": "Search keyword (class name, method name, or concept)"
      },
      "category": {
        "type": "string",
        "description": "Optional category filter: handler, library, web, batch, rest, messaging"
      }
    },
    "required": ["keyword"]
  }
}
```

#### 入力パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| `keyword` | string | はい | 検索キーワード。クラス名、メソッド名、概念等 |
| `category` | string | いいえ | カテゴリフィルタ。`handler`, `library`, `web`, `batch`, `rest`, `messaging`, `error`, `module` |

#### 検索対象

| カテゴリ | 検索フィールド | データソース |
|---------|--------------|------------|
| APIパターン | name, description, fqcn, category | api-patterns.yaml |
| モジュール | name, description, artifactId, keyClasses | module-catalog.yaml |
| ハンドラ | name, description, fqcn | handler-catalog.yaml |
| 設計パターン | name, description, problem, category | design-patterns.yaml |
| エラー | id, errorMessage, cause, category | error-catalog.yaml |

#### 出力形式

テキスト形式。各結果は以下のフォーマットで表記されます。

```
検索結果: "UniversalDao"
件数: 3件

[APIパターン] universal-dao (library) — UniversalDaoを使ったCRUD操作 | FQCN: nablarch.common.dao.UniversalDao
[モジュール] nablarch-common-dao (library) — ユニバーサルDAO
[設計パターン] dao-pattern (data-access) — データアクセスオブジェクトパターン
```

結果タイプの接頭辞:
- `[APIパターン]`: API使用パターン
- `[モジュール]`: Nablarchモジュール
- `[ハンドラ]`: ハンドラコンポーネント
- `[設計パターン]`: 設計パターン
- `[エラー]`: エラー情報

#### エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| keyword が空またはnull | `検索キーワードを指定してください。` |
| マッチする結果なし | `検索結果なし: {keyword}` (カテゴリ指定時はカテゴリも表示) |

---

### validate_handler_queue

NablarchハンドラキューのXML設定を検証し、順序制約・必須ハンドラ・互換性をチェックします。

#### Tool定義

```json
{
  "name": "validate_handler_queue",
  "description": "Validate a Nablarch handler queue XML configuration. Checks handler ordering constraints, required handlers, and best practices. Use this to verify handler queue configurations before deployment.",
  "inputSchema": {
    "type": "object",
    "properties": {
      "handlerQueueXml": {
        "type": "string",
        "description": "Handler queue XML configuration content"
      },
      "applicationType": {
        "type": "string",
        "description": "Application type: web, rest, batch, or messaging"
      }
    },
    "required": ["handlerQueueXml", "applicationType"]
  }
}
```

#### 入力パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| `handlerQueueXml` | string | はい | ハンドラキューXML設定内容。`class="FQCN"` 属性を含むXML |
| `applicationType` | string | はい | アプリケーションタイプ: `web`, `rest`, `batch`, `messaging` |

#### 検証内容

1. **必須ハンドラチェック**: アプリタイプに定義された必須ハンドラが存在するか
2. **順序制約チェック**: `handler-constraints.yaml` の `must_before` / `must_after` 制約を検証
3. **インライン制約チェック**: `handler-catalog.yaml` のハンドラ定義内の制約を検証
4. **互換性チェック**: `incompatible_with` 指定のあるハンドラの同時使用を警告

#### 出力形式

```
## 検証結果: OK
アプリタイプ: web
ハンドラ数: 15

ハンドラキューの順序は正しいです。
```

検証エラーがある場合:

```
## 検証結果: NG
アプリタイプ: web
ハンドラ数: 3

### エラー (2件)
- 必須ハンドラが不足: GlobalErrorHandler (グローバルエラーハンドラ)
- 順序違反: ThreadContextHandler (位置2) は HttpCharacterEncodingHandler (位置3) より後に配置すべき

### 警告 (1件)
- 互換性警告: HandlerA と HandlerB は同時使用非推奨
```

#### エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| handlerQueueXml が空/null | `ハンドラキューXMLを指定してください。` |
| applicationType が空/null | `アプリケーションタイプを指定してください（web, rest, batch, messaging）。` |
| XML内にclass属性なし | `XMLからハンドラクラスを抽出できませんでした。...` |
| 不明なappType | `エラー: 不明なアプリケーションタイプ: {appType}` |

---

## Resources仕様

全リソースはMCP標準の `resources/read` メソッドでアクセスします。レスポンスはMarkdown形式（`text/markdown`）です。

### handler/{app_type}

アプリケーションタイプ別のハンドラキュー仕様を提供します。

#### URI一覧

| URI | 名前 | 説明 |
|-----|------|------|
| `nablarch://handler/web` | Nablarch Web Handler Catalog | Webアプリケーションのハンドラ仕様・順序制約 |
| `nablarch://handler/rest` | Nablarch REST Handler Catalog | RESTアプリケーションのハンドラ仕様・順序制約 |
| `nablarch://handler/batch` | Nablarch Batch Handler Catalog | バッチアプリケーションのハンドラ仕様・順序制約 |
| `nablarch://handler/messaging` | Nablarch Messaging Handler Catalog | メッセージングアプリケーションのハンドラ仕様・順序制約 |
| `nablarch://handler/http-messaging` | Nablarch HTTP Messaging Handler Catalog | HTTPメッセージングのハンドラ仕様・順序制約 |
| `nablarch://handler/jakarta-batch` | Nablarch Jakarta Batch Handler Catalog | Jakarta Batchのハンドラ仕様・順序制約 |

#### レスポンス構造

```markdown
# Nablarch {AppType} Application Handler Queue

{description}

## Handler Queue (in order)

### 1. {HandlerName} [Required]
- **FQCN**: `{fqcn}`
- **Thread**: {thread}
- **Description**: {description}
- **Constraints**:
  - Must be before: {handler1}, {handler2}
  - Must be after: {handler3}

...

## Ordering Constraints Summary

| Handler | Rule | Details |
|---------|------|---------|
| {handler} | {rule} | {reason} |
```

#### データソース

- `handler-catalog.yaml`: 6アプリタイプ、計64ハンドラ定義（FQCN、スレッドモデル、必須区分、制約）
- `handler-constraints.yaml`: 24件の順序制約ルール

#### ハンドラ数（アプリタイプ別）

| アプリタイプ | ハンドラ数 |
|-------------|----------|
| web | 15 |
| rest | 9 |
| batch | 12 |
| messaging | 13 |
| http-messaging | 9 |
| jakarta-batch | 6 |

#### エラーレスポンス

不正なアプリタイプを指定した場合:

```markdown
# Unknown Application Type

Unknown application type: {appType}

Valid types: web, rest, batch, messaging, http-messaging, jakarta-batch
```

---

### guide/{topic}

トピック別の開発ガイドを提供します。

#### URI一覧

| URI | 名前 | 説明 |
|-----|------|------|
| `nablarch://guide/setup` | Nablarch Setup Guide | プロジェクトセットアップ・設定ガイド |
| `nablarch://guide/testing` | Nablarch Testing Guide | テストパターン・ベストプラクティスガイド |
| `nablarch://guide/validation` | Nablarch Validation Guide | バリデーションパターン・設計ガイド |
| `nablarch://guide/database` | Nablarch Database Guide | データベースアクセスパターン・設定ガイド |
| `nablarch://guide/handler-queue` | Nablarch Handler Queue Guide | ハンドラキューアーキテクチャ・構成ガイド |
| `nablarch://guide/error-handling` | Nablarch Error Handling Guide | エラーハンドリング・トラブルシューティングガイド |

#### トピック別データソースマッピング

| トピック | データソース | 含まれる情報 |
|---------|------------|------------|
| setup | config-templates.yaml | web-xml, web-component, db-connection テンプレート |
| testing | api-patterns.yaml | request-unit-test, excel-test-data パターン |
| validation | api-patterns.yaml, design-patterns.yaml | form-validation, inject-form-on-error, form-validation-pattern |
| database | api-patterns.yaml, config-templates.yaml | universal-dao, sql-file, entity-class, exclusive-control, db-connection |
| handler-queue | handler-catalog.yaml, handler-constraints.yaml | 全アプリタイプサマリ、主要順序制約 |
| error-handling | error-catalog.yaml | カテゴリ別エラー一覧（原因・解決策付き） |

#### レスポンス構造（例: setupトピック）

```markdown
# Nablarch Setup Guide

## Overview

Nablarchプロジェクトの初期構築に必要な設定テンプレートとガイドです。

## Configuration Templates

### web-xml

{description}

```xml
{template}
```

...

---
*Source: config-templates.yaml*
```

#### エラーレスポンス

不正なトピックを指定した場合:

```markdown
# Unknown Guide Topic

Unknown guide topic: {topic}

Valid topics: setup, testing, validation, database, handler-queue, error-handling
```

---

## Prompts仕様

全プロンプトはMCP標準の `prompts/get` メソッドでアクセスします。
レスポンスは `GetPromptResult` 形式で、`messages` 配列にMarkdown形式のコンテンツが含まれます。

### setup-handler-queue

ハンドラキュー構成を支援するプロンプトテンプレート。

#### 定義

```json
{
  "name": "setup-handler-queue",
  "description": "Set up a Nablarch handler queue configuration",
  "arguments": [
    {
      "name": "app_type",
      "description": "Application type: web, rest, batch, messaging",
      "required": true
    }
  ]
}
```

#### 出力内容

- 指定アプリタイプのハンドラキュー概要
- 推奨ハンドラ一覧テーブル（順序、名前、FQCN、必須区分、スレッドモデル、説明）
- 順序制約セクション（FQCN、ルール、前後配置要件、理由）
- アプリタイプに該当するXML設定テンプレート

---

### create-action

Nablarchアクションクラスのスケルトン生成を支援するプロンプトテンプレート。

#### 定義

```json
{
  "name": "create-action",
  "description": "Generate a Nablarch action class skeleton",
  "arguments": [
    {
      "name": "app_type",
      "description": "Application type: web, rest, batch, messaging",
      "required": true
    },
    {
      "name": "action_name",
      "description": "Name of the action class to generate",
      "required": true
    }
  ]
}
```

#### 出力内容

- アクションクラス生成ガイド（タイトル、アプリタイプ）
- アプリタイプに該当する推奨APIパターン（名前、説明、FQCN、コード例、関連パターン）
- 命名規則（クラス名形式、パッケージ構造、メソッド名の"do"プレフィックスルール）

---

### review-config

NablarchのXML設定ファイルをレビューするプロンプトテンプレート。

#### 定義

```json
{
  "name": "review-config",
  "description": "Review a Nablarch XML configuration file for correctness",
  "arguments": [
    {
      "name": "config_xml",
      "description": "XML configuration content to review",
      "required": true
    }
  ]
}
```

#### 出力内容

- レビュー対象セクション（XML先頭500文字または全文）
- ハンドラ順序制約チェックリスト（FQCN、ルール、前後配置要件、理由）
- 一般的な問題パターン（handler/configカテゴリのエラーID、メッセージ、原因、解決策）
- 一般検証チェックリスト（必須ハンドラ、FQCN正確性、コンポーネント定義、データソース設定等）

---

### explain-handler

Nablarchハンドラの詳細説明を提供するプロンプトテンプレート。

#### 定義

```json
{
  "name": "explain-handler",
  "description": "Get a detailed explanation of a Nablarch handler",
  "arguments": [
    {
      "name": "handler_name",
      "description": "Name of the handler to explain",
      "required": true
    }
  ]
}
```

#### 出力内容

- 基本情報テーブル（名前、FQCN、説明、スレッドモデル、必須区分、推奨順序）
- ハンドラが使用されるアプリケーションタイプ一覧
- ハンドラ固有の制約情報
- 詳細な順序制約（ルール、前後配置要件、理由）
- ハンドラが見つからない場合のフォールバックメッセージ

---

### migration-guide

Nablarchバージョン間の移行ガイドを提供するプロンプトテンプレート。

#### 定義

```json
{
  "name": "migration-guide",
  "description": "Get a migration guide between Nablarch versions",
  "arguments": [
    {
      "name": "from_version",
      "description": "Source Nablarch version",
      "required": true
    },
    {
      "name": "to_version",
      "description": "Target Nablarch version",
      "required": true
    }
  ]
}
```

#### 出力内容

- バージョン移行タイトル（from → to）
- モジュール一覧テーブル（モジュール名、artifactId、カテゴリ、説明、合計数）
- 各モジュールの主要クラス確認セクション（FQCN、説明）
- モジュール間の依存関係セクション
- 一般的な移行ステップ（BOMバージョン更新、破壊的変更確認、コンパイルチェック、テスト実行、ハンドラカタログ確認、XML設定チェック、統合テスト）

---

### best-practices

Nablarch開発のベストプラクティスを提供するプロンプトテンプレート。

#### 定義

```json
{
  "name": "best-practices",
  "description": "Get Nablarch best practices for a specific topic",
  "arguments": [
    {
      "name": "topic",
      "description": "Topic: handler-queue, action, validation, database, testing",
      "required": true
    }
  ]
}
```

#### トピック→カテゴリマッピング

| トピック | 設計パターンカテゴリ | APIパターンカテゴリ |
|---------|-------------------|------------------|
| handler-queue | handler, architecture | config |
| action | action | web, rest |
| validation | validation | web |
| database | data-access | library |
| testing | — | testing |

#### 出力内容

- トピック別ベストプラクティスタイトル
- 設計パターンセクション（名前、説明、問題、解決策、構造図、コード例、参照）
- 推奨実装パターンセクション（名前、カテゴリ、説明、FQCN、コード例）
- 一般的なベストプラクティスノート

---

## データソース

Phase 1の知識ベースは7つの静的YAMLファイルで構成されます。

| ファイル | 内容 | 件数 |
|---------|------|------|
| `handler-catalog.yaml` | アプリタイプ別ハンドラカタログ | 6タイプ、64ハンドラ |
| `handler-constraints.yaml` | ハンドラ順序制約 | 24件 |
| `api-patterns.yaml` | API使用パターン | 25パターン |
| `module-catalog.yaml` | Nablarchモジュールカタログ | 21モジュール |
| `error-catalog.yaml` | エラーカタログ | 17エラー |
| `config-templates.yaml` | XML設定テンプレート | 9テンプレート |
| `design-patterns.yaml` | 設計パターン | 11パターン |

全ファイルは `src/main/resources/knowledge/` に配置され、サーバー起動時にメモリにロードされます。

### ロードアーキテクチャ

```
起動時 (@PostConstruct)
  ├─ NablarchKnowledgeBase: 7 YAML → モデルクラス + インデックス構築
  ├─ HandlerResourceProvider: handler-catalog.yaml + handler-constraints.yaml → Map/List
  ├─ GuideResourceProvider: 6 YAML → Map/List
  └─ 各Promptクラス: 必要な YAML → Map/List
```

## エラーハンドリング

### MCP プロトコルレベル

MCP SDKが処理するプロトコルレベルのエラー:

| エラーコード | 説明 |
|-------------|------|
| -32700 | Parse error（JSON解析エラー） |
| -32600 | Invalid Request（不正なリクエスト） |
| -32601 | Method not found（メソッドが見つからない） |
| -32602 | Invalid params（不正なパラメータ） |
| -32603 | Internal error（内部エラー） |

### アプリケーションレベル

ツール・リソース・プロンプトの各エンドポイントでのバリデーション:

| エンドポイント | 条件 | 挙動 |
|--------------|------|------|
| search_api | keyword 未指定 | エラーメッセージを返却 |
| validate_handler_queue | XML 未指定 | エラーメッセージを返却 |
| validate_handler_queue | appType 未指定 | エラーメッセージを返却 |
| handler/{app_type} | 不正なappType | 有効なタイプ一覧を含むエラーMarkdownを返却 |
| guide/{topic} | 不正なtopic | 有効なトピック一覧を含むエラーMarkdownを返却 |
| Prompts | 必須引数未指定 | 引数指定を求めるメッセージを返却 |

---

## 制限事項・既知の問題

### Phase 1の制限

1. **STDIOトランスポートのみ**: HTTP/SSEトランスポートは未サポート。1プロセス1クライアント
2. **静的知識ベース**: YAMLファイルの内容は起動時に固定。動的更新なし
3. **単純文字列マッチング**: 検索は大文字小文字無視の部分一致のみ。セマンティック検索なし
4. **バージョン固有の知識なし**: migration-guideプロンプトはモジュール一覧を提供するが、バージョン間の差分情報は含まない
5. **Nablarch 6系のみ**: 知識ベースはNablarch 6系を対象。5系以前の固有情報は限定的
6. **日本語出力**: ツールのレスポンスは日本語。多言語サポートなし

### Phase 2ロードマップ

Phase 2では以下の拡張を計画しています:

- **RAGエンジン統合**: ベクトル検索によるセマンティック検索
- **SSEトランスポート**: HTTP Server-Sent Eventsによるリモート接続サポート
- **動的知識更新**: 知識ベースのホットリロード
- **公式ドキュメント連携**: Nablarch公式ドキュメントからの自動知識抽出
- **バージョン差分**: バージョン間の破壊的変更データベース

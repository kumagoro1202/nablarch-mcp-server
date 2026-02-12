# Resource URI仕様書

> **対象読者**: 開発者（Resource URI仕様を参照したい人）
> **前提知識**: MCP Resource, URIパターンの概念
> **概要**: MCP Resource URIの命名規則と全16リソースの仕様

---

## 1. 概要

Nablarch MCP Serverが提供するMCP Resourceの完全なURI仕様書。
8種のResourceProvider実装に基づく、全16リソースのURIパターン・レスポンス形式・使用例を記載する。

### 1.1 Resource一覧

| # | Resource種別 | URI | Provider | Content-Type | 登録状況 |
|---|-------------|-----|----------|-------------|----------|
| 1 | Handler | `nablarch://handler/{type}` (6種) | HandlerResourceProvider | `text/markdown` | ✅ 登録済み |
| 2 | Guide | `nablarch://guide/{topic}` (6種) | GuideResourceProvider | `text/markdown` | ✅ 登録済み |
| 3 | API | `nablarch://api/modules` | ApiResourceProvider | `application/json` | ✅ 登録済み |
| 4 | Pattern | `nablarch://pattern/list` | PatternResourceProvider | `text/markdown` | ✅ 登録済み |
| 5 | Antipattern | `nablarch://antipattern/list` | AntipatternResourceProvider | `application/json` | ✅ 登録済み |
| 6 | Config | `nablarch://config/list` | ConfigResourceProvider | `text/plain` | ✅ 登録済み |
| 7 | Example | `nablarch://example/list` | ExampleResourceProvider | `application/json` | ✅ 登録済み |
| 8 | Version | `nablarch://version/info` | VersionResourceProvider | `application/json` | ✅ 登録済み |

### 1.2 URIスキーム

URIは2つのパターンがあります:

**パラメトリックパターン**（Handler, Guide）:
```
nablarch://{resource_type}/{key}
```
キー値ごとに個別のMCP Resourceとして登録されます（例: `nablarch://handler/web`, `nablarch://handler/rest` がそれぞれ独立リソース）。

**固定URIパターン**（API, Pattern, Example, Config, Antipattern, Version）:
```
nablarch://{resource_type}/{固定パス}
```
各種別につき1つのMCP Resourceが登録されます（例: `nablarch://api/modules`, `nablarch://pattern/list`）。

---

## 2. Handler Resource

### 2.1 概要

アプリケーションタイプ別のハンドラキュー仕様を提供する。

| 項目 | 値 |
|------|-----|
| Provider | `HandlerResourceProvider` |
| Content-Type | `text/markdown` |
| データソース | `handler-catalog.yaml`, `handler-constraints.yaml` |

### 2.2 URI一覧

| URI | 説明 |
|-----|------|
| `nablarch://handler/web` | Webアプリケーション用ハンドラキュー |
| `nablarch://handler/rest` | RESTful Webサービス用ハンドラキュー |
| `nablarch://handler/batch` | バッチアプリケーション用ハンドラキュー |
| `nablarch://handler/messaging` | メッセージング用ハンドラキュー |
| `nablarch://handler/http-messaging` | HTTP同期メッセージング用ハンドラキュー |
| `nablarch://handler/jakarta-batch` | Jakarta Batch用ハンドラキュー |

### 2.3 有効なキー値

```
web, rest, batch, messaging, http-messaging, jakarta-batch
```

### 2.4 レスポンス形式

```markdown
# Nablarch {AppType} Application Handler Queue

{description}

## Handler Queue (in order)

### 1. {HandlerName} [Required|Optional]
- **FQCN**: `{fqcn}`
- **Thread**: {thread}
- **Description**: {description}
- **Constraints**:
  - Must be before: {handlers}
  - Must be after: {handlers}

### 2. ...

## Ordering Constraints Summary

| Handler | Rule | Details |
|---------|------|---------|
| {name} | {rule} | {reason} |

---
*Source: handler-catalog.yaml, handler-constraints.yaml*
```

### 2.5 エラーレスポンス

不正なキー指定時:

```markdown
# Unknown Application Type

Unknown application type: {invalid_key}

Valid types: web, rest, batch, messaging, http-messaging, jakarta-batch
```

### 2.6 使用例

**リクエスト（MCP JSON-RPC）:**

```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "nablarch://handler/web"
  },
  "id": 1
}
```

**レスポンス（抜粋）:**

```json
{
  "jsonrpc": "2.0",
  "result": {
    "contents": [{
      "uri": "nablarch://handler/web",
      "mimeType": "text/markdown",
      "text": "# Nablarch Web Application Handler Queue\n\n..."
    }]
  },
  "id": 1
}
```

---

## 3. Guide Resource

### 3.1 概要

トピック別の開発ガイドを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `GuideResourceProvider` |
| Content-Type | `text/markdown` |
| データソース | 複数YAMLファイル（トピックにより異なる） |

### 3.2 URI一覧

| URI | 説明 | データソース |
|-----|------|------------|
| `nablarch://guide/setup` | プロジェクト設定ガイド | config-templates.yaml |
| `nablarch://guide/testing` | テストパターンガイド | api-patterns.yaml |
| `nablarch://guide/validation` | バリデーションガイド | api-patterns.yaml, design-patterns.yaml |
| `nablarch://guide/database` | データベースガイド | api-patterns.yaml, config-templates.yaml |
| `nablarch://guide/handler-queue` | ハンドラキューガイド | handler-catalog.yaml, handler-constraints.yaml |
| `nablarch://guide/error-handling` | エラーハンドリングガイド | error-catalog.yaml |

### 3.3 有効なキー値

```
setup, testing, validation, database, handler-queue, error-handling
```

### 3.4 レスポンス形式

```markdown
# Nablarch {Topic} Guide

## Overview
{トピックの概要}

## {Section 1}
{内容}

## {Section 2}
{内容}

---
*Source: {データソースファイル名}*
```

### 3.5 エラーレスポンス

```markdown
# Unknown Guide Topic

Unknown guide topic: {invalid_topic}

Valid topics: setup, testing, validation, database, handler-queue, error-handling
```

### 3.6 使用例

```json
{
  "jsonrpc": "2.0",
  "method": "resources/read",
  "params": {
    "uri": "nablarch://guide/database"
  },
  "id": 2
}
```

---

## 4. API Resource

### 4.1 概要

NablarchのAPIリファレンス（モジュール・クラス情報）を提供する。

| 項目 | 値 |
|------|-----|
| Provider | `ApiResourceProvider` |
| Content-Type | `application/json` |
| データソース | `module-catalog.yaml`, `api-patterns.yaml` |

### 4.2 URI

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://api/modules` | モジュール一覧（全モジュールの概要をJSON形式で返却） | `getModuleList()` |

> **注**: 個別モジュールの詳細は `search_api` Tool経由で取得してください。Resource URIとしてはモジュール一覧のみ提供しています。

### 4.3 レスポンス形式

```json
{
  "type": "module_list",
  "modules": [
    {
      "name": "Nablarch Framework Web",
      "module_key": "fw-web",
      "category": "framework",
      "description": "Web application framework",
      "class_count": 15,
      "uri": "nablarch://api/fw-web"
    }
  ],
  "total_modules": 20
}
```

---

## 5. Pattern Resource

### 5.1 概要

Nablarch固有の設計パターンカタログを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `PatternResourceProvider` |
| Content-Type | `text/markdown` |
| データソース | `design-patterns.yaml` |

### 5.2 URI

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://pattern/list` | パターン一覧（全パターンをMarkdown形式で返却） | `getPatternList()` |

### 5.3 レスポンス形式

```markdown
# Nablarch デザインパターンカタログ

| # | パターン名 | カテゴリ | 説明 |
|---|-----------|---------|------|
| 1 | form-validation-pattern | validation | フォームバリデーションパターン |
| 2 | ... | ... | ... |

---
*Source: design-patterns.yaml*
```

---

## 6. Antipattern Resource

### 6.1 概要

Nablarch開発でよく見られるアンチパターンとその修正方法を提供する。

| 項目 | 値 |
|------|-----|
| Provider | `AntipatternResourceProvider` |
| Content-Type | `application/json` |
| データソース | `antipattern-catalog.yaml` |

### 6.2 URI

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://antipattern/list` | アンチパターン一覧（JSON形式で返却） | `getAntipatternList()` |

### 6.3 レスポンス形式

アンチパターンカタログ全体がJSON形式で返却されます。各アンチパターンには名前・カテゴリ・重要度・説明・問題・悪い例・良い例・修正方針が含まれます。

---

## 7. Config Resource

### 7.1 概要

NablarchのXML設定テンプレートを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `ConfigResourceProvider` |
| Content-Type | `text/plain` |
| データソース | `config-templates.yaml` |

### 7.2 URI

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://config/list` | 設定テンプレート一覧（テキスト形式で返却） | `getTemplateList()` |

### 7.3 レスポンス形式

設定テンプレートカタログ全体がテキスト形式で返却されます。各テンプレートにはカテゴリ・説明・XMLテンプレート本文が含まれます。

---

## 8. Example Resource

### 8.1 概要

Nablarchのサンプルアプリケーションコードを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `ExampleResourceProvider` |
| Content-Type | `application/json` |
| データソース | `example-catalog.yaml` |

### 8.2 URI

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://example/list` | サンプル一覧（JSON形式で返却） | `getExampleList()` |

### 8.3 レスポンス形式

```json
{
  "type": "example_list",
  "examples": [
    {
      "type": "web-crud",
      "description": "Web CRUD application example",
      "app_type": "web",
      "file_count": 5,
      "reference_repo": "nablarch-example-web"
    }
  ],
  "total_examples": 10
}
```

---

## 9. Version Resource

### 9.1 概要

Nablarchフレームワークのバージョン情報を提供する。

| 項目 | 値 |
|------|-----|
| Provider | `VersionResourceProvider` |
| Content-Type | `application/json` |
| データソース | `version-info.yaml`, `module-catalog.yaml` |

### 9.2 URI

| URI | 説明 |
|-----|------|
| `nablarch://version/info` | バージョン情報 |

### 9.3 レスポンス形式

```json
{
  "type": "version_info",
  "framework_name": "Nablarch",
  "latest_version": "6u2",
  "release_date": "2024-xx-xx",
  "supported_versions": ["6u2", "6u1", "5u21"],
  "platforms": {
    "java": ["17", "21"],
    "application_server": ["Tomcat 10.x", "WildFly 31"]
  },
  "bom": {
    "group_id": "com.nablarch.profile",
    "artifact_id": "nablarch-bom",
    "version": "6u2"
  },
  "modules": [
    {
      "name": "Nablarch Framework Web",
      "artifact_id": "nablarch-fw-web",
      "category": "framework",
      "description": "Web application framework",
      "key_class_count": 15
    }
  ],
  "total_modules": 20,
  "links": {
    "documentation": "https://nablarch.github.io/docs/",
    "github": "https://github.com/nablarch"
  }
}
```

---

## 10. MCP登録仕様

### 10.1 Resource登録パターン

```java
// McpServerConfig.java
@Bean
public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
        HandlerResourceProvider handlerProvider,
        GuideResourceProvider guideProvider,
        ApiResourceProvider apiProvider,
        PatternResourceProvider patternProvider,
        // ... 他のProvider
) {
    return List.of(
        // Handler Resources (6種)
        createHandlerResourceSpec("web", "...", handlerProvider),
        // Guide Resources (6種)
        createGuideResourceSpec("setup", "...", guideProvider),
        // API Resources
        createApiResourceSpec(...),
        // ... 他のResource
    );
}
```

### 10.2 SyncResourceSpecification構造

```java
new McpServerFeatures.SyncResourceSpecification(
    new McpSchema.Resource(
        uri,           // "nablarch://handler/web"
        name,          // "Nablarch Web Handler Catalog"
        description,   // 説明文
        mimeType,      // "text/markdown" or "application/json"
        annotations    // null
    ),
    (exchange, request) -> new McpSchema.ReadResourceResult(
        List.of(new McpSchema.TextResourceContents(
            request.uri(),
            mimeType,
            provider.getContent(key)
        ))
    )
);
```

---

## 11. エラーハンドリング共通仕様

### 11.1 不正URI

| エラー種別 | 処理 |
|-----------|------|
| 存在しないリソース種別 | MCPプロトコルエラー（-32602 Invalid params） |
| 存在しないキー | 各Providerが "Unknown..." メッセージを含むレスポンスを返却 |

### 11.2 エラーレスポンス形式

**Markdown形式Resource:**

```markdown
# Unknown {ResourceType}

Unknown {type}: {invalid_key}

Valid {types}: {valid_values}
```

**JSON形式Resource:**

```json
{
  "error": "Unknown {type}: {invalid_key}",
  "valid_{types}": [...]
}
```

---

## 付録A: データソースYAMLファイル一覧

| ファイル | 用途 | 参照Resource |
|---------|------|-------------|
| `handler-catalog.yaml` | ハンドラ定義 | Handler, Guide |
| `handler-constraints.yaml` | ハンドラ順序制約 | Handler, Guide |
| `api-patterns.yaml` | APIパターン | Guide, API |
| `design-patterns.yaml` | 設計パターン | Guide, Pattern |
| `antipattern-catalog.yaml` | アンチパターン | Antipattern |
| `config-templates.yaml` | XML設定テンプレート | Guide, Config |
| `error-catalog.yaml` | エラーカタログ | Guide |
| `example-catalog.yaml` | サンプルカタログ | Example |
| `module-catalog.yaml` | モジュールカタログ | API, Version |
| `version-info.yaml` | バージョン情報 | Version |

---

## 付録B: 変更履歴

| バージョン | 日付 | 変更内容 |
|-----------|------|---------|
| 1.0 | 2026-02-04 | 初版作成（全8 Resource仕様） |
| 1.1 | 2026-02-12 | 実装に合わせてURI仕様を修正（パラメトリックURI → 固定URI、MIMEタイプ修正、全16リソース明記） |

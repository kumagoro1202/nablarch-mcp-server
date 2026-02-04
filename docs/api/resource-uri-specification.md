# Resource URI仕様書

> **バージョン**: 1.0
> **作成日**: 2026-02-04
> **Phase**: 3
> **WBS**: 3.4.2

---

## 1. 概要

Nablarch MCP Serverが提供するMCP Resourceの完全なURI仕様書。
全8種のResourceProvider実装に基づく、URIパターン・レスポンス形式・使用例を記載する。

### 1.1 Resource一覧

| # | Resource種別 | URIプレフィックス | Provider | 登録状況 |
|---|-------------|------------------|----------|----------|
| 1 | Handler | `nablarch://handler/` | HandlerResourceProvider | ✅ 登録済み |
| 2 | Guide | `nablarch://guide/` | GuideResourceProvider | ✅ 登録済み |
| 3 | API | `nablarch://api/` | ApiResourceProvider | 🔧 実装済み |
| 4 | Pattern | `nablarch://pattern/` | PatternResourceProvider | 🔧 実装済み |
| 5 | Antipattern | `nablarch://antipattern/` | AntipatternResourceProvider | 🔧 実装済み |
| 6 | Config | `nablarch://config/` | ConfigResourceProvider | 🔧 実装済み |
| 7 | Example | `nablarch://example/` | ExampleResourceProvider | 🔧 実装済み |
| 8 | Version | `nablarch://version` | VersionResourceProvider | 🔧 実装済み |

### 1.2 URIスキーム

```
nablarch://{resource_type}/{resource_key}
```

- **スキーム**: `nablarch`（固定）
- **resource_type**: リソース種別（handler, guide, api, pattern等）
- **resource_key**: リソース識別子（オプション、種別による）

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

### 4.2 URI体系

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://api/` | モジュール一覧 | `getModuleList()` |
| `nablarch://api/{module}` | モジュール内クラス一覧 | `getClassList(moduleKey)` |
| `nablarch://api/{module}/{class}` | クラス詳細 | `getClassDetail(moduleKey, className)` |

### 4.3 有効なモジュールキー

```
fw-web, fw-batch, fw-messaging, core, common,
common-dao, common-jdbc, ...
```

※ `nablarch-` プレフィックスを除去した値

### 4.4 レスポンス形式

#### モジュール一覧

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

#### クラス一覧

```json
{
  "type": "class_list",
  "module_key": "fw-web",
  "classes": [
    {
      "simple_name": "HttpRequest",
      "fqcn": "nablarch.fw.web.HttpRequest",
      "description": "HTTPリクエストを表すインターフェース"
    }
  ],
  "total_classes": 15
}
```

#### クラス詳細

```json
{
  "type": "class_detail",
  "module": "Nablarch Framework Web",
  "simple_name": "HttpRequest",
  "fqcn": "nablarch.fw.web.HttpRequest",
  "description": "HTTPリクエストを表すインターフェース"
}
```

### 4.5 エラーレスポンス

```json
{
  "error": "Unknown module: invalid-module",
  "valid_modules": ["fw-web", "fw-batch", "core", ...]
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

### 5.2 URI体系

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://pattern/` | パターン一覧 | `getPatternList()` |
| `nablarch://pattern/{name}` | パターン詳細 | `getPatternDetail(name)` |

### 5.3 レスポンス形式

#### パターン一覧

```markdown
# Nablarch デザインパターンカタログ

| # | パターン名 | カテゴリ | 説明 |
|---|-----------|---------|------|
| 1 | form-validation-pattern | validation | フォームバリデーションパターン |
| 2 | ... | ... | ... |

---
*Source: design-patterns.yaml*
```

#### パターン詳細

```markdown
# {パターン名}

**カテゴリ**: {category}

## 概要
{description}

## 問題
{problem}

## 解決策
{solution}

## コード例
```java
{code_example}
```

---
*Source: design-patterns.yaml*
```

### 5.4 エラーレスポンス

```markdown
# Unknown Pattern

Unknown pattern: {invalid_name}

Valid patterns: form-validation-pattern, ...
```

---

## 6. Antipattern Resource

### 6.1 概要

Nablarch開発でよく見られるアンチパターンとその修正方法を提供する。

| 項目 | 値 |
|------|-----|
| Provider | `AntipatternResourceProvider` |
| Content-Type | `text/markdown` |
| データソース | `antipattern-catalog.yaml` |

### 6.2 URI体系

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://antipattern/` | アンチパターン一覧 | `getAntipatternList()` |
| `nablarch://antipattern/{name}` | アンチパターン詳細 | `getAntipatternDetail(name)` |

### 6.3 レスポンス形式

#### 一覧

```markdown
# Nablarch アンチパターンカタログ

| # | 名前 | カテゴリ | 重要度 | 説明 |
|---|------|---------|--------|------|
| 1 | handler-order-violation | handler-queue | high | ハンドラ順序違反 |

---
*Source: antipattern-catalog.yaml*
```

#### 詳細

```markdown
# {タイトル}

**名前**: {name}
**カテゴリ**: {category}
**重要度**: {severity}

## 概要
{description}

## 問題
{problem}

## 悪い例
```java
{bad_example}
```

## 良い例
```java
{good_example}
```

## 修正方針
{fix_strategy}

---
*Source: antipattern-catalog.yaml*
```

---

## 7. Config Resource

### 7.1 概要

NablarchのXML設定テンプレートを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `ConfigResourceProvider` |
| Content-Type | `text/xml` (テンプレート) / `text/markdown` (一覧) |
| データソース | `config-templates.yaml` |

### 7.2 URI体系

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://config/` | テンプレート一覧 | `getTemplateList()` |
| `nablarch://config/{name}` | テンプレート取得 | `getTemplate(name)` |

### 7.3 有効なテンプレート名

```
web-xml, web-component, rest-component, batch-component,
db-connection, ...
```

### 7.4 レスポンス形式

#### 一覧

```markdown
# Nablarch XML設定テンプレート一覧

| # | テンプレート名 | カテゴリ | 説明 |
|---|--------------|---------|------|
| 1 | web-xml | web | web.xml設定テンプレート |

---
*Source: config-templates.yaml*
```

#### テンプレート

```xml
<!--
  Nablarch Configuration Template: {name}
  Category: {category}
  Description: {description}
-->
{XMLテンプレート本文}
```

---

## 8. Example Resource

### 8.1 概要

Nablarchのサンプルアプリケーションコードを提供する。

| 項目 | 値 |
|------|-----|
| Provider | `ExampleResourceProvider` |
| Content-Type | `application/json` |
| データソース | `example-catalog.yaml` |

### 8.2 URI体系

| URI | 説明 | メソッド |
|-----|------|---------|
| `nablarch://example/` | サンプル一覧 | `getExampleList()` |
| `nablarch://example/{type}` | サンプル詳細 | `getExampleDetail(type)` |

### 8.3 レスポンス形式

#### 一覧

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

#### 詳細

```json
{
  "type": "example_detail",
  "example_type": "web-crud",
  "description": "Web CRUD application example",
  "app_type": "web",
  "reference_repo": "nablarch-example-web",
  "key_patterns": ["universal-dao", "form-validation"],
  "files": [
    {
      "path": "src/main/java/.../Action.java",
      "description": "Action class example"
    }
  ]
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
| `nablarch://version` | バージョン情報（キーなし） |

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

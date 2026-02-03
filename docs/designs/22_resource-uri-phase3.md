# 追加Resource URI設計書（Phase 3: 6種）

> **バージョン**: 1.0
> **作成日**: 2026-02-03
> **WBS**: 3.1.8
> **親タスク**: subtask_065 (cmd_032)
> **対応ユースケース**: UC2, UC4, UC6, UC8, UC9, UC11, UC12

---

## 目次

1. [概要](#1-概要)
2. [api/{module}/{class} リソース（UC2対応）](#2-apimoduleclass-リソースuc2対応)
3. [pattern/{name} リソース（UC8対応）](#3-patternname-リソースuc8対応)
4. [example/{type} リソース（UC11, UC12対応）](#4-exampletype-リソースuc11-uc12対応)
5. [config/{name} リソース（UC4対応）](#5-configname-リソースuc4対応)
6. [antipattern/{name} リソース（UC6対応）](#6-antipatternname-リソースuc6対応)
7. [version リソース（UC9対応）](#7-version-リソースuc9対応)
8. [実装設計](#8-実装設計)
9. [Phase移行計画](#9-phase移行計画)
10. [参考文献](#10-参考文献)

---

## 1. 概要

### 1.1 本設計書の位置づけ

Phase 1で実装済みの2種Resource（`handler/{app_type}`, `guide/{topic}`）に加え、Phase 3で追加する6種のResource URIを設計する。これによりアーキテクチャ設計書で定義された全8種のResourceタイプが揃う。

### 1.2 対象Resource URI一覧

| # | URIパターン | MIMEタイプ | 対応UC | 説明 |
|---|------------|-----------|--------|------|
| 1 | `nablarch://api/{module}/{class}` | `application/json` | UC2 | Javadoc構造化データ提供 |
| 2 | `nablarch://pattern/{name}` | `text/markdown` | UC8 | デザインパターンカタログ提供 |
| 3 | `nablarch://example/{type}` | `application/json` | UC11, UC12 | サンプルコード提供 |
| 4 | `nablarch://config/{name}` | `application/xml` | UC4 | XML設定テンプレート提供 |
| 5 | `nablarch://antipattern/{name}` | `text/markdown` | UC6 | アンチパターンと修正方法提供 |
| 6 | `nablarch://version` | `application/json` | UC9 | バージョン情報・モジュール一覧 |

### 1.3 既存Resourceとの設計一貫性

全ResourceはPhase 1で確立した以下の設計原則に従う:

| 原則 | 説明 |
|------|------|
| **URIスキーム統一** | `nablarch://` スキームを使用 |
| **ナレッジYAMLベース** | `src/main/resources/knowledge/` 配下のYAMLファイルをデータソースとする |
| **起動時ロード** | `@PostConstruct` でYAMLを読み込みインメモリ保持 |
| **読み取り専用** | 全Resourceは読み取り専用（MCP仕様に準拠） |
| **エラーの統一形式** | 不正パラメータ時は有効な値一覧を含むエラーメッセージを返却 |
| **日本語コンテンツ** | レスポンスの説明文は日本語 |

### 1.4 全体URI構成（Phase 1 + Phase 3）

```
nablarch://
├── handler/{app_type}    ← Phase 1 実装済み（text/markdown）
├── guide/{topic}         ← Phase 1 実装済み（text/markdown）
├── api/{module}/{class}  ← Phase 3 NEW（application/json）
├── pattern/{name}        ← Phase 3 NEW（text/markdown）
├── example/{type}        ← Phase 3 NEW（application/json）
├── config/{name}         ← Phase 3 NEW（application/xml）
├── antipattern/{name}    ← Phase 3 NEW（text/markdown）
└── version               ← Phase 3 NEW（application/json）
```

---

## 2. api/{module}/{class} リソース（UC2対応）

### 2.1 概要

NablarchのAPIリファレンス（Javadoc相当）を構造化データとして提供する。AIクライアントはこのリソースを使って、Nablarchの特定クラスのメソッドシグネチャ・使用方法を参照する。

### 2.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://api` | モジュール一覧を返却 |
| `nablarch://api/{module}` | 指定モジュールのクラス一覧を返却 |
| `nablarch://api/{module}/{class}` | 指定クラスのAPI詳細を返却 |

**パラメータ定義**:

| パラメータ | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `module` | string | Nablarchモジュール名（artifactIdから `nablarch-` プレフィックスを除いた形式） | `common-dao`, `core`, `fw-web` |
| `class` | string | クラスの単純名 | `UniversalDao`, `Handler`, `ExecutionContext` |

### 2.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/module-catalog.yaml` | 21モジュール、主要クラス（FQCN、説明） |
| Phase 3（静的） | `knowledge/api-patterns.yaml` | 25パターン（FQCN、コード例、関連パターン） |
| Phase 2+（RAG） | `javadoc_index`（pgvector） | 全Javadocのセマンティック検索 |

### 2.4 レスポンスデータ構造

#### 2.4.1 モジュール一覧（`nablarch://api`）

MIMEタイプ: `application/json`

```json
{
  "type": "module_list",
  "modules": [
    {
      "name": "nablarch-core",
      "module_key": "core",
      "category": "core",
      "description": "Nablarchのコアモジュール。基本的なインターフェース・ユーティリティを提供",
      "class_count": 3,
      "uri": "nablarch://api/core"
    }
  ],
  "total_modules": 21
}
```

#### 2.4.2 クラス一覧（`nablarch://api/{module}`）

MIMEタイプ: `application/json`

```json
{
  "type": "class_list",
  "module": {
    "name": "nablarch-common-dao",
    "module_key": "common-dao",
    "category": "library",
    "description": "ユニバーサルDAO"
  },
  "classes": [
    {
      "simple_name": "UniversalDao",
      "fqcn": "nablarch.common.dao.UniversalDao",
      "description": "汎用的なDAO。CRUD操作を提供",
      "uri": "nablarch://api/common-dao/UniversalDao"
    }
  ],
  "total_classes": 2
}
```

#### 2.4.3 クラス詳細（`nablarch://api/{module}/{class}`）

MIMEタイプ: `application/json`

```json
{
  "type": "class_detail",
  "module": "nablarch-common-dao",
  "simple_name": "UniversalDao",
  "fqcn": "nablarch.common.dao.UniversalDao",
  "description": "汎用的なDAO。CRUD操作を提供",
  "javadoc_url": "https://nablarch.github.io/docs/LATEST/javadoc/nablarch/common/dao/UniversalDao.html",
  "github_url": "https://github.com/nablarch/nablarch-common-dao",
  "related_patterns": [
    {
      "name": "universal-dao",
      "description": "UniversalDaoを使ったCRUD操作",
      "code_example": "UniversalDao.findAll(Entity.class);",
      "related_patterns": ["entity-class", "sql-file"]
    }
  ],
  "usage_notes": "Entityクラスに@Entity, @Tableアノテーションが必要"
}
```

#### 2.4.4 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Nablarch API Resource Response",
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "type": { "const": "module_list" },
        "modules": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "name": { "type": "string" },
              "module_key": { "type": "string" },
              "category": { "type": "string" },
              "description": { "type": "string" },
              "class_count": { "type": "integer" },
              "uri": { "type": "string", "format": "uri" }
            },
            "required": ["name", "module_key", "category", "description", "uri"]
          }
        },
        "total_modules": { "type": "integer" }
      },
      "required": ["type", "modules", "total_modules"]
    },
    {
      "type": "object",
      "properties": {
        "type": { "const": "class_list" },
        "module": { "type": "object" },
        "classes": { "type": "array" },
        "total_classes": { "type": "integer" }
      },
      "required": ["type", "module", "classes", "total_classes"]
    },
    {
      "type": "object",
      "properties": {
        "type": { "const": "class_detail" },
        "module": { "type": "string" },
        "simple_name": { "type": "string" },
        "fqcn": { "type": "string" },
        "description": { "type": "string" },
        "javadoc_url": { "type": "string", "format": "uri" },
        "github_url": { "type": "string", "format": "uri" },
        "related_patterns": { "type": "array" },
        "usage_notes": { "type": "string" }
      },
      "required": ["type", "module", "simple_name", "fqcn", "description"]
    }
  ]
}
```

### 2.5 エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| 不正なmodule | `{"error": "Unknown module: {module}", "valid_modules": ["core", "common-dao", ...]}` |
| 不正なclass | `{"error": "Class not found: {class} in module {module}", "available_classes": [...]}` |

---

## 3. pattern/{name} リソース（UC8対応）

### 3.1 概要

Nablarch固有の設計パターンカタログを提供する。各パターンには問題定義、解決策、コード例、構造図、関連パターンが含まれる。アーキテクトの設計判断を支援する。

### 3.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://pattern` | パターン一覧を返却 |
| `nablarch://pattern/{name}` | 指定パターンの詳細を返却 |

**パラメータ定義**:

| パラメータ | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `name` | string | パターン名（ケバブケース） | `handler-queue-pattern`, `dao-pattern`, `dual-db-connection` |

### 3.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/design-patterns.yaml` | 11パターン（問題、解決策、コード例、構造図、参照） |
| Phase 2+（RAG） | `docs_index`（pgvector） | 公式ドキュメント・Fintanからのセマンティック検索 |

### 3.4 レスポンスデータ構造

#### 3.4.1 パターン一覧（`nablarch://pattern`）

MIMEタイプ: `text/markdown`

```markdown
# Nablarch デザインパターンカタログ

## パターン一覧

| # | パターン名 | カテゴリ | 説明 |
|---|-----------|---------|------|
| 1 | handler-queue-pattern | architecture | ハンドラキューパターン。Nablarchの根幹アーキテクチャ |
| 2 | action-class-pattern | action | アクションクラスパターン |
| ...

## カテゴリ別索引

### architecture
- handler-queue-pattern

### action
- action-class-pattern

---
*Source: design-patterns.yaml*
```

#### 3.4.2 パターン詳細（`nablarch://pattern/{name}`）

MIMEタイプ: `text/markdown`

```markdown
# {パターン名}

**カテゴリ**: {category}
**適用アプリタイプ**: {web, rest, batch, messaging}

## 概要
{description}

## 問題
{problem}

## 解決策
{solution}

## 構造
{structure}

## コード例
```java
{code_example}
```

## 関連パターン
- {related_pattern_1}
- {related_pattern_2}

## 参考
- {reference_url_1}
- {reference_url_2}

---
*Source: design-patterns.yaml*
```

### 3.5 エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| 不正なname | 以下のMarkdownを返却 |

```markdown
# Unknown Pattern

Unknown pattern: {name}

Valid patterns: handler-queue-pattern, action-class-pattern, ...
```

---

## 4. example/{type} リソース（UC11, UC12対応）

### 4.1 概要

Nablarchのサンプルアプリケーションコードを提供する。各サンプルは複数ファイル（Action、Entity、SQL、XML設定等）で構成され、実際に動くプロジェクト構成の雛形を提供する。初学者の学習やREST APIスキャフォールディングに活用する。

### 4.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://example` | サンプルタイプ一覧を返却 |
| `nablarch://example/{type}` | 指定タイプのサンプルコードを返却 |

**パラメータ定義**:

| パラメータ | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `type` | string | サンプルタイプ（ケバブケース） | `rest-api`, `web-crud`, `batch-db`, `messaging-mom` |

### 4.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/example-catalog.yaml`（新規） | 4タイプのサンプルプロジェクト構成 |
| Phase 2+（RAG） | `code_index`（pgvector） | nablarch-exampleリポジトリのセマンティック検索 |

#### 4.3.1 新規ナレッジファイル: example-catalog.yaml

```yaml
# Nablarch Example Catalog
# サンプルアプリケーションの構成・コードテンプレート
# Version: 1.0

examples:
  - type: rest-api
    description: "NablarchによるRESTful API サンプル"
    app_type: rest
    reference_repo: "https://github.com/nablarch/nablarch-example-rest"
    files:
      - path: "src/main/java/com/example/action/PersonAction.java"
        language: java
        description: "REST APIアクションクラス（CRUD操作）"
        content: |
          // PersonAction.java の内容
      - path: "src/main/java/com/example/entity/Person.java"
        language: java
        description: "Entityクラス"
        content: |
          // Person.java の内容
      - path: "src/main/resources/com/example/entity/Person.sql"
        language: sql
        description: "SQL定義ファイル"
        content: |
          // Person.sql の内容
      - path: "src/main/resources/rest-component-configuration.xml"
        language: xml
        description: "REST用ハンドラキュー設定"
        content: |
          // XML設定の内容
    handler_queue_type: rest
    key_patterns:
      - "JAX-RSアノテーション（@Produces, @Consumes）"
      - "UniversalDaoによるCRUD"
      - "BeanValidationによる入力バリデーション"

  - type: web-crud
    description: "Nablarch Webアプリケーション サンプル（CRUD画面）"
    app_type: web
    reference_repo: "https://github.com/nablarch/nablarch-example-web"
    files: []  # 同様の構造
    handler_queue_type: web
    key_patterns: []

  - type: batch-db
    description: "Nablarchバッチアプリケーション サンプル（DB読み込み→処理）"
    app_type: batch
    reference_repo: "https://github.com/nablarch/nablarch-example-batch-ee"
    files: []
    handler_queue_type: batch
    key_patterns: []

  - type: messaging-mom
    description: "Nablarchメッセージング サンプル（MOM連携）"
    app_type: messaging
    reference_repo: "https://github.com/nablarch/nablarch-example-mom-testing-common"
    files: []
    handler_queue_type: messaging
    key_patterns: []
```

### 4.4 レスポンスデータ構造

#### 4.4.1 サンプル一覧（`nablarch://example`）

MIMEタイプ: `application/json`

```json
{
  "type": "example_list",
  "examples": [
    {
      "type": "rest-api",
      "description": "NablarchによるRESTful API サンプル",
      "app_type": "rest",
      "file_count": 4,
      "reference_repo": "https://github.com/nablarch/nablarch-example-rest",
      "uri": "nablarch://example/rest-api"
    }
  ],
  "total_examples": 4
}
```

#### 4.4.2 サンプル詳細（`nablarch://example/{type}`）

MIMEタイプ: `application/json`

```json
{
  "type": "example_detail",
  "example_type": "rest-api",
  "description": "NablarchによるRESTful API サンプル",
  "app_type": "rest",
  "reference_repo": "https://github.com/nablarch/nablarch-example-rest",
  "handler_queue_type": "rest",
  "key_patterns": [
    "JAX-RSアノテーション（@Produces, @Consumes）",
    "UniversalDaoによるCRUD",
    "BeanValidationによる入力バリデーション"
  ],
  "files": [
    {
      "path": "src/main/java/com/example/action/PersonAction.java",
      "language": "java",
      "description": "REST APIアクションクラス（CRUD操作）",
      "content": "package com.example.action;\n\nimport jakarta.ws.rs.*;\n..."
    },
    {
      "path": "src/main/java/com/example/entity/Person.java",
      "language": "java",
      "description": "Entityクラス",
      "content": "package com.example.entity;\n\nimport jakarta.persistence.*;\n..."
    },
    {
      "path": "src/main/resources/com/example/entity/Person.sql",
      "language": "sql",
      "description": "SQL定義ファイル",
      "content": "FIND_ALL = select ...\nFIND_BY_ID = select ..."
    },
    {
      "path": "src/main/resources/rest-component-configuration.xml",
      "language": "xml",
      "description": "REST用ハンドラキュー設定",
      "content": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<component-configuration ..."
    }
  ],
  "total_files": 4
}
```

#### 4.4.3 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Nablarch Example Resource Response",
  "oneOf": [
    {
      "type": "object",
      "properties": {
        "type": { "const": "example_list" },
        "examples": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "type": { "type": "string" },
              "description": { "type": "string" },
              "app_type": { "type": "string" },
              "file_count": { "type": "integer" },
              "reference_repo": { "type": "string", "format": "uri" },
              "uri": { "type": "string", "format": "uri" }
            },
            "required": ["type", "description", "app_type", "uri"]
          }
        },
        "total_examples": { "type": "integer" }
      },
      "required": ["type", "examples", "total_examples"]
    },
    {
      "type": "object",
      "properties": {
        "type": { "const": "example_detail" },
        "example_type": { "type": "string" },
        "description": { "type": "string" },
        "app_type": { "type": "string" },
        "reference_repo": { "type": "string", "format": "uri" },
        "handler_queue_type": { "type": "string" },
        "key_patterns": { "type": "array", "items": { "type": "string" } },
        "files": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "path": { "type": "string" },
              "language": { "type": "string" },
              "description": { "type": "string" },
              "content": { "type": "string" }
            },
            "required": ["path", "language", "content"]
          }
        },
        "total_files": { "type": "integer" }
      },
      "required": ["type", "example_type", "description", "app_type", "files"]
    }
  ]
}
```

### 4.5 エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| 不正なtype | `{"error": "Unknown example type: {type}", "valid_types": ["rest-api", "web-crud", "batch-db", "messaging-mom"]}` |

---

## 5. config/{name} リソース（UC4対応）

### 5.1 概要

NablarchのXML設定テンプレートを提供する。AIクライアントはこのリソースを使って、正しい設定XMLの雛形を取得し、開発者への提案やvalidate_configツールとの連携に活用する。

### 5.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://config` | 設定テンプレート一覧を返却 |
| `nablarch://config/{name}` | 指定テンプレートのXML設定を返却 |

**パラメータ定義**:

| パラメータ | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `name` | string | テンプレート名（ケバブケース） | `web-component`, `rest-component`, `batch-component`, `web-xml`, `db-connection` |

### 5.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/config-templates.yaml`（既存） | 9テンプレート（XML設定、パラメータ、カスタマイズポイント） |
| Phase 2+（RAG） | `config_index`（pgvector） | 全サンプルプロジェクトのXML設定のセマンティック検索 |

### 5.4 レスポンスデータ構造

#### 5.4.1 テンプレート一覧（`nablarch://config`）

MIMEタイプ: `text/markdown`

```markdown
# Nablarch XML設定テンプレート一覧

| # | テンプレート名 | カテゴリ | アプリタイプ | 説明 |
|---|--------------|---------|------------|------|
| 1 | web-xml | web-xml | web | Webアプリケーション用 web.xml テンプレート |
| 2 | web-component | component | web | Webアプリケーション用コンポーネント設定 |
| 3 | rest-component | component | rest | RESTアプリケーション用コンポーネント設定 |
| 4 | batch-component | component | batch | バッチ用コンポーネント設定 |
| 5 | db-connection | database | common | データベース接続設定 |
| ...

---
*Source: config-templates.yaml*
```

#### 5.4.2 テンプレート詳細（`nablarch://config/{name}`）

MIMEタイプ: `application/xml`

個別テンプレートのレスポンスはXML本文を直接返す。テンプレートの説明・パラメータ情報はXMLコメントとして先頭に付与する。

```xml
<!--
  Nablarch Configuration Template: {name}
  Category: {category}
  App Type: {app_type}
  Description: {description}

  Parameters:
    - {param_name}: {param_description} (default: {default_value})

  Customization Points:
    - {customization_point_1}
    - {customization_point_2}
-->
{template XML本文}
```

### 5.5 エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| 不正なname | 以下のMarkdownを返却 |

```markdown
# Unknown Config Template

Unknown config template: {name}

Valid templates: web-xml, web-component, rest-component, batch-component, db-connection, ...
```

---

## 6. antipattern/{name} リソース（UC6対応）

### 6.1 概要

Nablarch開発でよく見られるアンチパターンとその修正方法を提供する。AIクライアントはコードレビュー（UC6）の際にこのリソースを参照し、規約違反やアンチパターンを検出・修正提案に活用する。

### 6.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://antipattern` | アンチパターン一覧を返却 |
| `nablarch://antipattern/{name}` | 指定アンチパターンの詳細を返却 |

**パラメータ定義**:

| パラメータ | 型 | 説明 | 例 |
|-----------|-----|------|-----|
| `name` | string | アンチパターン名（ケバブケース） | `action-instance-field`, `direct-sql-execution`, `missing-transaction-handler` |

### 6.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/antipattern-catalog.yaml`（新規） | アンチパターン定義 |
| Phase 2+（RAG） | `docs_index`（pgvector） | Fintan等のベストプラクティスドキュメント検索 |

#### 6.3.1 新規ナレッジファイル: antipattern-catalog.yaml

```yaml
# Nablarch Antipattern Catalog
# Nablarch開発でよく見られるアンチパターンと修正方法
# Version: 1.0

antipatterns:
  - name: action-instance-field
    category: thread-safety
    severity: critical
    title: "アクションクラスのインスタンスフィールド"
    description: "アクションクラスにインスタンスフィールドを定義するアンチパターン"
    problem: |
      Nablarchのアクションクラスはシングルトンスコープで管理される。
      インスタンスフィールドを持つと、複数リクエスト間で値が共有され、
      スレッドセーフティが破壊される。
    bad_example: |
      public class UserAction {
          private String cachedValue;  // NG: インスタンスフィールド
          public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
              cachedValue = req.getParam("name");
              return new HttpResponse("/result.jsp");
          }
      }
    good_example: |
      public class UserAction {
          public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
              String value = req.getParam("name");  // OK: ローカル変数
              ctx.setRequestScopedVar("name", value);  // OK: リクエストスコープ
              return new HttpResponse("/result.jsp");
          }
      }
    fix_strategy: "インスタンスフィールドをローカル変数またはExecutionContextのリクエストスコープに移動する"
    related_patterns:
      - action-class-pattern
    references:
      - "https://nablarch.github.io/docs/LATEST/doc/application_framework/application_framework/nablarch/policy.html"

  - name: direct-sql-execution
    category: data-access
    severity: warning
    title: "直接SQL実行（UniversalDao未使用）"
    description: "DbAccessSupportのsearch/update等を直接使用するアンチパターン"
    problem: "UniversalDaoを使用せず、低レベルAPIで直接SQL操作を行うと保守性が低下する"
    bad_example: "SqlResultSet result = search(\"SELECT_USERS\", condition);"
    good_example: "List<Users> result = UniversalDao.findAllBySqlFile(Users.class, \"SELECT_USERS\", condition);"
    fix_strategy: "DbAccessSupportからUniversalDaoへ移行する"
    related_patterns:
      - dao-pattern
    references: []

  - name: missing-transaction-handler
    category: handler-queue
    severity: critical
    title: "トランザクションハンドラの欠落"
    description: "ハンドラキューにTransactionManagementHandlerを含めずにDB操作を行うアンチパターン"
    problem: "トランザクション管理なしのDB操作は、障害時のデータ不整合を引き起こす"
    bad_example: "DbConnectionManagementHandlerのみでTransactionManagementHandlerなし"
    good_example: "DbConnectionManagementHandler + TransactionManagementHandlerをセットで配置"
    fix_strategy: "DbConnectionManagementHandlerの直後にTransactionManagementHandlerを追加する"
    related_patterns:
      - handler-queue-pattern
    references: []
```

### 6.4 レスポンスデータ構造

#### 6.4.1 アンチパターン一覧（`nablarch://antipattern`）

MIMEタイプ: `text/markdown`

```markdown
# Nablarch アンチパターンカタログ

## アンチパターン一覧

| # | 名前 | カテゴリ | 重要度 | 説明 |
|---|------|---------|--------|------|
| 1 | action-instance-field | thread-safety | critical | アクションクラスのインスタンスフィールド |
| 2 | direct-sql-execution | data-access | warning | 直接SQL実行（UniversalDao未使用） |
| 3 | missing-transaction-handler | handler-queue | critical | トランザクションハンドラの欠落 |

## カテゴリ別索引

### thread-safety（スレッドセーフティ）
- action-instance-field

### data-access（データアクセス）
- direct-sql-execution

### handler-queue（ハンドラキュー）
- missing-transaction-handler

---
*Source: antipattern-catalog.yaml*
```

#### 6.4.2 アンチパターン詳細（`nablarch://antipattern/{name}`）

MIMEタイプ: `text/markdown`

```markdown
# {title}

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

## 関連パターン
- {related_pattern}

## 参考
- {reference_url}

---
*Source: antipattern-catalog.yaml*
```

### 6.5 エラーレスポンス

| 条件 | レスポンス |
|------|----------|
| 不正なname | 以下のMarkdownを返却 |

```markdown
# Unknown Antipattern

Unknown antipattern: {name}

Valid antipatterns: action-instance-field, direct-sql-execution, missing-transaction-handler, ...
```

---

## 7. version リソース（UC9対応）

### 7.1 概要

Nablarchフレームワークのバージョン情報、対応プラットフォーム、モジュール一覧を提供する。バージョンアップ支援（UC9）や初学者の環境構築支援に活用する。パラメータなしの単一エンドポイントである。

### 7.2 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://version` | バージョン情報を返却 |

パラメータなし。

### 7.3 データソース

| Phase | データソース | 説明 |
|-------|------------|------|
| Phase 3（静的） | `knowledge/module-catalog.yaml`（既存） | モジュール一覧（21モジュール） |
| Phase 3（静的） | `knowledge/version-info.yaml`（新規） | バージョン情報・プラットフォーム情報 |

#### 7.3.1 新規ナレッジファイル: version-info.yaml

```yaml
# Nablarch Version Information
# Version: 1.0

version_info:
  framework_name: "Nablarch"
  latest_version: "6u3"
  release_date: "2024-09"
  supported_versions:
    - version: "6u3"
      status: "current"
      java_versions: ["17", "21"]
      jakarta_ee_version: "10"
    - version: "5u24"
      status: "maintenance"
      java_versions: ["8", "11"]
      java_ee_version: "8"

  platforms:
    application_server:
      - name: "Apache Tomcat"
        versions: ["10.1"]
      - name: "WildFly"
        versions: ["30"]
      - name: "Open Liberty"
        versions: ["24.0"]
      - name: "WebSphere Liberty"
        versions: ["24.0"]
    database:
      - name: "Oracle Database"
        versions: ["19c", "21c", "23ai"]
      - name: "PostgreSQL"
        versions: ["14", "15", "16"]
      - name: "SQL Server"
        versions: ["2019", "2022"]
      - name: "H2 Database"
        versions: ["2.x"]
    java:
      - name: "Oracle JDK"
        versions: ["17", "21"]
      - name: "Eclipse Temurin"
        versions: ["17", "21"]
    build_tool:
      - name: "Maven"
        versions: ["3.9+"]
      - name: "Gradle"
        versions: ["8.x"]

  bom:
    group_id: "com.nablarch.profile"
    artifact_id: "nablarch-bom"
    version: "6u3"

  links:
    official_docs: "https://nablarch.github.io/docs/LATEST/doc/"
    github_org: "https://github.com/nablarch"
    fintan: "https://fintan.jp/"
    release_notes: "https://nablarch.github.io/docs/LATEST/doc/about_nablarch/versionup_policy.html"
```

### 7.4 レスポンスデータ構造

MIMEタイプ: `application/json`

```json
{
  "type": "version_info",
  "framework_name": "Nablarch",
  "latest_version": "6u3",
  "release_date": "2024-09",
  "supported_versions": [
    {
      "version": "6u3",
      "status": "current",
      "java_versions": ["17", "21"],
      "jakarta_ee_version": "10"
    },
    {
      "version": "5u24",
      "status": "maintenance",
      "java_versions": ["8", "11"],
      "java_ee_version": "8"
    }
  ],
  "platforms": {
    "application_server": [
      { "name": "Apache Tomcat", "versions": ["10.1"] },
      { "name": "WildFly", "versions": ["30"] },
      { "name": "Open Liberty", "versions": ["24.0"] },
      { "name": "WebSphere Liberty", "versions": ["24.0"] }
    ],
    "database": [
      { "name": "Oracle Database", "versions": ["19c", "21c", "23ai"] },
      { "name": "PostgreSQL", "versions": ["14", "15", "16"] },
      { "name": "SQL Server", "versions": ["2019", "2022"] },
      { "name": "H2 Database", "versions": ["2.x"] }
    ],
    "java": [
      { "name": "Oracle JDK", "versions": ["17", "21"] },
      { "name": "Eclipse Temurin", "versions": ["17", "21"] }
    ],
    "build_tool": [
      { "name": "Maven", "versions": ["3.9+"] },
      { "name": "Gradle", "versions": ["8.x"] }
    ]
  },
  "bom": {
    "group_id": "com.nablarch.profile",
    "artifact_id": "nablarch-bom",
    "version": "6u3"
  },
  "modules": [
    {
      "name": "nablarch-core",
      "artifact_id": "nablarch-core",
      "category": "core",
      "description": "Nablarchのコアモジュール。基本的なインターフェース・ユーティリティを提供",
      "key_class_count": 3
    }
  ],
  "total_modules": 21,
  "links": {
    "official_docs": "https://nablarch.github.io/docs/LATEST/doc/",
    "github_org": "https://github.com/nablarch",
    "fintan": "https://fintan.jp/",
    "release_notes": "https://nablarch.github.io/docs/LATEST/doc/about_nablarch/versionup_policy.html"
  }
}
```

#### 7.4.1 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Nablarch Version Resource Response",
  "type": "object",
  "properties": {
    "type": { "const": "version_info" },
    "framework_name": { "type": "string" },
    "latest_version": { "type": "string" },
    "release_date": { "type": "string" },
    "supported_versions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "version": { "type": "string" },
          "status": { "type": "string", "enum": ["current", "maintenance", "eol"] },
          "java_versions": { "type": "array", "items": { "type": "string" } }
        },
        "required": ["version", "status", "java_versions"]
      }
    },
    "platforms": { "type": "object" },
    "bom": {
      "type": "object",
      "properties": {
        "group_id": { "type": "string" },
        "artifact_id": { "type": "string" },
        "version": { "type": "string" }
      },
      "required": ["group_id", "artifact_id", "version"]
    },
    "modules": { "type": "array" },
    "total_modules": { "type": "integer" },
    "links": { "type": "object" }
  },
  "required": ["type", "framework_name", "latest_version", "supported_versions", "platforms", "bom", "modules", "links"]
}
```

### 7.5 エラーレスポンス

本リソースはパラメータなしの単一エンドポイントのため、正常系のみ。内部エラー時はMCPプロトコルレベルのエラー（-32603 Internal error）を返す。

---

## 8. 実装設計

### 8.1 クラス構成

```
resources/
├── HandlerResourceProvider.java     # Phase 1 実装済み — handler/{app_type}
├── GuideResourceProvider.java       # Phase 1 実装済み — guide/{topic}
├── HandlerResource.java             # Phase 1 スタブ — handler関連ユーティリティ
├── ApiSpecResource.java             # Phase 1 スタブ → Phase 3 実装
├── ApiResourceProvider.java         # Phase 3 NEW — api/{module}/{class}
├── PatternResourceProvider.java     # Phase 3 NEW — pattern/{name}
├── ExampleResourceProvider.java     # Phase 3 NEW — example/{type}
├── ConfigResourceProvider.java      # Phase 3 NEW — config/{name}
├── AntipatternResourceProvider.java # Phase 3 NEW — antipattern/{name}
└── VersionResourceProvider.java     # Phase 3 NEW — version
```

### 8.2 新規プロバイダの共通設計

全プロバイダは既存の `GuideResourceProvider` と同じパターンに従う:

```java
@Component
public class XxxResourceProvider {

    private List<Map<String, Object>> data;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // knowledge/ 配下のYAMLファイルを読み込み
    }

    // 一覧取得メソッド
    public String getListXxx();

    // 個別取得メソッド
    public String getXxx(String key);
}
```

### 8.3 各プロバイダの詳細

#### 8.3.1 ApiResourceProvider

```java
@Component
public class ApiResourceProvider {

    private List<Map<String, Object>> modules;      // module-catalog.yaml
    private List<Map<String, Object>> apiPatterns;   // api-patterns.yaml

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** モジュール一覧をJSON形式で返す */
    public String getModuleList();

    /** 指定モジュールのクラス一覧をJSON形式で返す */
    public String getClassList(String moduleKey);

    /** 指定クラスの詳細をJSON形式で返す */
    public String getClassDetail(String moduleKey, String className);
}
```

- `module_key` は `artifact_id` から `nablarch-` プレフィックスを除いた値（例: `nablarch-common-dao` → `common-dao`）
- JSON生成には `ObjectMapper` のシリアライズ機能を使用

#### 8.3.2 PatternResourceProvider

```java
@Component
public class PatternResourceProvider {

    private List<Map<String, Object>> patterns;  // design-patterns.yaml

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** パターン一覧をMarkdown形式で返す */
    public String getPatternList();

    /** 指定パターンの詳細をMarkdown形式で返す */
    public String getPatternDetail(String name);
}
```

#### 8.3.3 ExampleResourceProvider

```java
@Component
public class ExampleResourceProvider {

    private List<Map<String, Object>> examples;  // example-catalog.yaml (新規)

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** サンプル一覧をJSON形式で返す */
    public String getExampleList();

    /** 指定タイプのサンプル詳細をJSON形式で返す */
    public String getExampleDetail(String type);
}
```

#### 8.3.4 ConfigResourceProvider

```java
@Component
public class ConfigResourceProvider {

    private List<Map<String, Object>> templates;  // config-templates.yaml (既存)

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** テンプレート一覧をMarkdown形式で返す */
    public String getTemplateList();

    /** 指定テンプレートのXMLをコメント付きで返す */
    public String getTemplate(String name);
}
```

#### 8.3.5 AntipatternResourceProvider

```java
@Component
public class AntipatternResourceProvider {

    private List<Map<String, Object>> antipatterns;  // antipattern-catalog.yaml (新規)

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** アンチパターン一覧をMarkdown形式で返す */
    public String getAntipatternList();

    /** 指定アンチパターンの詳細をMarkdown形式で返す */
    public String getAntipatternDetail(String name);
}
```

#### 8.3.6 VersionResourceProvider

```java
@Component
public class VersionResourceProvider {

    private Map<String, Object> versionInfo;     // version-info.yaml (新規)
    private List<Map<String, Object>> modules;   // module-catalog.yaml (既存)

    @PostConstruct
    public void init() throws IOException { /* ... */ }

    /** バージョン情報をJSON形式で返す */
    public String getVersionInfo();
}
```

### 8.4 新規ナレッジYAMLファイル

Phase 3で追加が必要なナレッジファイル:

| ファイル | 説明 | 参照元Resource |
|---------|------|--------------|
| `knowledge/example-catalog.yaml` | サンプルアプリケーション構成（4タイプ） | example/{type} |
| `knowledge/antipattern-catalog.yaml` | アンチパターンカタログ | antipattern/{name} |
| `knowledge/version-info.yaml` | バージョン・プラットフォーム情報 | version |

既存ファイルの再利用:

| ファイル | 追加利用元Resource |
|---------|-----------------|
| `knowledge/module-catalog.yaml` | api/{module}/{class}, version |
| `knowledge/api-patterns.yaml` | api/{module}/{class} |
| `knowledge/design-patterns.yaml` | pattern/{name} |
| `knowledge/config-templates.yaml` | config/{name} |

### 8.5 McpServerConfig統合

```java
@Bean
public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
        HandlerResourceProvider handlerProvider,    // Phase 1
        GuideResourceProvider guideProvider,        // Phase 1
        ApiResourceProvider apiProvider,            // Phase 3 NEW
        PatternResourceProvider patternProvider,    // Phase 3 NEW
        ExampleResourceProvider exampleProvider,    // Phase 3 NEW
        ConfigResourceProvider configProvider,      // Phase 3 NEW
        AntipatternResourceProvider antipatternProvider, // Phase 3 NEW
        VersionResourceProvider versionProvider) {  // Phase 3 NEW

    List<McpServerFeatures.SyncResourceSpecification> resources = new ArrayList<>();

    // Phase 1: handler (6) + guide (6) = 12
    // Phase 3: api (1+N) + pattern (1+N) + example (1+N) + config (1+N)
    //        + antipattern (1+N) + version (1)

    return resources;
}
```

### 8.6 Resource登録方式

MCPプロトコルでは `resources/list` で全Resource URIを列挙する必要がある。Phase 3のResourceは以下の方式で登録する:

| Resource | 登録方式 | 理由 |
|----------|---------|------|
| `nablarch://api` | 静的登録 | 一覧エンドポイント |
| `nablarch://api/{module}` | 動的（URIテンプレート） | モジュール数に依存 |
| `nablarch://api/{module}/{class}` | 動的（URIテンプレート） | クラス数に依存 |
| `nablarch://pattern` | 静的登録 | 一覧エンドポイント |
| `nablarch://pattern/{name}` | パターン数分を静的登録 | 11パターン（管理可能な数） |
| `nablarch://example` | 静的登録 | 一覧エンドポイント |
| `nablarch://example/{type}` | 4タイプ分を静的登録 | 4タイプ（固定） |
| `nablarch://config` | 静的登録 | 一覧エンドポイント |
| `nablarch://config/{name}` | テンプレート数分を静的登録 | 9テンプレート（管理可能な数） |
| `nablarch://antipattern` | 静的登録 | 一覧エンドポイント |
| `nablarch://antipattern/{name}` | アンチパターン数分を静的登録 | 数十件を想定 |
| `nablarch://version` | 静的登録 | 単一エンドポイント |

**注**: MCP SDK 0.17.xではResource URIテンプレート（RFC 6570）がサポートされているため、`api/{module}/{class}` は `nablarch://api/{module}/{class}` をURIテンプレートとして登録し、実行時にパス解析でパラメータを抽出する方式も検討可能。

---

## 9. Phase移行計画

### 9.1 Phase 3（静的ナレッジ）→ Phase 2+（RAG統合）

各ResourceのPhase別データソース進化:

| Resource | Phase 3（現在） | Phase 2+（RAG統合後） |
|----------|---------------|---------------------|
| api/{module}/{class} | module-catalog.yaml + api-patterns.yaml | javadoc_index（pgvector）によるJavadoc全件検索 |
| pattern/{name} | design-patterns.yaml | docs_index（pgvector）によるFintanパターン検索 |
| example/{type} | example-catalog.yaml | code_index（pgvector）による113リポジトリのコード検索 |
| config/{name} | config-templates.yaml | config_index（pgvector）による実設定XML検索 |
| antipattern/{name} | antipattern-catalog.yaml | docs_index（pgvector）による規約・ベストプラクティス検索 |
| version | version-info.yaml + module-catalog.yaml | 同左（バージョン情報は静的で十分） |

### 9.2 RAG統合時の拡張ポイント

各プロバイダに `RAGEngine` への依存を追加し、静的データとRAG検索結果をマージする:

```java
@Component
public class ApiResourceProvider {

    private final RAGEngine ragEngine;  // Phase 2+ で注入

    public String getClassDetail(String moduleKey, String className) {
        // 1. 静的データから基本情報を取得
        Map<String, Object> staticData = findFromStatic(moduleKey, className);

        // 2. RAGで補完情報を検索（Phase 2+）
        if (ragEngine != null) {
            List<RAGResult> ragResults = ragEngine.search(
                className + " Javadoc メソッド", "javadoc");
            mergeRagResults(staticData, ragResults);
        }

        // 3. JSON整形して返却
        return formatJson(staticData);
    }
}
```

---

## 10. 参考文献

- [Phase 1 Resource URI設計書](06_resource-uri-design.md) — Phase 1のResource設計（handler/*, guide/*）
- [知識ベース設計書](03_knowledge-base.md) — YAMLスキーマ定義
- [アーキテクチャ設計書](../architecture.md) — MCP Resource仕様（8タイプの定義元）
- [ユースケース集](../use-cases.md) — UC2, UC4, UC6, UC8, UC9, UC11, UC12の詳細
- [API仕様書](../api-specification.md) — 既存Resource仕様・データソース
- [MCP仕様: Resources](https://spec.modelcontextprotocol.io/specification/server/resources/) — MCPプロトコルのResource仕様

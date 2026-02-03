# generate_code Tool 詳細設計書

> **WBS番号**: 3.1.2
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru2 (subtask_061)
> **関連文書**: architecture.md §5.1 Tool 3, use-cases.md UC3/UC12, 13_semantic-search-tool.md
> **対応ユースケース**: UC3（コード自動生成）, UC12（REST APIスキャフォールディング）

---

## 目次

1. [概要](#1-概要)
2. [Tool定義](#2-tool定義)
3. [入力スキーマ](#3-入力スキーマ)
4. [出力スキーマ](#4-出力スキーマ)
5. [コード生成テンプレート設計](#5-コード生成テンプレート設計)
6. [RAG連携フロー](#6-rag連携フロー)
7. [テンプレートエンジン設計](#7-テンプレートエンジン設計)
8. [アプリタイプ別生成ロジック](#8-アプリタイプ別生成ロジック)
9. [生成コードの品質保証](#9-生成コードの品質保証)
10. [エラーハンドリング](#10-エラーハンドリング)
11. [MCP Tool登録](#11-mcp-tool登録)
12. [設定パラメータ](#12-設定パラメータ)

---

## 1. 概要

### 1.1 目的

本設計書は、Phase 3で実装する `generate_code` MCP Toolの詳細設計を定義する。
このToolはNablarch準拠のJavaコード（Action、Form、SQL定義、Entity、Handler、Interceptor）を
自動生成し、開発者がボイラープレートコードの記述から解放されてビジネスロジックに集中できるようにする。

### 1.2 スコープ

- `generate_code` Toolの入出力スキーマ定義
- 6種のコード生成テンプレート設計（action / form / sql / entity / handler / interceptor）
- RAG連携によるコーディング規約・パターン検索フロー
- テンプレートエンジンの選定と設計
- アプリタイプ別（Web / REST / Batch / Messaging）の生成ロジック
- 生成コードの品質保証方針
- エラーハンドリング方針

### 1.3 対応ユースケース

| ユースケース | 説明 | 主な入力 | 主な出力 |
|------------|------|---------|---------|
| UC3 | バッチアプリケーションコード生成 | type, name, app_type="batch", specifications | Action + DataReader + Entity + SQL + XML |
| UC12 | REST APIスキャフォールディング | type, name, app_type="rest", specifications | Action + Form + Entity + SQL + routes.xml |

---

## 2. Tool定義

### 2.1 Tool概要

| 項目 | 値 |
|------|-----|
| Tool名 | `generate_code` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `CodeGenerationTool` |
| 説明 | Nablarch準拠のコード（Action、Form、SQL定義等）を生成する。Nablarchのコーディング規約に従ったボイラープレートコードを自動生成し、開発者のビジネスロジック実装を支援する |
| カテゴリ | コード生成系Tool |

### 2.2 他Toolとの関係

| Tool | 関係 | 連携パターン |
|------|------|------------|
| `semantic_search` | RAG連携 | コーディング規約・APIパターンの検索に使用 |
| `search_api` | フォールバック | RAG未稼働時に静的知識ベースから規約を検索 |
| `validate_config` | 後工程連携 | 生成したXML設定の検証に使用（クライアント側で連鎖呼び出し） |
| `design_handler_queue` | 後工程連携 | 生成コードに対応するハンドラキュー設計（クライアント側で連鎖呼び出し） |

---

## 3. 入力スキーマ

### 3.1 パラメータ定義

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `type` | string | ○ | — | 生成対象: "action", "form", "sql", "entity", "handler", "interceptor" |
| `name` | string | ○ | — | 生成するクラス/ファイルの名前（例: "UserRegistration", "Product"） |
| `app_type` | string | × | "web" | アプリケーションタイプ: "web", "rest", "batch", "messaging" |
| `specifications` | object | × | null | タイプ固有の詳細パラメータ |

### 3.2 specifications オブジェクト（タイプ別）

#### action 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `routing_path` | string | ルーティングパス（例: "/api/products"） |
| `methods` | string[] | 生成するメソッド名（例: ["list", "show", "create", "update", "delete"]） |
| `entity_name` | string | 関連Entityクラス名（例: "Product"） |
| `use_universal_dao` | boolean | UniversalDaoを使用するか（デフォルト: true） |

#### form 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `fields` | object[] | フィールド定義の配列 |
| `fields[].name` | string | フィールド名（例: "userName"） |
| `fields[].type` | string | Java型（例: "String", "Long", "Integer"） |
| `fields[].validations` | string[] | バリデーション（例: ["@NotNull", "@Size(max=100)"]） |

#### sql 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `queries` | string[] | 生成するクエリ種別（例: ["find_by_id", "find_all", "insert", "update", "delete"]） |
| `table_name` | string | 対象テーブル名（例: "USERS"） |
| `columns` | object[] | カラム定義（例: [{name: "USER_ID", type: "BIGINT", pk: true}]） |

#### entity 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `table_name` | string | テーブル名（例: "PRODUCTS"） |
| `fields` | object[] | フィールド定義の配列 |
| `fields[].name` | string | フィールド名（例: "productId"） |
| `fields[].type` | string | Java型（例: "Long", "String"） |
| `fields[].column` | string | カラム名（例: "PRODUCT_ID"） |
| `fields[].pk` | boolean | 主キーか否か |
| `fields[].version` | boolean | 楽観ロック用バージョンカラムか否か |

#### handler 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `input_type` | string | 入力型（例: "HttpRequest"） |
| `output_type` | string | 出力型（例: "HttpResponse"） |
| `description` | string | ハンドラの役割説明 |

#### interceptor 用 specifications

| フィールド | 型 | 説明 |
|----------|-----|------|
| `annotation_name` | string | インターセプタアノテーション名（例: "AuditLog"） |
| `target` | string | 適用対象: "action", "handler" |
| `description` | string | インターセプタの役割説明 |

### 3.3 JSON Schema

```json
{
  "type": "object",
  "properties": {
    "type": {
      "type": "string",
      "enum": ["action", "form", "sql", "entity", "handler", "interceptor"],
      "description": "Type of code to generate."
    },
    "name": {
      "type": "string",
      "description": "Name for the generated class/file (e.g. 'UserRegistration', 'Product')."
    },
    "app_type": {
      "type": "string",
      "enum": ["web", "rest", "batch", "messaging"],
      "default": "web",
      "description": "Nablarch application type."
    },
    "specifications": {
      "type": "object",
      "description": "Type-specific parameters (routing path, fields, queries, etc.)."
    }
  },
  "required": ["type", "name"]
}
```

---

## 4. 出力スキーマ

### 4.1 レスポンス構造

```json
{
  "files": [
    {
      "path": "src/main/java/com/example/action/ProductAction.java",
      "content": "package com.example.action;\n\nimport ...",
      "language": "java"
    }
  ],
  "conventions_applied": [
    "アクションクラスはシングルトン — インスタンスフィールド禁止",
    "メソッド名に 'do' プレフィックスを付与（Webアプリの場合）",
    "@InjectForm + @OnError による宣言的バリデーション"
  ],
  "dependencies": [
    "nablarch-fw-web",
    "nablarch-common-dao"
  ]
}
```

### 4.2 MCP Tool応答フォーマット

MCPツールはテキスト形式で応答する。JSON構造は内部的に整形し、AIアシスタントが解釈しやすいMarkdown形式で返却する。

```
## 生成結果: ProductAction (rest/action)

### 適用されたNablarch規約
- アクションクラスはシングルトン — インスタンスフィールド禁止
- JAX-RSアノテーションによるルーティング
- UniversalDaoによるCRUD操作

### 必要な依存モジュール
- nablarch-fw-jaxrs
- nablarch-common-dao

---

### ファイル 1: ProductAction.java
パス: `src/main/java/com/example/action/ProductAction.java`

```java
package com.example.action;
// ... 生成されたコード ...
```

---

### ファイル 2: ProductForm.java
パス: `src/main/java/com/example/form/ProductForm.java`
// ...
```

---

## 5. コード生成テンプレート設計

### 5.1 テンプレート一覧

| テンプレートID | 生成対象 | 対応app_type | ファイル数 | 説明 |
|-------------|---------|------------|----------|------|
| TMPL-ACTION-WEB | Webアクション | web | 1 | HttpRequest/HttpResponse + @InjectForm + @OnError |
| TMPL-ACTION-REST | RESTアクション | rest | 1 | JAX-RSアノテーション + JSON応答 |
| TMPL-ACTION-BATCH | バッチアクション | batch | 1-2 | BatchAction<SqlRow> + DataReader（オプション） |
| TMPL-ACTION-MSG | メッセージングアクション | messaging | 1 | MessagingAction |
| TMPL-FORM | フォームBean | web, rest | 1 | BeanValidationアノテーション付き |
| TMPL-SQL | SQL定義ファイル | 全タイプ | 1 | Nablarch SQL定義形式 |
| TMPL-ENTITY | Entityクラス | 全タイプ | 1 | @Entity + @Table + @Column + @Version |
| TMPL-HANDLER | カスタムハンドラ | 全タイプ | 1 | Handler<I, O>実装 |
| TMPL-INTERCEPTOR | インターセプタ | 全タイプ | 2 | アノテーション + Interceptor実装 |

### 5.2 テンプレート詳細

#### 5.2.1 TMPL-ACTION-WEB（Webアクション）

```java
package ${package}.action;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.interceptor.InjectForm;
import nablarch.fw.web.interceptor.OnError;
import nablarch.common.dao.UniversalDao;

/**
 * ${name}アクション。
 *
 * <p>${description}</p>
 */
public class ${name}Action {

    /**
     * 一覧表示。
     *
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @return HTTPレスポンス
     */
    public HttpResponse doList(HttpRequest request, ExecutionContext context) {
        // TODO: ビジネスロジックを実装
        return new HttpResponse("/WEB-INF/view/${pathName}/list.jsp");
    }

    /**
     * 登録処理。
     *
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @return HTTPレスポンス
     */
    @InjectForm(form = ${name}Form.class, prefix = "form")
    @OnError(type = ApplicationException.class,
             path = "/WEB-INF/view/${pathName}/input.jsp")
    public HttpResponse doRegister(HttpRequest request, ExecutionContext context) {
        ${name}Form form = context.getRequestScopedVar("form");
        ${entityName} entity = new ${entityName}();
        BeanUtil.copy(form, entity);
        UniversalDao.insert(entity);
        return new HttpResponse("redirect:///action/${pathName}/list");
    }
}
```

#### 5.2.2 TMPL-ACTION-REST（RESTアクション）

```java
package ${package}.action;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import nablarch.common.dao.UniversalDao;
import nablarch.fw.ExecutionContext;
import nablarch.fw.jaxrs.EntityResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * ${name} RESTアクション。
 */
public class ${name}Action {

    /**
     * 一覧取得。
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EntityResponse findAll(HttpRequest request, ExecutionContext context) {
        return new EntityResponse(HttpResponse.Status.OK.getStatusCode(),
                UniversalDao.findAll(${entityName}.class));
    }

    /**
     * 1件取得。
     */
    @GET
    @Path("/{${idField}}")
    @Produces(MediaType.APPLICATION_JSON)
    public EntityResponse findById(HttpRequest request, ExecutionContext context) {
        String id = request.getParam("${idField}")[0];
        return new EntityResponse(HttpResponse.Status.OK.getStatusCode(),
                UniversalDao.findById(${entityName}.class, Long.parseLong(id)));
    }

    /**
     * 新規登録。
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Valid
    public HttpResponse create(HttpRequest request, ExecutionContext context) {
        ${name}Form form = context.getRequestScopedVar("form");
        ${entityName} entity = new ${entityName}();
        BeanUtil.copy(form, entity);
        UniversalDao.insert(entity);
        return new HttpResponse(HttpResponse.Status.CREATED.getStatusCode());
    }

    /**
     * 更新。
     */
    @PUT
    @Path("/{${idField}}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Valid
    public HttpResponse update(HttpRequest request, ExecutionContext context) {
        ${name}Form form = context.getRequestScopedVar("form");
        ${entityName} entity = UniversalDao.findById(${entityName}.class,
                Long.parseLong(request.getParam("${idField}")[0]));
        BeanUtil.copy(form, entity);
        UniversalDao.update(entity);
        return new HttpResponse(HttpResponse.Status.OK.getStatusCode());
    }

    /**
     * 削除。
     */
    @DELETE
    @Path("/{${idField}}")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse delete(HttpRequest request, ExecutionContext context) {
        ${entityName} entity = UniversalDao.findById(${entityName}.class,
                Long.parseLong(request.getParam("${idField}")[0]));
        UniversalDao.delete(entity);
        return new HttpResponse(HttpResponse.Status.NO_CONTENT.getStatusCode());
    }
}
```

#### 5.2.3 TMPL-ACTION-BATCH（バッチアクション）

```java
package ${package}.action;

import nablarch.core.db.statement.SqlRow;
import nablarch.fw.DataReader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.action.BatchAction;
import nablarch.common.dao.UniversalDao;

/**
 * ${name}バッチアクション。
 *
 * <p>${description}</p>
 */
public class ${name}Action extends BatchAction<SqlRow> {

    /**
     * データリーダを生成する。
     *
     * @param context 実行コンテキスト
     * @return データリーダ
     */
    @Override
    public DataReader<SqlRow> createReader(ExecutionContext context) {
        return new ${name}Reader();
    }

    /**
     * 1件分の業務処理を実行する。
     *
     * @param inputData 入力データ
     * @param context 実行コンテキスト
     * @return 処理結果
     */
    @Override
    public Result handle(SqlRow inputData, ExecutionContext context) {
        // TODO: ビジネスロジックを実装
        return new Result.Success();
    }
}
```

#### 5.2.4 TMPL-FORM（フォームBean）

```java
package ${package}.form;

import jakarta.validation.constraints.*;
import java.io.Serializable;

/**
 * ${name}フォーム。
 */
public class ${name}Form implements Serializable {

#foreach($field in $fields)
    /** ${field.description} */
    ${field.validationAnnotations}
    private ${field.type} ${field.name};

#end

#foreach($field in $fields)
    /**
     * ${field.name}を取得する。
     *
     * @return ${field.name}
     */
    public ${field.type} get${field.capitalizedName}() {
        return ${field.name};
    }

    /**
     * ${field.name}を設定する。
     *
     * @param ${field.name} ${field.name}
     */
    public void set${field.capitalizedName}(${field.type} ${field.name}) {
        this.${field.name} = ${field.name};
    }

#end
}
```

#### 5.2.5 TMPL-SQL（SQL定義ファイル）

```sql
-- ${name} SQL定義ファイル
-- テーブル: ${tableName}

#if($queries.contains("find_all"))
FIND_ALL =
SELECT
#foreach($col in $columns)
    ${col.name}#if($foreach.hasNext),#end

#end
FROM
    ${tableName}
ORDER BY
    ${pkColumn}
#end

#if($queries.contains("find_by_id"))
FIND_BY_ID =
SELECT
#foreach($col in $columns)
    ${col.name}#if($foreach.hasNext),#end

#end
FROM
    ${tableName}
WHERE
    ${pkColumn} = :${pkField}
#end

#if($queries.contains("insert"))
-- INSERT/UPDATE/DELETE は UniversalDao が自動生成するため、
-- 明示的なSQL定義は通常不要です。
-- カスタムクエリが必要な場合のみ以下を使用してください。
#end
```

#### 5.2.6 TMPL-ENTITY（Entityクラス）

```java
package ${package}.entity;

import jakarta.persistence.*;

/**
 * ${name}エンティティ。
 *
 * <p>テーブル: ${tableName}</p>
 */
@Entity
@Table(name = "${tableName}")
public class ${name} {

#foreach($field in $fields)
#if($field.pk)
    @Id
#if($field.generatedValue)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
#end
#end
#if($field.version)
    @Version
#end
    @Column(name = "${field.column}")
    public ${field.type} ${field.name};

#end
}
```

#### 5.2.7 TMPL-HANDLER（カスタムハンドラ）

```java
package ${package}.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;

/**
 * ${name}ハンドラ。
 *
 * <p>${description}</p>
 */
public class ${name}Handler implements Handler<${inputType}, ${outputType}> {

    /**
     * ハンドラ処理を実行する。
     *
     * @param input 入力データ
     * @param context 実行コンテキスト
     * @return 処理結果
     */
    @Override
    public ${outputType} handle(${inputType} input, ExecutionContext context) {
        // --- 前処理 ---
        // TODO: ハンドラの前処理を実装

        // 後続ハンドラの実行
        ${outputType} result = context.handleNext(input);

        // --- 後処理 ---
        // TODO: ハンドラの後処理を実装

        return result;
    }
}
```

#### 5.2.8 TMPL-INTERCEPTOR（インターセプタ）

**アノテーション定義:**

```java
package ${package}.interceptor;

import nablarch.fw.interceptor.Interceptor;
import java.lang.annotation.*;

/**
 * ${annotationName}インターセプタアノテーション。
 *
 * <p>${description}</p>
 */
@Interceptor(${annotationName}Interceptor.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ${annotationName} {
}
```

**インターセプタ実装:**

```java
package ${package}.interceptor;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;

/**
 * ${annotationName}インターセプタ。
 *
 * <p>${description}</p>
 */
public class ${annotationName}Interceptor
        implements Handler<Object, Object> {

    /**
     * インターセプト処理を実行する。
     *
     * @param input 入力データ
     * @param context 実行コンテキスト
     * @return 処理結果
     */
    @Override
    public Object handle(Object input, ExecutionContext context) {
        // --- 前処理 ---
        // TODO: インターセプタの前処理を実装

        // 元のメソッドの実行
        Object result = context.handleNext(input);

        // --- 後処理 ---
        // TODO: インターセプタの後処理を実装

        return result;
    }
}
```

---

## 6. RAG連携フロー

### 6.1 全体フロー

```
generate_code Tool 呼び出し
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 1: パラメータ解析・バリデーション                       │
│                                                           │
│  入力: type, name, app_type, specifications               │
│  処理: パラメータ検証 + テンプレートID決定                    │
│  出力: 有効パラメータセット + テンプレートID                  │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 2: RAG検索（コーディング規約・パターン取得）             │
│                                                           │
│  ┌────────────────────────────────────────────┐          │
│  │ RAG検索クエリ構築                           │          │
│  │  - "Nablarch {type} {app_type} コーディング │          │
│  │    規約 ベストプラクティス"                   │          │
│  │  - "Nablarch {entityName} パターン 実装例"  │          │
│  └────────────────┬───────────────────────────┘          │
│                    │                                      │
│         ┌──────────┴──────────┐                          │
│         ▼                     ▼                           │
│  ┌─────────────┐      ┌─────────────┐                   │
│  │ RAGパイプライン│      │ 静的知識     │                   │
│  │ (Phase 2+)  │      │ ベース       │                   │
│  │ semantic    │      │ (Phase 1)   │                   │
│  │ _search     │      │ search_api  │                   │
│  └──────┬──────┘      └──────┬──────┘                   │
│         │                     │                           │
│         └──────────┬──────────┘                           │
│                    ▼                                      │
│  コーディング規約 + APIパターン + 設計パターン               │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 3: テンプレート展開                                    │
│                                                           │
│  テンプレートID + パラメータ + RAG結果                       │
│       │                                                   │
│       ▼                                                   │
│  ┌────────────────────────────────────────────┐          │
│  │ テンプレートエンジン (Mustache)              │          │
│  │  - テンプレート選択                          │          │
│  │  - 変数バインド                             │          │
│  │  - RAGから取得した規約を反映                  │          │
│  │  - コード生成                               │          │
│  └────────────────┬───────────────────────────┘          │
│                    │                                      │
│  出力: 生成済みJavaコード文字列                              │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 4: 品質チェック                                       │
│                                                           │
│  ┌────────────────────────────────────────────┐          │
│  │ 静的検証                                    │          │
│  │  - import文の整合性                          │          │
│  │  - Nablarch規約準拠チェック                   │          │
│  │    - インスタンスフィールド不使用              │          │
│  │    - 適切なアノテーション使用                  │          │
│  │    - パッケージ命名規約                       │          │
│  │  - アプリタイプとテンプレートの整合性           │          │
│  └────────────────────────────────────────────┘          │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 5: 結果整形                                          │
│                                                           │
│  - 生成コードをMarkdown形式に整形                           │
│  - 適用された規約一覧を付与                                 │
│  - 必要な依存モジュール一覧を付与                            │
│  - 推奨ファイルパスを付与                                   │
└──────────────────────────────────────────────────────────┘
```

### 6.2 RAG検索クエリの構築ルール

| type | 検索クエリ | 期待する検索結果 |
|------|----------|---------------|
| action | "Nablarch {app_type} アクションクラス コーディング規約 パターン" | アクションクラスの命名規則、インターセプタ使用方法、戻り値パターン |
| form | "Nablarch フォーム BeanValidation バリデーション規約" | フォームクラスの設計方針、バリデーションアノテーション使用方法 |
| sql | "Nablarch SQL定義ファイル 規約 UniversalDao" | SQL定義ファイルの記述形式、命名規約 |
| entity | "Nablarch Entity JPA アノテーション UniversalDao" | Entity設計規約、@Table/@Column使用方法 |
| handler | "Nablarch カスタムハンドラ 実装 Handler<I,O>" | ハンドラ実装パターン、handleNext呼び出し規約 |
| interceptor | "Nablarch インターセプタ @Interceptor アノテーション" | インターセプタ実装パターン |

### 6.3 Phase 1知識ベースとのフォールバック

RAGパイプラインが利用できない場合（Phase 2未稼働、DB障害等）、以下の静的知識ベースを使用する。

| 知識ベースファイル | 取得情報 |
|----------------|---------|
| api-patterns.yaml | アクションクラスパターン、フォームパターン、SQL/Entity使用例 |
| design-patterns.yaml | アクションクラスパターン、ハンドラキューパターン、インターセプタパターン |
| config-templates.yaml | XML設定テンプレート |
| module-catalog.yaml | 依存モジュール情報 |

---

## 7. テンプレートエンジン設計

### 7.1 テンプレートエンジン選定

| 候補 | 特徴 | 評価 |
|------|------|------|
| **Mustache (JMustache)** | ロジックレス、軽量、Java標準ライブラリなし | **採用** |
| FreeMarker | 高機能、複雑なテンプレート対応 | ×（過剰機能） |
| Velocity | Apache製、歴史が長い | ×（メンテナンス低下） |
| StringTemplate | ANTLRベース、厳密な分離 | ×（学習コスト高） |
| String.format / StringBuilder | 依存なし、最軽量 | ×（保守性低） |

**採用理由**: JMustache

1. **ロジックレス設計** — テンプレートにビジネスロジックが混入しない。生成ロジックはJava側で完結する
2. **軽量** — jar1つ（約50KB）、外部依存なし
3. **十分な機能** — 変数展開、セクション（条件・ループ）、パーシャル（テンプレート分割）で要件を満たす
4. **Javaエコシステムとの親和性** — Spring Boot環境で容易に統合可能

### 7.2 テンプレート管理

```
src/main/resources/templates/
├── action/
│   ├── web-action.mustache
│   ├── rest-action.mustache
│   ├── batch-action.mustache
│   └── messaging-action.mustache
├── form/
│   └── form.mustache
├── sql/
│   └── sql-definition.mustache
├── entity/
│   └── entity.mustache
├── handler/
│   └── handler.mustache
└── interceptor/
    ├── annotation.mustache
    └── interceptor.mustache
```

### 7.3 テンプレートコンテキスト構築

テンプレートエンジンに渡すコンテキスト（変数マップ）は `TemplateContextBuilder` で構築する。

```java
package com.tis.nablarch.mcp.codegen;

import java.util.Map;

/**
 * テンプレートコンテキストビルダー。
 *
 * <p>入力パラメータ、RAG検索結果、命名規約を統合して
 * テンプレートエンジンに渡すコンテキストマップを構築する。</p>
 */
public class TemplateContextBuilder {

    /**
     * テンプレートコンテキストを構築する。
     *
     * @param type 生成対象タイプ
     * @param name クラス/ファイル名
     * @param appType アプリケーションタイプ
     * @param specifications タイプ固有パラメータ
     * @param ragConventions RAGから取得した規約情報
     * @return テンプレートコンテキストマップ
     */
    public Map<String, Object> build(
            String type, String name, String appType,
            Map<String, Object> specifications,
            List<String> ragConventions) {
        // パッケージ名の導出
        // パス名の導出（CamelCase → lowercase）
        // Entity名の推定
        // フィールド情報の変換
        // etc.
    }
}
```

### 7.4 命名規約

| 要素 | 規約 | 例 |
|------|------|-----|
| Actionクラス名 | `{Name}Action` | `ProductAction` |
| Formクラス名 | `{Name}Form` | `ProductForm` |
| Entityクラス名 | `{Name}` | `Product` |
| SQL定義ファイル | `{Name}.sql` | `Product.sql` |
| Handlerクラス名 | `{Name}Handler` | `AuditLogHandler` |
| Interceptorクラス名 | `{Name}Interceptor` | `AuditLogInterceptor` |
| パッケージ（action） | `{basePackage}.action` | `com.example.action` |
| パッケージ（form） | `{basePackage}.form` | `com.example.form` |
| パッケージ（entity） | `{basePackage}.entity` | `com.example.entity` |
| パッケージ（handler） | `{basePackage}.handler` | `com.example.handler` |
| JSPパス | `/WEB-INF/view/{pathName}/` | `/WEB-INF/view/product/` |
| ルーティングパス | `/{pathName}` | `/product` |
| Webメソッドプレフィックス | `do{Method}` | `doList`, `doRegister` |

---

## 8. アプリタイプ別生成ロジック

### 8.1 Web（JSP + アクション）

| 生成要素 | 説明 |
|---------|------|
| Action | `HttpRequest`/`HttpResponse` + `@InjectForm` + `@OnError` + `do`プレフィックスメソッド |
| Form | `Serializable` + BeanValidationアノテーション |
| Entity | `@Entity` + `@Table` + publicフィールド |
| SQL | Nablarch SQL定義形式（FIND_ALL, FIND_BY_ID等） |
| 特記 | JSPパス生成、PRGパターン（Post-Redirect-Get）でのリダイレクト |

### 8.2 REST（JAX-RS + JSON）

| 生成要素 | 説明 |
|---------|------|
| Action | JAX-RSアノテーション（`@GET`, `@POST`, `@PUT`, `@DELETE`, `@Path`, `@Produces`, `@Consumes`） |
| Form | BeanValidation + JSON対応（`@Valid`による自動バリデーション） |
| Entity | Webと同一 |
| SQL | Webと同一 |
| 特記 | `EntityResponse`によるJSON応答、HTTPステータスコード適切な設定 |

### 8.3 Batch（BatchAction + DataReader）

| 生成要素 | 説明 |
|---------|------|
| Action | `BatchAction<SqlRow>` 継承 + `createReader` + `handle` |
| DataReader | `DataReader<SqlRow>` 実装（DB読み込み or ファイル読み込み） |
| Entity | 処理対象データに応じたEntity |
| SQL | バッチ処理用のSELECT/UPDATE/INSERT |
| 特記 | マルチスレッド対応（`MultiThreadExecutionHandler`との連携）、進捗ログ |

### 8.4 Messaging（MessagingAction）

| 生成要素 | 説明 |
|---------|------|
| Action | MOM受信バッチ（`MessagingAction`継承） or HTTP受信メッセージング |
| Form | 電文レイアウト定義に基づくフォーム |
| Entity | 永続化対象に応じたEntity |
| SQL | メッセージ処理用SQL |
| 特記 | 電文フォーマット定義との連携 |

### 8.5 アプリタイプ別テンプレート選択ロジック

```
入力: type + app_type
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ テンプレートセレクタ                                        │
│                                                           │
│  type == "action"                                        │
│    ├── app_type == "web"  → TMPL-ACTION-WEB              │
│    ├── app_type == "rest" → TMPL-ACTION-REST             │
│    ├── app_type == "batch" → TMPL-ACTION-BATCH           │
│    └── app_type == "messaging" → TMPL-ACTION-MSG         │
│                                                           │
│  type == "form"   → TMPL-FORM                            │
│  type == "sql"    → TMPL-SQL                             │
│  type == "entity" → TMPL-ENTITY                          │
│  type == "handler" → TMPL-HANDLER                        │
│  type == "interceptor" → TMPL-INTERCEPTOR                │
└──────────────────────────────────────────────────────────┘
```

---

## 9. 生成コードの品質保証

### 9.1 品質保証方針

生成されたコードはAIアシスタントのコンテキスト内で使用されるため、コンパイル可能な正確なコードの
生成が重要である。ただし、MCPサーバー内での完全なコンパイルチェック実行はスコープ外とする。

### 9.2 品質チェック項目

| チェック項目 | 方式 | Phase |
|------------|------|-------|
| **import文整合性** | 使用クラスに対するimport文の自動生成 | Phase 3 |
| **Nablarch規約準拠** | ルールベース検証 | Phase 3 |
| **テンプレート変数の欠落検出** | Mustacheレンダリング時の未解決変数チェック | Phase 3 |
| **パッケージ・クラス命名規約** | 命名規約テーブルとの照合 | Phase 3 |
| **アプリタイプ整合性** | type × app_type の組み合わせ検証 | Phase 3 |

### 9.3 Nablarch規約チェックルール

| ルールID | 規約 | チェック方法 |
|---------|------|------------|
| CONV-001 | アクションクラスにインスタンスフィールドを持たない | 生成テンプレートに含めない設計で保証 |
| CONV-002 | Webアクションメソッドに `do` プレフィックス | テンプレートで自動付与 |
| CONV-003 | `@InjectForm` + `@OnError` の組み合わせ | Webアクションテンプレートに固定で含む |
| CONV-004 | Entity のフィールドは public | テンプレートで保証 |
| CONV-005 | Entity に `@Version` フィールドを含める | デフォルトで version フィールドを生成 |
| CONV-006 | SQL定義ファイルの命名規約（大文字スネークケース） | テンプレートで自動変換 |
| CONV-007 | カスタムハンドラで `context.handleNext()` を呼び出す | テンプレートに固定で含む |

### 9.4 検証結果の出力

検証で問題が検出された場合、生成結果に警告として付与する。

```
### ⚠️ 注意事項
- specifications にフィールド定義が未指定のため、サンプルフィールドを使用しました
- app_type="batch" でのFormクラス生成は通常不要です。DataReaderの使用を推奨します
```

---

## 10. エラーハンドリング

### 10.1 エラーパターンと応答

| エラー | 応答 | ログ |
|-------|------|------|
| type が null/blank | "生成対象タイプを指定してください。有効値: action, form, sql, entity, handler, interceptor" | DEBUG |
| name が null/blank | "生成するクラス/ファイルの名前を指定してください。" | DEBUG |
| type が不正な値 | "不正な生成対象タイプ: {type}。有効値: action, form, sql, entity, handler, interceptor" | DEBUG |
| app_type が不正な値 | "不正なアプリケーションタイプ: {app_type}。有効値: web, rest, batch, messaging" | DEBUG |
| RAGパイプライン障害 | 静的知識ベースにフォールバック、生成は続行 | WARN |
| テンプレートレンダリング失敗 | "コード生成中にエラーが発生しました: {details}" | ERROR |
| specifications の型不一致 | 不正なフィールドを無視し、警告を付与して生成続行 | WARN |

### 10.2 フォールバック戦略

```
RAGパイプライン
    │ 障害発生
    ▼
静的知識ベース（api-patterns.yaml, design-patterns.yaml）
    │ 該当パターンなし
    ▼
デフォルトテンプレート（RAG/知識ベース結果なしでも基本コードを生成）
```

RAGや知識ベースから規約情報が取得できなくても、テンプレートの基本構造で最低限の
Nablarch規約（上記CONV-001〜007）を満たすコードを生成する。

---

## 11. MCP Tool登録

### 11.1 CodeGenerationTool クラス設計

```java
package com.tis.nablarch.mcp.tools;

import com.tis.nablarch.mcp.codegen.CodeGenerator;
import com.tis.nablarch.mcp.codegen.GenerationResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MCPツール: generate_code。
 *
 * <p>Nablarch準拠のJavaコード（Action、Form、SQL定義、Entity、Handler、Interceptor）を
 * 生成する。RAGパイプラインからコーディング規約を取得し、Nablarchのベストプラクティスに
 * 従ったコードを出力する。</p>
 */
@Service
public class CodeGenerationTool {

    private final CodeGenerator codeGenerator;

    /**
     * コンストラクタ。
     *
     * @param codeGenerator コード生成エンジン
     */
    public CodeGenerationTool(CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    /**
     * Nablarch準拠のコードを生成する。
     *
     * @param type 生成対象タイプ（action, form, sql, entity, handler, interceptor）
     * @param name 生成するクラス/ファイルの名前
     * @param appType アプリケーションタイプ（web, rest, batch, messaging）
     * @param specifications タイプ固有の詳細パラメータ（JSON文字列）
     * @return 生成結果のMarkdownフォーマット文字列
     */
    @Tool(description = "Generate Nablarch-compliant code (Action, Form, SQL, Entity, Handler, "
            + "Interceptor). Produces boilerplate code following Nablarch coding conventions "
            + "and best practices. Use this when developers need skeleton code that adheres "
            + "to Nablarch patterns.")
    public String generateCode(
            @ToolParam(description = "Type of code to generate: action, form, sql, entity, handler, interceptor")
            String type,
            @ToolParam(description = "Name for the generated class/file (e.g. 'UserRegistration', 'Product')")
            String name,
            @ToolParam(description = "Application type: web (default), rest, batch, messaging")
            String appType,
            @ToolParam(description = "Type-specific parameters as JSON (fields, queries, routing, etc.)")
            String specifications) {

        // 入力検証
        if (type == null || type.isBlank()) {
            return "生成対象タイプを指定してください。"
                    + "有効値: action, form, sql, entity, handler, interceptor";
        }
        if (name == null || name.isBlank()) {
            return "生成するクラス/ファイルの名前を指定してください。";
        }

        String effectiveType = type.toLowerCase().trim();
        if (!isValidType(effectiveType)) {
            return "不正な生成対象タイプ: " + type
                    + "。有効値: action, form, sql, entity, handler, interceptor";
        }

        String effectiveAppType = (appType != null && !appType.isBlank())
                ? appType.toLowerCase().trim() : "web";
        if (!isValidAppType(effectiveAppType)) {
            return "不正なアプリケーションタイプ: " + appType
                    + "。有効値: web, rest, batch, messaging";
        }

        // コード生成実行
        Map<String, Object> specs = parseSpecifications(specifications);
        GenerationResult result = codeGenerator.generate(
                effectiveType, name, effectiveAppType, specs);

        // 結果整形
        return formatResult(result);
    }

    private boolean isValidType(String type) {
        return switch (type) {
            case "action", "form", "sql", "entity",
                 "handler", "interceptor" -> true;
            default -> false;
        };
    }

    private boolean isValidAppType(String appType) {
        return switch (appType) {
            case "web", "rest", "batch", "messaging" -> true;
            default -> false;
        };
    }

    private Map<String, Object> parseSpecifications(String specifications) {
        // JSON文字列をMapにパース
        // null/空文字の場合はemptyMapを返却
        // パース失敗時はemptyMapを返却（警告ログ出力）
        return Map.of();
    }

    private String formatResult(GenerationResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 生成結果: ").append(result.name());
        sb.append(" (").append(result.appType()).append("/").append(result.type()).append(")\n\n");

        // 適用された規約
        if (!result.conventionsApplied().isEmpty()) {
            sb.append("### 適用されたNablarch規約\n");
            for (String convention : result.conventionsApplied()) {
                sb.append("- ").append(convention).append("\n");
            }
            sb.append("\n");
        }

        // 必要な依存モジュール
        if (!result.dependencies().isEmpty()) {
            sb.append("### 必要な依存モジュール\n");
            for (String dep : result.dependencies()) {
                sb.append("- ").append(dep).append("\n");
            }
            sb.append("\n");
        }

        // 警告
        if (!result.warnings().isEmpty()) {
            sb.append("### ⚠️ 注意事項\n");
            for (String warning : result.warnings()) {
                sb.append("- ").append(warning).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n\n");

        // 生成ファイル
        for (int i = 0; i < result.files().size(); i++) {
            GenerationResult.GeneratedFile file = result.files().get(i);
            sb.append("### ファイル ").append(i + 1).append(": ").append(file.fileName()).append("\n");
            sb.append("パス: `").append(file.path()).append("`\n\n");
            sb.append("```").append(file.language()).append("\n");
            sb.append(file.content()).append("\n");
            sb.append("```\n\n---\n\n");
        }

        return sb.toString();
    }
}
```

### 11.2 CodeGenerator インターフェース

```java
package com.tis.nablarch.mcp.codegen;

import java.util.Map;

/**
 * コード生成エンジンインターフェース。
 * テンプレート展開、RAG連携、品質チェックを統合する。
 */
public interface CodeGenerator {

    /**
     * Nablarch準拠のコードを生成する。
     *
     * @param type 生成対象タイプ
     * @param name クラス/ファイル名
     * @param appType アプリケーションタイプ
     * @param specifications タイプ固有パラメータ
     * @return 生成結果
     */
    GenerationResult generate(String type, String name, String appType,
                              Map<String, Object> specifications);
}
```

### 11.3 GenerationResult レコード

```java
package com.tis.nablarch.mcp.codegen;

import java.util.List;

/**
 * コード生成結果。
 *
 * @param type 生成対象タイプ
 * @param name クラス/ファイル名
 * @param appType アプリケーションタイプ
 * @param files 生成されたファイル一覧
 * @param conventionsApplied 適用されたNablarch規約
 * @param dependencies 必要な依存モジュール
 * @param warnings 警告メッセージ
 */
public record GenerationResult(
        String type,
        String name,
        String appType,
        List<GeneratedFile> files,
        List<String> conventionsApplied,
        List<String> dependencies,
        List<String> warnings) {

    /**
     * 生成されたファイル。
     *
     * @param path 推奨ファイルパス
     * @param fileName ファイル名
     * @param content 生成されたコード内容
     * @param language 言語（java, xml, sql）
     */
    public record GeneratedFile(
            String path,
            String fileName,
            String content,
            String language) {
    }
}
```

### 11.4 McpServerConfig への登録

```java
// McpServerConfig.java への追加（Phase 3）

@Bean
public MethodToolCallbackProvider nablarchTools(
        SearchApiTool searchApiTool,
        ValidateHandlerQueueTool validateHandlerQueueTool,
        SemanticSearchTool semanticSearchTool,
        CodeGenerationTool codeGenerationTool) {  // 新規追加
    return MethodToolCallbackProvider.builder()
            .toolObjects(searchApiTool, validateHandlerQueueTool,
                         semanticSearchTool, codeGenerationTool)
            .build();
}
```

---

## 12. 設定パラメータ

```yaml
# application.yml
nablarch:
  codegen:
    enabled: true                    # generate_code Toolの有効/無効
    base-package: "com.example"      # 生成コードのベースパッケージ（デフォルト）
    template-path: "classpath:templates/"  # テンプレートファイルのパス
    default-app-type: "web"          # デフォルトアプリタイプ
    rag-search:
      enabled: true                  # RAG連携の有効/無効
      fallback-to-static: true       # RAG障害時の静的知識ベースフォールバック
    quality-check:
      enabled: true                  # 品質チェックの有効/無効
      warn-on-missing-specs: true    # specifications未指定時の警告
```

---

## 付録

### A. パッケージ構造（Phase 3追加分）

```
com.tis.nablarch.mcp/
├── tools/
│   ├── SearchApiTool.java              ← 既存（Phase 1）
│   ├── ValidateHandlerQueueTool.java   ← 既存（Phase 1）
│   ├── SemanticSearchTool.java         ← 既存（Phase 2、stubs）
│   └── CodeGenerationTool.java         ← 本設計書
├── codegen/
│   ├── CodeGenerator.java              ← コード生成IF
│   ├── DefaultCodeGenerator.java       ← コード生成実装
│   ├── GenerationResult.java           ← 結果DTO
│   ├── TemplateContextBuilder.java     ← テンプレートコンテキスト構築
│   ├── TemplateSelector.java           ← テンプレート選択ロジック
│   ├── NamingConventionHelper.java     ← 命名規約ヘルパー
│   └── QualityChecker.java             ← 品質チェック
└── knowledge/
    └── NablarchKnowledgeBase.java      ← 既存（Phase 1）
```

### B. 関連WBSタスク

| WBS | タスク | 本設計との関係 |
|-----|-------|-------------|
| 3.1.2 | generate_code Tool設計 | **本設計書** |
| 3.2.x | generate_code Tool実装（予定） | 本設計書を実装 |
| 3.3.x | generate_code Tool統合テスト（予定） | 本設計の検証 |
| 2.1.6 | semantic_search Tool設計 | RAG連携パターンの参考元 |

### C. 依存ライブラリ（追加分）

| ライブラリ | バージョン | 用途 |
|----------|----------|------|
| JMustache | 1.16+ | テンプレートエンジン |

```groovy
// build.gradle への追加
implementation("com.samskivert:jmustache:1.16")
```

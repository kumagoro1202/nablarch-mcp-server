# Tool API仕様書

> **対象読者**: 開発者（Tool API仕様を参照したい人）
> **前提知識**: MCP Tool, JSON-RPCの概念
> **概要**: 10 Toolsの入出力仕様、パラメータ定義

---

## 1. 概要

Nablarch MCP Serverが提供する全9種のMCP Toolの完全なAPI仕様書。
各Toolの入出力パラメータ、使用例、エラーケースを記載する。

### 1.1 Tool一覧

| # | Tool名 | メソッド | 説明 | Phase |
|---|--------|---------|------|-------|
| 1 | search_api | `searchApi()` | Nablarch APIドキュメント検索 | 1 |
| 2 | validate_handler_queue | `validateHandlerQueue()` | ハンドラキューXML検証 | 1 |
| 3 | semantic_search | `semanticSearch()` | セマンティック検索（RAG） | 2 |
| 4 | design | `design()` | ハンドラキュー設計・XML生成 | 2 |
| 5 | generate_code | `generateCode()` | Nablarch準拠コード生成 | 2 |
| 6 | generate_test | `generateTest()` | テストコード生成 | 2 |
| 7 | optimize | `optimize()` | ハンドラキュー最適化提案 | 2 |
| 8 | recommend | `recommend()` | デザインパターン推薦 | 2 |
| 9 | analyze_migration | `analyzeMigration()` | 移行影響分析 | 2 |

### 1.2 共通仕様

- **レスポンス形式**: 全ToolはMarkdown形式の文字列を返す
- **エラー処理**: 入力検証エラーは日本語のエラーメッセージを返す
- **依存関係**: 一部ToolはNablarchKnowledgeBase、RAGパイプライン（Phase 2）に依存

---

## 2. search_api Tool

### 2.1 概要

Nablarch知識ベースからAPIパターン・モジュール・ハンドラ・設計パターン・エラー情報をキーワード検索する。

| 項目 | 値 |
|------|-----|
| クラス | `SearchApiTool` |
| メソッド | `searchApi(keyword, category)` |
| Phase | 1 (Phase 2のsemantic_searchの代替としても使用可能) |

### 2.2 パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| `keyword` | String | ○ | 検索キーワード（クラス名、メソッド名、概念） |
| `category` | String | - | カテゴリフィルタ: handler, library, web, batch, rest, messaging |

### 2.3 使用例

**リクエスト例1: 基本的なキーワード検索**

```json
{
  "keyword": "DbConnectionManagementHandler",
  "category": null
}
```

**レスポンス例:**

```markdown
検索結果: "DbConnectionManagementHandler"
件数: 1件

### DbConnectionManagementHandler
- **FQCN**: `nablarch.common.handler.DbConnectionManagementHandler`
- **カテゴリ**: handler
- **説明**: データベース接続の取得・解放を管理するハンドラ
```

**リクエスト例2: カテゴリ指定検索**

```json
{
  "keyword": "validation",
  "category": "web"
}
```

### 2.4 エラーケース

| 条件 | レスポンス |
|------|-----------|
| keyword未指定 | `検索キーワードを指定してください。` |
| 該当なし | `検索結果なし: {keyword} (カテゴリ: {category})` |

---

## 3. validate_handler_queue Tool

### 3.1 概要

NablarchハンドラキューXML設定を検証する。ハンドラの順序制約・必須ハンドラの有無・互換性をチェックする。

| 項目 | 値 |
|------|-----|
| クラス | `ValidateHandlerQueueTool` |
| メソッド | `validateHandlerQueue(handlerQueueXml, applicationType)` |
| Phase | 1 |

### 3.2 パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| `handlerQueueXml` | String | ○ | ハンドラキューXML設定内容 |
| `applicationType` | String | ○ | アプリケーションタイプ: web, rest, batch, messaging |

### 3.3 使用例

**リクエスト例:**

```json
{
  "handlerQueueXml": "<list name=\"handlerQueue\">\n  <component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>\n  <component class=\"nablarch.common.handler.DbConnectionManagementHandler\"/>\n</list>",
  "applicationType": "web"
}
```

**レスポンス例（検証成功）:**

```markdown
## ハンドラキュー検証結果

検証対象: web アプリケーション
ハンドラ数: 2

### 検証結果: ✅ 成功

| # | ハンドラ | ステータス |
|---|---------|----------|
| 1 | HttpResponseHandler | ✅ |
| 2 | DbConnectionManagementHandler | ✅ |

順序制約違反: なし
```

**レスポンス例（検証エラー）:**

```markdown
## ハンドラキュー検証結果

検証対象: web アプリケーション
ハンドラ数: 2

### 検証結果: ❌ エラーあり

#### エラー
- SecureHandler は HttpResponseHandler より後に配置すべきです

#### 警告
- GlobalErrorHandler が欠落しています（推奨）
```

### 3.4 エラーケース

| 条件 | レスポンス |
|------|-----------|
| XML未指定 | `ハンドラキューXMLを指定してください。` |
| アプリタイプ未指定 | `アプリケーションタイプを指定してください（web, rest, batch, messaging）。` |
| XML解析失敗 | `XMLからハンドラクラスを抽出できませんでした。...` |

---

## 4. semantic_search Tool

### 4.1 概要

Nablarch知識ベースに対するセマンティック検索。ハイブリッド検索（BM25+ベクトル）とCross-Encoderリランキングを組み合わせた高精度検索を提供する。

| 項目 | 値 |
|------|-----|
| クラス | `SemanticSearchTool` |
| メソッド | `semanticSearch(query, appType, module, source, sourceType, topK, mode)` |
| Phase | 2 (RAGパイプライン依存) |

### 4.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `query` | String | ○ | - | 検索クエリ（自然言語、日本語/英語対応） |
| `appType` | String | - | null | アプリ種別フィルタ: web, rest, batch, messaging |
| `module` | String | - | null | モジュール名フィルタ（例: nablarch-fw-web） |
| `source` | String | - | null | データソースフィルタ: nablarch-document, github, fintan, javadoc |
| `sourceType` | String | - | null | コンテンツ種別: documentation, code, javadoc, config, standard |
| `topK` | Integer | - | 5 | 返却する結果数（1-50） |
| `mode` | String | - | hybrid | 検索モード: hybrid, vector, keyword |

### 4.3 使用例

**リクエスト例1: 自然言語クエリ**

```json
{
  "query": "Nablarchでトランザクション管理を設定する方法",
  "appType": "web",
  "topK": 3
}
```

**レスポンス例:**

```markdown
## 検索結果: "Nablarchでトランザクション管理を設定する方法"
モード: hybrid | 結果数: 3件 | 検索時間: 245ms

---

### 結果 1 (スコア: 0.892)
**ソース**: nablarch-document | web | nablarch-common-jdbc
**URL**: https://nablarch.github.io/docs/...

TransactionManagementHandlerを使用してトランザクション境界を設定します...

---

### 結果 2 (スコア: 0.856)
...
```

**リクエスト例2: フィルタ付き検索**

```json
{
  "query": "handler queue configuration",
  "source": "github",
  "sourceType": "code",
  "mode": "keyword"
}
```

### 4.4 エラーケース

| 条件 | レスポンス |
|------|-----------|
| query未指定 | `検索クエリを指定してください。` |
| 検索結果0件 | 検索のヒント付きメッセージ |
| RAGパイプライン障害 | `検索中にエラーが発生しました。search_apiツールをお試しください。` |

---

## 5. design Tool

### 5.1 概要

アプリケーションタイプと要件に基づいて、最適なハンドラキュー構成を設計しXML設定を自動生成する。

| 項目 | 値 |
|------|-----|
| クラス | `DesignHandlerQueueTool` |
| メソッド | `design(appType, requirements, includeComments)` |
| Phase | 2 |

### 5.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `appType` | String | ○ | - | アプリケーションタイプ: web, rest, batch, messaging |
| `requirements` | String | - | null | オプション要件（カンマ区切り）: session, csrf, multipart, async, security, logging |
| `includeComments` | Boolean | - | true | 生成XMLにコメントを含めるか |

### 5.3 使用例

**リクエスト例:**

```json
{
  "appType": "web",
  "requirements": "csrf, session, security",
  "includeComments": true
}
```

**レスポンス例:**

```markdown
## ハンドラキュー設計結果

**アプリタイプ**: web
**ハンドラ数**: 14
**適用要件**: csrf, session, security

### ハンドラ構成

| # | ハンドラ | 説明 | 必須 |
|---|----------|------|------|
| 1 | StatusCodeConvertHandler | ステータスコード変換 | ○ |
| 2 | HttpResponseHandler | HTTPレスポンス処理 | ○ |
| 3 | GlobalErrorHandler | グローバルエラー処理 | ○ |
| 4 | SecureHandler | セキュリティヘッダー付与 | - |
...

### XML設定

```xml
<!-- webアプリケーション用ハンドラキュー設定 -->
<list name="handlerQueue">
  <!-- ステータスコード変換 -->
  <component class="nablarch.fw.handler.StatusCodeConvertHandler"/>
  <!-- HTTPレスポンス処理 -->
  <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
  ...
</list>
```

### 検証結果

✅ ハンドラキューは適切に構成されています。
```

### 5.4 エラーケース

| 条件 | レスポンス |
|------|-----------|
| appType未指定 | `エラー: アプリケーションタイプ（app_type）を指定してください。...` |
| 無効なappType | `エラー: 不明なアプリケーションタイプ: {appType}...` |

---

## 6. generate_code Tool

### 6.1 概要

Nablarch準拠のJavaコード（Action、Form、SQL定義、Entity、Handler、Interceptor）を生成する。

| 項目 | 値 |
|------|-----|
| クラス | `CodeGenerationTool` |
| メソッド | `generateCode(type, name, appType, specifications)` |
| Phase | 2 |

### 6.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `type` | String | ○ | - | 生成対象タイプ: action, form, sql, entity, handler, interceptor |
| `name` | String | ○ | - | 生成するクラス/ファイルの名前（例: 'UserRegistration'） |
| `appType` | String | - | web | アプリケーションタイプ: web, rest, batch, messaging |
| `specifications` | String | - | null | タイプ固有パラメータ（JSON文字列） |

### 6.3 使用例

**リクエスト例1: Action生成**

```json
{
  "type": "action",
  "name": "UserRegistration",
  "appType": "web",
  "specifications": "{\"routing\": \"/users/register\", \"methods\": [\"GET\", \"POST\"]}"
}
```

**レスポンス例:**

```markdown
## 生成結果: UserRegistration (web/action)

### 適用されたNablarch規約
- パッケージ構成: {project}.app.action
- リクエストパラメータはFormクラスで受け取る
- 単一責任: 1Action = 1ユースケース

### 必要な依存モジュール
- nablarch-fw-web
- nablarch-common-validation

### 注意事項
- 対応するFormクラス（UserRegistrationForm）も生成してください

---

### ファイル 1: UserRegistrationAction.java
パス: `src/main/java/com/example/app/action/UserRegistrationAction.java`

```java
package com.example.app.action;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
// ...
public class UserRegistrationAction {
    // ...
}
```
```

**リクエスト例2: Form生成（フィールド指定）**

```json
{
  "type": "form",
  "name": "UserRegistration",
  "appType": "web",
  "specifications": "{\"fields\": [{\"name\": \"userId\", \"type\": \"String\", \"required\": true}, {\"name\": \"email\", \"type\": \"String\", \"required\": true, \"format\": \"email\"}]}"
}
```

### 6.4 specifications の構造（タイプ別）

#### action

```json
{
  "routing": "/path/to/action",
  "methods": ["GET", "POST"],
  "transactional": true
}
```

#### form

```json
{
  "fields": [
    {"name": "fieldName", "type": "String", "required": true, "format": "email"}
  ]
}
```

#### sql

```json
{
  "queries": [
    {"id": "SELECT_USER", "sql": "SELECT * FROM USERS WHERE ID = :id"}
  ]
}
```

#### entity

```json
{
  "table": "USERS",
  "columns": [
    {"name": "ID", "type": "Long", "primaryKey": true},
    {"name": "NAME", "type": "String"}
  ]
}
```

### 6.5 エラーケース

| 条件 | レスポンス |
|------|-----------|
| type未指定 | `生成対象タイプを指定してください。...` |
| name未指定 | `生成するクラス/ファイルの名前を指定してください。` |
| 無効なtype | `不正な生成対象タイプ: {type}...` |
| 無効なappType | `不正なアプリケーションタイプ: {appType}...` |

---

## 7. generate_test Tool

### 7.1 概要

Nablarch Testing Framework（JUnit5 + Excelテストデータ）に準拠したテストコードを生成する。

| 項目 | 値 |
|------|-----|
| クラス | `TestGenerationTool` |
| メソッド | `generateTest(targetClass, testType, format, testCases, includeExcel, coverageTarget)` |
| Phase | 2 |

### 7.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `targetClass` | String | ○ | - | テスト対象クラスのFQCN |
| `testType` | String | ○ | - | テストタイプ: unit, request-response, batch, messaging |
| `format` | String | - | junit5 | 出力フォーマット: junit5, nablarch-excel |
| `testCases` | String | - | null | テストケースの自然言語記述 |
| `includeExcel` | String | - | true | Excelテストデータ構造を含めるか |
| `coverageTarget` | String | - | standard | カバレッジ目標: minimal, standard, comprehensive |

### 7.3 使用例

**リクエスト例:**

```json
{
  "targetClass": "com.example.app.action.UserRegistrationAction",
  "testType": "request-response",
  "format": "nablarch-excel",
  "testCases": "正常登録、バリデーションエラー、重複メールエラー",
  "includeExcel": "true",
  "coverageTarget": "standard"
}
```

**レスポンス例:**

```markdown
## テスト生成結果: UserRegistrationAction
テストタイプ: request-response | フォーマット: nablarch-excel | カバレッジ: standard

---

### テストクラス: UserRegistrationActionRequestTest

```java
package com.example.app.action;

import nablarch.test.core.http.SimpleRestTestSupport;
import org.junit.jupiter.api.Test;

public class UserRegistrationActionRequestTest extends SimpleRestTestSupport {

    @Test
    void 正常に登録できること() {
        execute("正常に登録できること");
    }

    @Test
    void バリデーションエラーの場合400を返すこと() {
        execute("バリデーションエラーの場合400を返すこと");
    }
    // ...
}
```

---

### Excelテストデータ構造

**ファイル名**: `UserRegistrationActionRequestTest.xlsx`

#### シート: 正常に登録できること

| setupTable: USERS | | | |
|-------------------|---|---|---|
| ID | NAME | EMAIL | STATUS |
| (空) | | | |

| requestParams | | |
|---------------|---|---|
| userId | testUser001 |
| email | test@example.com |

| expectedStatus | |
|----------------|---|
| 200 | |

---

### テストケース一覧

| # | テストメソッド | 説明 | 期待ステータス |
|---|-------------|------|-------------|
| 1 | `正常に登録できること` | 正常な入力で登録成功 | 200 |
| 2 | `バリデーションエラーの場合400を返すこと` | 必須項目欠落 | 400 |
| 3 | `重複メールエラーの場合409を返すこと` | 既存メールで登録失敗 | 409 |

---

### 適用されたNablarch規約

- テストメソッド名は日本語で振る舞いを記述
- テストデータはExcelファイルで管理（Nablarch Testing Framework規約）
- テストスーパークラス: SimpleRestTestSupport (nablarch.test.core.http.SimpleRestTestSupport)
- executeメソッドでHTTPリクエストをシミュレート
- Excelのシート名がテストメソッドに対応
```

### 7.4 テストタイプ別スーパークラス

| testType | スーパークラス | FQCN |
|----------|--------------|------|
| unit | （指定なし） | - |
| request-response | SimpleRestTestSupport | `nablarch.test.core.http.SimpleRestTestSupport` |
| batch | BatchRequestTestSupport | `nablarch.test.core.batch.BatchRequestTestSupport` |
| messaging | MessagingRequestTestSupport | `nablarch.test.core.messaging.MessagingRequestTestSupport` |

### 7.5 エラーケース

| 条件 | レスポンス |
|------|-----------|
| targetClass未指定 | `テスト対象クラスのFQCNを指定してください。` |
| testType未指定 | `テストタイプを指定してください（unit, request-response, batch, messaging）。` |
| 無効なtestType | `不明なテストタイプ: {testType}...` |

---

## 8. optimize Tool

### 8.1 概要

既存のハンドラキューXML設定を分析し、正確性・セキュリティ・パフォーマンスの3観点から最適化提案を生成する。

| 項目 | 値 |
|------|-----|
| クラス | `OptimizeHandlerQueueTool` |
| メソッド | `optimize(currentXml, appType, concern)` |
| Phase | 2 |

### 8.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `currentXml` | String | ○ | - | 現在のハンドラキューXML設定 |
| `appType` | String | - | 自動推定 | アプリケーションタイプ |
| `concern` | String | - | all | 最適化観点: all, correctness, security, performance |

### 8.3 使用例

**リクエスト例:**

```json
{
  "currentXml": "<list name=\"handlerQueue\">\n  <component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>\n  <component class=\"nablarch.common.handler.DbConnectionManagementHandler\"/>\n  <component class=\"nablarch.fw.handler.DispatchHandler\"/>\n</list>",
  "appType": null,
  "concern": "all"
}
```

**レスポンス例:**

```markdown
## ハンドラキュー最適化分析

**アプリタイプ**: web（自動推定）
**ハンドラ数**: 3
**検出された最適化ポイント**: 2件

### サマリ

| 観点 | 件数 | 高 | 中 | 低 |
|------|------|-----|-----|-----|
| 正確性 | 0 | 0 | 0 | 0 |
| セキュリティ | 2 | 2 | 0 | 0 |
| パフォーマンス | 0 | 0 | 0 | 0 |

---

### 🔴 [SEC-001] SecureHandler（高）

**観点**: セキュリティ
**タイプ**: ハンドラ追加
**問題**: セキュリティヘッダーが設定されていません
**修正提案**: HttpResponseHandlerの内側にSecureHandlerを追加してください

#### Before
```xml
<!-- 現在のハンドラキュー（一部抜粋） -->
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.common.handler.DbConnectionManagementHandler"/>
```

#### After
```xml
<!-- 推奨構成（一部抜粋） -->
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.fw.web.handler.SecureHandler"/> <!-- 追加 -->
<component class="nablarch.common.handler.DbConnectionManagementHandler"/>
```

---

### 🔴 [SEC-002] CsrfTokenVerificationHandler（高）
...
```

### 8.4 最適化ルール一覧

#### 正確性（Correctness）

| ルールID | 説明 | 重大度 |
|---------|------|--------|
| COR-001 | 必須ハンドラが欠落 | high |
| COR-002 | 順序制約違反 | high |

#### セキュリティ（Security）

| ルールID | 説明 | 重大度 |
|---------|------|--------|
| SEC-001 | SecureHandler未設定 | high |
| SEC-002 | CSRF対策未設定（Webのみ） | high |
| SEC-003 | セッションストア未設定 | medium |
| SEC-005 | 本番不要ハンドラ残存 | medium |

#### パフォーマンス（Performance）

| ルールID | 説明 | 重大度 |
|---------|------|--------|
| PERF-001 | 不要ハンドラの除去 | medium |
| PERF-002 | 重複ハンドラ検出 | medium |
| PERF-005 | ログハンドラの非同期化推奨 | low |

### 8.5 エラーケース

| 条件 | レスポンス |
|------|-----------|
| XML未指定 | `エラー: ハンドラキューXMLが指定されていません` |
| XML解析失敗 | `エラー: XMLからハンドラを抽出できませんでした。...` |
| appType推定失敗 | `エラー: アプリケーションタイプを自動推定できませんでした。...` |

---

## 9. recommend Tool

### 9.1 概要

ユーザーの自然言語要件に基づいて、Nablarch固有のデザインパターンから最適なものをスコアリング付きで推薦する。

| 項目 | 値 |
|------|-----|
| クラス | `RecommendPatternTool` |
| メソッド | `recommend(requirement, appType, constraints, maxResults)` |
| Phase | 2 |

### 9.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `requirement` | String | ○ | - | 自然言語要件記述（10文字以上） |
| `appType` | String | - | null | アプリケーションタイプ: web, rest, batch, messaging |
| `constraints` | String | - | null | 追加の制約条件（カンマ区切り） |
| `maxResults` | Integer | - | 3 | 返却する最大候補数（1-11） |

### 9.3 使用例

**リクエスト例:**

```json
{
  "requirement": "データベースへの排他制御を実装したい。複数ユーザーが同時に更新しても整合性を保ちたい",
  "appType": "web",
  "constraints": "楽観ロック",
  "maxResults": 3
}
```

**レスポンス例:**

```markdown
## デザインパターン推薦結果

**要件**: データベースへの排他制御を実装したい。複数ユーザーが同時に更新しても整合性を保ちたい...
**アプリタイプ**: web
**候補数**: 3件

---

### 🥇 第1位: OptimisticLockPattern（スコア: 87%）

**カテゴリ**: data-access
**適合理由**: 要件のキーワードと高い一致度、カテゴリが要件と一致、指定アプリタイプに最適

#### ソリューション概要
バージョン番号または更新日時を使用した楽観的ロックパターン。Nablarchでは@Versionアノテーションを...

#### コード例
```java
@Entity
public class User {
    @Id
    private Long id;

    @Version
    private Long version;
    // ...
}
```

#### スコア内訳
| ファクター | スコア |
|-----------|--------|
| キーワード一致 | 90% |
| カテゴリ一致 | 100% |
| app_type適合 | 100% |
| 制約一致 | 70% |

**対応アプリタイプ**: web, rest, batch

**📖 詳細**: `nablarch://pattern/OptimisticLockPattern`

---

### 🥈 第2位: PessimisticLockPattern（スコア: 72%）
...
```

### 9.4 スコアリング重み

| ファクター | 重み | 説明 |
|-----------|------|------|
| キーワード一致度 | 40% | 要件から抽出したキーワードとの一致 |
| カテゴリ一致度 | 25% | 推定カテゴリとパターンカテゴリの一致 |
| app_type適合度 | 20% | 指定アプリタイプへの対応度 |
| 制約一致度 | 15% | 追加制約との一致 |

### 9.5 エラーケース

| 条件 | レスポンス |
|------|-----------|
| requirement未指定 | `エラー: 要件テキストが指定されていません` |
| requirement短すぎ | `エラー: 要件テキストが短すぎます（10文字以上必要）` |
| 無効なappType | `エラー: 不明なアプリケーションタイプ: {appType}...` |
| 該当パターンなし | `指定された条件に一致するパターンが見つかりませんでした。...` |

---

## 10. analyze_migration Tool

### 10.1 概要

Nablarch 5から6への移行において、既存コードの非推奨API使用を検出し、移行影響を分析して修正提案を生成する。

| 項目 | 値 |
|------|-----|
| クラス | `MigrationAnalysisTool` |
| メソッド | `analyzeMigration(codeSnippet, sourceVersion, targetVersion, analysisScope)` |
| Phase | 2 |

### 10.2 パラメータ

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|-----------|-----|------|-----------|------|
| `codeSnippet` | String | ○ | - | 分析対象のコード（Java, XML, POM） |
| `sourceVersion` | String | - | 5 | 移行元バージョン（5, 5.0, 5.1, 5.2） |
| `targetVersion` | String | - | 6 | 移行先バージョン（6, 6.0, 6.1） |
| `analysisScope` | String | - | full | 分析範囲: full, namespace, dependency, api |

### 10.3 使用例

**リクエスト例1: Javaコード分析**

```json
{
  "codeSnippet": "import javax.servlet.http.HttpServletRequest;\nimport javax.persistence.Entity;\n\npublic class UserAction extends DbAccessSupport {\n    // ...\n}",
  "sourceVersion": "5",
  "targetVersion": "6"
}
```

**レスポンス例:**

```markdown
## Nablarch移行影響分析レポート

| 項目 | 値 |
|------|-----|
| 移行元バージョン | Nablarch 5 |
| 移行先バージョン | Nablarch 6 |
| コードタイプ | Java |
| 検出問題数 | 3件 |

### サマリ

| 分類 | 件数 |
|------|------|
| 🔧 自動修正可能 | 2件 |
| 🔨 手動修正必要 | 1件 |

### 工数見積もり

| 工数レベル | 件数 | 目安 |
|-----------|------|------|
| trivial | 2件 | 数分/件 |
| moderate | 0件 | 数時間/件 |
| major | 1件 | 数日/件 |

---

### 検出された問題

#### 名前空間の変更（javax → jakarta）

🔧 **BC-001**: Jakarta EE 9以降ではjavax.servletがjakarta.servletに変更されました

- **検出箇所**: 行1
- **該当コード**: `javax.servlet.http.HttpServletRequest`
- **工数**: trivial
- **推奨対応**: パッケージインポートを一括置換してください
- **修正後**: `jakarta.servlet`

🔧 **BC-002**: Jakarta EE 9以降ではjavax.persistenceがjakarta.persistenceに変更されました

- **検出箇所**: 行2
- **該当コード**: `javax.persistence.Entity`
- **工数**: trivial
- **推奨対応**: パッケージインポートを一括置換してください
- **修正後**: `jakarta.persistence`

#### 削除されたAPI

🔨 **BC-003**: DbAccessSupportクラスはNablarch 6で削除されました

- **検出箇所**: 行4
- **該当コード**: `extends DbAccessSupport`
- **工数**: major
- **推奨対応**: UniversalDaoまたはBasicDatabaseAccessを使用するようリファクタリングしてください

---

### 推奨移行手順

1. **自動修正可能な問題を先に対応**
   - 一括置換ツール（IDE機能やsed）を使用
   - namespace変更は全ファイル一括で実施

2. **手動修正が必要な問題に対応**
   - API削除は代替実装の検討が必要
   - 公式移行ガイドを参照

3. **テスト実行**
   - 単体テストの実行確認
   - 結合テストでの動作確認
```

**リクエスト例2: POM分析**

```json
{
  "codeSnippet": "<dependency>\n  <groupId>javax.servlet</groupId>\n  <artifactId>javax.servlet-api</artifactId>\n  <version>4.0.1</version>\n</dependency>",
  "analysisScope": "dependency"
}
```

### 10.4 破壊的変更パターン一覧

| パターンID | カテゴリ | 説明 | 自動修正 | 工数 |
|-----------|---------|------|---------|------|
| BC-001 | namespace | javax.servlet → jakarta.servlet | ○ | trivial |
| BC-002 | namespace | javax.persistence → jakarta.persistence | ○ | trivial |
| BC-003 | api_removal | DbAccessSupport削除 | × | major |
| BC-004 | namespace | javax.annotation → jakarta.annotation | ○ | trivial |
| BC-005 | api_change | SqlResultSet.searchメソッド変更 | × | moderate |
| BC-006 | dependency | javax.servlet-api → jakarta.servlet-api | ○ | trivial |
| BC-007 | dependency | nablarch-bom 5.x → 6.x | ○ | trivial |
| BC-008 | namespace | javax.validation → jakarta.validation | ○ | trivial |
| BC-009 | namespace | javax.inject → jakarta.inject | ○ | trivial |
| BC-010 | api_removal | 非推奨Handler（HttpAccessLogHandler等） | × | moderate |

### 10.5 エラーケース

| 条件 | レスポンス |
|------|-----------|
| codeSnippet未指定 | `エラー: 分析対象のコードが指定されていません` |
| 無効なsourceVersion | `エラー: サポートされていない移行元バージョン: {version}...` |
| 無効なtargetVersion | `エラー: サポートされていない移行先バージョン: {version}...` |

---

## 11. 参考リソース

### 11.1 関連ドキュメント

| ドキュメント | パス |
|-------------|------|
| アーキテクチャ設計書 | `docs/02-architecture.md` |
| ユースケース仕様書 | `docs/03-use-cases.md` |
| RAGパイプライン仕様書 | `docs/04-rag-pipeline-spec.md` |
| Resource URI仕様書 | `docs/api/resource-uri-specification.md` |

### 11.2 Tool設計書（designs/）

| Tool | 設計書 |
|------|-------|
| design | `docs/designs/15_tool-design-handler-queue.md` |
| analyze_migration | `docs/designs/19_tool-analyze-migration.md` |
| recommend | `docs/designs/20_tool-recommend-pattern.md` |
| optimize | `docs/designs/21_tool-optimize-handler-queue.md` |

---

*本仕様書は Nablarch MCP Server Phase 3 WBS 3.4.1 に基づき作成*

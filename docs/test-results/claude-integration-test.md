# Claude 統合テストシミュレーション

## 概要

Nablarch MCP Serverを Claude Desktop / Claude Code と連携して使用する場合の
代表的なユースケースシナリオを定義します。
各シナリオでは、ユーザーの入力に対してAIがどのMCP Primitiveをどの順序で呼び出し、
どのような応答を生成するかを記述します。

## 設定

### Claude Desktop 設定例

`claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

### Claude Code 設定例

`.mcp.json`:

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"
      ]
    }
  }
}
```

---

## シナリオ1: Webアプリケーションのハンドラキュー設定

### ユーザー入力

> 「NablarchのWebアプリケーションを新規構築する。ハンドラキューの設定を教えて。」

### 期待されるMCP呼び出しフロー

```
Step 1: Prompt → setup-handler-queue (app_type: "web")
  ↓ 推奨ハンドラ一覧・制約・XMLテンプレートを取得
Step 2: Resource → nablarch://handler/web
  ↓ 詳細なハンドラキュー仕様を補完情報として取得
Step 3: Resource → nablarch://guide/setup
  ↓ web.xml・コンポーネント定義テンプレートを取得
```

### Step 1: Prompt呼び出し

**MCP リクエスト:**
```json
{
  "method": "prompts/get",
  "params": {
    "name": "setup-handler-queue",
    "arguments": { "app_type": "web" }
  }
}
```

**期待レスポンス概要:**
- 15ハンドラの推奨順序テーブル（FQCN、必須区分、スレッドモデル付き）
- 8件の順序制約詳細（must_be_outer, relative_order等）
- XML設定テンプレート: web-xml, web-component, web-handler-queue, session-store

### Step 2: Resource呼び出し（補完）

**MCP リクエスト:**
```json
{
  "method": "resources/read",
  "params": { "uri": "nablarch://handler/web" }
}
```

**期待レスポンス概要:**
- 15ハンドラの詳細仕様（各ハンドラのFQCN、制約、Required/Optional区分）
- Ordering Constraints Summaryテーブル

### Step 3: Resource呼び出し（セットアップ情報）

**MCP リクエスト:**
```json
{
  "method": "resources/read",
  "params": { "uri": "nablarch://guide/setup" }
}
```

**期待レスポンス概要:**
- web.xmlテンプレート（NablarchServletContextListener、WebFrontController設定）
- コンポーネント定義XMLテンプレート（ハンドラキュー定義含む）
- DB接続設定テンプレート

### AIの期待出力

AIは上記3つのMCPレスポンスを統合し、以下を生成:

1. **web.xml** の完全な設定ファイル
2. **コンポーネント定義XML** の完全な設定ファイル（ハンドラキュー付き）
3. 各ハンドラの役割と順序の理由の説明
4. セッションストア・DB接続の設定手順

---

## シナリオ2: アクションクラスの実装ガイド

### ユーザー入力

> 「ユーザー登録画面のアクションクラスを作りたい。RESTじゃなくてWebアプリ（JSP）で。」

### 期待されるMCP呼び出しフロー

```
Step 1: Prompt → create-action (app_type: "web", action_name: "UserRegistrationAction")
  ↓ アクションクラスのスケルトン・推奨パターンを取得
Step 2: Tool → search_api (keyword: "form-validation")
  ↓ バリデーション関連のAPIパターンを検索
Step 3: Resource → nablarch://guide/validation
  ↓ バリデーションガイドでフォームバリデーションの詳細を取得
```

### Step 1: Prompt呼び出し

**MCP リクエスト:**
```json
{
  "method": "prompts/get",
  "params": {
    "name": "create-action",
    "arguments": { "app_type": "web", "action_name": "UserRegistrationAction" }
  }
}
```

**期待レスポンス概要:**
- 推奨パターン6件: action-class, inject-form-on-error, http-response-navigation, session-store-access, double-submit-prevention, file-download
- 各パターンのJavaコード例
- 命名規則（doプレフィックス、パッケージ構造）

### Step 2: Tool呼び出し

**MCP リクエスト:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "searchApi",
    "arguments": { "keyword": "form-validation", "category": "" }
  }
}
```

**期待レスポンス概要:**
- form-validation APIパターン（Bean Validationアノテーション、InjectForm使用法）
- 関連するFQCN情報

### Step 3: Resource呼び出し

**MCP リクエスト:**
```json
{
  "method": "resources/read",
  "params": { "uri": "nablarch://guide/validation" }
}
```

**期待レスポンス概要:**
- APIパターン: form-validation, inject-form-on-error
- デザインパターン: form-validation-pattern
- Javaコード例とFQCN

### AIの期待出力

AIは上記3つのMCPレスポンスを統合し、以下を生成:

1. **UserRegistrationAction.java** のスケルトンコード
   - `doInput()`: 入力画面表示（トークン生成）
   - `doConfirm()`: 確認画面表示（InjectForm + OnError）
   - `doRegister()`: 登録処理（OnDoubleSubmission）
   - `doComplete()`: 完了画面表示
2. **UserRegistrationForm.java** のバリデーション付きフォームクラス
3. フォームバリデーションの設定説明
4. 二重送信防止の説明

---

## シナリオ3: 設定ファイルレビュー

### ユーザー入力

> 「このハンドラキューの設定をレビューして。」
> （以下のXMLを貼り付け）
> ```xml
> <property name="handlerQueue">
>   <list>
>     <component class="nablarch.fw.handler.GlobalErrorHandler"/>
>     <component class="nablarch.fw.web.handler.HttpCharacterEncodingHandler"/>
>     <component class="nablarch.fw.web.handler.HttpResponseHandler"/>
>     <component class="nablarch.common.handler.DbConnectionManagementHandler"/>
>     <component class="nablarch.common.handler.TransactionManagementHandler"/>
>     <component class="nablarch.integration.router.RoutesMapping"/>
>   </list>
> </property>
> ```

### 期待されるMCP呼び出しフロー

```
Step 1: Prompt → review-config (config_xml: "<property ...>...")
  ↓ 設定レビューガイド（制約チェックリスト、問題パターン）を取得
Step 2: Tool → validate_handler_queue (handlerQueueXml: "...", applicationType: "web")
  ↓ 自動検証で問題を検出
Step 3: Resource → nablarch://handler/web
  ↓ 正しいハンドラ順序を参照
```

### Step 1: Prompt呼び出し

**MCP リクエスト:**
```json
{
  "method": "prompts/get",
  "params": {
    "name": "review-config",
    "arguments": { "config_xml": "<property name=\"handlerQueue\">..." }
  }
}
```

**期待レスポンス概要:**
- レビュー対象XMLの表示
- 24件のハンドラ順序制約チェックリスト
- よくある問題パターン（handler/configカテゴリのエラー）
- 一般的な確認事項

### Step 2: Tool呼び出し（自動検証）

**MCP リクエスト:**
```json
{
  "method": "tools/call",
  "params": {
    "name": "validateHandlerQueue",
    "arguments": {
      "handlerQueueXml": "<property name=\"handlerQueue\">...",
      "applicationType": "web"
    }
  }
}
```

**期待レスポンス概要:**
- 検証結果: **NG**
- 検出される問題:
  - 順序違反: HttpCharacterEncodingHandler は GlobalErrorHandler より前に配置すべき
  - 必須ハンドラ不足: SecureHandler, SessionStoreHandler, ThreadContextHandler, ThreadContextClearHandler

### Step 3: Resource呼び出し（正しい順序の参照）

**MCP リクエスト:**
```json
{
  "method": "resources/read",
  "params": { "uri": "nablarch://handler/web" }
}
```

**期待レスポンス概要:**
- 正しい順序の15ハンドラ一覧
- 各ハンドラのRequired/Optional区分

### AIの期待出力

AIは上記3つのMCPレスポンスを統合し、以下を生成:

1. **検出された問題の一覧**
   - 順序違反: HttpCharacterEncodingHandler が GlobalErrorHandler の後にある
   - 不足ハンドラ: SecureHandler, SessionStoreHandler, ThreadContextHandler等
2. **修正済みXML**
   - 正しい順序でハンドラを再配置
   - 不足ハンドラを追加
3. **各修正の理由**
   - なぜHttpCharacterEncodingHandlerが最初に必要か
   - なぜSecureHandlerが必要か、等

---

## Primitive間の連携パターン

上記シナリオから抽出される典型的なMCP Primitive連携パターン:

### パターンA: Prompt → Resource（知識補完）

```
Prompt（概要・ガイド生成）→ Resource（詳細データ取得）
```

Promptが全体的なガイドを生成し、Resourceが詳細な仕様データを補完する。

### パターンB: Prompt → Tool（自動検証）

```
Prompt（レビューガイド生成）→ Tool（自動検証実行）
```

Promptがレビュー観点を提示し、Toolが実際の検証を実行する。

### パターンC: Tool → Resource（結果の文脈化）

```
Tool（検索/検証）→ Resource（関連情報の文脈提供）
```

Toolが検索・検証結果を返し、Resourceが結果を理解するための文脈を提供する。

### パターンD: Prompt → Tool → Resource（フル連携）

```
Prompt（初期ガイド）→ Tool（データ取得/検証）→ Resource（詳細参照）
```

最も包括的なパターン。シナリオ1〜3の全てがこのパターンに該当。

---

## テスト結果

| シナリオ | Prompt | Tool | Resource | 連携パターン | 状態 |
|---------|--------|------|----------|-------------|------|
| 1: ハンドラキュー設定 | setup-handler-queue | — | handler/web, guide/setup | A (Prompt→Resource) | シミュレーション完了 |
| 2: アクションクラス実装 | create-action | search_api | guide/validation | D (Prompt→Tool→Resource) | シミュレーション完了 |
| 3: 設定ファイルレビュー | review-config | validate_handler_queue | handler/web | D (Prompt→Tool→Resource) | シミュレーション完了 |

全3シナリオのMCPリクエスト/レスポンスパターンを定義済み。
MCP Inspectorテスト（Test #1〜#15）で各Primitiveの個別動作は確認済みのため、
上記シナリオの各Stepは実行可能な状態です。

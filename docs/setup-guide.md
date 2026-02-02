# Nablarch MCP Server セットアップガイド

Nablarch MCP Serverは、AIアシスタント（Claude等）がNablarchフレームワークの知識にアクセスし、
開発支援を行うためのMCP（Model Context Protocol）サーバーです。

## 前提条件

| 項目 | 要件 |
|------|------|
| Java | JDK 17以上 |
| ビルドツール | Gradle 8.x（Gradle Wrapperを同梱） |
| MCPクライアント | Claude Desktop、Claude Code、またはMCP Inspector |
| OS | Windows、macOS、Linux |

## インストール手順

### 1. リポジトリのクローン

```bash
git clone https://github.com/kumagoro1202/nablarch-mcp-server.git
cd nablarch-mcp-server
```

### 2. ビルド

```bash
./gradlew bootJar
```

ビルドが成功すると、以下にJARファイルが生成されます。

```
build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar
```

### 3. 動作確認（単体）

サーバーが正しく起動するか確認します。

```bash
java -jar build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar
```

サーバーはSTDIO（標準入出力）トランスポートで動作するため、起動後はJSON-RPCメッセージを待機します。
`Ctrl+C` で終了してください。

## MCPクライアント設定

### Claude Desktop

`claude_desktop_config.json` に以下を追加します。

**設定ファイルの場所:**
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`
- Linux: `~/.config/Claude/claude_desktop_config.json`

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

`/path/to/` は実際のJARファイルのパスに置き換えてください。

### Claude Code

プロジェクトルートの `.mcp.json` に以下を追加します。

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

設定後、Claude Code起動時にMCPサーバーが自動的に接続されます。

### MCP Inspector（開発・デバッグ用）

MCP Inspectorを使ってサーバーの動作を対話的に確認できます。

```bash
npx @modelcontextprotocol/inspector java -jar build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar
```

ブラウザが開き、Tools / Resources / Prompts の一覧と実行テストが可能です。

## 動作確認

### MCP Inspectorでの確認手順

1. 上記コマンドでMCP Inspectorを起動
2. 左メニューから **Tools** を選択 → `search_api`、`validate_handler_queue` が表示されることを確認
3. 左メニューから **Resources** を選択 → 12個のリソースURIが表示されることを確認
4. 左メニューから **Prompts** を選択 → 6個のプロンプトが表示されることを確認

### テストコマンド例

MCP Inspectorの **Tools** タブで以下を試行できます。

**search_api の実行例:**
- keyword: `UniversalDao`
- category: （空欄）
- → UniversalDaoに関するAPIパターン・モジュール情報が返却されます

**validate_handler_queue の実行例:**
- applicationType: `web`
- handlerQueueXml:
  ```xml
  <component-configuration>
    <component class="nablarch.fw.web.handler.HttpCharacterEncodingHandler"/>
    <component class="nablarch.common.handler.threadcontext.ThreadContextHandler"/>
    <component class="nablarch.fw.handler.GlobalErrorHandler"/>
  </component-configuration>
  ```
- → ハンドラキューの順序検証結果（不足ハンドラの警告等）が返却されます

## 利用可能な機能一覧

### Tools（2種）

| Tool名 | 説明 | 用途 |
|---------|------|------|
| `search_api` | Nablarch APIドキュメント検索 | クラス名・メソッド名・概念でNablarchの知識を検索 |
| `validate_handler_queue` | ハンドラキューXML検証 | ハンドラの順序制約・必須ハンドラ・互換性をチェック |

#### search_api

キーワードとオプションのカテゴリフィルタでNablarchの知識ベースを横断検索します。

```
パラメータ:
  keyword (必須): 検索キーワード（クラス名、メソッド名、概念）
  category (任意): カテゴリフィルタ（handler, library, web, batch, rest, messaging）
```

使用例（Claude対話内）:
> 「NablarchのUniversalDaoの使い方を教えて」
> → AIがsearch_apiツールを呼び出し、UniversalDaoのAPIパターン・コード例を取得

#### validate_handler_queue

NablarchハンドラキューのXML設定を検証し、順序制約違反や必須ハンドラの不足を検出します。

```
パラメータ:
  handlerQueueXml (必須): ハンドラキューXML設定内容
  applicationType (必須): アプリケーションタイプ（web, rest, batch, messaging）
```

使用例（Claude対話内）:
> 「このハンドラキュー設定をレビューして」（XMLを貼り付け）
> → AIがvalidate_handler_queueツールを呼び出し、設定の問題点を指摘

### Resources（12種）

MCP Resources は読み取り専用の知識エンドポイントです。AIアシスタントが必要に応じてアクセスします。

#### handler/{app_type}（6種）

アプリケーションタイプ別のハンドラキュー仕様をMarkdown形式で提供します。

| URI | 説明 |
|-----|------|
| `nablarch://handler/web` | Webアプリケーションのハンドラキュー仕様 |
| `nablarch://handler/rest` | RESTアプリケーションのハンドラキュー仕様 |
| `nablarch://handler/batch` | バッチアプリケーションのハンドラキュー仕様 |
| `nablarch://handler/messaging` | メッセージングアプリケーションのハンドラキュー仕様 |
| `nablarch://handler/http-messaging` | HTTPメッセージングのハンドラキュー仕様 |
| `nablarch://handler/jakarta-batch` | Jakarta Batchのハンドラキュー仕様 |

各リソースには以下の情報が含まれます:
- ハンドラ一覧（FQCN、スレッド情報、説明、必須/任意区分）
- ハンドラ間の順序制約
- アプリタイプ固有の制約サマリ

#### guide/{topic}（6種）

トピック別の開発ガイドをMarkdown形式で提供します。

| URI | 説明 | データソース |
|-----|------|------------|
| `nablarch://guide/setup` | プロジェクトセットアップガイド | config-templates.yaml |
| `nablarch://guide/testing` | テストパターンガイド | api-patterns.yaml |
| `nablarch://guide/validation` | バリデーションガイド | api-patterns.yaml, design-patterns.yaml |
| `nablarch://guide/database` | データベースアクセスガイド | api-patterns.yaml, config-templates.yaml |
| `nablarch://guide/handler-queue` | ハンドラキューガイド | handler-catalog.yaml, handler-constraints.yaml |
| `nablarch://guide/error-handling` | エラーハンドリングガイド | error-catalog.yaml |

### Prompts（6種）

MCP Promptsはテンプレート化された対話パターンです。AIアシスタントがNablarch開発タスクを支援する際に使用します。

| Prompt名 | 説明 | 引数 |
|-----------|------|------|
| `setup-handler-queue` | ハンドラキュー構成の支援 | `app_type` (必須) |
| `create-action` | アクションクラススケルトン生成 | `app_type` (必須), `action_name` (必須) |
| `review-config` | XML設定ファイルのレビュー | `config_xml` (必須) |
| `explain-handler` | ハンドラの詳細説明 | `handler_name` (必須) |
| `migration-guide` | バージョン移行ガイド | `from_version` (必須), `to_version` (必須) |
| `best-practices` | ベストプラクティス参照 | `topic` (必須) |

#### 使用例

**setup-handler-queue:**
> 「Webアプリケーションのハンドラキューを構成して」
> → Promptが呼び出され、推奨ハンドラ一覧・順序・XMLテンプレートが提示されます

**create-action:**
> 「RESTアプリのUserActionクラスを作って」
> → Promptが呼び出され、アクションクラスのスケルトン・推奨パターンが提示されます

**review-config:**
> 「この設定XMLをレビューして」（XML貼り付け）
> → Promptが呼び出され、順序制約チェック・一般的な問題パターンが提示されます

**explain-handler:**
> 「HttpCharacterEncodingHandlerについて教えて」
> → Promptが呼び出され、ハンドラの詳細情報・制約・使用箇所が提示されます

**migration-guide:**
> 「Nablarch 5u21から6への移行ガイドを見せて」
> → Promptが呼び出され、モジュール一覧・移行ステップが提示されます

**best-practices:**
> 「データベースアクセスのベストプラクティスを教えて」
> → Promptが呼び出され、設計パターン・推奨実装パターンが提示されます

## トラブルシューティング

### サーバーが起動しない

**症状:** `java -jar` コマンドでエラーが発生する

| エラー | 原因 | 対処法 |
|--------|------|--------|
| `UnsupportedClassVersionError` | Javaバージョンが17未満 | `java -version` で確認し、JDK 17以上をインストール |
| `FileNotFoundException: knowledge/*.yaml` | JARファイルが破損 | `./gradlew clean bootJar` で再ビルド |
| `Address already in use` | 別プロセスがポートを使用 | Phase 1はSTDIO transportのため通常発生しない |

### MCPクライアントから接続できない

**症状:** Claude DesktopまたはClaude CodeでNablarchサーバーが認識されない

1. **JARパスを確認**: `claude_desktop_config.json` / `.mcp.json` のパスが正しいか確認
2. **Javaパスを確認**: `command` の `java` がPATHに存在するか確認。フルパス指定も可能:
   ```json
   "command": "/path/to/java"
   ```
3. **設定ファイルを再読み込み**: Claude Desktopを再起動、Claude Codeを再起動
4. **ログを確認**: サーバーのログレベルはデフォルトで `WARN`。詳細ログが必要な場合:
   ```json
   "args": [
     "-jar",
     "/path/to/nablarch-mcp-server-0.1.0-SNAPSHOT.jar",
     "--logging.level.com.tis.nablarch.mcp=DEBUG"
   ]
   ```

### ツール実行でエラーが返る

**症状:** search_apiやvalidate_handler_queueがエラーメッセージを返す

| メッセージ | 原因 | 対処法 |
|-----------|------|--------|
| 「検索キーワードを指定してください」 | keywordが空 | キーワードを入力 |
| 「検索結果なし」 | マッチする知識がない | 別のキーワード・カテゴリを試行 |
| 「ハンドラキューXMLを指定してください」 | XMLが空 | XMLコンテンツを入力 |
| 「アプリケーションタイプを指定してください」 | appTypeが空 | web, rest, batch, messagingのいずれかを指定 |
| 「XMLからハンドラクラスを抽出できませんでした」 | XML形式が不正 | `class="FQCN"` 属性を持つXMLを指定 |

### ログ確認方法

サーバーのログ設定（`application.yaml`）:

```yaml
logging:
  level:
    root: WARN
    com.tis.nablarch.mcp: INFO
```

デバッグ情報が必要な場合は、起動時に以下のオプションを追加:

```bash
java -jar nablarch-mcp-server-0.1.0-SNAPSHOT.jar \
  --logging.level.com.tis.nablarch.mcp=DEBUG
```

知識ベースの読み込み状況やツール呼び出しの詳細が出力されます。

## 技術仕様

| 項目 | 値 |
|------|-----|
| フレームワーク | Spring Boot 3.4.2 |
| MCP SDK | Spring AI MCP Server (BOM 1.0.0) |
| Java | 17 |
| トランスポート | STDIO（標準入出力） |
| プロトコル | JSON-RPC 2.0（MCP仕様準拠） |
| サーバー名 | nablarch-mcp-server |
| バージョン | 0.1.0 |

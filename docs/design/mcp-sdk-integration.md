# WBS 1.1.2: MCP SDK統合設計

> **作成日**: 2026-02-02
> **作成者**: ashigaru2 (subtask_042)

## 1. MCP Java SDK Spring Boot Auto-configuration

### 使用するモジュール

| モジュール | 用途 |
|-----------|------|
| `org.springframework.ai:spring-ai-starter-mcp-server` | Spring AI MCP Server STDIO統合 |
| `org.springframework.ai:spring-ai-bom:1.0.0` | BOMによるバージョン管理 |

> **実装時の変更**: 当初設計ではMCP SDK 0.17.0 BOを直接使用する予定だったが、
> 実装時にSpring AI 1.0.0 Starterに変更。理由: `@Tool`/`ToolCallbackProvider`等の
> Spring AIアノテーション・クラスが必要なため。Spring AI 1.0.0はMCP SDK 0.10.0を同梱。

### Auto-configurationの仕組み

`spring-ai-starter-mcp-server` を依存関係に追加すると、Spring Boot Auto-configurationにより以下が自動設定される:

1. **McpServerAutoConfiguration**: MCPサーバーの基盤Bean生成
2. **Transport設定**: `spring.ai.mcp.server.stdio=true` によりSTDIOトランスポートが有効化
3. **ToolCallbackProvider検出**: `@Bean` として登録されたToolCallbackProviderを自動検出し、MCPサーバーにTool登録
4. **Resource/Prompt**: `SyncResourceSpecification` / `SyncPromptSpecification` Beanを自動検出

### 重要な設定キー

```yaml
spring:
  ai:
    mcp:
      server:
        name: nablarch-mcp-server     # サーバー名（MCP initialize応答に含まれる）
        version: 0.1.0                 # サーバーバージョン
        type: SYNC                     # SYNC or ASYNC（Phase 1はSYNC）
        stdio: true                    # STDIOトランスポート有効化
        resource-change-notification: true   # Resource変更通知（オプション）
        prompt-change-notification: true     # Prompt変更通知（オプション）
```

## 2. STDIOトランスポート設定詳細

### 動作原理

```
┌──────────────┐  stdin (JSON-RPC)  ┌──────────────────┐
│   AI Client  │ ─────────────────> │  Nablarch MCP    │
│ (Claude Code)│ <───────────────── │  Server (JVM)    │
└──────────────┘  stdout (JSON-RPC) └──────────────────┘
```

- **stdin**: クライアントからのJSON-RPCリクエスト受信
- **stdout**: サーバーからのJSON-RPCレスポンス送信
- **stderr**: アプリケーションログ（JSON-RPCと干渉しないよう分離）

### 設定要件

1. `spring.main.web-application-type: none` — Webサーバーを起動しない
2. `spring.main.banner-mode: off` — Spring Bootバナーをstdoutに出力しない
3. `logging.pattern.console:` — コンソールログパターンを空にする（stdout保護）
4. ログはファイルに出力する

### 起動方法

```bash
# Gradleで直接起動
./gradlew bootRun

# JARで起動
java -jar build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar

# Claude Code MCP設定（claude_desktop_config.json）
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": ["-jar", "/path/to/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"]
    }
  }
}
```

## 3. Tool登録パターン

### 現行方式（@Tool + ToolCallbackProvider）

```java
// Tool実装（既存パターン）
@Service
public class SearchApiTool {
    @Tool(description = "Search the Nablarch API documentation...")
    public String searchApi(
            @ToolParam(description = "Search keyword") String keyword,
            @ToolParam(description = "Category filter") String category) {
        // 実装
    }
}

// McpServerConfig で登録
@Bean
public ToolCallbackProvider nablarchTools(
        SearchApiTool searchApiTool,
        ValidateHandlerQueueTool validateHandlerQueueTool) {
    return MethodToolCallbackProvider.builder()
            .toolObjects(searchApiTool, validateHandlerQueueTool)
            .build();
}
```

**この方式は既存コードで採用済み。変更不要。**

### Phase 1で登録するTool一覧

| Tool名 | クラス | Phase 1での状態 |
|--------|-------|---------------|
| search_api | SearchApiTool | stub（既存） |
| validate_handler_queue | ValidateHandlerQueueTool | stub（既存） |

## 4. Resource登録パターン

### MCP SDK Resource API

MCP SDKでは`McpServerFeatures.SyncResourceSpecification` Beanを定義してResourceを登録する。

```java
@Bean
public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
        HandlerResource handlerResource,
        ApiSpecResource apiSpecResource) {
    return List.of(
        new McpServerFeatures.SyncResourceSpecification(
            new McpSchema.Resource(
                "nablarch://handler/web",
                "Nablarch Web Handler Catalog",
                "Web application handler specifications and ordering constraints",
                "text/markdown", null),
            (exchange, request) -> new McpSchema.ReadResourceResult(
                List.of(new McpSchema.ResourceContents.TextResourceContents(
                    request.uri(), "text/markdown",
                    handlerResource.getHandlerSpec("web"))))
        ),
        // 他のリソースも同様に登録
    );
}
```

### Phase 1で登録するResource一覧

| URI | 説明 | 提供クラス |
|-----|------|----------|
| nablarch://handler/web | Webハンドラカタログ | HandlerResource |
| nablarch://handler/rest | RESTハンドラカタログ | HandlerResource |
| nablarch://handler/batch | バッチハンドラカタログ | HandlerResource |
| nablarch://handler/messaging | メッセージングハンドラカタログ | HandlerResource |
| nablarch://guide/setup | セットアップガイド | （Phase 1 stub） |
| nablarch://guide/testing | テストガイド | （Phase 1 stub） |

## 5. Prompt登録パターン

### MCP SDK Prompt API

```java
@Bean
public List<McpServerFeatures.SyncPromptSpecification> nablarchPrompts() {
    return List.of(
        new McpServerFeatures.SyncPromptSpecification(
            new McpSchema.Prompt("setup-handler-queue",
                "Set up a Nablarch handler queue configuration",
                List.of(new McpSchema.PromptArgument(
                    "app_type", "Application type: web, rest, batch, messaging", true))),
            (exchange, request) -> {
                String appType = request.arguments().getOrDefault("app_type", "web");
                return new McpSchema.GetPromptResult(
                    "Setup handler queue for " + appType,
                    List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent("...")))
                );
            }
        )
    );
}
```

### Phase 1で登録するPrompt一覧

| Prompt名 | 引数 | 説明 |
|----------|------|------|
| setup-handler-queue | app_type | ハンドラキュー構成のセットアップ |
| create-action | app_type, action_name | アクションクラスの雛形生成 |
| review-config | config_xml | XML設定ファイルのレビュー |
| explain-handler | handler_name | ハンドラの説明取得 |
| migration-guide | from_version, to_version | バージョン移行ガイド |
| best-practices | topic | ベストプラクティス取得 |

## 6. JSON-RPC 2.0ハンドラマッピング

MCPプロトコルのJSON-RPCメソッドとサーバー側の処理対応:

| JSON-RPCメソッド | 処理 | 管理 |
|-----------------|------|------|
| `initialize` | サーバー能力通知 | SDK自動 |
| `initialized` | 初期化完了通知 | SDK自動 |
| `tools/list` | Tool一覧返却 | SDK自動（ToolCallbackProviderから生成） |
| `tools/call` | Tool実行 | SDK自動 → @Toolメソッド呼び出し |
| `resources/list` | Resource一覧返却 | SDK自動（SyncResourceSpecificationから生成） |
| `resources/read` | Resource読み込み | SDK自動 → ハンドラ呼び出し |
| `prompts/list` | Prompt一覧返却 | SDK自動（SyncPromptSpecificationから生成） |
| `prompts/get` | Prompt取得 | SDK自動 → ハンドラ呼び出し |
| `ping` | ヘルスチェック | SDK自動 |

**アプリケーション側でJSON-RPCハンドラを直接実装する必要はない。** SDK Auto-configurationが全て管理する。

## 7. サーバー初期化シーケンス

```
1. JVM起動
2. Spring Boot起動（SpringApplication.run）
3. Auto-configuration:
   a. McpServerAutoConfiguration 読み込み
   b. ToolCallbackProvider Bean検出 → Tool登録
   c. SyncResourceSpecification Bean検出 → Resource登録
   d. SyncPromptSpecification Bean検出 → Prompt登録
   e. STDIOTransportProvider生成（stdio=true）
4. ApplicationReadyEvent
   → STDIOトランスポート開始
   → stdin/stdoutでJSON-RPCメッセージ待受開始
5. クライアント接続
   → initialize リクエスト受信
   → サーバー能力（tools, resources, prompts）を応答
   → initialized 通知受信
   → 通常のtool/resource/promptリクエスト処理開始
```

## 8. 既存McpServerConfig.java拡張計画

### 現行（Tool登録のみ）

```java
@Configuration
public class McpServerConfig {
    @Bean
    public ToolCallbackProvider nablarchTools(...) { ... }
}
```

### 拡張後（Tool + Resource + Prompt登録）

```java
@Configuration
public class McpServerConfig {
    // 既存: Tool登録（変更なし）
    @Bean
    public ToolCallbackProvider nablarchTools(...) { ... }

    // 新規: Resource登録
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(...) { ... }

    // 新規: Prompt登録
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> nablarchPrompts() { ... }
}
```

**Phase 1ではResource/Promptのハンドラ内容はstub（固定文字列返却）でよい。** 登録の仕組みを確立することが目的。

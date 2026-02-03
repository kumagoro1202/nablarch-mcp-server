# WBS 3.1.9: Streamable HTTPトランスポート設計

> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru7 (subtask_066)
> **関連ドキュメント**: [02_mcp-sdk-integration.md](02_mcp-sdk-integration.md), [architecture.md](../architecture.md)
> **依存関係**: WBS 1.1.2（MCP SDK統合）

## 目次

1. [概要](#1-概要)
2. [MCP仕様 Streamable HTTP Transport](#2-mcp仕様-streamable-http-transport)
3. [エンドポイント設計](#3-エンドポイント設計)
4. [JSON-RPCマッピング](#4-json-rpcマッピング)
5. [SSEストリーミング設計](#5-sseストリーミング設計)
6. [セッション管理設計](#6-セッション管理設計)
7. [Spring Boot統合設計](#7-spring-boot統合設計)
8. [CORSポリシー設計](#8-corsポリシー設計)
9. [Originヘッダ検証設計](#9-originヘッダ検証設計)
10. [エラーレスポンス設計](#10-エラーレスポンス設計)
11. [パフォーマンス設計](#11-パフォーマンス設計)
12. [STDIO/HTTP切替設計](#12-stdiohttp切替設計)
13. [後方互換性](#13-後方互換性)

---

## 1. 概要

### 1.1 背景

Phase 1-2ではSTDIOトランスポートを使用してMCPサーバーを実装した。Phase 3ではHTTPトランスポートを追加し、Webベースのクライアントやリモートアクセスに対応する。

### 1.2 目的

- MCP仕様（2025-03-26版）のStreamable HTTP Transportを実装
- 既存のSTDIOトランスポートとの共存（プロファイル切替）
- 将来のプロダクション環境への対応基盤

### 1.3 スコープ

| 項目 | Phase 3（本設計） | Phase 4（将来） |
|------|------------------|----------------|
| HTTPエンドポイント | `/mcp` 実装 | - |
| セッション管理 | 基本実装 | 分散セッション |
| Originヘッダ検証 | 設計のみ | 実装 |
| 認証・認可 | - | 実装 |
| TLS | - | 実装 |

---

## 2. MCP仕様 Streamable HTTP Transport

### 2.1 仕様バージョン

本設計はMCP仕様 **2025-03-26版** に基づく。

> 参照: https://modelcontextprotocol.io/specification/2025-03-26/basic/transports

### 2.2 トランスポート特性

| 特性 | 説明 |
|------|------|
| プロトコル | HTTP/1.1 または HTTP/2 |
| メッセージ形式 | JSON-RPC 2.0 |
| ストリーミング | Server-Sent Events (SSE) |
| セッション | ステートフル（オプション） |

### 2.3 HTTPメソッドと用途

| HTTPメソッド | 用途 |
|-------------|------|
| POST | JSON-RPCメッセージの送信（リクエスト、レスポンス、通知） |
| GET | SSEストリームの確立（サーバーからクライアントへの通知） |
| DELETE | セッションの終了 |

---

## 3. エンドポイント設計

### 3.1 エンドポイントパス

```
/mcp
```

すべてのMCPトランスポート操作は単一エンドポイントで処理される。

### 3.2 POST /mcp

クライアントからサーバーへのJSON-RPCメッセージ送信。

#### リクエスト

```http
POST /mcp HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Mcp-Session-Id: <session-id>  # セッション確立後は必須

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

#### レスポンスパターン

**パターン1: 即時レスポンス（Content-Type: application/json）**

```http
HTTP/1.1 200 OK
Content-Type: application/json
Mcp-Session-Id: <session-id>

{
  "jsonrpc": "2.0",
  "id": 1,
  "result": { ... }
}
```

**パターン2: SSEストリームレスポンス（Content-Type: text/event-stream）**

長時間実行されるリクエストの場合、サーバーはSSEストリームで複数メッセージを返却可能。

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Mcp-Session-Id: <session-id>

event: message
data: {"jsonrpc":"2.0","id":1,"result":{...}}

event: message
data: {"jsonrpc":"2.0","method":"notifications/progress","params":{...}}
```

**パターン3: 通知受理（202 Accepted）**

クライアントからの通知（idなしメッセージ）の場合。

```http
HTTP/1.1 202 Accepted
```

### 3.3 GET /mcp

サーバーからクライアントへの非同期通知用SSEストリーム。

#### リクエスト

```http
GET /mcp HTTP/1.1
Host: localhost:8080
Accept: text/event-stream
Mcp-Session-Id: <session-id>
```

#### レスポンス

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

event: message
data: {"jsonrpc":"2.0","method":"notifications/resources/updated","params":{...}}

event: message
data: {"jsonrpc":"2.0","method":"notifications/tools/list_changed","params":{}}
```

### 3.4 DELETE /mcp

セッションの明示的終了。

#### リクエスト

```http
DELETE /mcp HTTP/1.1
Host: localhost:8080
Mcp-Session-Id: <session-id>
```

#### レスポンス

```http
HTTP/1.1 200 OK
```

---

## 4. JSON-RPCマッピング

### 4.1 リクエスト/レスポンス対応

| MCP操作 | JSON-RPCメソッド | HTTPメソッド |
|---------|-----------------|--------------|
| 初期化 | `initialize` | POST |
| 初期化完了通知 | `notifications/initialized` | POST |
| Tool一覧取得 | `tools/list` | POST |
| Tool実行 | `tools/call` | POST |
| Resource一覧取得 | `resources/list` | POST |
| Resource読み込み | `resources/read` | POST |
| Prompt一覧取得 | `prompts/list` | POST |
| Prompt取得 | `prompts/get` | POST |
| ヘルスチェック | `ping` | POST |

### 4.2 バッチリクエスト

JSON-RPC 2.0のバッチリクエストをサポート。

```json
[
  {"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}},
  {"jsonrpc": "2.0", "id": 2, "method": "resources/list", "params": {}}
]
```

レスポンス:

```json
[
  {"jsonrpc": "2.0", "id": 1, "result": {"tools": [...]}},
  {"jsonrpc": "2.0", "id": 2, "result": {"resources": [...]}}
]
```

---

## 5. SSEストリーミング設計

### 5.1 SSEイベント形式

```
event: message
data: <JSON-RPC message>

event: message
data: <JSON-RPC message>
```

- `event`: 常に `message`
- `data`: JSON-RPCメッセージ（1行）

### 5.2 ストリーム用途

| 用途 | 説明 |
|------|------|
| サーバープッシュ通知 | Resource変更、Tool変更の通知 |
| 進捗報告 | 長時間実行タスクの進捗 |
| ログストリーム | 実行ログのリアルタイム配信 |

### 5.3 ハートビート

接続維持のため、30秒間隔でコメント行を送信。

```
: heartbeat
```

### 5.4 Spring実装

```java
@GetMapping(value = "/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamEvents(
        @RequestHeader("Mcp-Session-Id") String sessionId) {
    return sessionManager.getNotificationStream(sessionId)
        .map(msg -> ServerSentEvent.<String>builder()
            .event("message")
            .data(objectMapper.writeValueAsString(msg))
            .build());
}
```

---

## 6. セッション管理設計

### 6.1 セッションID

| 項目 | 仕様 |
|------|------|
| 形式 | UUID v4 |
| ヘッダ名 | `Mcp-Session-Id` |
| 生成タイミング | `initialize` レスポンス時 |

### 6.2 セッションライフサイクル

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  [Client]                           [Server]                    │
│     │                                   │                       │
│     │  POST /mcp (initialize)           │                       │
│     │ ─────────────────────────────────>│                       │
│     │                                   │ Session作成           │
│     │  200 OK + Mcp-Session-Id          │ UUID生成              │
│     │ <─────────────────────────────────│                       │
│     │                                   │                       │
│     │  POST /mcp (notifications/init)   │                       │
│     │  Mcp-Session-Id: xxx              │                       │
│     │ ─────────────────────────────────>│                       │
│     │                                   │                       │
│     │  GET /mcp (SSE stream)            │                       │
│     │  Mcp-Session-Id: xxx              │                       │
│     │ ─────────────────────────────────>│                       │
│     │ <─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │ SSE接続維持           │
│     │                                   │                       │
│     │  POST /mcp (tools/call, etc.)     │                       │
│     │  Mcp-Session-Id: xxx              │                       │
│     │ ─────────────────────────────────>│                       │
│     │                                   │                       │
│     │  DELETE /mcp                      │                       │
│     │  Mcp-Session-Id: xxx              │                       │
│     │ ─────────────────────────────────>│                       │
│     │                                   │ Session破棄           │
│     │  200 OK                           │                       │
│     │ <─────────────────────────────────│                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 セッション設定

```yaml
mcp:
  http:
    session:
      timeout: 30m           # セッションタイムアウト
      max-sessions: 100      # 最大同時セッション数
      cleanup-interval: 5m   # クリーンアップ間隔
```

### 6.4 セッションデータ構造

```java
public class McpSession {
    private final String sessionId;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;
    private final McpSchema.ClientCapabilities clientCapabilities;
    private final Sinks.Many<McpSchema.JSONRPCMessage> notificationSink;

    // セッション状態
    public enum State {
        INITIALIZING,  // initialize受信後
        ACTIVE,        // initialized通知後
        CLOSING,       // 終了処理中
        CLOSED         // 終了済み
    }
}
```

---

## 7. Spring Boot統合設計

### 7.1 プロファイル構成

| プロファイル | トランスポート | 用途 |
|-------------|---------------|------|
| `stdio` | STDIO | Claude Code等のローカルクライアント |
| `http` | Streamable HTTP | Webクライアント、リモートアクセス |

### 7.2 設定ファイル構成

**application.yaml（共通設定）**

```yaml
spring:
  ai:
    mcp:
      server:
        name: nablarch-mcp-server
        version: ${project.version}
        type: SYNC
```

**application-stdio.yaml**

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: off
  ai:
    mcp:
      server:
        stdio: true

logging:
  pattern:
    console: ""
  file:
    name: logs/nablarch-mcp-server.log
```

**application-http.yaml**

```yaml
spring:
  main:
    web-application-type: servlet
  ai:
    mcp:
      server:
        stdio: false

server:
  port: 8080

mcp:
  http:
    enabled: true
    endpoint: /mcp
    session:
      timeout: 30m
      max-sessions: 100
    cors:
      allowed-origins:
        - "http://localhost:3000"
      allowed-methods:
        - GET
        - POST
        - DELETE
```

### 7.3 Transport Provider設定

MCP Java SDKの `WebMvcStreamableServerTransportProvider` を使用。

```java
@Configuration
@Profile("http")
public class McpHttpTransportConfig {

    @Bean
    public WebMvcStreamableServerTransportProvider mcpTransportProvider(
            ObjectMapper objectMapper) {
        return new WebMvcStreamableServerTransportProvider(
            objectMapper,
            "/mcp"
        );
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRoutes(
            WebMvcStreamableServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }
}
```

### 7.4 起動方法

```bash
# STDIOモード（デフォルト）
java -jar nablarch-mcp-server.jar --spring.profiles.active=stdio

# HTTPモード
java -jar nablarch-mcp-server.jar --spring.profiles.active=http

# 環境変数での指定
export SPRING_PROFILES_ACTIVE=http
java -jar nablarch-mcp-server.jar
```

---

## 8. CORSポリシー設計

### 8.1 CORS設定

```java
@Configuration
@Profile("http")
public class McpCorsConfig {

    @Bean
    public CorsFilter corsFilter(McpHttpProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Content-Type",
            "Mcp-Session-Id",
            "Accept"
        ));
        config.setExposedHeaders(List.of("Mcp-Session-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/mcp", config);
        return new CorsFilter(source);
    }
}
```

### 8.2 CORS設定項目

| 項目 | 設定値 | 説明 |
|------|--------|------|
| allowed-origins | 設定ファイルで指定 | 許可するオリジン |
| allowed-methods | GET, POST, DELETE, OPTIONS | 許可するHTTPメソッド |
| allowed-headers | Content-Type, Mcp-Session-Id, Accept | 許可するリクエストヘッダ |
| exposed-headers | Mcp-Session-Id | クライアントに公開するレスポンスヘッダ |
| allow-credentials | true | 認証情報の送信を許可 |
| max-age | 3600 | プリフライトキャッシュ時間（秒） |

---

## 9. Originヘッダ検証設計

### 9.1 MCP仕様要件

> **MUST**: サーバーはOriginヘッダを検証し、DNSリバインディング攻撃を防止しなければならない。

### 9.2 Phase 3での方針

Phase 3では設計のみ行い、Phase 4で実装する。

### 9.3 検証ロジック（設計）

```java
@Component
@Profile("http")
public class OriginValidator {

    private final List<String> allowedOrigins;
    private final boolean allowLocalhostOrigins;

    public boolean isValidOrigin(String origin) {
        if (origin == null) {
            // ブラウザ以外のクライアント（curl等）は許可
            return true;
        }

        // localhost系は開発環境で許可
        if (allowLocalhostOrigins && isLocalhostOrigin(origin)) {
            return true;
        }

        // 明示的に許可されたオリジンをチェック
        return allowedOrigins.contains(origin);
    }

    private boolean isLocalhostOrigin(String origin) {
        try {
            URI uri = new URI(origin);
            String host = uri.getHost();
            return "localhost".equals(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
```

### 9.4 検証失敗時のレスポンス

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": null,
  "error": {
    "code": -32001,
    "message": "Origin not allowed"
  }
}
```

---

## 10. エラーレスポンス設計

### 10.1 HTTPステータスとJSON-RPCエラーのマッピング

| シナリオ | HTTPステータス | JSON-RPCエラーコード |
|---------|---------------|---------------------|
| 正常レスポンス | 200 OK | - |
| 通知受理 | 202 Accepted | - |
| JSONパースエラー | 400 Bad Request | -32700 (Parse error) |
| 無効なリクエスト | 400 Bad Request | -32600 (Invalid Request) |
| メソッド不明 | 404 Not Found | -32601 (Method not found) |
| パラメータエラー | 400 Bad Request | -32602 (Invalid params) |
| 内部エラー | 500 Internal Server Error | -32603 (Internal error) |
| セッション無効 | 404 Not Found | -32001 (Session not found) |
| Origin検証失敗 | 403 Forbidden | -32002 (Origin not allowed) |
| セッション上限 | 503 Service Unavailable | -32003 (Too many sessions) |

### 10.2 エラーレスポンス形式

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32601,
    "message": "Method not found",
    "data": {
      "method": "unknown/method"
    }
  }
}
```

### 10.3 例外ハンドラ

```java
@RestControllerAdvice
@Profile("http")
public class McpExceptionHandler {

    @ExceptionHandler(McpSessionNotFoundException.class)
    public ResponseEntity<JsonRpcError> handleSessionNotFound(
            McpSessionNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new JsonRpcError(-32001, "Session not found", null));
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<JsonRpcError> handleParseError(
            JsonProcessingException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new JsonRpcError(-32700, "Parse error", e.getMessage()));
    }
}
```

---

## 11. パフォーマンス設計

### 11.1 目標値

| 指標 | 目標 |
|------|------|
| 同時接続数 | 10以上 |
| レスポンス時間（tools/list） | < 100ms |
| レスポンス時間（tools/call） | < 5s（検索処理含む） |
| SSE接続維持時間 | 30分以上 |

### 11.2 スレッドプール設定

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    connection-timeout: 20000
```

### 11.3 SSE接続管理

```java
@Configuration
@Profile("http")
public class SseConfig {

    @Bean
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-");
        return executor;
    }
}
```

### 11.4 セッションメモリ管理

- セッションあたりの推定メモリ: ~50KB
- 最大セッション数100の場合: ~5MB
- タイムアウトによる自動クリーンアップ

---

## 12. STDIO/HTTP切替設計

### 12.1 切替方式

Spring Bootプロファイルによる切替を採用。

### 12.2 共通コンポーネント

以下のコンポーネントはトランスポートに依存せず共通利用される:

- `McpServerConfig` - Tool/Resource/Prompt登録
- `SearchApiTool`, `ValidateHandlerQueueTool` 等のToolクラス
- `NablarchKnowledgeBase` - 知識ベースアクセス
- `EmbeddingService` - 埋め込み生成
- `HybridSearchService` - ハイブリッド検索

### 12.3 トランスポート固有コンポーネント

| コンポーネント | STDIOプロファイル | HTTPプロファイル |
|---------------|------------------|-----------------|
| TransportProvider | StdioServerTransportProvider (SDK自動設定) | WebMvcStreamableServerTransportProvider |
| セッション管理 | 不要（単一接続） | McpSessionManager |
| CORS | 不要 | CorsFilter |

### 12.4 コンポーネント図

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   Common Components                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │  │
│  │  │McpServerConf│  │  Tools      │  │ KnowledgeBase   │  │  │
│  │  │  (Shared)   │  │  (Shared)   │  │   (Shared)      │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                  │
│              ┌───────────────┴───────────────┐                 │
│              ▼                               ▼                  │
│  ┌─────────────────────┐       ┌─────────────────────────┐    │
│  │  @Profile("stdio")  │       │   @Profile("http")      │    │
│  │                     │       │                         │    │
│  │ StdioServerTransport│       │ WebMvcStreamableServer  │    │
│  │     Provider        │       │   TransportProvider     │    │
│  │                     │       │                         │    │
│  │   (Auto-configured  │       │ McpHttpTransportConfig  │    │
│  │    by Spring AI)    │       │ McpSessionManager       │    │
│  │                     │       │ CorsFilter              │    │
│  └──────────┬──────────┘       └───────────┬─────────────┘    │
│             │                               │                  │
└─────────────┼───────────────────────────────┼──────────────────┘
              ▼                               ▼
         ┌────────┐                    ┌────────────┐
         │ STDIO  │                    │ HTTP:8080  │
         │stdin/  │                    │  /mcp      │
         │stdout  │                    │            │
         └────────┘                    └────────────┘
```

---

## 13. 後方互換性

### 13.1 既存STDIO動作への影響

- デフォルトプロファイルは `stdio` を維持
- 既存の `application.yaml` 設定は変更不要
- Claude Code等の既存クライアントはそのまま動作

### 13.2 API互換性

- JSON-RPCメソッド名、パラメータ、レスポンス形式は変更なし
- Tool/Resource/Promptの動作は変更なし

### 13.3 移行パス

1. **Phase 3**: HTTP Transport追加（本設計）
2. **Phase 4**: Origin検証、認証機能追加
3. **将来**: STDIO deprecation（検討）

---

## 付録

### A. MCP仕様参照

- [MCP Specification 2025-03-26 - Transports](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports)
- [MCP Java SDK - Transports](https://modelcontextprotocol.io/sdk/java/mcp-server)

### B. 関連設計書

- [02_mcp-sdk-integration.md](02_mcp-sdk-integration.md) - STDIO Transport設計
- [architecture.md](../architecture.md) - 全体アーキテクチャ

### C. 用語集

| 用語 | 説明 |
|------|------|
| Streamable HTTP | MCP仕様で定義されたHTTPベースのトランスポート |
| SSE | Server-Sent Events。サーバーからクライアントへの一方向ストリーム |
| JSON-RPC 2.0 | MCPで使用されるRPCプロトコル |
| Mcp-Session-Id | HTTPセッション識別用ヘッダ |

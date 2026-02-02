# WBS 1.1.1: Spring Boot基盤設計

> **作成日**: 2026-02-02
> **作成者**: ashigaru2 (subtask_042)

## 1. パッケージ構成

```
com.tis.nablarch.mcp
├── NablarchMcpServerApplication.java   # Spring Bootエントリポイント（既存）
├── config/                              # Spring設定クラス
│   └── McpServerConfig.java            # MCP Server Bean登録（既存・拡張対象）
├── tools/                               # MCP Toolクラス
│   ├── SearchApiTool.java              # API検索ツール（既存stub）
│   └── ValidateHandlerQueueTool.java   # ハンドラキュー検証ツール（既存stub）
├── resources/                           # MCP Resourceプロバイダ
│   ├── ApiSpecResource.java            # API仕様リソース（既存stub）
│   └── HandlerResource.java            # ハンドラリソース（既存stub）
├── prompts/                             # MCP Promptテンプレート（新規）
│   └── （Phase 1ではstubクラスのみ）
├── knowledge/                           # 知識ベースサービス
│   └── NablarchKnowledgeBase.java      # 知識ベース本体（既存stub）
└── common/                              # 共通ユーティリティ（新規）
    └── （Phase 1では必要に応じて追加）
```

## 2. レイヤー定義

```
┌─────────────────────────────────────────────────┐
│  MCP Protocol Layer (JSON-RPC 2.0 over STDIO)   │
│  ← MCP Java SDK + Spring Boot Auto-config管理    │
└──────────────┬──────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────┐
│  Application Layer                               │
│  ┌─────────┐  ┌──────────┐  ┌────────┐         │
│  │  Tools   │  │ Resources│  │ Prompts│         │
│  │ @Service │  │@Component│  │@Component        │
│  └────┬─────┘  └────┬─────┘  └────┬───┘         │
│       └──────────────┼─────────────┘             │
│                      │                           │
│  ┌───────────────────▼─────────────────────────┐ │
│  │  Knowledge Layer                            │ │
│  │  NablarchKnowledgeBase (@Component)         │ │
│  │  - YAML読み込み                              │ │
│  │  - 検索・フィルタリング                        │ │
│  └──────────────────┬──────────────────────────┘ │
└─────────────────────┼────────────────────────────┘
                      │
┌─────────────────────▼────────────────────────────┐
│  Data Layer                                       │
│  classpath:knowledge/                             │
│  - handlers.yaml  (ハンドラカタログ)               │
│  - api-patterns.yaml (APIパターン)                 │
│  - guides.yaml (設計ガイドライン)                   │
│  ※ Batch Bの担当。Phase 1 Batch Aでは変更しない    │
└───────────────────────────────────────────────────┘
```

**レイヤー間の依存ルール**:
- Tools/Resources/Prompts → NablarchKnowledgeBase への依存のみ許可
- NablarchKnowledgeBase → Data Layer（YAMLファイル）のみ参照
- レイヤー逆転（KnowledgeBase → Tools等）は禁止

## 3. 設定管理方針

### application.yaml（共通設定）

```yaml
spring:
  application:
    name: nablarch-mcp-server
  main:
    banner-mode: off
    web-application-type: none    # STDIO専用のためWebサーバー不要
  ai:
    mcp:
      server:
        name: nablarch-mcp-server
        version: 0.1.0
        type: SYNC                # 同期モード（Phase 1）
        stdio: true               # STDIOトランスポート有効化

nablarch:
  mcp:
    knowledge:
      base-path: classpath:knowledge/  # YAMLデータのベースパス
```

### 環境別プロファイル

| プロファイル | 用途 | 設定内容 |
|------------|------|---------|
| (default) | 本番・STDIO | 上記の共通設定 |
| dev | 開発時 | ログレベルDEBUG、詳細ログ有効 |
| test | テスト時 | テスト用モックデータパス |

Phase 1ではdefaultプロファイルのみ使用。dev/testは必要に応じて追加。

## 4. ログ設計方針

```yaml
logging:
  pattern:
    console:                        # STDIO干渉防止のためconsoleパターン空
  level:
    root: WARN
    com.tis.nablarch.mcp: INFO
    io.modelcontextprotocol: WARN   # SDK内部ログ抑制
  file:
    name: logs/nablarch-mcp-server.log  # ファイルログ出力（STDIOと分離）
```

**重要**: STDIOトランスポート使用時、stdoutはJSON-RPCメッセージ専用。アプリケーションログはstdoutに出力してはならない。
- ログはファイル出力（`logs/nablarch-mcp-server.log`）に振り向ける
- `logging.pattern.console` を空にしてコンソールログを抑制
- デバッグ時は `--logging.level.com.tis.nablarch.mcp=DEBUG` で切替可能

## 5. エラーハンドリング方針

### MCP JSON-RPC 2.0 エラーコード

| コード | 定数名 | 用途 |
|-------|--------|------|
| -32700 | PARSE_ERROR | JSON解析エラー（SDK管理） |
| -32600 | INVALID_REQUEST | 不正リクエスト（SDK管理） |
| -32601 | METHOD_NOT_FOUND | 未登録メソッド（SDK管理） |
| -32602 | INVALID_PARAMS | パラメータエラー（Tool/Resource内でバリデーション） |
| -32603 | INTERNAL_ERROR | 内部エラー（予期しない例外） |

### アプリケーション層のエラーハンドリング

- **Tool実行エラー**: `isError=true` の `CallToolResult` を返却。例外をスローしない
- **Resource読み込みエラー**: エラー内容をテキストとして返却。リソースが見つからない場合は空リスト
- **Prompt展開エラー**: エラーメッセージをPrompt結果に含める
- **KnowledgeBase初期化エラー**: YAMLパースエラーはログ出力し、該当カテゴリを空として初期化（graceful degradation）

### 例外戦略

```java
// Tool内でのエラーハンドリングパターン
@Tool(description = "...")
public String searchApi(String keyword, String category) {
    try {
        // 業務ロジック
        return knowledgeBase.search(keyword, category).toString();
    } catch (IllegalArgumentException e) {
        // パラメータエラー → エラーメッセージを返す（例外をスローしない）
        return "Error: " + e.getMessage();
    } catch (Exception e) {
        // 予期しないエラー → ログ出力 + エラーメッセージを返す
        log.error("Unexpected error in searchApi", e);
        return "Internal error occurred. Please try again.";
    }
}
```

## 6. 既存スタブクラスとの整合性

| クラス | 現状 | Phase 1 Batch Aでの変更 |
|-------|------|----------------------|
| NablarchMcpServerApplication | エントリポイント。動作OK | **変更不要** |
| McpServerConfig | Tool2つを登録。ToolCallbackProvider使用 | **拡張**: Resource/Prompt登録を追加 |
| NablarchKnowledgeBase | stub（全メソッドUnsupportedOperationException） | **変更不要**（Batch B / Wave 2で実装） |
| SearchApiTool | stub | **変更不要**（Wave 2で実装） |
| ValidateHandlerQueueTool | stub | **変更不要**（Wave 2で実装） |
| ApiSpecResource | stub。MCP Resource登録なし | **変更不要**（McpServerConfigで登録の仕組みを作る） |
| HandlerResource | stub。MCP Resource登録なし | **変更不要**（McpServerConfigで登録の仕組みを作る） |

**Batch Aの作業範囲**: McpServerConfigの拡張（Resource/Prompt登録機構の追加）と、パッケージ構造整備（prompts/, common/ ディレクトリ作成）のみ。既存stubの本格実装は行わない。

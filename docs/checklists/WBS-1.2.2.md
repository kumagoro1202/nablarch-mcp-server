# WBS 1.2.2 完了基準チェックリスト: MCP SDK統合・STDIOトランスポート

| # | チェック項目 | 状態 |
|---|------------|------|
| 1 | McpServerConfig.java にResource登録Bean（6リソース）が追加されている | ✅ PASS — handler/web,rest,batch,messaging + guide/setup,testing |
| 2 | McpServerConfig.java にPrompt登録Bean（6プロンプト）が追加されている | ✅ PASS — setup-handler-queue, create-action, review-config, explain-handler, migration-guide, best-practices |
| 3 | 既存Tool登録（ToolCallbackProvider）が維持されている | ✅ PASS — nablarchTools Bean変更なし |
| 4 | `./gradlew build` でビルドが通る | ✅ PASS — BUILD SUCCESSFUL in 7s |
| 5 | `./gradlew bootRun` でサーバーが起動しSTDIO待受状態になる | ✅ PASS — JSON-RPC initializeレスポンスをstdoutに出力確認 |
| 6 | 設計書（docs/designs/02_mcp-sdk-integration.md）と実装が整合している | ✅ PASS — Resource 6件、Prompt 6件、Tool既存2件が設計通り |

## 動作確認結果

### MCP Initialize レスポンス（stdout）
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "completions": {},
      "logging": {},
      "prompts": {"listChanged": true},
      "resources": {"subscribe": false, "listChanged": true},
      "tools": {"listChanged": true}
    },
    "serverInfo": {
      "name": "nablarch-mcp-server",
      "version": "0.1.0"
    }
  }
}
```

### 依存関係変更
- BOM: `io.modelcontextprotocol.sdk:mcp-bom:0.17.0` → `org.springframework.ai:spring-ai-bom:1.0.0`
- 依存: `mcp-spring-webflux` + `spring-boot-starter-webflux` → `spring-ai-starter-mcp-server`
- MCP SDK実効バージョン: 0.10.0（Spring AI 1.0.0同梱）

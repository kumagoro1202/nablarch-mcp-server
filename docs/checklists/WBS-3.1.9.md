# WBS 3.1.9: Streamable HTTPトランスポート設計 完了基準チェックリスト

## 基本情報

| 項目 | 内容 |
|------|------|
| WBS番号 | 3.1.9 |
| タスク名 | Streamable HTTPトランスポート設計 |
| 担当 | ashigaru7 (subtask_066) |
| 作成日 | 2026-02-03 |
| ステータス | 完了 |

## 成果物チェック

### 設計書 (docs/designs/23_streamable-http-transport.md)

- [x] Streamable HTTPの概要（MCP仕様に基づくHTTPトランスポート）
- [x] エンドポイント設計（`/mcp` エンドポイント）
- [x] セッション管理設計（ステートフルセッション、タイムアウト、同時接続数制御）
- [x] JSON-RPCマッピング（HTTP Request → JSON-RPC 2.0 → HTTP Response）
- [x] SSEストリーミング設計（Server-Sent Eventsによる非同期通知）
- [x] Spring Boot統合設計（既存STDIO構成との共存方法、プロファイル切替）
- [x] CORSポリシー設計
- [x] Originヘッダ検証の方針（Phase 4で実装、ここでは設計のみ）
- [x] エラーレスポンス設計（HTTP Status Code + JSON-RPC Error Codeのマッピング）
- [x] パフォーマンス考慮事項（同時接続10以上の目標）
- [x] 既存STDIOトランスポートとの切替方法（設定ファイルベース）
- [x] MCP仕様2025-03-26版のStreamable HTTP仕様を参照

## 品質チェック

- [x] 全てのドキュメントが日本語で記述されている
- [x] 既存設計書(01〜14)のフォーマット・構成に倣っている
- [x] MCP仕様の最新版（2025-03-26）を参照している
- [x] Spring Boot プロファイルによるSTDIO/HTTP切替方式が設計されている
- [x] 既存のMcpServerConfig.javaとの互換性が考慮されている

## 依存関係チェック

- [x] docs/architecture.md の Transport層設計との整合性を確認
- [x] docs/designs/02_mcp-sdk-integration.md の STDIO設計との整合性を確認

## 参照資料

- MCP仕様: https://modelcontextprotocol.io/specification/2025-03-26/basic/transports
- MCP Java SDK: https://modelcontextprotocol.io/sdk/java/mcp-server

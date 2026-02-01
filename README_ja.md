# Nablarch MCP Server

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/) サーバー。
[Nablarch](https://nablarch.github.io/) フレームワークの知識をAIコーディングツールに提供する。

## 概要

本MCPサーバーにより、Claude Code、Copilot、CursorなどのAIツールが
Nablarchフレームワークの正確なコードを生成できるようになる。

提供機能:

- **Tools**: API検索、ハンドラキュー構成検証
- **Resources**: API仕様、ハンドラカタログ、設計パターン
- **Knowledge Base**: Nablarchフレームワーク知識（ハンドラキューアーキテクチャ、コーディングパターン、ベストプラクティス）

## アーキテクチャ

```
AIコーディングツール ←→ MCP Client ←→ Nablarch MCP Server ←→ Knowledge Base
```

詳細は [docs/architecture.md](docs/architecture.md) を参照。

## 動作要件

- Java 17 以上
- Gradle 8.x（ラッパー同梱）

## クイックスタート

### ビルド

```bash
./gradlew build
```

### 実行（STDIOモード）

```bash
./gradlew bootRun
```

### Claude Code での設定

MCP設定に以下を追加:

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": ["-jar", "build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"]
    }
  }
}
```

## 技術スタック

| コンポーネント | 技術 |
|---|---|
| 言語 | Java 17+ |
| フレームワーク | Spring Boot 3.4.x |
| MCP SDK | [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) 0.17.x |
| ビルド | Gradle (Kotlin DSL) |
| テスト | JUnit 5 + Spring Test |
| トランスポート | STDIO（Phase 1） |

## プロジェクト状況

**Phase 1**: プロジェクトスケルトン（スタブ実装）

- [x] プロジェクト構造
- [x] MCP SDK統合
- [x] Toolスタブ（search_api, validate_handler_queue）
- [x] Resourceスタブ（API仕様、ハンドラ仕様）
- [x] Knowledge Base構造
- [ ] Tool実装
- [ ] Resource実装
- [ ] MCP Inspectorテスト

## ライセンス

[Apache License 2.0](LICENSE)

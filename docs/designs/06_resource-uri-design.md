# Resource URI設計書

> **バージョン**: 1.0
> **作成日**: 2026-02-02
> **WBS**: 1.1.6
> **親タスク**: subtask_047 (cmd_025 Wave 2 Batch D1)

---

## 1. 概要

Phase 1 MVPで実装するMCP Resourceは2種類:
- **handler/{app_type}**: アプリケーションタイプ別のハンドラキュー仕様
- **guide/{topic}**: トピック別の開発ガイド

両Resourceとも `text/markdown` 形式でAIクライアントに返却する。

---

## 2. handler/{app_type} リソース

### 2.1 URI体系

| URI | 説明 |
|-----|------|
| `nablarch://handler/web` | Webアプリケーション用ハンドラキュー |
| `nablarch://handler/rest` | RESTful Webサービス用ハンドラキュー |
| `nablarch://handler/batch` | バッチアプリケーション用ハンドラキュー |
| `nablarch://handler/messaging` | メッセージング用ハンドラキュー |
| `nablarch://handler/http-messaging` | HTTP同期メッセージング用ハンドラキュー |
| `nablarch://handler/jakarta-batch` | Jakarta Batch用ハンドラキュー |

### 2.2 データソース

| ファイル | 用途 |
|---------|------|
| `knowledge/handler-catalog.yaml` | ハンドラ一覧（name, fqcn, description, order, required, thread, constraints） |
| `knowledge/handler-constraints.yaml` | ハンドラ間の順序制約ルール |

### 2.3 レスポンス形式

```markdown
# Nablarch {AppType} Application Handler Queue

{description}

## Handler Queue (in order)

### 1. {HandlerName} {required ? "[Required]" : "[Optional]"}
- **FQCN**: `{fqcn}`
- **Thread**: {thread}
- **Description**: {description}
- **Constraints**:
  - Must be before: {must_before list}
  - Must be after: {must_after list}

### 2. ...

## Ordering Constraints Summary

| Handler | Rule | Details |
|---------|------|---------|
| {name} | {rule} | {reason} |
```

### 2.4 異常系

- 存在しないapp_type → "Unknown application type: {type}" メッセージを含むMarkdownを返す

---

## 3. guide/{topic} リソース

### 3.1 URI体系

| URI | 説明 | データソース |
|-----|------|------------|
| `nablarch://guide/setup` | プロジェクト設定ガイド | config-templates.yaml |
| `nablarch://guide/testing` | テストパターンガイド | api-patterns.yaml(testing) + design-patterns.yaml |
| `nablarch://guide/validation` | バリデーションガイド | api-patterns.yaml(validation関連) |
| `nablarch://guide/database` | データベースガイド | api-patterns.yaml(DAO関連) + config-templates.yaml |
| `nablarch://guide/handler-queue` | ハンドラキューガイド | handler-catalog.yaml + handler-constraints.yaml |
| `nablarch://guide/error-handling` | エラーハンドリングガイド | error-catalog.yaml |

### 3.2 トピック別データソースマッピング

#### setup
- config-templates.yaml の `web-xml`, `web-component`, `db-connection` テンプレートを集約
- プロジェクト初期構築の手順ガイド

#### testing
- api-patterns.yaml の `request-unit-test`, `excel-test-data` パターン
- テスト作成のベストプラクティス

#### validation
- api-patterns.yaml の `form-validation`, `inject-form-on-error` パターン
- バリデーション設計のガイド

#### database
- api-patterns.yaml の `universal-dao`, `sql-file`, `entity-class`, `exclusive-control` パターン
- config-templates.yaml の `db-connection` テンプレート

#### handler-queue
- handler-catalog.yaml の全アプリタイプ概要（サマリ版）
- handler-constraints.yaml の主要制約ルール

#### error-handling
- error-catalog.yaml の全エラーエントリ

### 3.3 レスポンス形式

```markdown
# Nablarch {Topic} Guide

## Overview
{トピックの概要}

## {Section 1}
{内容}

## {Section 2}
{内容}

---
*Source: {データソースファイル名}*
```

### 3.4 異常系

- 存在しないtopic → "Unknown guide topic: {topic}" メッセージを含むMarkdownを返す

---

## 4. 実装設計

### 4.1 クラス構成

```
resources/
├── HandlerResourceProvider.java   # handler/{app_type} の実装
└── GuideResourceProvider.java     # guide/{topic} の実装
```

### 4.2 HandlerResourceProvider

```java
@Component
public class HandlerResourceProvider {
    // handler-catalog.yaml, handler-constraints.yaml を読み込み
    // アプリタイプに応じたMarkdownを生成
    public String getHandlerMarkdown(String appType);
}
```

- YAMLファイルを `@PostConstruct` で読み込み、インメモリに保持
- Jackson YAMLパーサ（`jackson-dataformat-yaml`）を使用

### 4.3 GuideResourceProvider

```java
@Component
public class GuideResourceProvider {
    // 複数のYAMLファイルを読み込み
    // トピックに応じたMarkdownを生成
    public String getGuideMarkdown(String topic);
}
```

### 4.4 McpServerConfig統合

```java
@Bean
public List<McpServerFeatures.SyncResourceSpecification> nablarchResources(
        HandlerResourceProvider handlerProvider,
        GuideResourceProvider guideProvider) {
    // 6 handler resources + 6 guide resources = 12 total
}
```

---

## 参考文献

- [知識ベース設計書](03_knowledge-base.md) — YAMLスキーマ定義
- [アーキテクチャ設計書](../architecture.md) — MCP Resource仕様

# optimize_handler_queue Tool 詳細設計書

> **WBS番号**: 3.1.7
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru5 (subtask_064)
> **関連文書**: architecture.md §5.1 Tool 3, use-cases.md §UC10, 05_tool-validate-config.md

---

## 目次

1. [概要](#1-概要)
2. [Tool定義](#2-tool定義)
3. [入力スキーマ](#3-入力スキーマ)
4. [出力スキーマ](#4-出力スキーマ)
5. [XML解析ロジック](#5-xml解析ロジック)
6. [app_type自動推定ロジック](#6-app_type自動推定ロジック)
7. [3観点の最適化ルール設計](#7-3観点の最適化ルール設計)
8. [RAG連携フロー](#8-rag連携フロー)
9. [最適化提案の生成ロジック](#9-最適化提案の生成ロジック)
10. [design_handler_queue / validate_handler_queue Toolとの役割分担](#10-role-separation)
11. [MCP Tool登録・クラス設計](#11-mcp-tool登録クラス設計)
12. [エラーハンドリング](#12-エラーハンドリング)
13. [設定パラメータ](#13-設定パラメータ)

---

## 1. 概要

### 1.1 目的

本設計書は、Phase 3で新規追加する `optimize_handler_queue` MCP Toolの詳細設計を定義する。
このToolは、既存のNablarchハンドラキューXML設定を入力として受け取り、
パフォーマンス・セキュリティ・正確性の3観点から最適化提案を行うツールである。

### 1.2 スコープ

- `optimize_handler_queue` Toolの入出力スキーマ定義
- 既存component-configuration.xmlのXML解析ロジック
- 3観点（正確性・セキュリティ・パフォーマンス）の最適化ルール設計
- RAGエンジン（`semantic_search`経由）との連携フロー
- Before/After XML形式の最適化提案生成ロジック

### 1.3 背景

Nablarchのハンドラキューは、アプリケーションの成長に伴い肥大化・形骸化しやすい。
本Toolは既存のキュー構成を分析し、具体的な最適化提案をBefore/After形式で提示する。

---

## 2. Tool定義

### 2.1 Tool概要

| 項目 | 値 |
|------|-----|
| Tool名 | `optimize_handler_queue` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `OptimizeHandlerQueueTool` |
| 説明 | 既存のNablarchハンドラキューXMLを分析し、3観点から最適化提案を生成する |
| カテゴリ | 分析・最適化系Tool |

### 2.2 対応ユースケース

| UC | ユースケース名 | 本Toolの役割 |
|-----|-------------|-------------|
| UC10 | ハンドラキュー最適化 | メインTool。既存キューの分析→最適化提案 |
| UC4 | 設定XML生成・検証 | XML検証の延長で最適化提案を提供 |
| UC5 | トラブルシューティング | ハンドラキュー起因の問題に対して最適化視点で提案 |

---

## 3. 入力スキーマ

### 3.1 パラメータ定義

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `current_xml` | string | ○ | — | 現在のハンドラキューXML設定 |
| `app_type` | string | × | (自動推定) | アプリケーション種別 |
| `concern` | string | × | "all" | 最適化観点: "all", "correctness", "security", "performance" |

### 3.2 JSON Schema

```json
{
  "type": "object",
  "properties": {
    "current_xml": {
      "type": "string",
      "description": "Current handler queue XML configuration.",
      "minLength": 10
    },
    "app_type": {
      "type": "string",
      "enum": ["web", "rest", "batch", "resident-batch", "mom-messaging", "http-messaging"]
    },
    "concern": {
      "type": "string",
      "enum": ["all", "correctness", "security", "performance"],
      "default": "all"
    }
  },
  "required": ["current_xml"]
}
```

---

## 4. 出力スキーマ

### 4.1 レスポンス構造

```json
{
  "app_type": "web",
  "handler_count": 7,
  "total_proposals": 5,
  "proposals": [
    {
      "id": "SEC-001",
      "concern": "security",
      "severity": "high",
      "type": "add",
      "handler": "SecureHandler",
      "reason": "セキュリティヘッダーが設定されていない",
      "suggested_fix": "HttpResponseHandlerの内側にSecureHandlerを追加"
    }
  ],
  "optimized_xml": "<list name=\"handlerQueue\">...</list>",
  "summary": {
    "correctness": { "count": 1, "high": 0, "medium": 1, "low": 0 },
    "security": { "count": 2, "high": 1, "medium": 1, "low": 0 },
    "performance": { "count": 2, "high": 0, "medium": 1, "low": 1 }
  }
}
```

### 4.2 MCP Tool応答フォーマット（Markdown）

```markdown
## ハンドラキュー最適化分析

**アプリタイプ**: web
**ハンドラ数**: 7
**検出された最適化ポイント**: 5件

### サマリ

| 観点 | 件数 | 高 | 中 | 低 |
|------|------|-----|-----|-----|
| 正確性 | 1 | 0 | 1 | 0 |
| セキュリティ | 2 | 1 | 1 | 0 |
| パフォーマンス | 2 | 0 | 1 | 1 |

---

### 🔴 [SEC-001] SecureHandler未設定（高）

**観点**: セキュリティ
**タイプ**: ハンドラ追加
**修正提案**: HttpResponseHandlerの内側にSecureHandlerを追加

#### Before
```xml
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.common.handler.threadcontext.ThreadContextHandler"/>
```

#### After
```xml
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.fw.web.handler.SecureHandler"/>
<component class="nablarch.common.handler.threadcontext.ThreadContextHandler"/>
```
```

---

## 5. XML解析ロジック

### 5.1 HandlerQueueXmlParser（共有ユーティリティ）

`ValidateHandlerQueueTool`と同じXML解析ロジックを共有する。

```java
public class HandlerQueueXmlParser {
    private static final Pattern CLASS_ATTR_PATTERN =
        Pattern.compile("class\\s*=\\s*\"([^\"]+)\"");

    public static List<HandlerEntry> parse(String xml) {
        List<HandlerEntry> entries = new ArrayList<>();
        Matcher matcher = CLASS_ATTR_PATTERN.matcher(xml);
        int order = 0;
        while (matcher.find()) {
            String fqcn = matcher.group(1);
            String simpleName = extractSimpleName(fqcn);
            entries.add(new HandlerEntry(order++, fqcn, simpleName));
        }
        return entries;
    }
}
```

### 5.2 HandlerEntry モデル

```java
public record HandlerEntry(
    int order,
    String fqcn,
    String simpleName
) {}
```

---

## 6. app_type自動推定ロジック

### 6.1 推定ルール

```
1. JaxRsResponseHandler が存在 → "rest"
2. HttpResponseHandler + RoutesMapping が存在 → "web"
3. RequestThreadLoopHandler が存在 → "mom-messaging"
4. MultiThreadExecutionHandler + DataReadHandler → "batch"
5. ProcessStopHandler が存在 → "resident-batch"
6. HttpResponseHandler（単体） → "http-messaging"
7. 上記以外 → null（推定不可）
```

---

## 7. 3観点の最適化ルール設計

### 7.1 正確性観点（Correctness）

| ルールID | ルール名 | 重大度 |
|---------|---------|-------|
| COR-001 | 必須ハンドラ欠落 | high |
| COR-002 | 順序制約違反 | high |
| COR-003 | 外殻/内殻配置違反 | medium |
| COR-004 | 非互換ハンドラ同居 | medium |

### 7.2 セキュリティ観点（Security）

| ルールID | ルール名 | 重大度 |
|---------|---------|-------|
| SEC-001 | SecureHandler未設定 | high |
| SEC-002 | CSRF対策未設定 | high |
| SEC-003 | セッションストア未設定 | medium |
| SEC-004 | 認証ハンドラ配置不適 | medium |
| SEC-005 | 本番不要ハンドラ残存 | medium |

### 7.3 パフォーマンス観点（Performance）

| ルールID | ルール名 | 重大度 |
|---------|---------|-------|
| PERF-001 | 不要ハンドラの除去 | medium |
| PERF-002 | 重複ハンドラ | medium |
| PERF-003 | 軽量ハンドラの後方配置 | low |
| PERF-004 | 条件付き適用推奨 | low |
| PERF-005 | ログハンドラの非同期化推奨 | low |

---

## 8. RAG連携フロー

### 8.1 Phase 1（静的知識のみ）

handler-constraints.yaml の静的知識のみを使用。

### 8.2 Phase 2+（RAG強化）

- ベストプラクティス検索
- アンチパターン検索
- パフォーマンスTips検索

---

## 9. 最適化提案の生成ロジック

### 9.1 提案タイプ

| タイプ | 説明 |
|-------|------|
| `add` | ハンドラの追加 |
| `remove` | ハンドラの削除 |
| `reorder` | ハンドラの順序変更 |
| `replace` | ハンドラの置き換え |
| `configure` | 設定値の変更推奨 |

### 9.2 Before/After XML生成

各提案に対して、変更箇所周辺のBefore/After XMLを生成する。

---

## 10. design_handler_queue / validate_handler_queue Toolとの役割分担 {#role-separation}

### 10.1 3ツールの役割比較

| 観点 | design_handler_queue | validate_handler_queue | optimize_handler_queue |
|------|---------------------|----------------------|----------------------|
| 入力 | app_type + 要件 | XML + app_type | XML + app_type + concern |
| 目的 | 新規ハンドラキュー設計 | 既存キューの検証 | 既存キューの最適化提案 |
| 出力 | 新規XML | 検証結果（OK/NG） | 最適化提案 + 修正XML |
| ユースケース | UC1 | UC4 | UC10 |

---

## 11. MCP Tool登録・クラス設計

### 11.1 クラス図

```
com.tis.nablarch.mcp.tools
└── OptimizeHandlerQueueTool
    ├── optimize(currentXml, appType, concern): String
    ├── parseXml(xml): List<HandlerEntry>
    ├── detectAppType(handlers): String
    ├── runCorrectnessRules(handlers, appType): List<Proposal>
    ├── runSecurityRules(handlers, appType): List<Proposal>
    └── runPerformanceRules(handlers, appType): List<Proposal>

com.tis.nablarch.mcp.tools.optimize
├── HandlerQueueXmlParser（共有）
├── AppTypeDetector
├── CorrectnessAnalyzer
├── SecurityAnalyzer
├── PerformanceAnalyzer
├── XmlDiffGenerator
└── OptimizedXmlGenerator
```

### 11.2 Tool登録

```java
@Component
public class OptimizeHandlerQueueTool {

    @Tool(description = "Analyzes an existing Nablarch handler queue and generates optimization proposals.")
    public String optimize(
            @ToolParam(description = "Current handler queue XML") String currentXml,
            @ToolParam(description = "Application type", required = false) String appType,
            @ToolParam(description = "Optimization concern", required = false) String concern) {
        // 実装
    }
}
```

---

## 12. エラーハンドリング

| エラー条件 | エラーメッセージ | 対処 |
|-----------|----------------|------|
| current_xmlが空 | "エラー: ハンドラキューXMLが指定されていません" | 即座にエラー返却 |
| XMLからハンドラ抽出不可 | "エラー: XMLからハンドラを抽出できませんでした" | 即座にエラー返却 |
| app_type自動推定失敗 | "エラー: アプリケーションタイプを自動推定できませんでした" | 即座にエラー返却 |
| RAG検索失敗 | 静的ルールのみで分析を続行 | 処理継続 |

---

## 13. 設定パラメータ

```yaml
nablarch:
  mcp:
    tools:
      optimize-handler-queue:
        development-only-handlers:
          - HotDeployHandler
          - DumpVariableHandler
          - RequestDumpHandler
        severity:
          required-handler-missing: high
          order-constraint-violation: high
          secure-handler-missing: high
        rag:
          enabled: false
          best-practice-top-k: 3
```

# optimize_handler_queue Tool 詳細設計書

> **WBS番号**: 3.1.7
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru5 (subtask_064)
> **関連文書**: architecture.md §5.1 Tool 3, use-cases.md §UC10, 05_tool-validate-config.md, 15_tool-design-handler-queue.md, 13_semantic-search-tool.md
> **依存タスク**: Phase 1 validate_handler_queue実装済み, Phase 3 design_handler_queue設計済み

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
10. [design_handler_queue / validate_handler_queue Toolとの役割分担](#10-design_handler_queue--validate_handler_queue-toolとの役割分担)
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
- `design_handler_queue` Tool（3.1.1）、`validate_handler_queue` Tool（Phase 1）との役割分担

### 1.3 背景

Nablarchのハンドラキューは、アプリケーションの成長に伴い肥大化・形骸化しやすい。
開発時に追加したが本番で不要なハンドラ、セキュリティ設定の不足、順序の非最適性など、
運用中のハンドラキューには改善余地が存在する。本Toolは既存のキュー構成を分析し、
具体的な最適化提案をBefore/After形式で提示する。

`validate_handler_queue`がルール違反の「検出」に特化するのに対し、
本Toolは違反の修正案と追加の最適化提案を「生成」するところに価値がある。

---

## 2. Tool定義

### 2.1 Tool概要

| 項目 | 値 |
|------|-----|
| Tool名 | `optimize_handler_queue` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `OptimizeHandlerQueueTool` |
| 説明 | 既存のNablarchハンドラキューXML設定を分析し、パフォーマンス・セキュリティ・正確性の3観点から最適化提案を生成する |
| カテゴリ | 分析・最適化系Tool |

### 2.2 対応ユースケース

| UC | ユースケース名 | 本Toolの役割 |
|-----|-------------|-------------|
| UC10 | ハンドラキュー最適化 | メインTool。既存キューの分析→最適化提案の全工程を担当 |
| UC4 | 設定XML生成・検証 | XML検証の延長で最適化提案を提供 |
| UC5 | トラブルシューティング | ハンドラキュー起因の問題に対して最適化視点で提案 |

---

## 3. 入力スキーマ

### 3.1 パラメータ定義

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `current_xml` | string | ○ | — | 現在のハンドラキューXML設定（component-configuration.xml形式） |
| `app_type` | string | × | (自動推定) | アプリケーション種別。未指定時はXMLのハンドラ構成から自動推定 |
| `concern` | string | × | "all" | 最適化観点の指定: "all", "correctness", "security", "performance" |

### 3.2 JSON Schema

```json
{
  "type": "object",
  "properties": {
    "current_xml": {
      "type": "string",
      "description": "Current handler queue XML configuration (component-configuration.xml format).",
      "minLength": 10
    },
    "app_type": {
      "type": "string",
      "enum": ["web", "rest", "batch", "resident-batch", "mom-messaging", "http-messaging"],
      "description": "Application type. Auto-detected from XML if not specified."
    },
    "concern": {
      "type": "string",
      "enum": ["all", "correctness", "security", "performance"],
      "default": "all",
      "description": "Optimization concern to focus on."
    }
  },
  "required": ["current_xml"]
}
```

### 3.3 入力例

```json
{
  "current_xml": "<component name=\"webFrontController\" class=\"nablarch.fw.web.servlet.WebFrontController\">\n  <property name=\"handlerQueue\">\n    <list>\n      <component class=\"nablarch.fw.web.handler.HttpCharacterEncodingHandler\"/>\n      <component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>\n      <component class=\"nablarch.fw.web.handler.HttpResponseHandler\"/>\n      <component class=\"nablarch.common.handler.threadcontext.ThreadContextHandler\"/>\n      <component class=\"nablarch.common.handler.DbConnectionManagementHandler\"/>\n      <component class=\"nablarch.common.handler.TransactionManagementHandler\"/>\n      <component class=\"nablarch.integration.router.RoutesMapping\"/>\n    </list>\n  </property>\n</component>",
  "app_type": "web",
  "concern": "all"
}
```

---

## 4. 出力スキーマ

### 4.1 レスポンス構造

本ToolはMarkdown形式のテキストとして結果を返却する。
内部的には以下の構造化データを生成し、Markdown形式にフォーマットする。

```json
{
  "app_type": "web",
  "detected_app_type": "web",
  "handler_count": 7,
  "total_proposals": 5,
  "proposals": [
    {
      "id": "SEC-001",
      "concern": "security",
      "severity": "high",
      "type": "add",
      "handler": "SecureHandler",
      "reason": "セキュリティヘッダー（Content-Security-Policy, X-Frame-Options等）が設定されていない",
      "suggested_fix": "HttpResponseHandlerの内側にSecureHandlerを追加する"
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

### 4.2 MCP Tool応答フォーマット

```markdown
## ハンドラキュー最適化分析

**アプリタイプ**: web（指定）
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
**対象**: SecureHandler
**理由**: セキュリティヘッダー（Content-Security-Policy, X-Frame-Options等）が設定されていない。
Webアプリケーションでは必須のセキュリティ対策。
**修正提案**: HttpResponseHandlerの内側にSecureHandlerを追加する。

#### Before
\```xml
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.common.handler.threadcontext.ThreadContextHandler"/>
\```

#### After
\```xml
<component class="nablarch.fw.web.handler.HttpResponseHandler"/>
<component class="nablarch.fw.web.handler.SecureHandler"/>
<component class="nablarch.common.handler.threadcontext.ThreadContextHandler"/>
\```

---

### 🟡 [COR-002] ...

---

### 最適化後のハンドラキューXML

\```xml
<list name="handlerQueue">
  ...最適化済みXML...
</list>
\```
```

---

## 5. XML解析ロジック

### 5.1 HandlerQueueXmlParser（共有ユーティリティ）

`ValidateHandlerQueueTool`（Phase 1実装済み）と同じXML解析ロジックを共有する。
共通ユーティリティクラス `HandlerQueueXmlParser` として切り出す。

```java
public class HandlerQueueXmlParser {

    // ValidateHandlerQueueTool.java より移植
    private static final Pattern CLASS_ATTR_PATTERN =
        Pattern.compile("class\\s*=\\s*\"([^\"]+)\"");

    /**
     * XMLテキストからハンドラのFQCNリストを抽出する。
     * component要素およびhandler要素のclass属性を対象とする。
     */
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

    /**
     * FQCNから単純クラス名を抽出する。
     */
    public static String extractSimpleName(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }
}
```

### 5.2 HandlerEntry モデル

```java
public record HandlerEntry(
    int order,          // キュー内の順序（0始まり）
    String fqcn,        // 完全修飾クラス名
    String simpleName   // 単純クラス名
) {}
```

### 5.3 対応するXML形式

```xml
<!-- 形式1: component要素のclass属性 -->
<component class="nablarch.fw.handler.GlobalErrorHandler"/>

<!-- 形式2: property付きcomponent要素 -->
<component class="nablarch.common.handler.DbConnectionManagementHandler">
  <property name="connectionFactory" ref="connectionFactory"/>
</component>

<!-- 形式3: handler要素のclass属性（レガシー） -->
<handler class="nablarch.fw.handler.GlobalErrorHandler"/>
```

FQCNの末尾（最後の`.`以降）を単純クラス名として使用し、
handler-constraints.yaml の `handler` フィールドと照合する。

---

## 6. app_type自動推定ロジック

### 6.1 推定方法

`app_type`が未指定の場合、ハンドラ構成からアプリケーション種別を推定する。
各app_typeに特徴的なハンドラの有無で判定する。

### 6.2 推定ルール

```
判定優先順:

1. JaxRsResponseHandler が存在 → "rest"
2. HttpResponseHandler が存在 AND PackageMapping/RoutesMapping が存在 → "web"
3. RequestThreadLoopHandler が存在 → "mom-messaging"
4. MultiThreadExecutionHandler が存在 AND DataReadHandler が存在 → "batch"
5. ProcessStopHandler が存在 → "resident-batch"
6. HttpResponseHandler が存在（単体） → "http-messaging"
7. 上記いずれにも該当しない → null（推定不可、エラー返却）
```

### 6.3 特徴的ハンドラマッピング

| app_type | 必須ハンドラ | 特徴的ハンドラ |
|---------|------------|--------------|
| web | HttpResponseHandler, RoutesMapping | SecureHandler, SessionStoreHandler, MultipartHandler |
| rest | JaxRsResponseHandler | StatusCodeConvertHandler, BodyConvertHandler |
| batch | MultiThreadExecutionHandler, DataReadHandler | LoopHandler, RetryHandler |
| resident-batch | ProcessStopHandler, RequestThreadLoopHandler | MultiThreadExecutionHandler |
| mom-messaging | RequestThreadLoopHandler | MultiThreadExecutionHandler |
| http-messaging | HttpResponseHandler | HttpCharacterEncodingHandler |

---

## 7. 3観点の最適化ルール設計

### 7.1 正確性観点（Correctness）

ハンドラキューの機能的正確性を確保するルール。
`validate_handler_queue` Toolの検証ロジックを再利用し、修正案を追加で生成する。

| ルールID | ルール名 | 重大度 | 説明 |
|---------|---------|-------|------|
| COR-001 | 必須ハンドラ欠落 | high | app_typeで必須のハンドラが含まれていない。handler-constraints.yamlの`required_by_app_type`で判定 |
| COR-002 | 順序制約違反 | high | handler-constraints.yamlの`must_before`/`must_after`ルールに違反している |
| COR-003 | 外殻/内殻配置違反 | medium | `must_be_outer`ハンドラが内側に、`must_be_inner`ハンドラが外側に配置されている |
| COR-004 | 非互換ハンドラ同居 | medium | 同時使用が推奨されないハンドラの組み合わせが存在する |

#### COR-001: 必須ハンドラ欠落の検出ロジック

```
for each constraint in handler-constraints.yaml:
  if constraint.required_by_app_type contains app_type:
    if constraint.handler not in current_handlers:
      → proposal: { type: "add", handler: constraint.handler,
                     severity: "high", concern: "correctness" }
         suggested_position を制約から算出（must_before/must_after参照）
```

#### COR-002: 順序制約違反の検出ロジック

```
for each handler in current_handlers:
  constraint = findConstraint(handler)
  if constraint.must_before exists:
    for each target in constraint.must_before:
      if indexOf(handler) > indexOf(target):
        → proposal: { type: "reorder", handler: handler,
                       severity: "high", concern: "correctness" }
  if constraint.must_after exists:
    for each target in constraint.must_after:
      if indexOf(handler) < indexOf(target):
        → proposal: { type: "reorder" }
```

### 7.2 セキュリティ観点（Security）

セキュリティ上の脆弱性やベストプラクティス違反を検出する。

| ルールID | ルール名 | 重大度 | 説明 |
|---------|---------|-------|------|
| SEC-001 | SecureHandler未設定 | high | Webアプリでセキュリティヘッダー付与ハンドラがない |
| SEC-002 | CSRF対策未設定 | high | Webアプリでフォーム送信があるのにCSRFトークン検証がない |
| SEC-003 | セッションストア未設定 | medium | Webアプリでセッション管理ハンドラがない |
| SEC-004 | 認証ハンドラ配置不適 | medium | 認証ハンドラがDB接続ハンドラより前に配置されている（認証にDB参照が必要な場合） |
| SEC-005 | 本番不要ハンドラ残存 | medium | HotDeployHandler等の開発専用ハンドラが含まれている |

#### SEC-001: SecureHandler未設定の検出ロジック

```
if app_type in ["web", "http-messaging"]:
  if "SecureHandler" not in current_handlers:
    → proposal: { type: "add", handler: "SecureHandler",
                   severity: "high", concern: "security",
                   reason: "セキュリティヘッダー（CSP, X-Frame-Options等）が未設定" }
```

#### SEC-005: 本番不要ハンドラの検出ロジック

```
DEVELOPMENT_ONLY_HANDLERS = [
  "HotDeployHandler",
  "DumpVariableHandler",
  "RequestDumpHandler"
]

for each handler in current_handlers:
  if handler.simpleName in DEVELOPMENT_ONLY_HANDLERS:
    → proposal: { type: "remove", handler: handler,
                   severity: "medium", concern: "security",
                   reason: "開発環境専用ハンドラが含まれている。本番環境では不要" }
```

### 7.3 パフォーマンス観点（Performance）

リクエスト処理のパフォーマンスを改善するためのルール。

| ルールID | ルール名 | 重大度 | 説明 |
|---------|---------|-------|------|
| PERF-001 | 不要ハンドラの除去 | medium | 機能的に不要（app_typeと不一致）なハンドラが含まれている |
| PERF-002 | 重複ハンドラ | medium | 同一ハンドラが複数回定義されている（意図的なmulti-db以外） |
| PERF-003 | 軽量ハンドラの後方配置 | low | 軽量な前処理ハンドラが重いハンドラの後に配置されている |
| PERF-004 | 条件付き適用推奨 | low | 全リクエストに適用されているが特定パスのみ必要なハンドラがある |
| PERF-005 | ログハンドラの非同期化推奨 | low | 同期的なログ出力ハンドラが存在する |

#### PERF-001: 不要ハンドラの検出ロジック

```
for each handler in current_handlers:
  constraint = findConstraint(handler)
  if constraint != null AND constraint.required_by_app_type exists:
    if app_type not in constraint.required_by_app_type:
      → proposal: { type: "remove", handler: handler,
                     severity: "medium", concern: "performance",
                     reason: "{app_type}アプリでは不要なハンドラ" }
```

#### PERF-002: 重複ハンドラの検出ロジック

```
handlerCounts = countBySimpleName(current_handlers)
for each (name, count) in handlerCounts:
  if count > 1:
    if name == "DbConnectionManagementHandler" AND multi_db:
      → skip  // multi-dbパターンでは正常
    else:
      → proposal: { type: "remove", handler: name,
                     severity: "medium", concern: "performance",
                     reason: "同一ハンドラが{count}回定義されている" }
```

---

## 8. RAG連携フロー

### 8.1 Phase 1（静的知識のみ）

Phase 1では、handler-constraints.yaml および handler-catalog.yaml の静的知識のみを使用する。
RAG連携は行わない。

### 8.2 Phase 2+（RAG強化）

Phase 2以降では、`semantic_search` Toolを内部呼び出しし、以下の情報を検索する。

#### 8.2.1 ベストプラクティス検索

```
semantic_search({
  query: "Nablarch {app_type} ハンドラキュー ベストプラクティス 推奨構成",
  filters: { type: "best-practice" },
  top_k: 3,
  mode: "hybrid"
})
```

検索結果から、推奨構成パターンと比較して差分を最適化提案に反映。

#### 8.2.2 アンチパターン検索

```
semantic_search({
  query: "Nablarch ハンドラキュー アンチパターン 誤り 注意",
  filters: { type: "anti-pattern" },
  top_k: 3,
  mode: "hybrid"
})
```

検索結果から、既知のアンチパターンと現在の構成を照合し、該当するものを最適化提案に追加。

#### 8.2.3 パフォーマンスTips検索

```
semantic_search({
  query: "Nablarch ハンドラ パフォーマンス 最適化 レイテンシ",
  filters: { type: "performance" },
  top_k: 3,
  mode: "hybrid"
})
```

### 8.3 RAG結果の活用

RAG検索結果は以下の目的で使用する:

1. **追加ルールの発見**: 静的ルール（§7）でカバーできない最適化ポイント
2. **根拠の強化**: 提案の理由にドキュメント参照を追加
3. **具体的なベンチマーク情報**: パフォーマンス改善の推定値

---

## 9. 最適化提案の生成ロジック

### 9.1 提案タイプ

| タイプ | 説明 | Before/After生成 |
|-------|------|:----------------:|
| `add` | ハンドラの追加 | ○ |
| `remove` | ハンドラの削除 | ○ |
| `reorder` | ハンドラの順序変更 | ○ |
| `replace` | ハンドラの置き換え | ○ |
| `configure` | 設定値の変更推奨 | △（設定例のみ） |

### 9.2 Before/After XML生成

各提案に対して、変更箇所周辺のBefore/After XMLを生成する。

```java
public class XmlDiffGenerator {

    /**
     * 追加提案のBefore/After XMLを生成する。
     * 挿入位置の前後2行のコンテキストを含む。
     */
    public DiffResult generateAddDiff(
            String originalXml,
            HandlerEntry insertAfter,
            String newHandlerFqcn) {
        // 挿入位置を特定
        // Before: 挿入位置の前後
        // After: 新ハンドラ挿入後の前後
    }

    /**
     * 削除提案のBefore/After XMLを生成する。
     */
    public DiffResult generateRemoveDiff(
            String originalXml,
            HandlerEntry target) {
        // Before: 対象ハンドラとその前後
        // After: 対象ハンドラ除去後
    }

    /**
     * 順序変更提案のBefore/After XMLを生成する。
     */
    public DiffResult generateReorderDiff(
            String originalXml,
            HandlerEntry handler,
            int newPosition) {
        // Before: 元の位置でのコンテキスト
        // After: 新しい位置でのコンテキスト
    }
}
```

### 9.3 最適化後XML全体の生成

全ての提案を適用した後の完全なXMLを生成する。

```
処理フロー:
1. 全proposalをseverity降順（high → medium → low）でソート
2. 各proposalを順次適用:
   a. add: 指定位置にハンドラ挿入
   b. remove: 対象ハンドラを除去
   c. reorder: 対象ハンドラを新位置に移動
3. 適用後のXMLで再度制約検証を実行
4. 検証がOKなら最適化後XMLとして出力
5. 検証がNGなら、問題のある提案を除外して再生成
```

---

## 10. design_handler_queue / validate_handler_queue Toolとの役割分担

### 10.1 3ツールの役割比較

| 観点 | design_handler_queue | validate_handler_queue | optimize_handler_queue |
|------|---------------------|----------------------|----------------------|
| 入力 | app_type + 要件 | XML + app_type | XML + app_type + concern |
| 目的 | 新規ハンドラキュー設計 | 既存キューの検証 | 既存キューの最適化提案 |
| 出力 | 新規XML | 検証結果（OK/NG） | 最適化提案 + 修正XML |
| フェーズ | Phase 3 | Phase 1（実装済み） | Phase 3 |
| ユースケース | UC1 | UC4 | UC10 |
| 対象 | ゼロからの設計 | ルール違反の検出 | 改善ポイントの発見と修正案 |

### 10.2 validate_handler_queue Toolとの関係

`optimize_handler_queue` Toolは、`validate_handler_queue` Toolの検証ロジックを**内部的に再利用**する。

```
optimize_handler_queue の処理フロー:
  1. XML解析（HandlerQueueXmlParser: 共有ユーティリティ）
  2. validate_handler_queue 相当のチェック実行
     → 検出されたエラーをCOR-001〜COR-004の最適化提案に変換
  3. セキュリティ固有のルール適用（SEC-001〜SEC-005）
  4. パフォーマンス固有のルール適用（PERF-001〜PERF-005）
  5. 全提案をマージし、Before/After XML + 最適化後XMLを生成
```

再利用対象:
- `HandlerQueueXmlParser`: XML解析（共有ユーティリティ化）
- `NablarchKnowledgeBase.validateHandlerQueue()`: 制約検証ロジック
- `handler-constraints.yaml`: 制約定義データ

### 10.3 design_handler_queue Toolとの関係

- `design_handler_queue`: 「何もない状態からハンドラキューを**新規設計**する」
- `optimize_handler_queue`: 「既存のハンドラキューを**分析して改善提案**する」

両者は相互補完的であり、以下のワークフローで連携する:

```
新規プロジェクト:
  design_handler_queue → 初期XML生成
  → 運用後
  optimize_handler_queue → 改善提案

既存プロジェクト:
  validate_handler_queue → 検証（OK/NG）
  → NGの場合
  optimize_handler_queue → 具体的な修正案
```

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
    ├── runPerformanceRules(handlers, appType): List<Proposal>
    └── generateResult(proposals, originalXml, appType): String

com.tis.nablarch.mcp.tools.optimize
├── HandlerQueueXmlParser（共有ユーティリティ）
│   ├── parse(xml): List<HandlerEntry>
│   └── extractSimpleName(fqcn): String
├── AppTypeDetector
│   └── detect(handlers): String
├── CorrectnessAnalyzer
│   ├── checkRequiredHandlers(handlers, appType): List<Proposal>
│   ├── checkOrderConstraints(handlers): List<Proposal>
│   ├── checkPlacementRules(handlers): List<Proposal>
│   └── checkIncompatibility(handlers): List<Proposal>
├── SecurityAnalyzer
│   ├── checkSecureHandler(handlers, appType): List<Proposal>
│   ├── checkCsrfProtection(handlers, appType): List<Proposal>
│   ├── checkSessionStore(handlers, appType): List<Proposal>
│   ├── checkAuthPlacement(handlers): List<Proposal>
│   └── checkDevOnlyHandlers(handlers): List<Proposal>
├── PerformanceAnalyzer
│   ├── checkUnnecessaryHandlers(handlers, appType): List<Proposal>
│   ├── checkDuplicateHandlers(handlers): List<Proposal>
│   ├── checkHandlerOrdering(handlers): List<Proposal>
│   ├── checkConditionalApply(handlers): List<Proposal>
│   └── checkAsyncLogging(handlers): List<Proposal>
├── XmlDiffGenerator
│   ├── generateAddDiff(xml, position, fqcn): DiffResult
│   ├── generateRemoveDiff(xml, handler): DiffResult
│   └── generateReorderDiff(xml, handler, newPos): DiffResult
├── OptimizedXmlGenerator
│   ├── applyProposals(xml, proposals): String
│   └── validateResult(xml, appType): boolean
└── model/
    ├── HandlerEntry
    ├── Proposal
    └── DiffResult
```

### 11.2 Tool登録

```java
@Component
public class OptimizeHandlerQueueTool {

    private final NablarchKnowledgeBase knowledgeBase;
    private final CorrectnessAnalyzer correctnessAnalyzer;
    private final SecurityAnalyzer securityAnalyzer;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final XmlDiffGenerator xmlDiffGenerator;
    private final OptimizedXmlGenerator optimizedXmlGenerator;

    public OptimizeHandlerQueueTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        this.correctnessAnalyzer = new CorrectnessAnalyzer(knowledgeBase);
        this.securityAnalyzer = new SecurityAnalyzer(knowledgeBase);
        this.performanceAnalyzer = new PerformanceAnalyzer(knowledgeBase);
        this.xmlDiffGenerator = new XmlDiffGenerator();
        this.optimizedXmlGenerator = new OptimizedXmlGenerator(knowledgeBase);
    }

    @Tool(description = "Analyzes an existing Nablarch handler queue XML configuration " +
        "and generates optimization proposals from correctness, security, " +
        "and performance perspectives. Returns Before/After XML diffs " +
        "and a fully optimized XML configuration.")
    public String optimize(
            @ToolParam(description = "Current handler queue XML configuration") String currentXml,
            @ToolParam(description = "Application type: web, rest, batch, etc. Auto-detected if omitted", required = false) String appType,
            @ToolParam(description = "Optimization concern: all, correctness, security, performance", required = false) String concern) {

        // デフォルト値設定
        if (concern == null || concern.isBlank()) {
            concern = "all";
        }

        // XML解析
        List<HandlerEntry> handlers = HandlerQueueXmlParser.parse(currentXml);
        if (handlers.isEmpty()) {
            return "エラー: XMLからハンドラを抽出できませんでした。" +
                   "component-configuration.xml形式のXMLを入力してください。";
        }

        // app_type推定（未指定時）
        if (appType == null || appType.isBlank()) {
            appType = AppTypeDetector.detect(handlers);
            if (appType == null) {
                return "エラー: アプリケーションタイプを自動推定できませんでした。" +
                       "app_typeパラメータを指定してください。";
            }
        }

        // 最適化ルール適用
        List<Proposal> proposals = new ArrayList<>();
        if ("all".equals(concern) || "correctness".equals(concern)) {
            proposals.addAll(correctnessAnalyzer.analyze(handlers, appType));
        }
        if ("all".equals(concern) || "security".equals(concern)) {
            proposals.addAll(securityAnalyzer.analyze(handlers, appType));
        }
        if ("all".equals(concern) || "performance".equals(concern)) {
            proposals.addAll(performanceAnalyzer.analyze(handlers, appType));
        }

        // 結果生成
        return generateResult(proposals, currentXml, handlers, appType, concern);
    }

    private String generateResult(
            List<Proposal> proposals,
            String originalXml,
            List<HandlerEntry> handlers,
            String appType,
            String concern) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ハンドラキュー最適化分析\n\n");
        sb.append("**アプリタイプ**: ").append(appType).append("\n");
        sb.append("**ハンドラ数**: ").append(handlers.size()).append("\n");
        sb.append("**検出された最適化ポイント**: ").append(proposals.size()).append("件\n\n");

        // サマリテーブル生成
        appendSummaryTable(sb, proposals);

        // 各提案のBefore/After
        for (Proposal p : proposals) {
            appendProposalDetail(sb, p, originalXml);
        }

        // 最適化後XML
        if (!proposals.isEmpty()) {
            String optimizedXml = optimizedXmlGenerator.applyProposals(originalXml, proposals);
            sb.append("### 最適化後のハンドラキューXML\n\n");
            sb.append("```xml\n").append(optimizedXml).append("\n```\n");
        }

        return sb.toString();
    }
}
```

---

## 12. エラーハンドリング

### 12.1 エラーパターン

| エラー条件 | エラーメッセージ | 対処 |
|-----------|----------------|------|
| current_xmlが空 or null | "エラー: ハンドラキューXMLが指定されていません。current_xmlパラメータが必要です。" | 即座にエラー返却 |
| XMLからハンドラ抽出不可 | "エラー: XMLからハンドラを抽出できませんでした。component-configuration.xml形式のXMLを入力してください。" | 即座にエラー返却 |
| app_type自動推定失敗 | "エラー: アプリケーションタイプを自動推定できませんでした。app_typeパラメータを指定してください。" | 即座にエラー返却 |
| app_typeが不正値 | "エラー: 不明なアプリケーションタイプ '{value}'。有効な値: web, rest, batch, resident-batch, mom-messaging, http-messaging" | 即座にエラー返却 |
| concernが不正値 | "all"にフォールバック | 警告なしで修正 |
| RAG検索失敗（Phase 2+） | 静的ルールのみで分析を続行 | ログ出力のみ、処理継続 |
| 最適化後XMLの制約検証失敗 | 問題のある提案を除外して再生成 | 最適化後XMLに「※一部提案は制約違反の可能性があるため除外しました」を付記 |

### 12.2 フォールバック戦略

```
RAG検索（semantic_search）が利用不可 or エラーの場合:
  → 静的知識ベース（handler-constraints.yaml）のみで最適化を実行
  → COR/SEC/PERFルールは全て静的ルールで動作可能
  → レスポンスに「※ RAGエンジンが利用できないため、静的知識のみで分析しています」を付記
```

---

## 13. 設定パラメータ

### 13.1 application.yml

```yaml
nablarch:
  mcp:
    tools:
      optimize-handler-queue:
        # 開発専用ハンドラリスト（SEC-005で検出対象）
        development-only-handlers:
          - HotDeployHandler
          - DumpVariableHandler
          - RequestDumpHandler
        # 重大度のデフォルト値
        severity:
          required-handler-missing: high
          order-constraint-violation: high
          secure-handler-missing: high
          csrf-missing: high
          session-store-missing: medium
          dev-handler-in-production: medium
          unnecessary-handler: medium
          duplicate-handler: medium
          handler-ordering: low
          conditional-apply: low
          async-logging: low
        # RAG検索設定（Phase 2+）
        rag:
          enabled: false  # Phase 1ではfalse
          best-practice-top-k: 3
          anti-pattern-top-k: 3
          performance-tips-top-k: 3
```

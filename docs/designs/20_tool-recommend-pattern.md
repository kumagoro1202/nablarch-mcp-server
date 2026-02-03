# recommend_pattern Tool 詳細設計書

> **WBS番号**: 3.1.6
> **ステータス**: 設計完了
> **作成日**: 2026-02-03
> **作成者**: ashigaru5 (subtask_064)
> **関連文書**: architecture.md §5.1 Tool 6, use-cases.md §UC8, 04_tool-search-api.md, 06_resource-uri-design.md, 13_semantic-search-tool.md
> **依存タスク**: Phase 1 knowledge-base実装済み, Phase 2 semantic_search実装済み（Phase 2+で活用）

---

## 目次

1. [概要](#1-概要)
2. [Tool定義](#2-tool定義)
3. [入力スキーマ](#3-入力スキーマ)
4. [出力スキーマ](#4-出力スキーマ)
5. [パターンマッチングロジック](#5-パターンマッチングロジック)
6. [RAG検索+スコアリング設計](#6-rag検索スコアリング設計)
7. [Nablarchデザインパターンカタログとの連携](#7-nablarchデザインパターンカタログとの連携)
8. [パターン推薦の根拠提示方針](#8-パターン推薦の根拠提示方針)
9. [MCP Tool登録・クラス設計](#9-mcp-tool登録クラス設計)
10. [エラーハンドリング](#10-エラーハンドリング)
11. [設定パラメータ](#11-設定パラメータ)

---

## 1. 概要

### 1.1 目的

本設計書は、Phase 3で新規追加する `recommend_pattern` MCP Toolの詳細設計を定義する。
このToolは、ユーザーの要件記述に基づいてNablarch固有のデザインパターンを推薦するツールであり、
11種のNablarchデザインパターンの中から最適なものをスコアリング付きで提案する。

### 1.2 スコープ

- `recommend_pattern` Toolの入出力スキーマ定義
- パターンマッチングロジック（4フェーズ）
- RAGエンジン（`semantic_search`経由）との連携フロー
- `design-patterns.yaml`（11パターン）との連携設計
- Resource URI（`nablarch://pattern/{name}`）との関係
- パターン推薦の根拠提示方針
- スコアリングモデルと複数候補のランキング方式

### 1.3 背景

Nablarchは独自のアーキテクチャパターン（ハンドラキュー、アクションクラス、Universal DAO等）を持つ。
開発者が要件に対して適切なパターンを選択するには、フレームワークの設計思想と各パターンの特性を
深く理解する必要がある。本Toolは、自然言語の要件記述からNablarch固有のパターンを推薦し、
パターン選択の意思決定を支援する。

---

## 2. Tool定義

### 2.1 Tool概要

| 項目 | 値 |
|------|-----|
| Tool名 | `recommend_pattern` |
| パッケージ | `com.tis.nablarch.mcp.tools` |
| クラス名 | `RecommendPatternTool` |
| 説明 | ユーザーの要件に基づいてNablarch固有のデザインパターンを推薦し、適合度スコアと根拠を提示する |
| カテゴリ | 推薦系Tool |

### 2.2 対応ユースケース

| UC | ユースケース名 | 本Toolの役割 |
|-----|-------------|-------------|
| UC8 | 設計パターン推奨 | メインTool。要件→パターン推薦の全工程を担当 |
| UC1 | ハンドラキュー自動設計 | design_handler_queueの前段で、アーキテクチャパターンの選択を支援 |
| UC11 | 初学者向け学習支援 | 学習コンテキストでNablarchパターンを解説 |

---

## 3. 入力スキーマ

### 3.1 パラメータ定義

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `requirement` | string | ○ | — | パターン推薦のための自然言語要件記述 |
| `app_type` | string | × | null | アプリケーション種別（web, rest, batch, messaging）。指定時は適合度スコアに反映 |
| `constraints` | array[string] | × | [] | 追加の制約条件（例: "高トラフィック", "レガシーDB連携"） |
| `max_results` | integer | × | 3 | 返却するパターン候補の最大数（1〜11） |

### 3.2 JSON Schema

```json
{
  "type": "object",
  "properties": {
    "requirement": {
      "type": "string",
      "description": "Natural language description of the requirement for pattern recommendation.",
      "minLength": 10
    },
    "app_type": {
      "type": "string",
      "enum": ["web", "rest", "batch", "messaging"],
      "description": "Application type to filter applicable patterns."
    },
    "constraints": {
      "type": "array",
      "items": { "type": "string" },
      "description": "Additional constraints for pattern selection."
    },
    "max_results": {
      "type": "integer",
      "minimum": 1,
      "maximum": 11,
      "default": 3,
      "description": "Maximum number of pattern candidates to return."
    }
  },
  "required": ["requirement"]
}
```

### 3.3 入力例

```json
{
  "requirement": "複数データベースへの接続が必要で、業務DBとログDBのトランザクションを分離管理したい",
  "app_type": "web",
  "constraints": ["高トラフィック"],
  "max_results": 3
}
```

---

## 4. 出力スキーマ

### 4.1 レスポンス構造

本ToolはMarkdown形式のテキストとして結果を返却する。
内部的には以下の構造化データを生成し、Markdown形式にフォーマットする。

```json
{
  "query": "複数データベースへの接続が必要で...",
  "app_type": "web",
  "total_candidates": 3,
  "patterns": [
    {
      "rank": 1,
      "name": "multi-db-pattern",
      "display_name": "複数データベース接続パターン",
      "category": "data-access",
      "score": 0.87,
      "score_breakdown": {
        "keyword_match": 0.95,
        "category_match": 0.80,
        "app_type_fit": 1.00,
        "constraint_match": 0.70
      },
      "rationale": {
        "fit_reason": "複数DB接続とトランザクション分離の要件に直接合致する",
        "solution_summary": "DbConnectionManagementHandlerを複数定義し、connectionNameで接続先を分離...",
        "trade_offs": ["接続プールが複数必要（リソース消費増）", "トランザクション管理の複雑さ増大"]
      },
      "code_example": "<!-- XML設定例 -->\n...",
      "resource_uri": "nablarch://pattern/multi-db-pattern",
      "references": ["https://nablarch.github.io/..."]
    }
  ]
}
```

### 4.2 MCP Tool応答フォーマット

```markdown
## デザインパターン推薦結果

**要件**: 複数データベースへの接続が必要で...
**アプリタイプ**: web
**候補数**: 3件

---

### 🥇 第1位: 複数データベース接続パターン（スコア: 87%）

**カテゴリ**: data-access
**適合理由**: 複数DB接続とトランザクション分離の要件に直接合致する

#### ソリューション概要
DbConnectionManagementHandlerを複数定義し、connectionNameプロパティで接続先を分離する。
ログDBへの書き込みはSimpleDbTransactionManagerで個別トランザクション管理。

#### コード例
\```xml
<component class="nablarch.common.handler.DbConnectionManagementHandler">
  <property name="connectionFactory" ref="businessDbConnectionFactory"/>
  <property name="connectionName" value="business"/>
</component>
\```

#### トレードオフ
- ⚠ 接続プールが複数必要（リソース消費増）
- ⚠ トランザクション管理の複雑さ増大

**📖 詳細**: `nablarch://pattern/multi-db-pattern`

---

### 🥈 第2位: ...
```

---

## 5. パターンマッチングロジック

### 5.1 全体フロー（4フェーズ）

```
recommend_pattern Tool 呼び出し
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 1: 要件分析（RequirementAnalyzer）                   │
│                                                           │
│  入力: requirement（自然言語テキスト）                     │
│  処理:                                                     │
│    1. キーワード抽出（形態素ベース分割）                   │
│    2. カテゴリ推定（architecture / action / validation /   │
│       data-access / security / handler のいずれか）        │
│    3. 意図分類（新規設計 / 既存改善 / トラブル対応）       │
│  出力: AnalyzedRequirement                                 │
│    { keywords: [...], estimated_category: "...",           │
│      intent: "...", app_type: "..." }                     │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 2: 候補収集（CandidateCollector）                     │
│                                                           │
│  ┌────────────────────────────────────────┐              │
│  │ 静的知識ベース参照                      │              │
│  │  design-patterns.yaml（11パターン）     │              │
│  │  → app_typeフィルタリング               │              │
│  │  → カテゴリフィルタリング               │              │
│  └────────────────────┬───────────────────┘              │
│                       │                                   │
│  ┌────────────────────────────────────────┐              │
│  │ RAG検索（Phase 2+、semantic_search経由）│              │
│  │  → 要件テキストでベクトル検索           │              │
│  │  → 追加のパターン情報・事例を取得       │              │
│  └────────────────────┬───────────────────┘              │
│                       │                                   │
│  出力: 候補パターンリスト（フィルタ済み）                  │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 3: スコアリング（PatternScorer）                      │
│                                                           │
│  各候補パターンに対してスコアを算出                        │
│  （詳細は §6 スコアリング設計を参照）                      │
│                                                           │
│  出力: スコア付きパターンリスト                             │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 4: ランキング生成（RankingGenerator）                 │
│                                                           │
│  1. スコア降順でソート                                     │
│  2. max_results件に切り詰め                                │
│  3. 各パターンの根拠テキストを生成                         │
│  4. コード例・Resource URI・参考URLを付与                  │
│  5. Markdown形式にフォーマット                              │
│                                                           │
│  出力: Markdown形式のパターン推薦結果                       │
└──────────────────────────────────────────────────────────┘
```

### 5.2 Phase 1: 要件分析の詳細

#### 5.2.1 キーワード抽出

入力テキストを空白・句読点で分割し、以下のキーワードカテゴリに分類する:

| カテゴリ | キーワード例 |
|---------|------------|
| アーキテクチャ | ハンドラ, キュー, パイプライン, リクエスト処理 |
| データアクセス | DB, データベース, SQL, DAO, CRUD, 検索, 更新 |
| バリデーション | バリデーション, 入力チェック, フォーム, 検証 |
| セキュリティ | 認証, CSRF, トークン, セキュリティ, 二重送信 |
| 排他制御 | 排他, ロック, 楽観, 悲観, バージョン, 競合 |
| ファイル | ダウンロード, CSV, Excel, PDF, ファイル |

#### 5.2.2 カテゴリ推定

抽出されたキーワードの出現頻度と重みから、最も関連するパターンカテゴリを推定する。
design-patterns.yaml の `category` フィールド値と対応:

- `architecture`: ハンドラキュー、システムリポジトリ
- `action`: アクションクラス、ファイルダウンロード
- `validation`: フォームバリデーション
- `data-access`: Universal DAO、外部SQL、排他制御、複数DB
- `security`: 二重送信防止
- `handler`: インターセプタ

### 5.3 Phase 2: 候補収集の詳細

#### 5.3.1 静的知識ベースフィルタリング

```
全11パターン
    │
    ├── app_typeフィルタ（指定時）
    │   → applicable_app_types に含まれるパターンのみ
    │
    ├── カテゴリフィルタ（推定カテゴリと一致 or 関連）
    │   → 完全一致: そのまま候補
    │   → 関連カテゴリ: related_patterns経由で候補追加
    │
    └── 全パターンを候補として保持（スコアで絞り込み）
```

#### 5.3.2 RAG検索（Phase 2+）

Phase 2以降では`semantic_search` Toolを内部呼び出しし、以下を検索する:

```
semantic_search({
  query: "{requirement}",
  filters: { type: "design-pattern" },
  top_k: 5,
  mode: "hybrid"
})
```

検索結果から、各パターンに関連する追加情報（適用事例、注意点、ベストプラクティス）を取得し、
スコアリングおよび根拠テキスト生成に活用する。

---

## 6. RAG検索+スコアリング設計

### 6.1 スコアリングモデル

各候補パターンに対して5つのスコアファクターを算出し、加重平均でトータルスコアを計算する。

#### 6.1.1 Phase 1 スコアリング（静的知識のみ）

| ファクター | 記号 | 重み | 算出方法 |
|-----------|------|------|---------|
| キーワード一致度 | S1 | 0.40 | パターンのdescription, problem, solutionに含まれるキーワードとの一致率 |
| カテゴリ一致度 | S2 | 0.25 | 推定カテゴリとパターンcategoryの一致（完全一致=1.0, 関連=0.5, 不一致=0.0） |
| app_type適合度 | S3 | 0.20 | パターンのapplicable_app_typesに指定app_typeが含まれるか（含む=1.0, 未指定=0.5, 不適合=0.0） |
| 制約一致度 | S4 | 0.15 | constraintsパラメータとパターン特性の一致度 |

```
Phase 1 Total Score = S1 × 0.40 + S2 × 0.25 + S3 × 0.20 + S4 × 0.15
```

#### 6.1.2 Phase 2+ スコアリング（RAG強化）

Phase 2以降では、セマンティック類似度（S5）を追加し、重みを再配分する。

| ファクター | 記号 | 重み（Phase 2+） | 算出方法 |
|-----------|------|-----------------|---------|
| キーワード一致度 | S1 | 0.25 | 同上 |
| カテゴリ一致度 | S2 | 0.20 | 同上 |
| app_type適合度 | S3 | 0.15 | 同上 |
| 制約一致度 | S4 | 0.15 | 同上 |
| セマンティック類似度 | S5 | 0.25 | semantic_searchのスコア（Cross-Encoder reranking後） |

```
Phase 2+ Total Score = S1 × 0.25 + S2 × 0.20 + S3 × 0.15 + S4 × 0.15 + S5 × 0.25
```

### 6.2 S1: キーワード一致度の算出

```
S1 = matched_keywords / total_keywords

ここで:
- total_keywords: 要件テキストから抽出されたキーワード数
- matched_keywords: パターンの description + problem + solution テキスト中に
                    出現するキーワード数
```

照合対象フィールド（design-patterns.yaml）:
- `description`: パターンの概要説明
- `problem`: 解決する課題
- `solution`: ソリューション説明

### 6.3 S2: カテゴリ一致度の算出

```
if (estimated_category == pattern.category) → 1.0
else if (pattern.name in related_patterns_of(estimated_category)) → 0.5
else → 0.0
```

### 6.4 S3: app_type適合度の算出

```
if (app_type == null) → 0.5  // 未指定の場合は中立
else if (app_type in pattern.applicable_app_types) → 1.0
else → 0.0
```

### 6.5 S4: 制約一致度の算出

constraintsパラメータの各制約文字列と、パターンの特性（description, solution, trade_offs）を
キーワード照合し、一致する制約の割合を算出する。

```
S4 = matched_constraints / total_constraints
     (constraintsが空の場合は 0.5)
```

### 6.6 S5: セマンティック類似度の算出（Phase 2+）

`semantic_search` Toolの検索結果スコアをそのまま使用する。
Cross-Encoder rerankingにより、文脈を考慮した高精度な類似度が得られる。

---

## 7. Nablarchデザインパターンカタログとの連携

### 7.1 パターンカタログ（design-patterns.yaml）

本Toolが参照する11パターンの一覧:

| # | パターン名 | カテゴリ | 対応app_type |
|---|-----------|---------|-------------|
| 1 | handler-queue-pattern | architecture | web, rest, batch, messaging |
| 2 | action-class-pattern | action | web, rest, batch |
| 3 | form-validation-pattern | validation | web, rest |
| 4 | universal-dao-pattern | data-access | web, rest, batch, messaging |
| 5 | sql-file-pattern | data-access | web, rest, batch, messaging |
| 6 | exclusive-control-pattern | data-access | web, rest |
| 7 | double-submit-prevention-pattern | security | web |
| 8 | interceptor-pattern | handler | web, rest |
| 9 | file-download-pattern | action | web |
| 10 | system-repository-pattern | architecture | web, rest, batch, messaging |
| 11 | multi-db-pattern | data-access | web, rest, batch |

### 7.2 Resource URIとの関係

各パターンはResource URI `nablarch://pattern/{name}` でアクセス可能（06_resource-uri-design.md参照）。
推薦結果にResource URIを含めることで、AIアシスタントがパターンの詳細情報を後続のリクエストで取得できる。

| パターン名 | Resource URI |
|-----------|-------------|
| handler-queue-pattern | `nablarch://pattern/handler-queue-pattern` |
| action-class-pattern | `nablarch://pattern/action-class-pattern` |
| form-validation-pattern | `nablarch://pattern/form-validation-pattern` |
| universal-dao-pattern | `nablarch://pattern/universal-dao-pattern` |
| sql-file-pattern | `nablarch://pattern/sql-file-pattern` |
| exclusive-control-pattern | `nablarch://pattern/exclusive-control-pattern` |
| double-submit-prevention-pattern | `nablarch://pattern/double-submit-prevention-pattern` |
| interceptor-pattern | `nablarch://pattern/interceptor-pattern` |
| file-download-pattern | `nablarch://pattern/file-download-pattern` |
| system-repository-pattern | `nablarch://pattern/system-repository-pattern` |
| multi-db-pattern | `nablarch://pattern/multi-db-pattern` |

### 7.3 パターン情報の利用フィールド

design-patterns.yaml の各パターンから以下のフィールドを利用する:

| フィールド | スコアリング | 出力 | 説明 |
|-----------|:---------:|:----:|------|
| `name` | — | ○ | パターン識別子 |
| `category` | S2 | ○ | カテゴリ一致度算出 |
| `description` | S1 | ○ | キーワード照合対象 + 概要表示 |
| `problem` | S1 | ○ | キーワード照合対象 + 根拠テキスト |
| `solution` | S1 | ○ | キーワード照合対象 + ソリューション説明 |
| `structure` | — | ○ | 構造図の表示 |
| `code_example` | — | ○ | コード例の表示 |
| `related_patterns` | S2 | ○ | 関連パターンのカテゴリ一致判定 + 関連パターン表示 |
| `applicable_app_types` | S3 | ○ | app_type適合度算出 |
| `references` | — | ○ | 参考URL表示 |

---

## 8. パターン推薦の根拠提示方針

### 8.1 3層構造の根拠

推薦の根拠は以下の3層で提示する:

#### 第1層: 適合理由（fit_reason）
なぜこのパターンが要件に適合するかの簡潔な説明。
要件テキストとパターンのproblem/solutionの対応を示す。

```
例: "複数DB接続とトランザクション分離の要件に直接合致する。
     multi-db-patternは、DbConnectionManagementHandlerを複数定義し、
     connectionNameで接続先を分離するNablarch標準のパターンである。"
```

#### 第2層: ソリューション概要（solution_summary）
パターンのsolutionフィールドを要件のコンテキストに合わせて要約。
code_exampleから主要部分を抜粋し、具体性を持たせる。

#### 第3層: トレードオフ（trade_offs）
パターン適用時の注意点・制約を列挙。
以下の観点で自動生成:
- リソース消費（メモリ、DB接続プール等）
- 複雑さ（設定、コード量、学習コスト）
- パフォーマンス影響
- 他パターンとの組み合わせ制約

### 8.2 根拠テキスト生成ロジック

```
fit_reason = generateFitReason(requirement, pattern.problem, pattern.solution)
  → 要件テキストとproblem/solutionの一致キーワードを基に文章生成
  → Phase 2+ではRAG検索結果からの追加コンテキストも活用

solution_summary = summarizeSolution(pattern.solution, pattern.code_example)
  → solutionを3文以内に要約
  → code_exampleの主要部分（XML設定例またはJavaコード）を添付

trade_offs = analyzeTradeOffs(pattern, app_type, constraints)
  → パターン固有のトレードオフ（ハードコード + RAG検索）
  → app_typeとconstraintsに応じた動的なトレードオフ追加
```

---

## 9. MCP Tool登録・クラス設計

### 9.1 クラス図

```
com.tis.nablarch.mcp.tools
└── RecommendPatternTool
    ├── recommend(requirement, appType, constraints, maxResults): String
    ├── analyzeRequirement(requirement): AnalyzedRequirement
    ├── collectCandidates(analyzed, appType): List<PatternCandidate>
    ├── scorePattern(candidate, analyzed, appType, constraints): ScoredPattern
    └── generateRanking(scored, maxResults): String

com.tis.nablarch.mcp.tools.pattern
├── RequirementAnalyzer
│   ├── extractKeywords(text): List<String>
│   ├── estimateCategory(keywords): String
│   └── classifyIntent(text): String
├── PatternScorer
│   ├── calcKeywordMatchScore(keywords, pattern): double
│   ├── calcCategoryMatchScore(category, pattern): double
│   ├── calcAppTypeFitScore(appType, pattern): double
│   ├── calcConstraintMatchScore(constraints, pattern): double
│   └── calcTotalScore(s1, s2, s3, s4): double
├── RankingGenerator
│   ├── sortByScore(patterns): List<ScoredPattern>
│   ├── generateRationale(pattern, requirement): Rationale
│   └── formatMarkdown(ranked): String
└── model/
    ├── AnalyzedRequirement
    ├── PatternCandidate
    ├── ScoredPattern
    └── Rationale
```

### 9.2 Tool登録

```java
@Component
public class RecommendPatternTool {

    private final NablarchKnowledgeBase knowledgeBase;
    private final RequirementAnalyzer requirementAnalyzer;
    private final PatternScorer patternScorer;
    private final RankingGenerator rankingGenerator;

    public RecommendPatternTool(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
        this.requirementAnalyzer = new RequirementAnalyzer();
        this.patternScorer = new PatternScorer();
        this.rankingGenerator = new RankingGenerator();
    }

    @Tool(description = "Recommends Nablarch design patterns based on requirements. " +
        "Analyzes the requirement text and returns ranked pattern candidates " +
        "with fitness scores, rationale, code examples, and resource URIs.")
    public String recommend(
            @ToolParam(description = "Natural language description of the requirement") String requirement,
            @ToolParam(description = "Application type: web, rest, batch, messaging", required = false) String appType,
            @ToolParam(description = "Additional constraints for pattern selection", required = false) List<String> constraints,
            @ToolParam(description = "Maximum number of pattern candidates (1-11, default: 3)", required = false) Integer maxResults) {

        // デフォルト値設定
        if (maxResults == null || maxResults < 1 || maxResults > 11) {
            maxResults = 3;
        }
        if (constraints == null) {
            constraints = List.of();
        }

        // Phase 1: 要件分析
        AnalyzedRequirement analyzed = requirementAnalyzer.analyze(requirement);

        // Phase 2: 候補収集
        List<PatternCandidate> candidates = collectCandidates(analyzed, appType);

        // Phase 3: スコアリング
        List<ScoredPattern> scored = candidates.stream()
            .map(c -> patternScorer.score(c, analyzed, appType, constraints))
            .sorted(Comparator.comparingDouble(ScoredPattern::getTotalScore).reversed())
            .limit(maxResults)
            .toList();

        // Phase 4: ランキング生成
        return rankingGenerator.generate(scored, requirement, appType, maxResults);
    }

    private List<PatternCandidate> collectCandidates(AnalyzedRequirement analyzed, String appType) {
        List<DesignPattern> allPatterns = knowledgeBase.getAllDesignPatterns();
        return allPatterns.stream()
            .filter(p -> appType == null || p.getApplicableAppTypes().contains(appType))
            .map(p -> new PatternCandidate(p, analyzed))
            .toList();
    }
}
```

---

## 10. エラーハンドリング

### 10.1 エラーパターン

| エラー条件 | エラーメッセージ | 対処 |
|-----------|----------------|------|
| requirementが空文字 or null | "エラー: 要件テキストが指定されていません。パターン推薦にはrequirementパラメータが必要です。" | 即座にエラー返却 |
| requirementが10文字未満 | "エラー: 要件テキストが短すぎます（最低10文字）。具体的な要件を記述してください。" | 即座にエラー返却 |
| app_typeが不正値 | "エラー: 不明なアプリケーションタイプ '{value}'。有効な値: web, rest, batch, messaging" | 即座にエラー返却 |
| max_resultsが範囲外 | 自動的に1〜11にクランプ | 警告なしで修正 |
| 該当パターンなし | "指定された条件に一致するパターンが見つかりませんでした。条件を緩和して再度お試しください。" | 空結果を返却 |
| RAG検索失敗（Phase 2+） | Phase 1のスコアリングにフォールバック | ログ出力のみ、処理継続 |

### 10.2 フォールバック戦略

```
RAG検索（semantic_search）が利用不可 or エラーの場合:
  → Phase 1のスコアリングモデル（S1〜S4のみ）にフォールバック
  → 静的知識ベース（design-patterns.yaml）のみで推薦を実行
  → レスポンスに「※ RAGエンジンが利用できないため、静的知識のみで推薦しています」を付記
```

---

## 11. 設定パラメータ

### 11.1 application.yml

```yaml
nablarch:
  mcp:
    tools:
      recommend-pattern:
        # スコアリング重み（Phase 1）
        scoring:
          phase1:
            keyword-weight: 0.40
            category-weight: 0.25
            app-type-weight: 0.20
            constraint-weight: 0.15
          phase2:
            keyword-weight: 0.25
            category-weight: 0.20
            app-type-weight: 0.15
            constraint-weight: 0.15
            semantic-weight: 0.25
        # デフォルト返却件数
        default-max-results: 3
        # 最低スコア閾値（これ未満のパターンは結果に含めない）
        min-score-threshold: 0.20
```

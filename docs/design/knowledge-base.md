# 静的知識ベース設計書

> **バージョン**: 1.0
> **作成日**: 2026-02-02
> **WBS**: 1.1.3
> **親タスク**: subtask_043 (cmd_025 Batch B)

---

## 1. 知識ファイル全体構成

Phase 1 MVPでは、静的YAMLファイルベースの知識ベースを構築する。RAGエンジン（Phase 2）導入前の基盤として、MCPサーバーの各ツール・リソースが参照する知識データを提供する。

### 1.1 ディレクトリ構成

```
src/main/resources/knowledge/
├── handler-catalog.yaml      # ハンドラカタログ（既存・拡充）
├── api-patterns.yaml         # APIパターン集（既存・拡充）
├── handler-constraints.yaml  # ハンドラ順序制約ルール（新規）
├── module-catalog.yaml       # Nablarchモジュール一覧（新規）
├── error-catalog.yaml        # エラーメッセージ・解決策（新規）
├── config-templates.yaml     # XML設定テンプレート（新規）
└── design-patterns.yaml      # Nablarch設計パターン集（新規）
```

### 1.2 各ファイルの役割

| ファイル | 役割 | 主な利用ツール/リソース |
|---------|------|----------------------|
| handler-catalog.yaml | 6アプリタイプ別のハンドラキュー定義 | `design_handler_queue`, `nablarch://handler/*` |
| api-patterns.yaml | Nablarch APIの使用パターンとコード例 | `search_api`, `generate_code`, `nablarch://pattern/*` |
| handler-constraints.yaml | ハンドラ間の順序制約ルール | `design_handler_queue`, `validate_config`, `optimize_handler_queue` |
| module-catalog.yaml | Nablarchモジュール一覧と主要クラス | `search_api`, `nablarch://version` |
| error-catalog.yaml | よくあるエラーと解決策 | `troubleshoot` |
| config-templates.yaml | XML設定のテンプレート | `generate_code`, `nablarch://config/*` |
| design-patterns.yaml | Nablarch固有の設計パターン | `recommend_pattern`, `nablarch://pattern/*` |

---

## 2. YAMLスキーマ定義

### 2.1 handler-catalog.yaml

6つのアプリケーションタイプ別にハンドラキューを定義する。

```yaml
# トップレベル: アプリケーションタイプをキーとするマップ
<app_type>:                    # web | rest | batch | messaging | http-messaging | jakarta-batch
  description: string          # アプリタイプの説明
  handlers:                    # ハンドラリスト（順序通り）
    - name: string             # ハンドラ名（クラス名）
      fqcn: string             # 完全修飾クラス名
      description: string      # ハンドラの役割説明
      order: integer           # ハンドラキュー内の順番（1始まり）
      required: boolean        # このアプリタイプで必須か
      thread: string           # "main" | "sub"（マルチスレッド時のスレッド区分）
      constraints:             # 順序制約（オプション）
        must_before: [string]  # このハンドラより後に来るべきハンドラ名リスト
        must_after: [string]   # このハンドラより前に来るべきハンドラ名リスト
```

**アプリケーションタイプ一覧**:

| タイプ | 説明 |
|-------|------|
| web | Webアプリケーション（JSP/サーブレット） |
| rest | RESTful Webサービス（JAX-RS） |
| batch | Nablarchバッチ（都度起動型） |
| messaging | テーブルキューイング型メッセージング |
| http-messaging | HTTP同期メッセージング |
| jakarta-batch | Jakarta Batch（JSR 352）対応バッチ |

### 2.2 api-patterns.yaml

Nablarch APIの使用パターンをカテゴリ別に管理する。

```yaml
patterns:
  - name: string               # パターン名（英字ケバブケース）
    category: string           # web | rest | batch | messaging | library | testing | config
    description: string        # パターンの概要説明
    fqcn: string               # 主要クラスのFQCN（オプション）
    related_patterns: [string] # 関連パターン名のリスト（オプション）
    example: |                 # コード例（完全な動作レベル）
      // Java/XML/SQLのコード例
```

**カテゴリ一覧**:

| カテゴリ | 説明 |
|---------|------|
| web | Webアプリケーション固有パターン |
| rest | REST API固有パターン |
| batch | バッチ処理固有パターン |
| messaging | メッセージング固有パターン |
| library | 共通ライブラリの使用パターン |
| testing | テスト関連パターン |
| config | 設定・構成パターン |

### 2.3 handler-constraints.yaml

ハンドラ間の順序制約と互換性ルールを定義する。

```yaml
constraints:
  - handler: string            # ハンドラ名
    fqcn: string               # 完全修飾クラス名
    rule: string               # "must_be_outer" | "must_be_inner" | "relative_order" | "conditional"
    must_before: [string]      # このハンドラより後に来るべきハンドラ（オプション）
    must_after: [string]       # このハンドラより前に来るべきハンドラ（オプション）
    incompatible_with: [string]  # 同時に使用できないハンドラ（オプション）
    required_by_app_type: [string]  # このハンドラが必須のアプリタイプ（オプション）
    reason: string             # 制約の理由
```

### 2.4 module-catalog.yaml

Nablarchの主要モジュール（Mavenアーティファクト）を一覧管理する。

```yaml
modules:
  - name: string               # モジュール名（artifactId）
    group_id: string           # Maven groupId
    artifact_id: string        # Maven artifactId
    description: string        # モジュールの概要
    category: string           # "core" | "web" | "batch" | "messaging" | "testing" | "library" | "integration"
    key_classes:               # 主要クラスリスト
      - fqcn: string           # 完全修飾クラス名
        description: string    # クラスの役割
    since_version: string      # 導入バージョン（オプション）
    dependencies: [string]     # 依存する他のNablarchモジュール（オプション）
```

### 2.5 error-catalog.yaml

よくあるNablarchエラーとその解決策を管理する。

```yaml
errors:
  - id: string                 # エラーID（例: "ERR-001"）
    category: string           # "handler" | "database" | "validation" | "config" | "messaging" | "batch" | "general"
    error_message: string      # エラーメッセージ（パターン）
    cause: string              # 原因の説明
    solution: string           # 解決策
    example_stack_trace: string  # スタックトレース例（オプション）
    related_handlers: [string]   # 関連するハンドラ（オプション）
    related_modules: [string]    # 関連するモジュール（オプション）
    severity: string           # "critical" | "error" | "warning"
```

### 2.6 config-templates.yaml

NablarchのXML設定テンプレートを管理する。

```yaml
templates:
  - name: string               # テンプレート名
    category: string           # "web-xml" | "component" | "handler-queue" | "database" | "routing"
    app_type: string           # 対象アプリタイプ（オプション）
    description: string        # テンプレートの説明
    template: |                # XMLテンプレート本体
      <?xml version="1.0" encoding="UTF-8"?>
      ...
    parameters:                # テンプレートのパラメータ説明（オプション）
      - name: string
        description: string
        default: string
```

### 2.7 design-patterns.yaml

Nablarch固有の設計パターンを管理する。

```yaml
patterns:
  - name: string               # パターン名
    category: string           # "architecture" | "handler" | "action" | "data-access" | "validation" | "security" | "batch" | "messaging"
    description: string        # パターンの概要
    problem: string            # このパターンが解決する問題
    solution: string           # 解決方法の説明
    structure: string          # 構造の説明（テキストまたはASCII図）
    code_example: |            # 実装コード例
      // Java/XMLコード
    related_patterns: [string] # 関連パターン名
    applicable_app_types: [string]  # 適用可能なアプリタイプ
    references: [string]       # 参考URL（オプション）
```

---

## 3. 検索インデックス設計

### 3.1 概要

Phase 1では、静的YAMLファイルをアプリケーション起動時にメモリにロードし、キーワード検索・カテゴリフィルタを提供する。Phase 2でRAGエンジンに置き換える際、同じインターフェースを維持してシームレスに移行する。

### 3.2 NablarchKnowledgeBase クラス設計

```java
@Component
public class NablarchKnowledgeBase {

    // インメモリデータ構造
    private Map<String, HandlerQueue> handlerCatalog;          // key: app_type
    private List<ApiPattern> apiPatterns;
    private List<HandlerConstraint> handlerConstraints;
    private List<NablarchModule> moduleCatalog;
    private List<ErrorEntry> errorCatalog;
    private List<ConfigTemplate> configTemplates;
    private List<DesignPattern> designPatterns;

    // 検索用インデックス（起動時構築）
    private Map<String, List<ApiPattern>> patternsByCategoryIndex;
    private Map<String, List<ApiPattern>> patternsByKeywordIndex;
    private Map<String, List<NablarchModule>> modulesByKeyClassIndex;
    private Map<String, List<ErrorEntry>> errorsByCategoryIndex;

    @PostConstruct
    public void initialize() {
        // 1. YAMLファイル読み込み（SnakeYAML使用）
        // 2. データモデルへのデシリアライズ
        // 3. 検索インデックスの構築
    }

    // --- 検索API ---

    /** ハンドラカタログをアプリタイプで取得 */
    public HandlerQueue getHandlerQueue(String appType);

    /** APIパターンをキーワード検索 */
    public List<ApiPattern> searchPatterns(String keyword, String category);

    /** ハンドラの順序制約を取得 */
    public List<HandlerConstraint> getConstraintsForHandler(String handlerName);

    /** ハンドラキューの順序制約を検証 */
    public List<ConstraintViolation> validateHandlerOrder(String appType, List<String> handlerNames);

    /** モジュールを検索 */
    public List<NablarchModule> searchModules(String keyword);

    /** FQCNでモジュールを取得 */
    public Optional<NablarchModule> findModuleByClass(String fqcn);

    /** エラーメッセージで解決策を検索 */
    public List<ErrorEntry> searchErrors(String errorMessage);

    /** 設定テンプレートを取得 */
    public List<ConfigTemplate> getTemplates(String category, String appType);

    /** 設計パターンを検索 */
    public List<DesignPattern> searchDesignPatterns(String keyword, String appType);
}
```

### 3.3 インメモリデータ構造

```
handlerCatalog:
  Map<String, HandlerQueue>
  └─ "web" → HandlerQueue { description, List<Handler> }
  └─ "rest" → HandlerQueue { ... }
  └─ "batch" → HandlerQueue { ... }
  └─ "messaging" → HandlerQueue { ... }
  └─ "http-messaging" → HandlerQueue { ... }
  └─ "jakarta-batch" → HandlerQueue { ... }

patternsByCategoryIndex:
  Map<String, List<ApiPattern>>
  └─ "web" → [action-class, form-validation, http-response, ...]
  └─ "rest" → [rest-action, jaxrs-body-convert, ...]
  └─ "library" → [universal-dao, sql-file, code-manager, ...]

patternsByKeywordIndex:
  Map<String, List<ApiPattern>>
  └─ "dao" → [universal-dao, universal-dao-crud, ...]
  └─ "form" → [form-validation, inject-form-on-error, ...]
  └─ "handler" → [handler-queue-pattern, ...]

modulesByKeyClassIndex:
  Map<String, NablarchModule>
  └─ "nablarch.common.dao.UniversalDao" → module(nablarch-common-dao)
  └─ "nablarch.fw.web.HttpResponse" → module(nablarch-fw-web)

errorsByCategoryIndex:
  Map<String, List<ErrorEntry>>
  └─ "handler" → [ERR-001, ERR-002, ...]
  └─ "database" → [ERR-003, ERR-004, ...]
```

### 3.4 起動時ロードの仕組み

```
1. Spring Boot起動
2. @PostConstruct: NablarchKnowledgeBase.initialize()
   a. ClassPathResourceで knowledge/*.yaml を読み込み
   b. SnakeYAML (org.yaml.snakeyaml) でパース
   c. 各データモデルクラスにマッピング
   d. 検索インデックス（Map）を構築
3. MCPツール/リソースハンドラが NablarchKnowledgeBase をDI注入
4. リクエスト時にインメモリ検索を実行
```

**ロード時のエラーハンドリング**:
- YAMLパースエラー → 起動失敗（フェイルファスト）
- ファイル不在 → 起動失敗（全ファイル必須）
- FQCNの検証 → ログ警告（起動は継続）

### 3.5 Phase 2 への移行パス

Phase 2でRAGエンジンを導入する際、`NablarchKnowledgeBase`のインターフェースは維持し、内部実装のみRAGベースに切り替える:

```
Phase 1: NablarchKnowledgeBase → YAMLファイル（インメモリ）
Phase 2: NablarchKnowledgeBase → RAGEngine → pgvector
```

この設計により、MCPツール/リソースのコードは変更不要。

---

## 4. データモデルクラス（参考）

Phase 1で使用するJavaデータモデルの概要（実装はBatch Aが担当）:

```java
// ハンドラ
record Handler(String name, String fqcn, String description,
               int order, boolean required, String thread,
               HandlerConstraints constraints) {}

// ハンドラキュー
record HandlerQueue(String description, List<Handler> handlers) {}

// APIパターン
record ApiPattern(String name, String category, String description,
                  String fqcn, String example, List<String> relatedPatterns) {}

// ハンドラ制約
record HandlerConstraint(String handler, String fqcn, String rule,
                         List<String> mustBefore, List<String> mustAfter,
                         List<String> incompatibleWith,
                         List<String> requiredByAppType, String reason) {}

// モジュール
record NablarchModule(String name, String groupId, String artifactId,
                      String description, String category,
                      List<KeyClass> keyClasses, String sinceVersion,
                      List<String> dependencies) {}

// エラーエントリ
record ErrorEntry(String id, String category, String errorMessage,
                  String cause, String solution, String exampleStackTrace,
                  List<String> relatedHandlers, List<String> relatedModules,
                  String severity) {}

// 設定テンプレート
record ConfigTemplate(String name, String category, String appType,
                      String description, String template,
                      List<TemplateParameter> parameters) {}

// 設計パターン
record DesignPattern(String name, String category, String description,
                     String problem, String solution, String structure,
                     String codeExample, List<String> relatedPatterns,
                     List<String> applicableAppTypes, List<String> references) {}
```

---

## 参考文献

- [アーキテクチャ設計書](../architecture.md) — RAG-enhanced MCPサーバーの全体設計
- [ADR-001](../decisions/ADR-001_rag-enhanced-architecture.md) — RAG-enhanced アーキテクチャ採用の意思決定記録
- [Nablarch公式ドキュメント](https://nablarch.github.io/docs/LATEST/doc/)

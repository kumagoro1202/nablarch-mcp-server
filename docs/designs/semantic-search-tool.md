# semantic_search Tool 詳細設計書

> **WBS番号**: 2.1.6
> **ステータス**: 設計完了
> **作成日**: 2026-02-02
> **作成者**: ashigaru8 (subtask_056)
> **関連文書**: hybrid-search.md, reranking.md, architecture.md §4.6
> **依存タスク**: WBS 2.1.4（ハイブリッド検索設計）, WBS 2.1.5（リランキング設計）

---

## 目次

1. [概要](#1-概要)
2. [Tool定義](#2-tool定義)
3. [入力スキーマ](#3-入力スキーマ)
4. [出力スキーマ](#4-出力スキーマ)
5. [RAGパイプライン呼び出しフロー](#5-ragパイプライン呼び出しフロー)
6. [MCP Tool登録](#6-mcp-tool登録)
7. [Phase 1 search_api との関係](#7-phase-1-search_api-との関係)
8. [エラーハンドリング](#8-エラーハンドリング)
9. [設定パラメータ](#9-設定パラメータ)

---

## 1. 概要

### 1.1 目的

本設計書は、Phase 2で新規追加する `semantic_search` MCP Toolの詳細設計を定義する。
このToolはRAGパイプライン全体（クエリ解析→Embedding生成→ハイブリッド検索→リランキング→結果整形）
を統合し、AIアシスタントがNablarchの知識ベースに対してセマンティック検索を実行するための
単一エントリポイントを提供する。

### 1.2 スコープ

- `semantic_search` Toolの入出力スキーマ定義
- RAGパイプライン呼び出しフローの設計
- MCP Tool登録パターンの設計
- 既存 `search_api` Toolとの共存方針

---

## 2. Tool定義

### 2.1 Tool概要

| 項目 | 値 |
|------|-----|
| Tool名 | `semantic_search` |
| パッケージ | `com.tis.nablarch.mcp.rag.tools` |
| クラス名 | `SemanticSearchTool` |
| 説明 | Nablarch知識ベースに対するセマンティック検索。ハイブリッド検索（BM25+ベクトル）とCross-Encoderリランキングで高精度な結果を返す |
| カテゴリ | 検索系Tool |

### 2.2 ユースケース

| ユースケース | クエリ例 | 期待モード |
|------------|---------|----------|
| API使い方検索 | "Universal DAOでのデータベースアクセス方法" | hybrid |
| ハンドラ設定 | "REST APIの認証ハンドラキュー構成" | hybrid |
| FQCN逆引き | "nablarch.fw.web.HttpResponse" | keyword |
| 概念検索 | "バッチ処理のエラーハンドリング方針" | vector |
| トラブルシューティング | "SystemRepositoryのコンポーネント定義が見つからない" | hybrid |
| 設定例検索 | "web-component-configuration.xmlの設定例" | keyword |

---

## 3. 入力スキーマ

### 3.1 パラメータ定義

| パラメータ | 型 | 必須 | デフォルト | 説明 |
|----------|-----|------|----------|------|
| `query` | string | ○ | — | 検索クエリ（自然言語またはキーワード） |
| `filters` | object | × | null | メタデータフィルタ条件 |
| `top_k` | integer | × | 10 | 返却する結果数（1-50） |
| `mode` | string | × | "hybrid" | 検索モード: "hybrid", "vector", "keyword" |

### 3.2 filters オブジェクト

| フィールド | 型 | 説明 | 有効値 |
|----------|-----|------|--------|
| `app_type` | string | アプリケーション種別 | "web", "rest", "batch", "messaging", "http-messaging", "jakarta-batch" |
| `module` | string | モジュール名 | "nablarch-core-repository", "nablarch-fw-web" 等 |
| `source` | string | データソース | "nablarch-document", "github", "fintan", "javadoc" |
| `source_type` | string | コンテンツ種別 | "documentation", "code", "javadoc", "config", "standard" |
| `language` | string | 言語 | "ja", "en" |

### 3.3 JSON Schema

```json
{
  "type": "object",
  "properties": {
    "query": {
      "type": "string",
      "description": "Search query in natural language or keywords. Supports Japanese and English."
    },
    "filters": {
      "type": "object",
      "description": "Optional metadata filters to narrow results.",
      "properties": {
        "app_type": {
          "type": "string",
          "enum": ["web", "rest", "batch", "messaging", "http-messaging", "jakarta-batch"],
          "description": "Nablarch application type filter."
        },
        "module": {
          "type": "string",
          "description": "Nablarch module name filter (e.g. nablarch-fw-web)."
        },
        "source": {
          "type": "string",
          "enum": ["nablarch-document", "github", "fintan", "javadoc"],
          "description": "Data source filter."
        },
        "source_type": {
          "type": "string",
          "enum": ["documentation", "code", "javadoc", "config", "standard"],
          "description": "Content type filter."
        },
        "language": {
          "type": "string",
          "enum": ["ja", "en"],
          "description": "Language filter."
        }
      }
    },
    "top_k": {
      "type": "integer",
      "minimum": 1,
      "maximum": 50,
      "default": 10,
      "description": "Number of results to return."
    },
    "mode": {
      "type": "string",
      "enum": ["hybrid", "vector", "keyword"],
      "default": "hybrid",
      "description": "Search mode: hybrid (BM25+vector+rerank), vector (semantic only), keyword (BM25 only)."
    }
  },
  "required": ["query"]
}
```

---

## 4. 出力スキーマ

### 4.1 レスポンス構造

```json
{
  "query": "REST APIの認証ハンドラキュー構成",
  "mode": "hybrid",
  "total_results": 5,
  "search_time_ms": 245,
  "results": [
    {
      "content": "## REST APIのハンドラキュー構成\n\nRESTful Webサービスの...",
      "score": 0.952,
      "metadata": {
        "source": "nablarch-document",
        "source_type": "documentation",
        "app_type": "rest",
        "module": "nablarch-fw-jaxrs",
        "language": "ja",
        "section_hierarchy": ["Application Framework", "Web Service", "RESTful Web Service"]
      },
      "source_url": "https://nablarch.github.io/docs/LATEST/doc/application_framework/..."
    },
    {
      "content": "ハンドラキューの順序制約として...",
      "score": 0.891,
      "metadata": { ... },
      "source_url": "..."
    }
  ]
}
```

### 4.2 results 配列要素

| フィールド | 型 | 説明 |
|----------|-----|------|
| `content` | string | チャンクのテキスト内容（Markdown形式） |
| `score` | number | 関連度スコア [0, 1]（リランキング後のスコア） |
| `metadata` | object | チャンクのメタデータ（architecture.md §4.3準拠） |
| `source_url` | string | 元ドキュメントのURL |

### 4.3 MCP Tool応答フォーマット

MCP Toolはテキスト形式で応答する。JSON構造は内部的に整形し、
AIアシスタントが解釈しやすいMarkdown形式で返却する。

```
## 検索結果: "REST APIの認証ハンドラキュー構成"
モード: hybrid | 結果数: 5件 | 検索時間: 245ms

---

### 結果 1 (スコア: 0.952)
**ソース**: nablarch-document | rest | nablarch-fw-jaxrs
**URL**: https://nablarch.github.io/docs/...

REST APIのハンドラキュー構成

RESTful Webサービスの...

---

### 結果 2 (スコア: 0.891)
...
```

---

## 5. RAGパイプライン呼び出しフロー

### 5.1 全体フロー

```
semantic_search Tool 呼び出し
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 1: クエリ解析                                       │
│                                                           │
│  ┌────────────────────────────────────────┐              │
│  │ QueryAnalyzer                          │              │
│  │  - 言語検出（ja / en / mixed）          │              │
│  │  - エンティティ抽出                     │              │
│  │    (FQCN, ハンドラ名, モジュール名)      │              │
│  │  - クエリ分類                           │              │
│  │    (code / documentation / config)      │              │
│  │  - クエリ拡張（同義語・英語翻訳）        │              │
│  └────────────────────┬───────────────────┘              │
│                       │                                   │
│  出力: AnalyzedQuery                                      │
│   - originalQuery: "REST APIの認証ハンドラキュー構成"      │
│   - language: "ja"                                        │
│   - entities: ["REST", "認証", "ハンドラキュー"]            │
│   - queryType: "documentation"                            │
│   - expandedTerms: ["RESTful", "authentication",          │
│                      "handler queue"]                     │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 2: Embedding生成                                    │
│                                                           │
│  queryType に基づきモデル選択:                              │
│  - documentation → Jina v4                                │
│  - code → Voyage-code-3                                   │
│  - mixed → Jina v4（デフォルト）                           │
│                                                           │
│  入力: expandedQuery（原文 + 拡張語句）                    │
│  出力: float[1024] queryEmbedding                         │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 3: ハイブリッド検索（hybrid-search.md 参照）         │
│                                                           │
│  BM25検索 ──────────────┐                                │
│  (PostgreSQL FTS)       │                                │
│  Top-50                 ├──→ RRF統合 → Top-50候補        │
│                         │                                │
│  ベクトル検索 ──────────┘                                │
│  (pgvector cosine)                                       │
│  Top-50                                                  │
│                                                           │
│  ※ mode=keyword → BM25のみ                               │
│  ※ mode=vector → ベクトル検索のみ                         │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 4: リランキング（reranking.md 参照）                │
│                                                           │
│  Jina Reranker v2                                        │
│  入力: query + Top-50候補                                 │
│  出力: リランキング済みTop-K                               │
│                                                           │
│  ※ mode=keyword → リランキングスキップ                    │
│  ※ API障害時 → RRFスコアでフォールバック                   │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────┐
│ Phase 5: 結果整形                                         │
│                                                           │
│  - SearchResult → Markdown形式に変換                      │
│  - メタデータ付与（source_url, section_hierarchy等）       │
│  - 検索時間計測・付与                                     │
│  - AIアシスタント向けフォーマット出力                       │
└──────────────────────────────────────────────────────────┘
```

### 5.2 クエリ解析（QueryAnalyzer）の詳細

#### 5.2.1 言語検出

| パターン | 検出結果 | 処理 |
|---------|---------|------|
| 全角文字を含む | `ja` | 日本語辞書でFTS、日英両言語でEmbedding |
| ASCII文字のみ | `en` | 英語辞書でFTS |
| 混在 | `mixed` | 両辞書でFTS、日本語優先でEmbedding |

#### 5.2.2 エンティティ抽出

正規表現ベースのエンティティ抽出を行う。

| エンティティ型 | パターン | 例 |
|-------------|---------|-----|
| FQCN | `[a-z]+(\.[a-z]+)*\.[A-Z]\w+` | `nablarch.fw.Handler` |
| クラス名 | `[A-Z][a-zA-Z0-9]+` (PascalCase) | `HttpResponse`, `SystemRepository` |
| ハンドラ名 | `\w+Handler` | `ThreadContextHandler` |
| XML要素 | `<[\w-]+>` または `\w+-configuration\.xml` | `web-component-configuration.xml` |

#### 5.2.3 クエリ拡張

検出されたエンティティに基づき、同義語・関連語を追加する。

```
原文: "Universal DAOでのデータベースアクセス"
拡張: "Universal DAO UniversalDao database access nablarch-common-dao CRUD"
```

拡張は以下のソースから構築：
- Nablarch固有の用語辞書（知識ベースから自動生成）
- 日英翻訳マッピング（「ハンドラ」↔「Handler」等）
- FQCN逆引きマッピング（「Universal DAO」→ `nablarch.common.dao.UniversalDao`）

### 5.3 検索対象テーブルの選択

queryTypeに基づき検索対象テーブルを選択する：

| queryType | 検索テーブル | Embeddingモデル |
|-----------|------------|---------------|
| documentation | document_chunks | Jina v4 |
| code | code_chunks | Voyage-code-3 |
| mixed（デフォルト） | 両テーブル | 各テーブルに対応するモデル |

---

## 6. MCP Tool登録

### 6.1 SemanticSearchTool クラス設計

```java
package com.tis.nablarch.mcp.rag.tools;

import com.tis.nablarch.mcp.rag.pipeline.RagPipeline;
import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCPツール: semantic_search。
 *
 * <p>Nablarch知識ベースに対するセマンティック検索ツール。
 * BM25キーワード検索とベクトルセマンティック検索を組み合わせた
 * ハイブリッド検索により、高精度な検索結果を提供する。</p>
 *
 * <p>Phase 1の{@code search_api}ツールの上位互換として機能する。</p>
 */
@Service
public class SemanticSearchTool {

    private final RagPipeline ragPipeline;

    /**
     * コンストラクタ。
     *
     * @param ragPipeline RAGパイプライン
     */
    public SemanticSearchTool(RagPipeline ragPipeline) {
        this.ragPipeline = ragPipeline;
    }

    /**
     * Nablarch知識ベースに対するセマンティック検索を実行する。
     *
     * @param query 検索クエリ（自然言語またはキーワード、日本語・英語対応）
     * @param appType アプリケーション種別フィルタ（web, rest, batch, messaging等）
     * @param module モジュール名フィルタ（nablarch-fw-web等）
     * @param source データソースフィルタ（nablarch-document, github, fintan, javadoc）
     * @param topK 返却する結果数（1-50、デフォルト10）
     * @param mode 検索モード（hybrid, vector, keyword、デフォルトhybrid）
     * @return 検索結果のMarkdownフォーマット文字列
     */
    @Tool(description = "Semantic search over the Nablarch knowledge base. "
            + "Uses hybrid search (BM25 + vector) with Cross-Encoder reranking "
            + "for high-accuracy results. Supports natural language queries in "
            + "Japanese and English. Use this for finding Nablarch APIs, patterns, "
            + "configurations, and troubleshooting information.")
    public String semanticSearch(
            @ToolParam(description = "Search query in natural language or keywords")
            String query,
            @ToolParam(description = "Optional app type filter: web, rest, batch, messaging")
            String appType,
            @ToolParam(description = "Optional module filter: e.g. nablarch-fw-web")
            String module,
            @ToolParam(description = "Optional source filter: nablarch-document, github, fintan, javadoc")
            String source,
            @ToolParam(description = "Number of results (1-50, default 10)")
            Integer topK,
            @ToolParam(description = "Search mode: hybrid (default), vector, keyword")
            String mode) {

        // 入力検証
        if (query == null || query.isBlank()) {
            return "検索クエリを指定してください。";
        }

        // パラメータ構築
        int effectiveTopK = (topK != null && topK >= 1 && topK <= 50) ? topK : 10;
        SearchMode effectiveMode = parseMode(mode);
        SearchFilters filters = new SearchFilters(
                nullIfBlank(appType),
                nullIfBlank(module),
                nullIfBlank(source),
                null, // sourceType
                null  // language
        );

        // RAGパイプライン実行
        long startTime = System.currentTimeMillis();
        List<SearchResult> results = ragPipeline.search(
                query, filters, effectiveTopK, effectiveMode);
        long elapsed = System.currentTimeMillis() - startTime;

        // 結果整形
        return formatResults(query, effectiveMode, results, elapsed);
    }

    private SearchMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) return SearchMode.HYBRID;
        return switch (mode.toLowerCase()) {
            case "vector" -> SearchMode.VECTOR;
            case "keyword" -> SearchMode.KEYWORD;
            default -> SearchMode.HYBRID;
        };
    }

    private String nullIfBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    private String formatResults(
            String query, SearchMode mode,
            List<SearchResult> results, long elapsedMs) {

        if (results.isEmpty()) {
            return "検索結果なし: \"" + query + "\" (モード: " + mode + ")";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 検索結果: \"").append(query).append("\"\n");
        sb.append("モード: ").append(mode.name().toLowerCase());
        sb.append(" | 結果数: ").append(results.size()).append("件");
        sb.append(" | 検索時間: ").append(elapsedMs).append("ms\n\n---\n\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            sb.append("### 結果 ").append(i + 1);
            sb.append(" (スコア: ").append(String.format("%.3f", r.score())).append(")\n");

            Map<String, String> meta = r.metadata();
            sb.append("**ソース**: ");
            if (meta != null) {
                sb.append(meta.getOrDefault("source", "不明"));
                sb.append(" | ").append(meta.getOrDefault("app_type", ""));
                sb.append(" | ").append(meta.getOrDefault("module", ""));
            }
            sb.append("\n");

            if (r.sourceUrl() != null && !r.sourceUrl().isBlank()) {
                sb.append("**URL**: ").append(r.sourceUrl()).append("\n");
            }
            sb.append("\n").append(r.content()).append("\n\n---\n\n");
        }

        return sb.toString();
    }
}
```

### 6.2 McpServerConfig への登録

```java
// McpServerConfig.java への追加（Phase 2）

@Bean
public MethodToolCallbackProvider nablarchTools(
        SearchApiTool searchApiTool,
        ValidateHandlerQueueTool validateHandlerQueueTool,
        SemanticSearchTool semanticSearchTool) {  // 新規追加
    return MethodToolCallbackProvider.builder()
            .toolObjects(searchApiTool, validateHandlerQueueTool, semanticSearchTool)
            .build();
}
```

### 6.3 RagPipeline インターフェース

```java
package com.tis.nablarch.mcp.rag.pipeline;

import com.tis.nablarch.mcp.rag.search.SearchFilters;
import com.tis.nablarch.mcp.rag.search.SearchMode;
import com.tis.nablarch.mcp.rag.search.SearchResult;

import java.util.List;

/**
 * RAGパイプラインインターフェース。
 * クエリ解析→Embedding→ハイブリッド検索→リランキング→結果返却を統合する。
 */
public interface RagPipeline {

    /**
     * RAGパイプラインを実行して検索結果を返す。
     *
     * @param query ユーザークエリ
     * @param filters メタデータフィルタ
     * @param topK 返却件数
     * @param mode 検索モード
     * @return 検索結果リスト
     */
    List<SearchResult> search(String query, SearchFilters filters, int topK, SearchMode mode);
}
```

---

## 7. Phase 1 search_api との関係

### 7.1 共存方針

| ツール | Phase | 検索方式 | データソース | ステータス |
|-------|-------|---------|------------|----------|
| `search_api` | Phase 1 | キーワード部分文字列マッチ | YAML知識ファイル（インメモリ） | 維持 |
| `semantic_search` | Phase 2 | ハイブリッド（BM25+Vector+Rerank） | PostgreSQL（document_chunks, code_chunks） | 新規 |

### 7.2 移行ロードマップ

| フェーズ | search_api | semantic_search | 備考 |
|---------|------------|----------------|------|
| Phase 2初期 | 維持 | 新規追加 | 両方がアクティブ、AIに選択させる |
| Phase 2後半 | deprecated表記追加 | 推奨 | search_apiのdescriptionに非推奨を明記 |
| Phase 3 | 削除検討 | 標準 | search_apiの全機能がsemantic_searchに包含 |

### 7.3 search_api を残す理由

1. **RAGパイプライン未稼働時のフォールバック**: DB障害時も静的YAML知識で検索可能
2. **軽量検索**: DBアクセス不要の高速検索（10ms未満）
3. **後方互換性**: 既存のMCP設定を使用するクライアントへの影響回避

---

## 8. エラーハンドリング

### 8.1 エラーパターンと応答

| エラー | 応答 | ログ |
|-------|------|------|
| query が null/blank | "検索クエリを指定してください。" | DEBUG |
| Embedding API 失敗 | keyword モードにフォールバック | WARN |
| DB接続失敗 | "検索サービスが一時的に利用できません。search_apiをお試しください。" | ERROR |
| リランキング失敗 | ハイブリッド検索結果をそのまま返却 | WARN |
| タイムアウト（300ms超） | 完了した分の結果を返却 | WARN |
| 結果0件 | "検索結果なし: ..." + 検索ヒントの提示 | INFO |

### 8.2 検索ヒント

結果が0件の場合、以下のヒントを提示する：

```
検索結果なし: "xyz"

ヒント:
- フィルタ条件を緩和してください（app_type, module等を外す）
- 別のキーワードや表現を試してください
- mode="keyword" でFQCN完全一致検索を試してください
- search_api ツールで静的知識ベースを検索してください
```

---

## 9. 設定パラメータ

```yaml
# application.yml
nablarch:
  rag:
    tool:
      semantic-search:
        enabled: true              # semantic_search Toolの有効/無効
        default-top-k: 10         # デフォルト結果数
        max-top-k: 50             # 最大結果数
        default-mode: hybrid      # デフォルト検索モード
        total-timeout-ms: 300     # 全体タイムアウト

    query-analyzer:
      entity-extraction: true     # エンティティ抽出の有効/無効
      query-expansion: true       # クエリ拡張の有効/無効
      nablarch-dictionary-path: classpath:knowledge/nablarch-terms.yaml
```

---

## 付録

### A. パッケージ構造（Phase 2追加分）

```
com.tis.nablarch.mcp.rag/
├── tools/
│   └── SemanticSearchTool.java        ← 本設計書
├── pipeline/
│   ├── RagPipeline.java               ← パイプラインIF
│   └── DefaultRagPipeline.java        ← パイプライン実装
├── search/
│   ├── SearchResult.java              ← DTO
│   ├── SearchMode.java                ← enum
│   ├── SearchFilters.java             ← フィルタ条件
│   ├── BM25SearchService.java         ← WBS 2.2.10
│   ├── VectorSearchService.java       ← WBS 2.2.11
│   └── HybridSearchService.java       ← WBS 2.2.12
├── rerank/
│   ├── RerankerService.java           ← WBS 2.2.13
│   ├── RerankerClient.java            ← API抽象化
│   ├── JinaRerankerClient.java        ← Jina実装
│   └── RerankResult.java              ← DTO
├── embedding/
│   ├── EmbeddingService.java          ← Embedding統合
│   ├── JinaEmbeddingClient.java       ← WBS 2.2.6
│   └── VoyageEmbeddingClient.java     ← WBS 2.2.7
└── query/
    ├── QueryAnalyzer.java             ← WBS 2.2.14
    └── AnalyzedQuery.java             ← 解析結果DTO
```

### B. 関連WBSタスク

| WBS | タスク | 本設計との関係 |
|-----|-------|-------------|
| 2.1.4 | ハイブリッド検索設計 | パイプライン Phase 3 の内部設計 |
| 2.1.5 | リランキング設計 | パイプライン Phase 4 の内部設計 |
| 2.2.14 | クエリ解析・拡張エンジン | パイプライン Phase 1 の実装 |
| 2.2.15 | semantic_search Tool実装 | **本設計書を実装** |
| 2.3.7 | semantic_search 統合テスト | 本設計の検証 |

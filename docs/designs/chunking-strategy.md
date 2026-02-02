# チャンキング戦略設計書

> **WBS**: 2.1.3
> **ステータス**: 完了
> **作成日**: 2026-02-02
> **関連**: WBS 2.1.2 (embedding-pipeline.md), architecture.md §4.2

---

## 1. チャンキング方針

### 1.1 基本原則

1. **セマンティック境界を尊重する**: 見出し・メソッド・XML要素等の論理的な区切りでチャンクを分割する
2. **コンテキスト保持**: 各チャンクが単独で意味をなすよう、必要なコンテキスト情報を付与する
3. **検索精度の最適化**: チャンクサイズはEmbeddingモデルの最適入力長に合わせる
4. **メタデータの充実**: フィルタリング・ソース帰属に必要なメタデータを全チャンクに付与する

### 1.2 トークン数の近似計算

本実装ではトークンカウントの近似として以下を採用する：

```
トークン数 ≈ 文字数 / 4（英語テキスト）
トークン数 ≈ 文字数 / 2（日本語テキスト）
```

Nablarchドキュメントは日本語中心のため、日本語比率に応じた動的な近似を行う。
ただし、厳密なトークン化はEmbedding段階でモデル側が行うため、
チャンキング段階では近似で十分である。

## 2. 6コンテンツタイプ別チャンキングルール

### 2.1 ContentType enum 定義

```java
public enum ContentType {
    HTML,       // 公式ドキュメント（HTML）
    MARKDOWN,   // Fintan記事等（Markdown）
    JAVADOC,    // Javadocコメント
    JAVA,       // Javaソースコード
    XML,        // XML設定ファイル
    TEXT        // プレーンテキスト（フォールバック）
}
```

### 2.2 チャンキングルールサマリ

| # | ContentType | 区切り戦略 | 最大トークン | オーバーラップ | Embeddingモデル | 格納先 |
|---|---|---|---|---|---|---|
| 1 | HTML | セクション単位（h2/h3見出し） | 512 | 128 (25%) | Jina v4 | document_chunks |
| 2 | MARKDOWN | 見出し単位（## / ###） | 512 | 128 (25%) | Jina v4 | document_chunks |
| 3 | JAVADOC | クラス/メソッド単位 | 256 | なし（コンテキスト付与） | Voyage-code-3 | code_chunks |
| 4 | JAVA | メソッド単位 | 512 | なし（コンテキスト付与） | Voyage-code-3 | code_chunks |
| 5 | XML | 要素単位（handler/component） | 256 | なし（親要素コンテキスト） | Voyage-code-3 | code_chunks |
| 6 | TEXT | 段落単位 | 512 | 128 (25%) | Jina v4 | document_chunks |

### 2.3 各タイプの詳細ルール

#### Type 1: HTML（公式ドキュメント）

```
【区切り戦略】
- 主区切り: h2 見出し
- 副区切り: h3 見出し（h2セクションが512トークン超の場合）
- コードブロック: <pre><code> は分割せず1チャンクに含める
- テーブル: <table> は分割せず1チャンクに含める

【チャンク構造】
  ## {見出しテキスト}

  {セクション本文}

  ```{言語}
  {コードブロック}
  ```

【オーバーラップ処理】
- 前チャンクの末尾128トークンを次チャンクの先頭に付与
- 見出しテキストは常にチャンクの先頭に配置（オーバーラップ部分の後）

【コンテキスト付与】
- 親見出し（h1）をメタデータの section_hierarchy に含める
- ページタイトルを metadata.title に含める

【サイズ超過時の処理】
- コードブロック含むセクションが512トークン超:
  コードブロック前後で分割（コードブロック自体は分割しない）
- コードブロック単体が512トークン超:
  警告ログ出力、そのまま1チャンクとして処理（超過許容）
```

#### Type 2: MARKDOWN（Fintan記事）

```
【区切り戦略】
- 主区切り: ## 見出し
- 副区切り: ### 見出し（##セクションが512トークン超の場合）
- コードフェンス: ```...``` は分割せず1チャンクに含める
- リスト: 連続するリスト項目は1チャンクにまとめる

【チャンク構造】
  ## {見出しテキスト}

  {セクション本文}

【オーバーラップ処理】
- HTML と同様（128トークン）

【コンテキスト付与】
- 記事タイトル（# 見出し）を metadata.title に含める
- 記事URLを metadata.source_url に含める
```

#### Type 3: JAVADOC（クラス/メソッドドキュメント）

```
【区切り戦略】
- クラスレベルJavadoc: 1クラス = 1チャンク（クラスシグネチャ含む）
- メソッドレベルJavadoc: 1メソッド = 1チャンク（メソッドシグネチャ含む）
- オーバーロードメソッド: 各オーバーロードは独立チャンク

【チャンク構造 — クラスレベル】
  /**
   * {クラスJavadoc}
   */
  public class {ClassName} {
      // フィールド一覧（シグネチャのみ）
  }

【チャンク構造 — メソッドレベル】
  // クラス名: {ClassName}
  /**
   * {メソッドJavadoc}
   * @param ...
   * @return ...
   */
  public {ReturnType} {methodName}({params})

【コンテキスト付与】
- FQCN を metadata.fqcn に含める
- パッケージ情報を metadata.package に含める
- クラスのJavadoc先頭1行を metadata.class_description に含める
```

#### Type 4: JAVA（ソースコード）

```
【区切り戦略】
- メソッド単位: 1メソッド = 1チャンク
- クラスヘッダ: クラス宣言 + フィールド定義 = 1チャンク（コンテキスト用）
- 内部クラス: 独立したチャンクとして処理
- import文: チャンクに含めない（FQCNで特定可能）

【チャンク構造 — メソッド】
  // Class: {ClassName} ({FQCN})
  // Fields: {field1}, {field2}, ...

  {メソッドJavadoc（あれば）}
  public {ReturnType} {methodName}({params}) {
      {メソッド本体}
  }

【コンテキスト付与】
- クラス名・FQCNをチャンク先頭にコメントとして付与
- フィールド一覧をコメントとして付与（メソッドが参照する可能性のあるフィールド）

【サイズ超過時の処理】
- メソッドが512トークン超: そのまま1チャンクとして処理（超過許容）
- クラスヘッダが512トークン超: フィールドを省略し主要なもののみ含める
```

#### Type 5: XML（設定ファイル）

```
【区切り戦略】
- 要素単位: 1トップレベル要素 = 1チャンク
  例: <component name="...">...</component>
       <handler class="...">...</handler>
- ネストした要素: 親要素を含めて1チャンクに
- ルート要素の属性: メタデータとして保持

【チャンク構造】
  <!-- File: {filePath} -->
  <!-- Parent: <{rootElement}> -->
  <component name="{componentName}" class="{className}">
    <property name="{prop}" value="{val}" />
  </component>

【コンテキスト付与】
- ファイルパスをコメントとして付与
- 親要素（ルート要素）のタグ名をコメントとして付与
- component/handler の class 属性値を metadata.fqcn に含める
```

#### Type 6: TEXT（プレーンテキスト・フォールバック）

```
【区切り戦略】
- 段落単位: 空行（\n\n）で区切る
- 段落が512トークン超: 文末（。）で分割
- 単一行の連続: 意味的にまとまりのある行グループで区切る

【オーバーラップ処理】
- 128トークン（HTML/Markdownと同様）

【用途】
- 上記5タイプに分類できないコンテンツのフォールバック
- README.md 等の構造化されていないテキスト
```

## 3. メタデータスキーマ

### 3.1 共通メタデータ（全チャンク必須）

```json
{
  "source": "nablarch-document | fintan | github",
  "source_type": "documentation | code | javadoc | config",
  "content_type": "HTML | MARKDOWN | JAVADOC | JAVA | XML | TEXT",
  "language": "ja | en",
  "nablarch_version": "6u2",
  "source_url": "https://...",
  "chunk_index": 0,
  "total_chunks": 5,
  "created_at": "2026-02-02T00:00:00Z"
}
```

### 3.2 ドキュメント系メタデータ（document_chunks用）

```json
{
  "title": "ハンドラキューの設計",
  "section_hierarchy": ["Application Framework", "Handler Queue"],
  "category": "architecture | handler | validation | database | testing",
  "app_type": "web | rest | batch | messaging | common",
  "has_code_block": true
}
```

### 3.3 コード系メタデータ（code_chunks用）

```json
{
  "file_path": "src/main/java/nablarch/fw/Handler.java",
  "fqcn": "nablarch.fw.Handler",
  "class_name": "Handler",
  "package_name": "nablarch.fw",
  "module": "nablarch-fw",
  "element_type": "class | method | field | component | handler",
  "method_name": "handle",
  "class_description": "ハンドラインターフェース"
}
```

### 3.4 メタデータの用途

| メタデータ | 用途 |
|---|---|
| source | ソース帰属表示、データソースフィルタリング |
| content_type | Embeddingモデル選択（Jina / Voyage）|
| app_type | アプリタイプフィルタリング（web, rest, batch等）|
| module | モジュールフィルタリング |
| fqcn | コード検索の精度向上 |
| section_hierarchy | 階層的なコンテキスト理解 |
| chunk_index / total_chunks | チャンクの位置情報、隣接チャンク取得 |

## 4. チャンク品質基準

### 4.1 品質チェック項目

| # | 基準 | 閾値 | 対応 |
|---|---|---|---|
| Q1 | 最小チャンクサイズ | 50文字以上 | 50文字未満のチャンクは前後と結合 |
| Q2 | 最大チャンクサイズ | 2048文字以下（推奨512トークン） | 超過時は警告ログ、分割検討 |
| Q3 | メタデータ必須項目 | source, content_type, source_url | 欠落時はパイプラインエラー |
| Q4 | コンテンツの意味性 | 意味のある文章/コード | 空白のみ、記号のみのチャンクは除外 |
| Q5 | エンコーディング | UTF-8 | 非UTF-8はスキップ＋ログ |

### 4.2 品質メトリクス（バッチ実行後に集計）

```
チャンキング品質レポート:
  総入力ドキュメント数: N
  総出力チャンク数: M
  平均チャンクサイズ: X トークン
  サイズ分布:
    0-128 tokens: n1
    128-256 tokens: n2
    256-512 tokens: n3
    512+ tokens: n4
  除外チャンク数: E（理由別内訳）
  コンテンツタイプ別チャンク数:
    HTML: h, MARKDOWN: m, JAVADOC: jd, JAVA: j, XML: x, TEXT: t
```

## 5. ChunkingService のインターフェース設計

```java
/**
 * チャンキングサービス。
 *
 * <p>ParsedDocumentをコンテンツタイプに応じた戦略で
 * DocumentChunkDtoのリストに分割する。</p>
 */
@Component
public class ChunkingService {

    /**
     * パース済みドキュメントをチャンクに分割する。
     *
     * @param document パース済みドキュメント
     * @return チャンクのリスト
     */
    public List<DocumentChunkDto> chunk(ParsedDocument document) { ... }
}
```

```java
/**
 * チャンク結果DTO。
 *
 * @param content チャンクのテキストコンテンツ
 * @param metadata メタデータ（source, content_type, fqcn等）
 * @param chunkIndex チャンクのインデックス（0始まり）
 * @param totalChunks 元ドキュメントの総チャンク数
 * @param contentType コンテンツタイプ
 */
public record DocumentChunkDto(
    String content,
    Map<String, String> metadata,
    int chunkIndex,
    int totalChunks,
    ContentType contentType
) {}
```

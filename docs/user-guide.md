# Nablarch MCP Server — ユーザーガイド

> **ステータス**: 計画段階（将来的な利用方法の想定を記載）
> **最終更新**: 2026-02-02
> **プロジェクト**: nablarch-mcp-server

---

## 目次

1. [インストール手順](#1-インストール手順)
2. [各AIツールでの設定方法](#2-各aiツールでの設定方法)
3. [基本的な使い方](#3-基本的な使い方)
4. [高度な使い方](#4-高度な使い方)
5. [FAQ](#5-faq)

---

## 1. インストール手順

### 前提条件

| 要件 | バージョン | 備考 |
|------|----------|------|
| **Java** | 17以上 | Java 21推奨。`java -version` で確認 |
| **Docker**（オプション） | 20.10以上 | Docker Compose利用時に必要 |
| **Git**（オプション） | 2.x以上 | ソースからビルドする場合 |

### 方法1: JARファイルによるローカル実行

最もシンプルな導入方法。ダウンロードしたJARファイルを直接実行する。

```bash
# 1. JARファイルをダウンロード（将来的にリリースページから取得可能になる想定）
# https://github.com/kumanoGoro/nablarch-mcp-server/releases から最新版を取得

# 2. 実行確認
java -jar nablarch-mcp-server-x.x.x.jar --help

# 3. STDIOモードで起動（AIツールからの接続用）
java -jar nablarch-mcp-server-x.x.x.jar
```

### 方法2: ソースからビルド

```bash
# 1. リポジトリをクローン
git clone https://github.com/kumanoGoro/nablarch-mcp-server.git
cd nablarch-mcp-server

# 2. ビルド
./gradlew build

# 3. 実行
./gradlew bootRun
```

ビルド成果物は `build/libs/nablarch-mcp-server-x.x.x.jar` に生成される。

### 方法3: Docker Compose（将来対応予定）

RAGエンジン（ベクトルDB等）を含むフルスタック構成での実行を想定。

```yaml
# docker-compose.yml（将来提供予定の想定構成）
version: '3.8'
services:
  nablarch-mcp:
    image: nablarch-mcp-server:latest
    ports:
      - "8080:8080"
    environment:
      - TRANSPORT_MODE=http
      - VECTOR_DB_URL=jdbc:postgresql://pgvector:5432/nablarch_kb
    depends_on:
      - pgvector

  pgvector:
    image: pgvector/pgvector:pg16
    environment:
      - POSTGRES_DB=nablarch_kb
      - POSTGRES_USER=nablarch
      - POSTGRES_PASSWORD=changeme
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

```bash
# 起動
docker compose up -d

# ログ確認
docker compose logs -f nablarch-mcp
```

### 方法4: リモートサーバーデプロイ（将来対応予定）

チーム共有用のリモートサーバーとしてデプロイする想定。Streamable HTTPトランスポートを使用し、複数の開発者が同一のMCPサーバーに接続できる。

```bash
# リモートモードで起動（想定）
java -jar nablarch-mcp-server-x.x.x.jar \
  --transport=http \
  --port=8080 \
  --auth=oauth2
```

---

## 2. 各AIツールでの設定方法

### Claude Desktop

`claude_desktop_config.json` に以下を追加する。

**設定ファイルの場所:**
- macOS: `~/Library/Application Support/Claude/claude_desktop_config.json`
- Windows: `%APPDATA%\Claude\claude_desktop_config.json`
- Linux: `~/.config/Claude/claude_desktop_config.json`

#### ローカルモード（STDIO）

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/nablarch-mcp-server-x.x.x.jar"
      ]
    }
  }
}
```

#### リモートモード（将来対応予定）

```json
{
  "mcpServers": {
    "nablarch": {
      "url": "http://your-server:8080/mcp",
      "transport": "streamable-http"
    }
  }
}
```

### Claude Code

プロジェクトルートの `.mcp.json` に以下を追加する。

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/nablarch-mcp-server-x.x.x.jar"
      ]
    }
  }
}
```

または、グローバル設定として `~/.claude/mcp.json` に記述することも可能。

### VS Code

VS Codeの `settings.json` にMCPサーバーを追加する。

```json
{
  "mcp": {
    "servers": {
      "nablarch": {
        "command": "java",
        "args": [
          "-jar",
          "/path/to/nablarch-mcp-server-x.x.x.jar"
        ]
      }
    }
  }
}
```

MCP対応の拡張機能（GitHub Copilot等）がインストールされている場合、MCPサーバーの Tools/Resources が利用可能になる。

### Cursor

Cursorの設定画面から「MCP Servers」を開き、以下の設定を追加する。

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/nablarch-mcp-server-x.x.x.jar"
      ]
    }
  }
}
```

### JetBrains IDE（IntelliJ IDEA等）

JetBrains IDEでMCPサーバーに対応するプラグインが利用可能になった場合、以下の設定を想定する。

**Settings → Tools → MCP Servers → Add:**
- **Name**: `nablarch`
- **Command**: `java`
- **Arguments**: `-jar /path/to/nablarch-mcp-server-x.x.x.jar`
- **Transport**: `stdio`

---

## 3. 基本的な使い方

MCPサーバーが接続されたAIツール上で、以下のようなプロンプトを入力して利用する。

### プロンプト例1: バッチアプリケーションの設計

**プロンプト:**
```
Nablarchでバッチアプリケーションを作成したい。都度起動バッチで、
CSVファイルを読み込んでDBに登録する処理を設計して。
```

**想定レスポンス概要:**
MCPサーバーが都度起動バッチのハンドラキュー構成テンプレート、CSVファイル読み込みのNablarchライブラリ（`FileDataReader`等）、DB登録のUniversal DAO利用パターンを検索・提供し、AIがそれらを統合してハンドラキューXML定義、バッチアクションクラス、SQL定義ファイルの設計案を生成する。

---

### プロンプト例2: ハンドラキュー構成の検証

**プロンプト:**
```
このNablarchハンドラキュー設定を検証して問題点を指摘して。

<component name="webFrontController" class="nablarch.fw.web.servlet.WebFrontController">
  <property name="handlerQueue">
    <list>
      <component class="nablarch.fw.web.handler.HttpCharacterEncodingHandler"/>
      <component class="nablarch.common.handler.DbConnectionManagementHandler"/>
      <component class="nablarch.common.handler.TransactionManagementHandler"/>
      <component-ref name="multipartHandler"/>
      <component-ref name="sessionStoreHandler"/>
    </list>
  </property>
</component>
```

**想定レスポンス概要:**
`validate_handler_queue` ツールがハンドラの順序制約をチェックし、不足しているハンドラ（`GlobalErrorHandler`、`HttpResponseHandler`等）の指摘、順序の妥当性検証結果、修正案を提示する。

---

### プロンプト例3: バージョンアップの注意点

**プロンプト:**
```
Nablarch 6u2から6u3へのアップグレードで注意すべき変更点は？
```

**想定レスポンス概要:**
RAGがNablarchのリリースノート・変更履歴を検索し、6u2→6u3の変更点（APIの追加・非推奨化・削除、動作変更、依存ライブラリの更新等）を一覧化して提供する。影響を受ける可能性のある箇所とその対処方法も提示する。

---

### プロンプト例4: RESTful Webサービスの構築

**プロンプト:**
```
NablarchでRESTful Webサービスを作りたい。ユーザー情報のCRUD APIを
設計して。認証にはセッションストアを使いたい。
```

**想定レスポンス概要:**
REST用ハンドラキュー構成（`JaxRsResponseHandler`、`BodyConvertHandler`等）、JAX-RSアノテーションを使ったアクションクラス、セッションストアハンドラの設定、リクエスト/レスポンスのJSON定義を含む包括的な設計案を生成する。

---

### プロンプト例5: エラーのトラブルシューティング

**プロンプト:**
```
Nablarchアプリケーションで以下のエラーが発生した。原因と対処法を教えて。

java.lang.IllegalStateException:
  nablarch.fw.ExecutionContext: handler queue is empty
```

**想定レスポンス概要:**
`troubleshoot` プロンプトと `semantic_search` ツールが連携し、このエラーの一般的な原因（コンポーネント定義XMLの未読み込み、`handlerQueue`プロパティの未設定等）を特定。具体的な確認手順（XMLファイルの読み込みパス、SystemRepositoryの初期化状態等）と修正方法を提示する。

---

### プロンプト例6: Webアプリケーションの新規作成

**プロンプト:**
```
Nablarchで社内の勤怠管理Webアプリケーションを新規作成したい。
以下の機能を含む設計を提案して。
- ログイン/ログアウト
- 勤怠入力（出勤・退勤時刻の登録）
- 月次勤怠一覧表示
- CSVエクスポート
```

**想定レスポンス概要:**
`create-web-app` プロンプトがアプリケーション作成ガイドを提供し、RAGがWebアプリケーション用ハンドラキュー構成パターン・セッション管理・認証ハンドラの情報を検索。画面遷移設計、テーブル設計、ハンドラキュー構成XML、主要アクションクラスのスケルトンを含む設計案を生成する。

---

### プロンプト例7: Universal DAOの使い方

**プロンプト:**
```
NablarchのUniversal DAOを使ってページング付きの検索処理を実装したい。
SQLの定義方法とJavaコードの書き方を教えて。
```

**想定レスポンス概要:**
`search_api` ツールでUniversalDao APIの仕様を検索し、RAGがSQL定義ファイル（`.sql`）の記法、`UniversalDao.findAllBySqlFile()`の使用パターン、`Pagination`オブジェクトの利用方法を検索。具体的なSQLファイル、Entityクラス、検索アクションのコード例を提示する。

---

### プロンプト例8: テストコードの生成

**プロンプト:**
```
UserRegistrationActionクラスのテストを、
Nablarchのテスティングフレームワーク形式で生成して。
正常系・異常系（バリデーションエラー）のケースを含めて。
```

**想定レスポンス概要:**
`generate_test` ツールが呼び出され、RAGがNablarchテスティングフレームワークの仕様（リクエスト単体テスト、DBテスト）とテスト観点カタログ（Fintanコンテンツ）を検索。JUnitテストクラス、Excelテストデータの構造、テストケース一覧（正常登録、必須項目未入力、文字数超過、重複エラー等）を生成する。

---

### プロンプト例9: コードレビュー

**プロンプト:**
```
以下のNablarchアクションクラスをレビューして、
規約違反やアンチパターンがあれば指摘して。

public class UserSearchAction {
    @InjectForm(form = UserSearchForm.class, prefix = "form")
    public HttpResponse search(HttpRequest request, ExecutionContext context) {
        UserSearchForm form = context.getRequestScopedVar("form");
        List<User> users = UniversalDao.findAllBySqlFile(User.class, "FIND_USERS",
            new Object[]{form.getUserName()});
        context.setRequestScopedVar("users", users);
        return new HttpResponse("/WEB-INF/view/user/list.jsp");
    }
}
```

**想定レスポンス概要:**
`review-code` プロンプトに基づき、RAGがNablarchコーディング規約、スレッドセーフティルール、アンチパターン集を検索。SQL検索パラメータの渡し方、エラーハンドリングの有無、`@InjectForm`の使い方のベストプラクティスとの差異等を指摘し、改善案を提示する。

---

### プロンプト例10: メッセージングアプリケーション

**プロンプト:**
```
NablarchのMOM（Message Oriented Middleware）メッセージングで、
応答不要の非同期メッセージ送信処理を作りたい。
キューに電文を投入して後続バッチで処理する構成を設計して。
```

**想定レスポンス概要:**
MOMメッセージング用ハンドラキュー構成、メッセージ電文のフォーマット設計、送信アクションクラス、受信バッチの構成を含む設計案を生成する。RAGがメッセージング固有のハンドラ（`MessageSendHandler`等）と電文フォーマット管理の仕様を検索して提供する。

---

### プロンプト例11: ハンドラキューの概念理解

**プロンプト:**
```
Nablarchのハンドラキューアーキテクチャについて、
初心者にもわかるように図解付きで説明して。
Springのフィルタチェーンとの違いも教えて。
```

**想定レスポンス概要:**
RAGがハンドラキューの概念説明ドキュメントを検索し、リクエスト→ハンドラ群→アクション→レスポンスの流れ、各ハンドラの役割（認証、DB接続管理、トランザクション等）を段階的に解説。Springのフィルタチェーンとの共通点（パイプライン処理）と相違点（XML設定中心、双方向処理、インターセプタ機構）を比較する。

---

### プロンプト例12: 設定ファイルの生成

**プロンプト:**
```
Nablarch Webアプリケーション用のコンポーネント定義XMLを生成して。
以下の要件を満たすハンドラキュー構成にしたい。
- CSRF対策あり
- セッションストア使用（DBストア）
- 二重送信防止
- マルチパートリクエスト対応
```

**想定レスポンス概要:**
`generate_handler` ツールが要件に基づいたハンドラキュー構成XMLを生成。`CsrfTokenVerificationHandler`、`SessionStoreHandler`（DB連携設定付き）、`TokenHandler`（二重送信防止）、`MultipartHandler`の配置と、各ハンドラの順序制約を考慮した正しい配列順を提示する。

---

## 4. 高度な使い方

### 4.1 カスタムリソースの追加（社内ドキュメントの取り込み）

将来的に、社内固有のNablarch開発ドキュメント（コーディング規約のカスタマイズ、社内フレームワーク拡張の仕様等）をMCPサーバーの知識ベースに追加することを想定している。

#### 想定される手順

```bash
# 1. カスタムドキュメントを所定のディレクトリに配置
cp -r /path/to/company-docs/ nablarch-mcp-server/custom-resources/

# 2. インデックス更新（想定コマンド）
java -jar nablarch-mcp-server-x.x.x.jar --reindex --source=custom-resources/

# 3. サーバー再起動
java -jar nablarch-mcp-server-x.x.x.jar
```

#### 対応予定のドキュメント形式

| 形式 | 対応状況 |
|------|---------|
| Markdown (`.md`) | 対応予定 |
| HTML | 対応予定 |
| Javadoc (HTML) | 対応予定 |
| PDF | 将来検討 |
| XML（コンポーネント定義） | 対応予定 |
| Java ソースコード | 対応予定 |

### 4.2 プロンプトテンプレートのカスタマイズ

MCPの `Prompts` プリミティブを使って、プロジェクト固有のプロンプトテンプレートを追加することを想定している。

#### カスタムプロンプトの定義例（想定）

```yaml
# custom-prompts/create-company-batch.yaml
name: create-company-batch
description: "社内標準のバッチアプリケーション作成ガイド"
parameters:
  - name: batch_type
    description: "バッチ種別（都度起動/常駐/遅延）"
    required: true
  - name: input_source
    description: "入力ソース（CSV/DB/MQ）"
    required: true
  - name: output_target
    description: "出力先（DB/CSV/MQ）"
    required: true
template: |
  # 社内バッチアプリケーション設計ガイド

  ## 前提
  - バッチ種別: {{batch_type}}
  - 入力: {{input_source}}
  - 出力: {{output_target}}

  ## 社内標準に基づく設計手順
  1. ハンドラキュー構成（社内標準テンプレート使用）
  2. バッチアクション設計
  3. テストデータ設計（社内Excel形式）
  ...
```

### 4.3 RAGインデックスの更新方法

Nablarchの新バージョンリリース時やドキュメント更新時に、知識ベースのインデックスを更新する想定。

#### 手動更新（想定）

```bash
# 全体再インデックス
java -jar nablarch-mcp-server-x.x.x.jar --reindex

# 差分更新（変更されたドキュメントのみ）
java -jar nablarch-mcp-server-x.x.x.jar --reindex --incremental

# 特定データソースのみ更新
java -jar nablarch-mcp-server-x.x.x.jar --reindex --source=official-docs
java -jar nablarch-mcp-server-x.x.x.jar --reindex --source=github-repos
java -jar nablarch-mcp-server-x.x.x.jar --reindex --source=javadoc
```

#### 自動更新（将来対応予定）

GitHub Webhookと連携し、Nablarchリポジトリの更新を検知して自動的にインデックスを更新するパイプラインを想定している。

```
GitHub (nablarch org)
    │ Webhook
    ▼
Update Trigger
    │
    ▼
Re-indexing Pipeline → ベクトルDB更新 → MCPサーバー反映
```

---

## 5. FAQ

### 接続・設定に関する質問

#### Q1. MCPサーバーに接続できない

**想定される原因と対処法:**

| 原因 | 対処法 |
|------|--------|
| Javaがインストールされていない | `java -version` で確認。Java 17以上をインストール |
| JARファイルのパスが間違っている | 設定ファイル内のパスを絶対パスで正確に指定 |
| ポートが他のプロセスで使用中 | `lsof -i :8080` で確認し、ポートを変更 |
| AIツールがMCPに未対応 | AIツールのバージョンを確認し、MCP対応版にアップデート |

#### Q2. 複数のAIツールから同時に接続できるか？

STDIOモードでは、AIツールごとに個別のMCPサーバープロセスが起動される。リソース使用量は増えるが、設定の競合は発生しない。将来のHTTPモードでは、単一サーバーに複数クライアントから接続可能になる想定。

#### Q3. オフライン環境でも使えるか？

MCPサーバー自体はローカルで動作するため、インターネット接続は不要。ただし、AIツール側（Claude Desktop等）がクラウドのLLMを使用するため、AIツールの利用にはインターネット接続が必要。

### 機能に関する質問

#### Q4. Nablarchのどのバージョンに対応しているか？

初期リリースではNablarch 6（6u2、6u3）の知識を中心に提供する想定。Nablarch 5の情報も一部含まれるが、5→6のマイグレーション支援として位置づける。

#### Q5. 生成されたコードはそのまま本番利用できるか？

MCPサーバーが提供するのはAIへのコンテキスト情報であり、コードを生成するのはAIモデル側である。生成コードは参考として活用し、必ず人間によるレビューとテストを実施すること。特にセキュリティ関連のコードは慎重な検証が必要。

#### Q6. 社内独自のNablarch拡張にも対応できるか？

カスタムリソース機能（[4.1節](#41-カスタムリソースの追加社内ドキュメントの取り込み)）により、社内固有のドキュメント・コード規約をMCPサーバーに追加することを想定している。これにより、社内標準に合わせたコード生成・レビューが可能になる。

#### Q7. 応答が遅い場合はどうすればよいか？

**想定される原因と対処法:**

| 原因 | 対処法 |
|------|--------|
| RAG検索（セマンティック検索）の処理時間 | インデックスの最適化、キャッシュの有効化 |
| JVM起動のコールドスタート | JVMオプション（`-XX:+TieredCompilation`等）の調整 |
| 大量のドキュメントインデックス | 検索対象の絞り込み、Top-K数の調整 |
| ネットワーク遅延（HTTPモード） | ローカルモード（STDIO）への切り替え |

#### Q8. ハンドラキューの順序制約はどこまでチェックできるか？

`validate_handler_queue` ツールは、Nablarch公式ドキュメントに記載されているハンドラ間の順序制約（例: `DbConnectionManagementHandler` は `TransactionManagementHandler` より前に配置する必要がある等）をチェックする想定。RAGが順序制約の知識を提供し、ツールが構成XMLに対してルールを適用する。

### 運用に関する質問

#### Q9. Nablarchの新バージョンがリリースされたらどうなるか？

RAGインデックスの更新（[4.3節](#43-ragインデックスの更新方法)）により、新バージョンの情報を知識ベースに反映する。将来的にはGitHub Webhookによる自動更新パイプラインの提供を想定している。手動更新の場合は `--reindex` コマンドで対応。

#### Q10. セキュリティ上の懸念はあるか？

| 懸念 | 対策 |
|------|------|
| MCPサーバーが外部に公開される | STDIOモード（ローカル）を推奨。HTTPモードではlocalhost限定に設定 |
| 社内コードがMCPサーバー経由で漏洩 | MCPサーバー自体はローカル実行。コードはAIツール側のプライバシーポリシーに準拠 |
| プロンプトインジェクション | ツール入力のバリデーション、サンドボックス実行を想定 |
| 不正なツール実行 | 将来のHTTPモードではOAuth 2.0認証を想定 |

#### Q11. ライセンスはどうなっているか？

本プロジェクトはApache License 2.0でOSS公開する想定。Nablarch自体もApache License 2.0であり、ライセンスの互換性に問題はない。MCP Java SDKもMITライセンスで提供されている。

#### Q12. Spring Bootを使っているがNablarch専用か？

MCPサーバーの内部フレームワークにSpring Bootを使用しているが、提供するのはNablarch固有の知識とツールである。Spring Bootはあくまでサーバー基盤として採用しており、利用者がSpringの知識を持つ必要はない。

#### Q13. コントリビューションは受け付けているか？

将来的にOSSコミュニティへの公開を予定しており、コントリビューションガイドラインを整備する想定。特に以下の領域での貢献を歓迎する。

- Nablarch知識ベースの拡充（ドキュメント・コード例の追加）
- プロンプトテンプレートの追加
- バグ報告・機能要望
- ドキュメントの翻訳（英語化）

#### Q14. MCP以外のプロトコルには対応するか？

現時点ではMCPに集中する方針。MCPはAnthropicが策定しLinux Foundationに移管された標準プロトコルであり、Claude、Copilot、Cursor等の主要AIツールが対応済みまたは対応予定である。MCP普及の遅延リスクに備え、将来的にマルチプロトコル対応を検討する可能性はある。

---

*本ユーザーガイドは計画段階の想定を記載したものであり、実装の進捗に伴い内容が変更される可能性がある。コマンド・設定例は将来の提供を想定したものであり、現時点では動作しない。*

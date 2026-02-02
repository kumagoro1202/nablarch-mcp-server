# MCP Inspector 統合テスト結果

## テスト環境

| 項目 | 値 |
|------|-----|
| サーバー | nablarch-mcp-server 0.1.0 |
| Java | JDK 17 |
| トランスポート | STDIO（FIFOパイプ経由） |
| テスト方式 | JSON-RPCリクエスト→FIFOパイプ→サーバー→レスポンス検証 |
| テスト日時 | 2026-02-02 |
| テスト結果ファイル | /tmp/mcp-all-output.jsonl |

## テスト方式

FIFOパイプ（名前付きパイプ）を使用したSTDIO JSON-RPCテスト:

```bash
# FIFOパイプ作成
mkfifo /tmp/mcp-input

# サーバー起動（パイプからの入力を受け付ける）
java -jar target/nablarch-mcp-server-0.1.0-SNAPSHOT.jar \
  < /tmp/mcp-input > /tmp/mcp-all-output.jsonl 2>/dev/null &

# JSON-RPCリクエストをパイプに送信
echo '{"jsonrpc":"2.0","id":1,"method":"initialize",...}' > /tmp/mcp-input
```

## テスト結果サマリ

| 結果 | 件数 |
|------|------|
| 成功 | **15** |
| 失敗 | **0** |
| 合計 | **15** |

**判定: 全テスト成功**

## テスト一覧

| # | カテゴリ | テスト内容 | 入力 | 期待結果 | 判定 |
|---|---------|----------|------|---------|------|
| 1 | Protocol | initialize | — | サーバー情報・capabilities | OK |
| 2 | Tools | tools/list | — | 2ツール一覧 | OK |
| 3 | Tools | search_api (正常) | keyword="UniversalDao" | 検索結果5件 | OK |
| 4 | Tools | search_api (該当なし) | keyword="nonexistent_xyz_12345" | 検索結果なし | OK |
| 5 | Tools | validate_handler_queue | web, XML(3ハンドラ) | 検証NG (7件エラー) | OK |
| 6 | Resources | resources/list | — | 12リソース一覧 | OK |
| 7 | Resources | resources/read handler/web | URI | Webハンドラ15件Markdown | OK |
| 8 | Resources | resources/read guide/setup | URI | セットアップガイドMarkdown | OK |
| 9 | Prompts | prompts/list | — | 6プロンプト一覧 | OK |
| 10 | Prompts | setup-handler-queue | app_type=web | ハンドラキュー構成ガイド | OK |
| 11 | Prompts | explain-handler | handler_name=HttpCharacterEncodingHandler | ハンドラ詳細説明 | OK |
| 12 | Prompts | create-action | app_type=web, action_name=UserRegistrationAction | アクション生成ガイド | OK |
| 13 | Prompts | review-config | config_xml=GlobalErrorHandler XML | XML設定レビュー | OK |
| 14 | Prompts | migration-guide | from_version=5, to_version=6 | 移行ガイド | OK |
| 15 | Prompts | best-practices | topic=handler-queue | ベストプラクティス | OK |

## テスト詳細

### Test #1: initialize

**リクエスト:**
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{...}}
```

**レスポンス要約:**
- protocolVersion: `2024-11-05`
- serverInfo.name: `nablarch-mcp-server`
- serverInfo.version: `0.1.0`
- capabilities: tools, resources, prompts, logging, completions

**判定:** サーバー名・バージョン・capabilities全て正常。MCP Protocol v2024-11-05準拠。

---

### Test #2: tools/list

**リクエスト:**
```json
{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}
```

**レスポンス要約:**
- 2ツール返却:
  1. `validateHandlerQueue` — ハンドラキューXML設定の検証
  2. `searchApi` — Nablarch APIドキュメント検索
- 各ツールにinputSchema（JSON Schema）が付与

**判定:** 2ツール正常返却。スキーマ定義を含む。

---

### Test #3: search_api (正常系)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"searchApi","arguments":{"keyword":"UniversalDao","category":""}}}
```

**レスポンス要約:**
- 検索結果: 5件
  - [APIパターン] universal-dao (library)
  - [APIパターン] sql-file (library)
  - [APIパターン] exclusive-control (library)
  - [モジュール] nablarch-common-dao (library)
  - [エラー] ERR-005 (database/error)
- isError: false

**判定:** キーワード「UniversalDao」で5件の横断検索結果を正常返却。APIパターン・モジュール・エラーの各カテゴリから結果あり。

---

### Test #4: search_api (該当なし)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"searchApi","arguments":{"keyword":"nonexistent_xyz_12345","category":""}}}
```

**レスポンス要約:**
- `検索結果なし: nonexistent_xyz_12345`
- isError: false

**判定:** 存在しないキーワードで「検索結果なし」メッセージを正常返却。isErrorはfalse（アプリレベルの正常応答）。

---

### Test #5: validate_handler_queue

**リクエスト:**
```json
{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"validateHandlerQueue","arguments":{"handlerQueueXml":"<component-configuration>...</component-configuration>","applicationType":"web"}}}
```

入力XML: HttpCharacterEncodingHandler, ThreadContextHandler, GlobalErrorHandler の3ハンドラのみ。

**レスポンス要約:**
- 検証結果: **NG**
- ハンドラ数: 3
- エラー 7件:
  - 必須ハンドラ不足: SecureHandler, SessionStoreHandler, ThreadContextHandler系, DbConnectionManagementHandler, TransactionManagementHandler, PackageMapping

**判定:** 不完全なハンドラキューに対して7件の必須ハンドラ不足を正確に検出。検証ロジックが正常動作。

---

### Test #6: resources/list

**リクエスト:**
```json
{"jsonrpc":"2.0","id":6,"method":"resources/list","params":{}}
```

**レスポンス要約:**
- 12リソース返却:
  - handler: web, rest, batch, messaging, http-messaging, jakarta-batch (6種)
  - guide: setup, testing, validation, database, handler-queue, error-handling (6種)
- 各リソースにuri, name, description, mimeType("text/markdown")が付与

**判定:** 12リソース全て正常返却。URI形式`nablarch://handler/*`、`nablarch://guide/*`。

---

### Test #7: resources/read handler/web

**リクエスト:**
```json
{"jsonrpc":"2.0","id":7,"method":"resources/read","params":{"uri":"nablarch://handler/web"}}
```

**レスポンス要約:**
- Markdown形式で返却（text/markdown）
- 内容:
  - タイトル: "Nablarch Web Application Handler Queue"
  - Handler Queue: 15ハンドラ（順序番号付き、FQCN・スレッド・説明・制約情報）
  - Ordering Constraints Summary: テーブル形式で順序制約一覧
  - Source: handler-catalog.yaml, handler-constraints.yaml

**判定:** Webアプリの15ハンドラ全てをMarkdown形式で正常返却。FQCN・制約情報を含む。

---

### Test #8: resources/read guide/setup

**リクエスト:**
```json
{"jsonrpc":"2.0","id":8,"method":"resources/read","params":{"uri":"nablarch://guide/setup"}}
```

**レスポンス要約:**
- Markdown形式で返却
- 内容:
  - タイトル: "Nablarch Setup Guide"
  - Configuration Templates: web-xml, web-component, db-connection
  - 各テンプレートのXMLコード例を含む
  - Source: config-templates.yaml

**判定:** セットアップガイドをMarkdown形式で正常返却。XMLテンプレート3件を含む。

---

### Test #9: prompts/list

**リクエスト:**
```json
{"jsonrpc":"2.0","id":9,"method":"prompts/list","params":{}}
```

**レスポンス要約:**
- 6プロンプト返却:
  1. `best-practices` — topic (必須)
  2. `review-config` — config_xml (必須)
  3. `migration-guide` — from_version (必須), to_version (必須)
  4. `explain-handler` — handler_name (必須)
  5. `create-action` — app_type (必須), action_name (必須)
  6. `setup-handler-queue` — app_type (必須)

**判定:** 6プロンプト全て正常返却。引数定義（名前、説明、必須フラグ）を含む。

---

### Test #10: prompts/get setup-handler-queue (web)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":10,"method":"prompts/get","params":{"name":"setup-handler-queue","arguments":{"app_type":"web"}}}
```

**レスポンス要約:**
- description: "web アプリケーションのハンドラキュー構成ガイド"
- messages: 1メッセージ (role: user)
- 内容:
  - 推奨ハンドラキューテーブル（15ハンドラ、順序・FQCN・必須区分・スレッド）
  - 順序制約セクション（8制約の詳細）
  - XML設定テンプレート（web-xml, web-component, web-handler-queue, session-store）

**判定:** Webアプリのハンドラキュー構成ガイドを正常生成。ハンドラ一覧・制約・テンプレートの全セクションを含む。

---

### Test #11: prompts/get explain-handler (HttpCharacterEncodingHandler)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":11,"method":"prompts/get","params":{"name":"explain-handler","arguments":{"handler_name":"HttpCharacterEncodingHandler"}}}
```

**レスポンス要約:**
- description: "HttpCharacterEncodingHandler の詳細説明"
- 内容:
  - 基本情報テーブル（FQCN、説明、スレッド、必須、推奨順序）
  - 使用アプリタイプ: web
  - ハンドラ内制約: GlobalErrorHandlerの前に配置
  - 順序制約: must_be_outer

**判定:** ハンドラの詳細情報を正常返却。基本情報・制約・アプリタイプ情報を含む。

---

### Test #12: prompts/get create-action (web, UserRegistrationAction)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":12,"method":"prompts/get","params":{"name":"create-action","arguments":{"app_type":"web","action_name":"UserRegistrationAction"}}}
```

**レスポンス要約:**
- description: "UserRegistrationAction アクションクラス生成ガイド (web)"
- 内容:
  - 推奨パターン: action-class, inject-form-on-error, http-response-navigation, session-store-access, double-submit-prevention, file-download
  - 各パターンのコード例（FQCN付き）
  - 命名規則（doプレフィックス等）

**判定:** Webアプリ向けアクションクラス生成ガイドを正常返却。6パターンのコード例を含む。

---

### Test #13: prompts/get review-config

**リクエスト:**
```json
{"jsonrpc":"2.0","id":13,"method":"prompts/get","params":{"name":"review-config","arguments":{"config_xml":"<component class=\"nablarch.fw.handler.GlobalErrorHandler\"/>"}}}
```

**レスポンス要約:**
- description: "Nablarch XML設定レビュー"
- 内容:
  - レビュー対象XML表示
  - ハンドラ順序制約チェックリスト（24制約）
  - よくある問題パターン（ERR-001, ERR-002, ERR-003, ERR-010, ERR-011, ERR-012）
  - 一般的な確認事項（5項目）

**判定:** XML設定レビューガイドを正常返却。全24順序制約と6エラーパターンを含む。

---

### Test #14: prompts/get migration-guide (5→6)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":14,"method":"prompts/get","params":{"name":"migration-guide","arguments":{"from_version":"5","to_version":"6"}}}
```

**レスポンス要約:**
- description: "Nablarch 5 → 6 移行ガイド"
- 内容:
  - モジュール一覧テーブル（21モジュール: core 5, web 3, batch 2, messaging 2, library 5, testing 1, integration 3）
  - 各モジュールの主要クラス確認事項
  - 依存関係の確認リスト
  - 一般的な移行手順（7ステップ）

**判定:** バージョン移行ガイドを正常返却。21モジュール・主要クラス・依存関係・移行手順を含む。

---

### Test #15: prompts/get best-practices (handler-queue)

**リクエスト:**
```json
{"jsonrpc":"2.0","id":15,"method":"prompts/get","params":{"name":"best-practices","arguments":{"topic":"handler-queue"}}}
```

**レスポンス要約:**
- description: "Nablarch ベストプラクティス: handler-queue"
- 内容:
  - 設計パターン: handler-queue-pattern（パイプラインモデル、構造図、コード例）
  - 設計パターン: system-repository-pattern（DIコンテナ、XML設定、コード例）
  - 一般的な注意事項

**判定:** ハンドラキューのベストプラクティスを正常返却。2設計パターンのコード例・構造図を含む。

---

## カバレッジ分析

### MCP Primitive カバレッジ

| Primitive | メソッド | テスト数 | カバー率 |
|-----------|---------|---------|---------|
| Protocol | initialize | 1 | 100% |
| Tools | tools/list | 1 | 100% |
| Tools | tools/call (search_api) | 2 | 100% (正常+該当なし) |
| Tools | tools/call (validate_handler_queue) | 1 | 100% |
| Resources | resources/list | 1 | 100% |
| Resources | resources/read (handler) | 1 | 16.7% (1/6タイプ) |
| Resources | resources/read (guide) | 1 | 16.7% (1/6トピック) |
| Prompts | prompts/list | 1 | 100% |
| Prompts | prompts/get | 6 | 100% (6/6プロンプト) |

### 未テスト項目

- resources/read: rest, batch, messaging, http-messaging, jakarta-batch (handler)
- resources/read: testing, validation, database, handler-queue, error-handling (guide)
- search_api: カテゴリフィルタ付き検索
- validate_handler_queue: 正常なハンドラキュー（検証OK）ケース
- エラー系: 不正なリソースURI、不正なプロンプト名

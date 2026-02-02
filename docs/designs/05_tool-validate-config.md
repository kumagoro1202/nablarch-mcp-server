# WBS 1.1.5: validate_config (ValidateHandlerQueue) Tool API設計

> **作成日**: 2026-02-02
> **作成者**: ashigaru2 (subtask_046)

## 概要

NablarchハンドラキューXML設定を検証するMCPツール。ハンドラの順序制約・必須ハンドラの有無・互換性をチェックする。

## API仕様

### 入力パラメータ

| パラメータ | 型 | 必須 | 説明 |
|-----------|------|------|------|
| handlerQueueXml | string | ✅ | ハンドラキューXML設定（handler要素のclass属性からハンドラ名を抽出） |
| applicationType | string | ✅ | アプリケーションタイプ: web, rest, batch, messaging |

### 出力

検証結果のフォーマット済みテキスト（Markdown形式）:

```
## 検証結果: OK / NG
アプリタイプ: web
ハンドラ数: 12

### エラー (2件)
- ❌ 必須ハンドラが不足: GlobalErrorHandler (未処理の例外をキャッチ...)
- ❌ 順序違反: TransactionManagementHandler (位置3) は DbConnectionManagementHandler (位置5) より後に配置すべき

### 警告 (1件)
- ⚠ 互換性警告: HandlerA と HandlerB は同時使用非推奨
```

### エラー応答

| 条件 | 応答 |
|------|------|
| applicationType が不明 | "エラー: 不明なアプリケーションタイプ" + 有効なタイプ一覧 |
| XML解析失敗 | "XMLの解析に失敗しました" + エラー詳細 |

## 検証ルール

### 1. 必須ハンドラチェック
- handler-catalog.yaml でrequired=trueのハンドラが含まれているか

### 2. 順序制約チェック（2つのソース）
- **handler-constraints.yaml**: must_before / must_after ルール
- **handler-catalog.yaml**: 各ハンドラのconstraints.must_before / must_after

### 3. 互換性チェック
- handler-constraints.yaml の incompatible_with ルール

## XML解析仕様

入力XMLからハンドラクラス名を抽出する。対応する形式:

```xml
<!-- 形式1: component要素のclass属性 -->
<component class="nablarch.fw.handler.GlobalErrorHandler"/>

<!-- 形式2: handler要素のclass属性 -->
<handler class="nablarch.fw.handler.GlobalErrorHandler"/>

<!-- クラス名の単純名部分を使用（FQCN末尾のクラス名） -->
```

FQCN末尾の単純クラス名でハンドラを特定する。

## 実装クラス

- **ValidateHandlerQueueTool.java** (`@Tool` アノテーション付き)
- XML文字列からハンドラ名リストを抽出
- `NablarchKnowledgeBase.validateHandlerQueue(appType, handlerNames)` を呼び出し

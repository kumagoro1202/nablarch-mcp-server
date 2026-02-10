---
description: Nablarchのエラーハンドリング・例外設計ガイド。業務例外（ApplicationException）とシステム例外の使い分け、GlobalErrorHandlerの動作、HttpErrorHandler/HttpErrorResponseの設定、バリデーションエラー処理、よくあるエラーの原因と対処法を提供する。「Nablarchのエラーハンドリング」「例外処理の設計」「エラーメッセージ」「ApplicationExceptionの使い方」といった質問で使用。
globs:
  - "**/*Error*"
  - "**/*Exception*"
  - "**/*error*"
---

# Nablarch エラーハンドリング・例外設計ガイド

## 例外の分類

Nablarchでは例外を以下の2種類に分類する。

| 分類 | クラス | 対処 |
|------|--------|------|
| **業務例外** | `nablarch.core.message.ApplicationException` | ユーザーにエラーメッセージを表示して再操作を促す |
| **システム例外** | `RuntimeException`系 | ログ出力してシステムエラー画面を表示 |

## ハンドラによるエラー処理の多層構造

```
[GlobalErrorHandler] ← 最外殻: 全未処理例外をキャッチ
  [HttpResponseHandler]
    [HttpErrorHandler] ← Web: HTTPステータスコード別エラーページ遷移
      [DbConnectionManagementHandler]
        [TransactionManagementHandler] ← 例外時rollback
          [@OnError] ← Action: バリデーションエラー等の業務例外
            [Action] ← ビジネスロジック
```

## 1. GlobalErrorHandler

全未処理例外の最終キャッチポイント。ハンドラキューの最外殻に配置。

```java
// FQCN: nablarch.fw.handler.GlobalErrorHandler
```

- 全アプリケーションタイプ（Web/REST/バッチ/メッセージング）で必須
- キャッチした例外はFATALログに出力
- Web: 500エラーレスポンスを返却
- バッチ: プロセス終了コードを異常終了に設定

## 2. HttpErrorHandler（Web専用）

HTTPステータスコード別のエラーページ遷移を制御。

```java
// FQCN: nablarch.fw.web.handler.HttpErrorHandler
```

```xml
<component class="nablarch.fw.web.handler.HttpErrorHandler">
  <property name="defaultPages">
    <map>
      <entry key="4.." value="/WEB-INF/view/common/errorPages/4xx.jsp" />
      <entry key="403" value="/WEB-INF/view/common/errorPages/403.jsp" />
      <entry key="404" value="/WEB-INF/view/common/errorPages/404.jsp" />
      <entry key="5.." value="/WEB-INF/view/common/errorPages/500.jsp" />
    </map>
  </property>
</component>
```

## 3. JaxRsResponseHandler（REST専用）

REST APIのエラーレスポンスをJSON形式で返却。

```java
// FQCN: nablarch.fw.jaxrs.JaxRsResponseHandler
// FQCN: nablarch.fw.jaxrs.ErrorResponseBuilder
```

```xml
<component class="nablarch.fw.jaxrs.JaxRsResponseHandler">
  <property name="errorResponseBuilder">
    <component class="nablarch.fw.jaxrs.ErrorResponseBuilder" />
  </property>
</component>
```

## 4. 業務例外（ApplicationException）

### 生成と送出

```java
// FQCN: nablarch.core.message.ApplicationException
// FQCN: nablarch.core.message.MessageUtil
// FQCN: nablarch.core.message.MessageLevel

// 単一メッセージ
throw new ApplicationException(
    MessageUtil.createMessage(MessageLevel.ERROR, "errors.notFound"));

// 複数メッセージ
ApplicationException exception = new ApplicationException();
exception.addMessages(MessageUtil.createMessage(MessageLevel.ERROR, "errors.field1"));
exception.addMessages(MessageUtil.createMessage(MessageLevel.ERROR, "errors.field2"));
throw exception;
```

### @OnErrorによるエラー遷移

```java
// FQCN: nablarch.fw.web.interceptor.OnError

@InjectForm(form = UserForm.class, prefix = "form")
@OnError(type = ApplicationException.class,
         path = "/WEB-INF/view/user/input.jsp")
public HttpResponse doRegister(HttpRequest request, ExecutionContext context) {
    // ApplicationException発生時、自動的にinput.jspに遷移
    // フォーム入力値とエラーメッセージが保持される
}
```

### JSPでのエラーメッセージ表示

```jsp
<%-- 全エラーメッセージの表示 --%>
<n:errors filter="all" />

<%-- 特定フィールドのエラーメッセージ --%>
<n:error name="form.userName" />
```

## 5. HttpErrorResponse

プログラムで明示的にHTTPエラーを返す場合に使用。

```java
// FQCN: nablarch.fw.web.HttpErrorResponse

// 404エラー
throw new HttpErrorResponse(404);

// 403エラー（メッセージ付き）
throw new HttpErrorResponse(403, "/WEB-INF/view/common/errorPages/403.jsp");
```

## 6. トランザクション管理と例外

`TransactionManagementHandler`は例外の種類に応じて自動的にcommit/rollbackを制御。

```java
// FQCN: nablarch.common.handler.TransactionManagementHandler
```

| 状況 | トランザクション |
|------|-----------------|
| 正常完了 | `commit` |
| `ApplicationException` | `rollback` |
| `RuntimeException` | `rollback` |
| `Error` | `rollback` |

## 7. 楽観ロック例外

```java
// FQCN: jakarta.persistence.OptimisticLockException
// UniversalDao.updateで@Version不一致時に発生

try {
    UniversalDao.update(user);
} catch (jakarta.persistence.OptimisticLockException e) {
    throw new ApplicationException(
        MessageUtil.createMessage(MessageLevel.ERROR, "errors.optimisticLock"));
}
```

## 8. NoDataException

```java
// FQCN: nablarch.common.dao.NoDataException
// UniversalDao.findByIdでレコードが存在しない場合に発生

try {
    User user = UniversalDao.findById(User.class, userId);
} catch (NoDataException e) {
    throw new HttpErrorResponse(404);
}
```

## よくあるエラーと対処法

| エラー | 原因 | 対処 |
|--------|------|------|
| `ClassCastException: Cannot cast handler result` | ハンドラキューの順序不正 | handler-catalog参照で正しい順序に |
| `NoMoreHandlerException: handler queue is empty` | ルーティングハンドラ未設定 | `RoutesMapping`または`RequestPathJavaPackageMapping`を追加 |
| `SQLException: Connection is not available` | `DbConnectionManagementHandler`未設定 | ハンドラキューに追加 |
| `ComponentCreationException` | コンポーネント定義XMLの構文エラー | XML構文、FQCN、プロパティ名を確認 |
| `Component not found: [name]` | `SystemRepository.get`で未定義名を指定 | コンポーネント定義XMLのname属性確認 |
| `IllegalStateException: ThreadContext has not been set` | `ThreadContextHandler`未設定 | ハンドラキューに追加 |
| `DuplicateProcessException` | 同一バッチが既に実行中 | プロセス管理テーブルを確認 |
| `IllegalArgumentException: charset definition not found` | `@SystemChar`の文字種定義が未登録 | charsetDefMapの設定確認 |

## アンチパターン

### 例外の握りつぶし（絶対禁止）

```java
// NG: 例外を握りつぶす
try {
    UniversalDao.insert(user);
} catch (Exception e) {
    // 何もしない ← 原因究明が不可能になる
}

// OK: 想定内の例外は業務例外に変換
try {
    UniversalDao.insert(user);
} catch (DuplicateStatementException e) {
    throw new ApplicationException(
        MessageUtil.createMessage(MessageLevel.ERROR, "errors.duplicate"));
}
```

### メッセージ定義ファイル

```properties
# messages.properties（src/main/resources配下）
errors.notFound=対象のデータが見つかりません
errors.optimisticLock=他のユーザーによりデータが更新されています。再度操作してください
errors.duplicate=既に登録済みのデータです
errors.doubleSubmit=二重送信が検知されました
```

## 主要FQCN一覧

| クラス | FQCN |
|--------|------|
| ApplicationException | `nablarch.core.message.ApplicationException` |
| MessageUtil | `nablarch.core.message.MessageUtil` |
| MessageLevel | `nablarch.core.message.MessageLevel` |
| HttpErrorResponse | `nablarch.fw.web.HttpErrorResponse` |
| GlobalErrorHandler | `nablarch.fw.handler.GlobalErrorHandler` |
| HttpErrorHandler | `nablarch.fw.web.handler.HttpErrorHandler` |
| JaxRsResponseHandler | `nablarch.fw.jaxrs.JaxRsResponseHandler` |
| ErrorResponseBuilder | `nablarch.fw.jaxrs.ErrorResponseBuilder` |
| NoDataException | `nablarch.common.dao.NoDataException` |
| OptimisticLockException | `jakarta.persistence.OptimisticLockException` |

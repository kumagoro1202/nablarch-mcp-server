---
description: Nablarchフレームワークのハンドラキュー設計ガイド。Web/REST/バッチ/メッセージングの基本パターン、ハンドラの選択基準・順序制約・XML設定例を提供する。「Nablarchのハンドラキューを設計したい」「ハンドラの順序を知りたい」「バッチのハンドラキューを構成したい」といった質問で使用。
globs:
  - "**/*handler*"
  - "**/*-boot.xml"
  - "**/component-configuration*.xml"
---

# Nablarch ハンドラキュー設計ガイド

## 概要

Nablarchの根幹アーキテクチャである**ハンドラキュー**の設計ガイド。リクエスト処理を複数のハンドラによるパイプラインで実行する仕組み。

```
Request → [Handler1] → [Handler2] → ... → [HandlerN] → Action
                                                          ↓
Response ← [Handler1] ← [Handler2] ← ... ← [HandlerN] ← Result
```

## ハンドラインターフェース

```java
// 全ハンドラの基底インターフェース
// FQCN: nablarch.fw.Handler
public interface Handler<I, O> {
    O handle(I input, ExecutionContext context);
}
```

後続ハンドラの呼び出しは `context.handleNext(input)` で行う。

## アプリケーション別 基本パターン

### 1. Webアプリケーション

```xml
<component name="webFrontController"
           class="nablarch.fw.web.servlet.WebFrontController">
  <property name="handlerQueue">
    <list>
      <!-- 1. 文字エンコーディング設定 -->
      <component class="nablarch.fw.web.handler.HttpCharacterEncodingHandler">
        <property name="defaultEncoding" value="UTF-8" />
      </component>
      <!-- 2. グローバルエラーハンドラ（全例外の最終キャッチ） -->
      <component class="nablarch.fw.handler.GlobalErrorHandler" />
      <!-- 3. HTTPレスポンス構築 -->
      <component class="nablarch.fw.web.handler.HttpResponseHandler" />
      <!-- 4. セキュリティヘッダー（CSP, X-Frame-Options等） -->
      <component class="nablarch.fw.web.handler.SecureHandler" />
      <!-- 5. アクセスログ（任意） -->
      <component class="nablarch.common.web.handler.HttpAccessLogHandler" />
      <!-- 6. マルチパート処理（ファイルアップロード時） -->
      <component class="nablarch.fw.web.upload.MultipartHandler" />
      <!-- 7. セッション管理 -->
      <component-ref name="sessionStoreHandler" />
      <!-- 8. パラメータ正規化（任意） -->
      <component class="nablarch.fw.web.handler.NormalizationHandler" />
      <!-- 9. スレッドコンテキスト設定 -->
      <component-ref name="threadContextHandler" />
      <!-- 10. スレッドコンテキストクリア -->
      <component-ref name="threadContextClearHandler" />
      <!-- 11. HTTPエラーハンドリング（任意） -->
      <component class="nablarch.fw.web.handler.HttpErrorHandler" />
      <!-- 12. DB接続管理 -->
      <component-ref name="dbConnectionManagementHandler" />
      <!-- 13. トランザクション管理 -->
      <component-ref name="transactionManagementHandler" />
      <!-- 14. ルーティング（最内殻） -->
      <component-ref name="packageMapping" />
    </list>
  </property>
</component>
```

### 2. RESTful Webサービス

```xml
<component name="webFrontController"
           class="nablarch.fw.web.servlet.WebFrontController">
  <property name="handlerQueue">
    <list>
      <!-- 1. グローバルエラーハンドラ -->
      <component class="nablarch.fw.handler.GlobalErrorHandler" />
      <!-- 2. JAX-RSレスポンス変換 -->
      <component class="nablarch.fw.jaxrs.JaxRsResponseHandler">
        <property name="errorResponseBuilder">
          <component class="nablarch.fw.jaxrs.ErrorResponseBuilder" />
        </property>
      </component>
      <!-- 3. ステータスコード変換 -->
      <component class="nablarch.fw.handler.StatusCodeConvertHandler" />
      <!-- 4. アクセスログ（任意） -->
      <component class="nablarch.common.web.handler.HttpAccessLogHandler" />
      <!-- 5. スレッドコンテキスト設定 -->
      <component-ref name="threadContextHandler" />
      <!-- 6. スレッドコンテキストクリア -->
      <component-ref name="threadContextClearHandler" />
      <!-- 7. DB接続管理 -->
      <component-ref name="dbConnectionManagementHandler" />
      <!-- 8. トランザクション管理 -->
      <component-ref name="transactionManagementHandler" />
      <!-- 9. ルーティング（最内殻） -->
      <component-ref name="packageMapping" />
    </list>
  </property>
</component>
```

### 3. バッチアプリケーション（都度起動型）

メインスレッドとサブスレッドの2層構造。

```xml
<!-- メインスレッド -->
<list name="handlerQueue">
  <component class="nablarch.fw.handler.StatusCodeConvertHandler" />
  <component class="nablarch.fw.handler.GlobalErrorHandler" />
  <component class="nablarch.fw.handler.DuplicateProcessCheckHandler" />
  <component-ref name="dbConnectionManagementHandler" />
  <component-ref name="transactionManagementHandler" />
  <component class="nablarch.fw.handler.RequestPathJavaPackageMapping">
    <property name="basePackage" value="com.example.batch.action" />
  </component>
  <component-ref name="multiThreadProcessHandler" />
</list>

<!-- サブスレッド（MultiThreadExecutionHandler内） -->
<component name="multiThreadProcessHandler"
           class="nablarch.fw.handler.MultiThreadExecutionHandler">
  <property name="concurrentNumber" value="${batch.thread.count}" />
  <property name="handlerQueue">
    <list>
      <component class="nablarch.fw.handler.RetryHandler" />
      <component-ref name="dbConnectionManagementHandler" />
      <component class="nablarch.fw.handler.LoopHandler">
        <property name="commitInterval" value="${batch.commit.interval}" />
      </component>
      <component-ref name="transactionManagementHandler" />
      <component class="nablarch.fw.handler.DataReadHandler" />
    </list>
  </property>
</component>
```

### 4. テーブルキューイング型メッセージング

```xml
<list name="handlerQueue">
  <component class="nablarch.fw.handler.StatusCodeConvertHandler" />
  <component class="nablarch.fw.handler.GlobalErrorHandler" />
  <component class="nablarch.fw.handler.DuplicateProcessCheckHandler" />
  <component class="nablarch.fw.handler.ProcessStopHandler" />
  <component-ref name="dbConnectionManagementHandler" />
  <component-ref name="transactionManagementHandler" />
  <component class="nablarch.fw.handler.RequestThreadLoopHandler" />
  <component class="nablarch.fw.handler.MultiThreadExecutionHandler" />
  <!-- サブスレッド -->
  <component class="nablarch.fw.handler.RetryHandler" />
  <component-ref name="dbConnectionManagementHandler" />
  <component class="nablarch.fw.handler.LoopHandler" />
  <component-ref name="transactionManagementHandler" />
  <component class="nablarch.fw.handler.DataReadHandler" />
</list>
```

## ハンドラ順序制約（厳守）

| 制約 | 理由 |
|------|------|
| `DbConnectionManagementHandler` → `TransactionManagementHandler` | DB接続確立後にトランザクション開始 |
| `GlobalErrorHandler` は外殻に配置 | 未処理例外の最終キャッチ |
| `ThreadContextHandler` → `DbConnectionManagementHandler` | コンテキスト情報はDB操作前に設定 |
| `HttpResponseHandler` → `SecureHandler`（Web） | セキュリティヘッダーはレスポンス構築後に付加 |
| `LoopHandler` → `DataReadHandler`（バッチ） | データ読取はループの内側 |
| `MultiThreadExecutionHandler` → `RetryHandler`（バッチ） | リトライはサブスレッド内 |
| ルーティング（`RoutesMapping` / `RequestPathJavaPackageMapping`）は最内殻 | 全ハンドラ通過後にアクションへディスパッチ |

## 主要ハンドラ FQCN一覧

| ハンドラ | FQCN |
|---------|------|
| GlobalErrorHandler | `nablarch.fw.handler.GlobalErrorHandler` |
| HttpCharacterEncodingHandler | `nablarch.fw.web.handler.HttpCharacterEncodingHandler` |
| HttpResponseHandler | `nablarch.fw.web.handler.HttpResponseHandler` |
| SecureHandler | `nablarch.fw.web.handler.SecureHandler` |
| HttpAccessLogHandler | `nablarch.common.web.handler.HttpAccessLogHandler` |
| MultipartHandler | `nablarch.fw.web.upload.MultipartHandler` |
| SessionStoreHandler | `nablarch.common.web.session.SessionStoreHandler` |
| NormalizationHandler | `nablarch.fw.web.handler.NormalizationHandler` |
| ForwardingHandler | `nablarch.fw.web.handler.ForwardingHandler` |
| HttpErrorHandler | `nablarch.fw.web.handler.HttpErrorHandler` |
| ThreadContextHandler | `nablarch.common.handler.threadcontext.ThreadContextHandler` |
| ThreadContextClearHandler | `nablarch.common.handler.threadcontext.ThreadContextClearHandler` |
| DbConnectionManagementHandler | `nablarch.common.handler.DbConnectionManagementHandler` |
| TransactionManagementHandler | `nablarch.common.handler.TransactionManagementHandler` |
| StatusCodeConvertHandler | `nablarch.fw.handler.StatusCodeConvertHandler` |
| JaxRsResponseHandler | `nablarch.fw.jaxrs.JaxRsResponseHandler` |
| RoutesMapping | `nablarch.integration.router.RoutesMapping` |
| RequestPathJavaPackageMapping | `nablarch.fw.handler.RequestPathJavaPackageMapping` |
| MultiThreadExecutionHandler | `nablarch.fw.handler.MultiThreadExecutionHandler` |
| LoopHandler | `nablarch.fw.handler.LoopHandler` |
| DataReadHandler | `nablarch.fw.handler.DataReadHandler` |
| RetryHandler | `nablarch.fw.handler.RetryHandler` |
| DuplicateProcessCheckHandler | `nablarch.fw.handler.DuplicateProcessCheckHandler` |
| ProcessStopHandler | `nablarch.fw.handler.ProcessStopHandler` |
| RequestThreadLoopHandler | `nablarch.fw.handler.RequestThreadLoopHandler` |

## カスタムハンドラの作成例

```java
public class RequestLoggingHandler implements Handler<HttpRequest, HttpResponse> {
    private static final Logger LOGGER = LoggerManager.get(RequestLoggingHandler.class);

    @Override
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        LOGGER.logInfo("Request: " + request.getMethod() + " " + request.getRequestUri());
        long start = System.currentTimeMillis();

        // 後続ハンドラの実行
        HttpResponse response = context.handleNext(request);

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.logInfo("Response: " + response.getStatusCode() + " (" + elapsed + "ms)");
        return response;
    }
}
```

## よくある落とし穴

1. **DbConnectionManagementHandlerとTransactionManagementHandlerの順序逆転** → `ClassCastException`が発生
2. **ルーティングハンドラの未設定** → `NoMoreHandlerException`（ハンドラキュー空）
3. **バッチでLoopHandler未設定** → データが1件しか処理されない
4. **GlobalErrorHandlerの配置漏れ** → 未処理例外がアプリケーション外に漏れる
5. **サブスレッドでメインスレッド用のDB接続ハンドラを参照** → スレッド間のDB接続共有で不整合

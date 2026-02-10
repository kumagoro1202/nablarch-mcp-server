---
description: Nablarchのコンポーネント定義XML設定ガイド。DIコンテナ（SystemRepository）の設定方法、ハンドラキューXML定義、データベース接続設定、セッションストア設定、スレッドコンテキスト設定、web.xml構成、設定ファイル（config-file）の使い方を提供する。「NablarchのXML設定」「コンポーネント定義」「SystemRepositoryの使い方」「DIコンテナ設定」といった質問で使用。
globs:
  - "**/*.xml"
  - "**/*.config"
  - "**/web.xml"
  - "**/*-boot.xml"
---

# Nablarch コンポーネント定義XML設定ガイド

## 概要

NablarchはXMLベースのDIコンテナ（`SystemRepository`）でアプリケーション構成を管理する。Spring FrameworkのXML設定に似た仕組み。

## XMLの基本構造

```xml
<?xml version="1.0" encoding="UTF-8"?>
<component-configuration
    xmlns="http://tis.co.jp/nablarch/component-configuration"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration">

  <!-- 設定ファイル読み込み -->
  <config-file file="common.config" />
  <config-file file="env.config" />

  <!-- 他XMLのインポート -->
  <import file="nablarch/webui/web-boot.xml" />

  <!-- コンポーネント定義 -->
  <component name="myComponent" class="com.example.MyComponent">
    <property name="propertyName" value="propertyValue" />
  </component>

</component-configuration>
```

## 主要要素

### `<component>` — コンポーネント定義

```xml
<!-- 基本 -->
<component name="mailSender" class="nablarch.common.mail.MailSender">
  <property name="mailSessionConfig">
    <component class="nablarch.common.mail.MailSessionConfig">
      <property name="host" value="${mail.smtp.host}" />
      <property name="port" value="${mail.smtp.port}" />
    </component>
  </property>
</component>

<!-- リスト型プロパティ -->
<component name="handler" class="com.example.MyHandler">
  <property name="items">
    <list>
      <component class="com.example.Item1" />
      <component class="com.example.Item2" />
    </list>
  </property>
</component>

<!-- マップ型プロパティ -->
<component name="config" class="com.example.Config">
  <property name="settings">
    <map>
      <entry key="key1" value="value1" />
      <entry key="key2" value="value2" />
    </map>
  </property>
</component>
```

### `<component-ref>` — 他コンポーネントの参照

```xml
<!-- 定義済みコンポーネントを参照 -->
<component-ref name="dbConnectionManagementHandler" />
```

### `<config-file>` — 設定ファイル読み込み

```xml
<!-- プロパティファイルの読み込み -->
<config-file file="common.config" />
<config-file file="env.config" />
```

設定ファイルの形式（`common.config`）:
```properties
db.url=jdbc:postgresql://localhost:5432/mydb
db.user=app_user
db.password=secret
mail.smtp.host=smtp.example.com
```

XML内での参照: `${db.url}`

### `<import>` — 他XMLのインポート

```xml
<!-- Nablarch標準設定の読み込み -->
<import file="nablarch/webui/web-boot.xml" />
```

## web.xml 設定

### Webアプリケーション用

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

  <!-- コンポーネント定義XMLのパス -->
  <context-param>
    <param-name>di.config</param-name>
    <param-value>web-boot.xml</param-value>
  </context-param>

  <!-- Nablarch初期化リスナー -->
  <!-- FQCN: nablarch.fw.web.servlet.NablarchServletContextListener -->
  <listener>
    <listener-class>
      nablarch.fw.web.servlet.NablarchServletContextListener
    </listener-class>
  </listener>

  <!-- Nablarchフロントコントローラ -->
  <!-- FQCN: nablarch.fw.web.servlet.RepositoryBasedWebFrontController -->
  <filter>
    <filter-name>entryPoint</filter-name>
    <filter-class>
      nablarch.fw.web.servlet.RepositoryBasedWebFrontController
    </filter-class>
  </filter>
  <filter-mapping>
    <filter-name>entryPoint</filter-name>
    <url-pattern>/action/*</url-pattern>  <!-- Web: /action/*, REST: /api/* -->
  </filter-mapping>
</web-app>
```

## データベース接続設定

```xml
<!-- データソース -->
<!-- FQCN: nablarch.core.db.connection.BasicDbConnectionFactoryForDataSource -->
<component name="dataSource"
           class="nablarch.core.db.connection.BasicDbConnectionFactoryForDataSource">
  <property name="databaseDriverName" value="${db.driver}" />
  <property name="databaseUrl" value="${db.url}" />
  <property name="user" value="${db.user}" />
  <property name="password" value="${db.password}" />
  <property name="maxPoolSize" value="${db.pool.maxSize}" />
  <property name="transactionIsolationLevel" value="READ_COMMITTED" />
</component>

<!-- DB接続管理ハンドラ -->
<!-- FQCN: nablarch.common.handler.DbConnectionManagementHandler -->
<component name="dbConnectionManagementHandler"
           class="nablarch.common.handler.DbConnectionManagementHandler">
  <property name="connectionFactory" ref="dataSource" />
</component>

<!-- トランザクション管理ハンドラ -->
<!-- FQCN: nablarch.common.handler.TransactionManagementHandler -->
<component name="transactionManagementHandler"
           class="nablarch.common.handler.TransactionManagementHandler">
  <property name="transactionFactory" ref="jdbcTransactionFactory" />
</component>

<!-- FQCN: nablarch.core.db.transaction.JdbcTransactionFactory -->
<component name="jdbcTransactionFactory"
           class="nablarch.core.db.transaction.JdbcTransactionFactory">
  <property name="isolationLevel" value="READ_COMMITTED" />
  <property name="transactionTimeoutSec" value="0" />
</component>
```

## セッションストア設定

```xml
<!-- FQCN: nablarch.common.web.session.SessionStoreHandler -->
<component name="sessionStoreHandler"
           class="nablarch.common.web.session.SessionStoreHandler">
  <property name="sessionManager" ref="sessionManager" />
</component>

<!-- FQCN: nablarch.common.web.session.SessionManager -->
<component name="sessionManager"
           class="nablarch.common.web.session.SessionManager">
  <property name="availableStores">
    <list>
      <!-- DBストア -->
      <component class="nablarch.common.web.session.store.DbStore">
        <property name="tableName" value="USER_SESSION" />
        <property name="expirationDateTime" value="1800" />
      </component>
      <!-- HIDDENストア（画面間パラメータ引継ぎ） -->
      <component class="nablarch.common.web.session.store.HiddenStore" />
    </list>
  </property>
  <property name="defaultStoreName" value="db" />
</component>
```

## スレッドコンテキスト設定

```xml
<!-- FQCN: nablarch.common.handler.threadcontext.ThreadContextHandler -->
<component name="threadContextHandler"
           class="nablarch.common.handler.threadcontext.ThreadContextHandler">
  <property name="attributes">
    <list>
      <component class="nablarch.common.handler.threadcontext.UserIdAttribute">
        <property name="sessionKey" value="user.id" />
        <property name="anonymousId" value="anonymous" />
      </component>
      <component class="nablarch.common.handler.threadcontext.RequestIdAttribute" />
      <component class="nablarch.common.handler.threadcontext.LanguageAttribute">
        <property name="defaultLanguage" value="ja" />
      </component>
      <component class="nablarch.common.handler.threadcontext.ExecutionIdAttribute" />
    </list>
  </property>
</component>

<!-- FQCN: nablarch.common.handler.threadcontext.ThreadContextClearHandler -->
<component name="threadContextClearHandler"
           class="nablarch.common.handler.threadcontext.ThreadContextClearHandler" />
```

## SystemRepositoryの使用

```java
// FQCN: nablarch.core.repository.SystemRepository

// コンポーネントの取得（名前指定）
MailSender mailSender = SystemRepository.get("mailSender");

// 設定値の取得
String dbUrl = SystemRepository.getString("db.url");
```

## ルーティング設定

### Webアプリケーション（RoutesMapping）

```xml
<!-- FQCN: nablarch.integration.router.RoutesMapping -->
<component name="packageMapping"
           class="nablarch.integration.router.RoutesMapping">
  <property name="methodBinderFactory">
    <component class="nablarch.fw.web.handler.HttpMethodBinding$HttpMethodBinderFactory" />
  </property>
  <property name="basePackage" value="com.example.action" />
</component>
```

### バッチ（RequestPathJavaPackageMapping）

```xml
<!-- FQCN: nablarch.fw.handler.RequestPathJavaPackageMapping -->
<component class="nablarch.fw.handler.RequestPathJavaPackageMapping">
  <property name="basePackage" value="com.example.batch.action" />
</component>
```

## よくある落とし穴

1. **XML名前空間の記述ミス** — `xmlns="http://tis.co.jp/nablarch/component-configuration"` が正確でないとパースエラー
2. **プロパティ名のタイポ** — JavaBeanの命名規約（setXxx）と一致させる
3. **循環参照** — component-ref同士が循環するとエラー
4. **config-file未読み込み** — `${変数名}` が解決されず空文字になる
5. **import先ファイルパスの誤り** — クラスパスからの相対パスで指定
6. **classのFQCN誤り** — クラスがクラスパスに存在しないと `ComponentCreationException`

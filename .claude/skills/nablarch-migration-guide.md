---
description: Nablarch 5→6マイグレーションガイド。Java EE→Jakarta EEへの移行手順、非推奨API対応、パッケージ名変更（javax→jakarta）、BOM設定、Mavenモジュール更新、破壊的変更一覧、移行チェックリストを提供する。「Nablarch 5から6に移行したい」「Jakarta EE対応」「javax→jakarta変更」「Nablarch 6の変更点」「BOMの設定」といった質問で使用。
globs:
  - "**/pom.xml"
  - "**/build.gradle"
  - "**/*.xml"
---

# Nablarch 5→6 マイグレーションガイド

## 概要

Nablarch 6はJakarta EE 10ベースへの移行が最大の変更点。Java 17以上が必須。

| 項目 | Nablarch 5 (5u26) | Nablarch 6 (6u3) |
|------|-------------------|-------------------|
| Java | 8, 11 | **17, 21** |
| EE仕様 | Java EE 8 | **Jakarta EE 10** |
| 名前空間 | `javax.*` | **`jakarta.*`** |
| BOM | `com.nablarch.profile:nablarch-bom:5u26` | **`com.nablarch.profile:nablarch-bom:6u3`** |

## 移行チェックリスト

### Phase 1: ビルド環境

- [ ] **Java 17以上にアップグレード** — JDK 17 or 21（Eclipse Temurin推奨）
- [ ] **Maven 3.9以上にアップグレード**
- [ ] **BOMバージョンを6u3に変更**
  ```xml
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.nablarch.profile</groupId>
        <artifactId>nablarch-bom</artifactId>
        <version>6u3</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>
  ```
- [ ] **アプリケーションサーバー更新** — Tomcat 10.1.x / WildFly 35.x / Open Liberty 25.x

### Phase 2: パッケージ名変更（javax → jakarta）

最も影響範囲が大きい変更。全Javaファイルの`import`文を修正する必要がある。

| 旧（Nablarch 5） | 新（Nablarch 6） |
|-------------------|-------------------|
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.ws.rs.*` | `jakarta.ws.rs.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.inject.*` | `jakarta.inject.*` |
| `javax.batch.*` | `jakarta.batch.*` |

**一括置換コマンド例:**
```bash
# プロジェクト全体で一括置換（要バックアップ）
find src -name "*.java" -exec sed -i 's/javax\.persistence/jakarta.persistence/g' {} +
find src -name "*.java" -exec sed -i 's/javax\.validation/jakarta.validation/g' {} +
find src -name "*.java" -exec sed -i 's/javax\.ws\.rs/jakarta.ws.rs/g' {} +
find src -name "*.java" -exec sed -i 's/javax\.servlet/jakarta.servlet/g' {} +
```

### Phase 3: web.xml更新

```xml
<!-- 旧（Nablarch 5） -->
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         version="4.0">

<!-- 新（Nablarch 6） -->
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
```

### Phase 4: エンティティクラスのアノテーション

```java
// 旧（Nablarch 5）
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.Version;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

// 新（Nablarch 6）
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Version;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
```

### Phase 5: バリデーションアノテーション

```java
// 旧（Nablarch 5）— Nablarch独自バリデーション（変更なし）
import nablarch.core.validation.ee.Required;      // 変更なし
import nablarch.core.validation.ee.Length;         // 変更なし
import nablarch.core.validation.ee.NumberRange;    // 変更なし
import nablarch.core.validation.ee.SystemChar;     // 変更なし

// Bean Validation標準アノテーションを直接使用している場合
// 旧: import javax.validation.constraints.NotNull;
// 新: import jakarta.validation.constraints.NotNull;
```

### Phase 6: JAX-RSアノテーション

```java
// 旧（Nablarch 5）
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

// 新（Nablarch 6）
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
```

### Phase 7: Jakarta Batch（JSR 352 → Jakarta Batch）

```java
// 旧（Nablarch 5）
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.ItemProcessor;

// 新（Nablarch 6）
import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.api.chunk.ItemProcessor;
```

### Phase 8: テスト・ビルド検証

- [ ] `mvn clean compile` — コンパイルエラーの確認
- [ ] `mvn clean test` — 全テスト成功の確認
- [ ] アプリケーション起動確認
- [ ] 主要画面の動作確認
- [ ] REST APIの動作確認

## Nablarchフレームワーク側の変更

Nablarch自体のFQCNは**変更なし**。`nablarch.*` パッケージはそのまま。

```java
// 以下は全て変更なし（Nablarch 5でも6でも同じ）
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.ExecutionContext;
import nablarch.common.dao.UniversalDao;
import nablarch.fw.handler.GlobalErrorHandler;
import nablarch.common.handler.DbConnectionManagementHandler;
import nablarch.common.handler.TransactionManagementHandler;
import nablarch.core.repository.SystemRepository;
import nablarch.core.message.ApplicationException;
```

## 動作確認済みプラットフォーム（Nablarch 6u3）

| カテゴリ | 対応バージョン |
|---------|--------------|
| **Java** | Eclipse Temurin 17, 21 |
| **AP Server** | Tomcat 10.1.17, WildFly 35.0.1, Open Liberty 25.0.0.2, JBoss EAP 8.0.0 |
| **Database** | Oracle 19c/21c/23ai, Db2 11.5/12.1, SQL Server 2017/2019/2022, PostgreSQL 12.2-17.4 |
| **Build** | Maven 3.9+ |

## Mavenモジュール構成

BOMを使用すれば個別バージョン指定は不要。

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.nablarch.profile</groupId>
      <artifactId>nablarch-bom</artifactId>
      <version>6u3</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <!-- Web -->
  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-fw-web</artifactId>
  </dependency>
  <!-- REST -->
  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-fw-jaxrs</artifactId>
  </dependency>
  <!-- DAO -->
  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-common-dao</artifactId>
  </dependency>
  <!-- Routing -->
  <dependency>
    <groupId>com.nablarch.integration</groupId>
    <artifactId>nablarch-router-adaptor</artifactId>
  </dependency>
  <!-- Testing -->
  <dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-testing</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

## よくある落とし穴

1. **javax残留** — IDEの自動インポートが旧パッケージを挿入する場合がある。コンパイルは通るがランタイムで`ClassNotFoundException`
2. **web.xmlの名前空間更新漏れ** — `xmlns`を変更しないとサーブレットコンテナが認識しない
3. **サードパーティライブラリの互換性** — Jakarta EE 10に対応していないライブラリの確認
4. **テストの文字コード** — Java 17ではデフォルト文字コードがUTF-8
5. **BOMバージョン不統一** — 個別モジュールのバージョンを混在させない

## 公式情報

- Nablarch公式ドキュメント: `https://nablarch.github.io/docs/LATEST/doc/`
- GitHub Organization: `https://github.com/nablarch`
- Fintan（TIS技術ポータル）: `https://fintan.jp/`

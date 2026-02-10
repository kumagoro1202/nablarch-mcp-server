---
description: Nablarchテスティングフレームワーク利用ガイド。リクエスト単体テスト（SimpleRestTestSupport）、Excelテストデータ、DBアクセステスト（DbAccessTestSupport）、バッチテスト、テストクラスの書き方とテストデータ管理を提供する。「Nablarchのテストの書き方」「リクエスト単体テスト」「Excelテストデータ」「DBテスト」といった質問で使用。
globs:
  - "**/*Test.java"
  - "**/*Test*.java"
  - "**/*.xlsx"
  - "**/test/**"
---

# Nablarch テスティングフレームワーク利用ガイド

## 概要

Nablarchは独自のテスティングフレームワークを提供。JUnit 5と組み合わせ、リクエスト単体テスト・DBアクセステスト・バッチテストをサポートする。テストデータはExcelファイルで管理するのが特徴。

## 主要テストサポートクラス

| クラス | FQCN | 用途 |
|--------|------|------|
| SimpleRestTestSupport | `nablarch.test.core.http.SimpleRestTestSupport` | RESTリクエスト単体テスト |
| DbAccessTestSupport | `nablarch.test.core.db.DbAccessTestSupport` | DBアクセス・テストデータ管理 |
| BasicHttpRequestTestTemplate | `nablarch.test.core.http.BasicHttpRequestTestTemplate` | HTTP基本リクエストテスト |
| BatchRequestTestSupport | `nablarch.test.core.batch.BatchRequestTestSupport` | バッチテスト |

## 1. RESTリクエスト単体テスト

```java
import nablarch.test.core.http.SimpleRestTestSupport;
import nablarch.fw.web.HttpResponse;
import org.junit.jupiter.api.Test;

public class UsersActionTest extends SimpleRestTestSupport {

    @Test
    public void testFindAll() {
        HttpResponse response = sendRequest(
            get("/api/users"));
        assertStatusCode("200", response);
    }

    @Test
    public void testCreate() {
        HttpResponse response = sendRequest(
            post("/api/users")
                .setBody("{\"userName\":\"田中\",\"email\":\"tanaka@example.com\"}"));
        assertStatusCode("201", response);
    }

    @Test
    public void testFindById() {
        HttpResponse response = sendRequest(
            get("/api/users/1"));
        assertStatusCode("200", response);
    }

    @Test
    public void testUpdate() {
        HttpResponse response = sendRequest(
            put("/api/users/1")
                .setBody("{\"userName\":\"山田\",\"email\":\"yamada@example.com\"}"));
        assertStatusCode("200", response);
    }

    @Test
    public void testDelete() {
        HttpResponse response = sendRequest(
            delete("/api/users/1"));
        assertStatusCode("204", response);
    }

    @Test
    public void testValidationError() {
        // 必須項目未入力でバリデーションエラー
        HttpResponse response = sendRequest(
            post("/api/users")
                .setBody("{\"userName\":\"\"}"));  // userName必須
        assertStatusCode("400", response);
    }
}
```

## 2. Webリクエストテスト（画面遷移）

```java
import nablarch.test.core.http.BasicHttpRequestTestTemplate;

public class UserActionTest extends BasicHttpRequestTestTemplate {

    @Override
    protected String getBaseUri() {
        return "/action/user/";
    }

    /**
     * 一覧表示テスト
     * テストデータ: src/test/resources/com/example/action/UserActionTest/testList.xlsx
     */
    @Test
    public void testList() {
        execute("testList", new BasicAdvice() {
            @Override
            public void afterExecute(TestCaseInfo testCaseInfo,
                                     ExecutionContext context) {
                // リクエストスコープの検証
                List<User> users = context.getRequestScopedVar("users");
                assertNotNull(users);
            }
        });
    }
}
```

## 3. Excelテストデータ

### ファイル配置ルール

```
src/test/resources/
  com/example/action/
    UserActionTest/
      testList.xlsx      ← テストメソッド名と一致
      testRegister.xlsx
```

### Excelシートの構成

**テストケースシート（`testCases`）:**

| No | description | expectedStatusCode | uri |
|----|-------------|-------------------|-----|
| 1 | 正常表示 | 200 | /action/user/list |
| 2 | 存在しないURL | 404 | /action/user/notfound |

**DBセットアップシート（`SETUP_TABLE=USER_TABLE`）:**

| USER_ID | USER_NAME | EMAIL | STATUS |
|---------|-----------|-------|--------|
| 1 | 田中太郎 | tanaka@example.com | 1 |
| 2 | 山田花子 | yamada@example.com | 1 |

**期待結果シート（`EXPECTED_TABLE=USER_TABLE`）:**

| USER_ID | USER_NAME | EMAIL | STATUS |
|---------|-----------|-------|--------|
| 1 | 田中太郎 | tanaka@example.com | 1 |
| 2 | 山田花子 | yamada@example.com | 1 |
| 3 | 佐藤次郎 | sato@example.com | 1 |

### データ型の指定

| Excel表記 | 意味 |
|-----------|------|
| `1` | 数値 |
| `"文字列"` | 文字列（クォート付き） |
| `null` | NULL値 |
| `${systemDate}` | システム日付 |

## 4. DBアクセステスト

```java
import nablarch.test.core.db.DbAccessTestSupport;

public class UserDaoTest extends DbAccessTestSupport {

    @Test
    public void testInsert() {
        // Excelからテストデータをセットアップし、
        // 処理実行後にDBの期待値を検証
        execute("testInsert");
    }

    @Test
    public void testFindByName() {
        // 手動でテストデータをセットアップする場合
        setUpDb("testFindByName");  // Excelのセットアップデータを投入

        Map<String, String> condition = new HashMap<>();
        condition.put("userName", "%田中%");
        EntityList<User> result = UniversalDao.findAllBySqlFile(
            User.class, "FIND_BY_NAME", condition);

        assertEquals(1, result.size());
        assertEquals("田中太郎", result.get(0).getUserName());
    }
}
```

## 5. バッチテスト

```java
import nablarch.test.core.batch.BatchRequestTestSupport;

public class UserImportBatchTest extends BatchRequestTestSupport {

    @Test
    public void testNormalExecution() {
        // バッチ実行テスト
        // テストデータ: Excelファイルでセットアップ・期待値を定義
        execute("testNormalExecution");
    }

    @Test
    public void testNoData() {
        // 処理対象データなしの場合
        execute("testNoData");
    }
}
```

## 6. テスト設定

### テスト用コンポーネント定義

```xml
<!-- src/test/resources/unit-test-component.xml -->
<component-configuration
    xmlns="http://tis.co.jp/nablarch/component-configuration">

  <config-file file="unit-test.config" />

  <!-- テスト用DB設定（H2インメモリDB等） -->
  <component name="dataSource"
             class="nablarch.core.db.connection.BasicDbConnectionFactoryForDataSource">
    <property name="databaseDriverName" value="org.h2.Driver" />
    <property name="databaseUrl" value="jdbc:h2:mem:testdb;MODE=Oracle" />
    <property name="user" value="sa" />
    <property name="password" value="" />
  </component>
</component-configuration>
```

### Mavenの依存関係

```xml
<dependency>
  <groupId>com.nablarch.framework</groupId>
  <artifactId>nablarch-testing</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.nablarch.framework</groupId>
  <artifactId>nablarch-testing-junit5</artifactId>
  <scope>test</scope>
</dependency>
```

## 7. テストパターンの使い分け

| テスト種別 | テストサポートクラス | テストデータ | 適用場面 |
|-----------|---------------------|------------|---------|
| REST API | `SimpleRestTestSupport` | JSON文字列 | REST APIのCRUD |
| Web画面 | `BasicHttpRequestTestTemplate` | Excel | 画面遷移・バリデーション |
| DB操作 | `DbAccessTestSupport` | Excel | DAO・SQLファイルの検証 |
| バッチ | `BatchRequestTestSupport` | Excel | バッチ処理の実行検証 |
| ユニットテスト | 標準JUnit 5 | なし | ユーティリティ・ロジック |

## よくある落とし穴

1. **Excelファイルの配置パスミス** — テストクラスのFQCN + メソッド名と一致させる
2. **テストデータの文字コード** — Excel内の日本語がDBの文字コードと一致するか確認
3. **テスト実行順序の依存** — テスト間でDBデータが共有されないよう、各テストでセットアップする
4. **コンポーネント定義の不一致** — テスト用XMLとプロダクション用XMLの設定差異に注意
5. **H2とプロダクションDBの方言差** — H2のMODE設定でプロダクションDBに近づける
6. **テストのSKIP** — Nablarchテストのスキップは品質リスク。全テスト成功を目指す

## 公式サンプルプロジェクト

| サンプル | リポジトリ |
|---------|-----------|
| REST APIテスト | `https://github.com/nablarch/nablarch-example-rest` |
| Webアプリテスト | `https://github.com/nablarch/nablarch-example-web` |
| バッチテスト | `https://github.com/nablarch/nablarch-example-batch-ee` |

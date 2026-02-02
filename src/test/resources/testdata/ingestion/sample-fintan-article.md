# NablarchでRESTful APIを構築する実践ガイド

本記事では、Nablarchフレームワークを使用してRESTful APIを構築する手順を解説する。
Nablarch 6u3を前提に、プロジェクトセットアップからデプロイまでの流れを紹介する。

## プロジェクトセットアップ

Nablarchのプロジェクトは、Maven Archetypeを使用して初期構築する。
RESTful API向けのアーキタイプ `nablarch-jaxrs-archetype` を使用することで、
必要な依存関係と設定ファイルが自動生成される。

```xml
<dependency>
    <groupId>com.nablarch.framework</groupId>
    <artifactId>nablarch-fw-jaxrs</artifactId>
</dependency>
```

## アクション定義

RESTful APIのエンドポイントは、JAX-RSアノテーションを使用して定義する。
Nablarchでは `@Produces` と `@Consumes` でコンテンツネゴシエーションを制御し、
リクエストボディの自動マッピングを行う。

```java
@Path("/api/users")
public class UserAction {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserDto> list(HttpRequest request) {
        return UniversalDao.findAll(UserDto.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public HttpResponse create(HttpRequest request) {
        UserForm form = request.getEntity(UserForm.class);
        UniversalDao.insert(form);
        return new HttpResponse(201);
    }
}
```

## エラーハンドリング

Nablarchでは例外ハンドラをハンドラキューに登録することで、
統一的なエラーレスポンスを返すことができる。
業務例外（ApplicationException）と予期しない例外で処理を分ける。

### カスタムエラーレスポンス

独自のエラーレスポンス形式を定義する場合は、ErrorResponseBuilderを実装する。
JSONフォーマットでエラーコード・メッセージ・詳細を返す標準的なパターンを推奨する。

## テスト戦略

RESTful APIのテストは、リクエスト単体テスト機能を使用する。
実際のHTTPリクエストを送信せずに、アクションクラスの振る舞いを検証できる。
テストデータのセットアップはExcelファイルで行い、期待結果もExcelで定義する。

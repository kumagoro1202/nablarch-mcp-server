---
description: NablarchのAction/Form/Entity設計パターンガイド。Web（JSP）・REST（JAX-RS）・バッチのアクション設計、フォームバリデーション、UniversalDaoによるデータアクセス、エンティティ定義、外部SQLファイルの書き方を提供する。「Nablarchでアクションを作りたい」「REST APIを作りたい」「バリデーションの書き方を知りたい」「UniversalDaoの使い方」といった質問で使用。
globs:
  - "**/*Action.java"
  - "**/*Form.java"
  - "**/*Entity.java"
  - "**/*.sql"
---

# Nablarch API設計ガイド — Action / Form / Entity パターン

## 1. Webアクション（JSP画面遷移）

### 基本パターン

```java
// FQCN参照:
//   nablarch.fw.web.HttpRequest
//   nablarch.fw.web.HttpResponse
//   nablarch.fw.ExecutionContext
//   nablarch.common.web.interceptor.InjectForm
//   nablarch.fw.web.interceptor.OnError

public class UserAction {

    /** 一覧画面表示 */
    public HttpResponse list(HttpRequest request, ExecutionContext context) {
        List<User> users = UniversalDao.findAll(User.class);
        context.setRequestScopedVar("users", users);
        return new HttpResponse("/WEB-INF/view/user/list.jsp");
    }

    /** 登録処理（バリデーション付き） */
    @InjectForm(form = UserForm.class, prefix = "form")
    @OnError(type = ApplicationException.class,
             path = "/WEB-INF/view/user/input.jsp")
    public HttpResponse doRegister(HttpRequest request, ExecutionContext context) {
        UserForm form = context.getRequestScopedVar("form");
        User user = BeanUtil.createAndCopy(User.class, form);
        UniversalDao.insert(user);
        return new HttpResponse("redirect:///action/user/complete");
    }
}
```

### 画面遷移

```java
// フォワード（URLそのまま）
return new HttpResponse("/WEB-INF/view/user/detail.jsp");

// リダイレクト（POST-Redirect-GETパターン）
return new HttpResponse("redirect:///action/user/list");

// ステータスコード指定
return new HttpResponse(HttpResponse.Status.NOT_FOUND.getStatusCode());
```

### 二重送信防止

```java
// FQCN: nablarch.common.web.token.TokenUtil
// FQCN: nablarch.fw.web.interceptor.OnDoubleSubmission

// 入力画面表示時
public HttpResponse doInput(HttpRequest request, ExecutionContext context) {
    TokenUtil.setToken(context);  // トークン生成
    return new HttpResponse("/WEB-INF/view/user/input.jsp");
}

// 登録処理
@OnDoubleSubmission(path = "/WEB-INF/view/common/doubleSubmitError.jsp")
public HttpResponse doRegister(HttpRequest request, ExecutionContext context) {
    // トークン検証は@OnDoubleSubmissionが自動で行う
    return new HttpResponse("redirect:///action/user/complete");
}
```

## 2. RESTアクション（JAX-RS）

```java
// FQCN参照:
//   nablarch.fw.jaxrs.JaxRsHttpRequest
//   nablarch.common.dao.UniversalDao
//   nablarch.core.beans.BeanUtil

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsersAction {

    @GET
    public EntityList<UserResponse> findAll(HttpRequest request, ExecutionContext context) {
        return UniversalDao.findAll(UserResponse.class);
    }

    @POST
    @Valid
    public HttpResponse create(HttpRequest request, ExecutionContext context) {
        UserForm form = JaxRsHttpRequest.from(request)
                            .getRequest().readEntity(UserForm.class);
        User user = BeanUtil.createAndCopy(User.class, form);
        UniversalDao.insert(user);
        return new HttpResponse(HttpResponse.Status.CREATED.getStatusCode());
    }

    @GET
    @Path("/{userId}")
    public UserResponse findById(HttpRequest request, ExecutionContext context) {
        long userId = Long.parseLong(request.getParam("userId")[0]);
        return UniversalDao.findById(UserResponse.class, userId);
    }

    @PUT
    @Path("/{userId}")
    @Valid
    public HttpResponse update(HttpRequest request, ExecutionContext context) {
        UserForm form = JaxRsHttpRequest.from(request)
                            .getRequest().readEntity(UserForm.class);
        User user = BeanUtil.createAndCopy(User.class, form);
        UniversalDao.update(user);
        return new HttpResponse(HttpResponse.Status.OK.getStatusCode());
    }

    @DELETE
    @Path("/{userId}")
    public HttpResponse delete(HttpRequest request, ExecutionContext context) {
        long userId = Long.parseLong(request.getParam("userId")[0]);
        User user = UniversalDao.findById(User.class, userId);
        UniversalDao.delete(user);
        return new HttpResponse(HttpResponse.Status.NO_CONTENT.getStatusCode());
    }
}
```

RESTルーティング設定（コンポーネント定義XML）:
```xml
<component name="packageMapping"
           class="nablarch.integration.router.RoutesMapping">
  <property name="methodBinderFactory">
    <component class="nablarch.fw.jaxrs.JaxRsBeanValidationHandlerFactory" />
  </property>
  <property name="basePackage" value="com.example.api" />
</component>
```

## 3. バッチアクション

```java
// FQCN: nablarch.fw.action.BatchAction
// FQCN: nablarch.fw.reader.DatabaseRecordReader

public class UserImportAction extends BatchAction<SqlRow> {

    @Override
    public DataReader<SqlRow> createReader(ExecutionContext ctx) {
        return new DatabaseRecordReader()
            .setStatement(getSqlPStatement("SELECT_UNPROCESSED"));
    }

    @Override
    public Result handle(SqlRow inputData, ExecutionContext ctx) {
        User user = new User();
        user.setUserName(inputData.getString("USER_NAME"));
        user.setEmail(inputData.getString("EMAIL"));
        UniversalDao.insert(user);
        return new Result.Success();
    }
}
```

## 4. フォームクラス（バリデーション）

```java
// バリデーションアノテーション FQCN:
//   nablarch.core.validation.ee.Required
//   nablarch.core.validation.ee.Length
//   nablarch.core.validation.ee.NumberRange
//   nablarch.core.validation.ee.SystemChar
//   nablarch.core.validation.ee.MailAddress

public class UserRegistrationForm implements Serializable {

    @Required
    @Length(max = 50)
    private String userName;

    @Required
    @MailAddress
    private String email;

    @NumberRange(min = 0, max = 150)
    private String age;

    @Required
    @SystemChar(charsetDef = "全角文字")
    private String fullName;

    // getter/setter は必須
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    // ... 以下同様
}
```

## 5. エンティティクラス

```java
// JPAアノテーションでテーブルマッピング
// FQCN: nablarch.common.dao.UniversalDao が操作対象とする

@Entity
@Table(name = "USER_TABLE")
public class User {

    @Id
    @Column(name = "USER_ID", length = 10)
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long userId;

    @Column(name = "USER_NAME", length = 50, nullable = false)
    public String userName;

    @Column(name = "EMAIL", length = 100, nullable = false)
    public String email;

    @Version
    @Column(name = "VERSION")
    public Long version;  // 楽観ロック用
}
```

## 6. UniversalDao（データアクセス）

```java
// FQCN: nablarch.common.dao.UniversalDao

// 全件検索
EntityList<User> users = UniversalDao.findAll(User.class);

// 主キー検索
User user = UniversalDao.findById(User.class, userId);

// SQLファイルによる条件検索
Map<String, String> condition = new HashMap<>();
condition.put("userName", "%田中%");
EntityList<User> users = UniversalDao.findAllBySqlFile(
    User.class, "FIND_BY_NAME", condition);

// 挿入・バッチ挿入
UniversalDao.insert(user);
UniversalDao.batchInsert(userList);

// 更新・削除
UniversalDao.update(user);
UniversalDao.delete(user);
```

## 7. 外部SQLファイル

配置先: `src/main/resources/<エンティティFQCN>.sql`
例: `com.example.entity.User` → `src/main/resources/com/example/entity/User.sql`

```sql
FIND_BY_NAME =
SELECT
    USER_ID, USER_NAME, EMAIL, STATUS
FROM
    USER_TABLE
WHERE
    $if(userName) {USER_NAME LIKE :%userName%}
    AND $if(status) {STATUS = :status}
ORDER BY
    USER_ID

COUNT_BY_STATUS =
SELECT COUNT(*) AS CNT
FROM USER_TABLE
WHERE STATUS = :status
```

- `$if(param)` — パラメータが存在する場合のみ条件を含める（動的SQL）
- `:paramName` — 名前付きパラメータ

## 8. セッション管理

```java
// FQCN: nablarch.common.web.session.SessionUtil

// セッションへの保存
SessionUtil.put(context, "user", userEntity);

// セッションからの取得
User user = SessionUtil.get(context, "user");

// セッションの削除
SessionUtil.delete(context, "user");

// セッションの無効化（ログアウト時）
SessionUtil.invalidate(context);
```

## 9. 排他制御

```java
// 楽観ロック: @Versionアノテーション付きエンティティ
try {
    UniversalDao.update(user);
} catch (jakarta.persistence.OptimisticLockException e) {
    throw new ApplicationException(
        MessageUtil.createMessage(MessageLevel.ERROR, "errors.optimisticLock"));
}
```

## よくある落とし穴

1. **FormとEntityを同一クラスにしない** — 入力用（Form）とDB用（Entity）は責務が異なる
2. **アクションクラスにインスタンスフィールドを持たない** — シングルトンスコープのためスレッドセーフティが破壊される
3. **SQLファイルの配置パスミス** — エンティティのFQCNと一致させる必要がある
4. **`@InjectForm`のprefix未指定** — リクエストパラメータが正しくバインドされない
5. **`UniversalDao.findById`でNoDataException** — try-catchで適切にハンドリングする

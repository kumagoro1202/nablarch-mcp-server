# Nablarchバリデーション入門

Nablarchのバリデーション機能について解説する。

## Bean Validation

Nablarchでは、Jakarta Bean Validationに基づいたバリデーションを提供する。

### アノテーション定義

```java
public class UserForm {
    @Required
    @Length(max = 100)
    private String name;
}
```

## ドメインバリデーション

ドメイン定義に基づくバリデーションも可能である。

ドメインバリデーションは業務ロジックに特化した入力チェックを実現する。

## エラーメッセージ

エラーメッセージはプロパティファイルで管理する。

```properties
nablarch.core.validation.ee.Required.message=必須項目です
```

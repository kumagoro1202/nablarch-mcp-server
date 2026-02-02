package nablarch.example.action;

import nablarch.core.beans.BeanUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * ユーザー管理アクションクラス。
 *
 * <p>ユーザーの一覧表示、登録、更新、削除を行う。</p>
 */
public class SampleAction {

    /** ユーザーサービス */
    private UserService userService;

    /**
     * ユーザー一覧を表示する。
     *
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @return 一覧画面のHTTPレスポンス
     */
    public HttpResponse list(HttpRequest request, ExecutionContext context) {
        context.setRequestScopedVar("users", userService.findAll());
        return new HttpResponse("/WEB-INF/view/user/list.jsp");
    }

    /**
     * ユーザーを登録する。
     *
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @return リダイレクトレスポンス
     */
    public HttpResponse create(HttpRequest request, ExecutionContext context) {
        UserForm form = BeanUtil.createAndCopy(UserForm.class, request.getParamMap());
        userService.register(form);
        return new HttpResponse(303, "redirect:///action/user/list");
    }
}

package com.tis.nablarch.mcp.codegen;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * コード生成エンジンのデフォルト実装。
 *
 * <p>Nablarch知識ベースと連携し、コーディング規約を適用した
 * ボイラープレートコードを生成する。</p>
 */
@Service
public class DefaultCodeGenerator implements CodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(DefaultCodeGenerator.class);
    private static final String DEFAULT_PACKAGE = "com.example";

    private final NablarchKnowledgeBase knowledgeBase;

    /**
     * コンストラクタ。
     *
     * @param knowledgeBase Nablarch知識ベース
     */
    public DefaultCodeGenerator(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    @Override
    public GenerationResult generate(String type, String name, String appType,
                                     Map<String, Object> specifications) {
        log.info("コード生成開始: type={}, name={}, appType={}", type, name, appType);

        List<GenerationResult.GeneratedFile> files = new ArrayList<>();
        List<String> conventions = new ArrayList<>();
        List<String> dependencies = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 知識ベースから規約・依存関係を取得
        lookupConventions(type, appType, conventions);
        lookupDependencies(type, appType, dependencies);

        // specifications からパッケージ取得（デフォルト: com.example）
        String basePackage = getStringSpec(specifications, "package", DEFAULT_PACKAGE);

        switch (type) {
            case "action" -> generateAction(name, appType, basePackage, specifications, files, warnings);
            case "form" -> generateForm(name, basePackage, specifications, files, warnings);
            case "sql" -> generateSql(name, basePackage, specifications, files, warnings);
            case "entity" -> generateEntity(name, basePackage, specifications, files, warnings);
            case "handler" -> generateHandler(name, basePackage, specifications, files, warnings);
            case "interceptor" -> generateInterceptor(name, basePackage, specifications, files, warnings);
            default -> warnings.add("未対応の生成タイプ: " + type);
        }

        return new GenerationResult(type, name, appType, files, conventions, dependencies, warnings);
    }

    // ====== 規約・依存関係の取得 ======

    private void lookupConventions(String type, String appType, List<String> conventions) {
        // 基本規約は常に適用
        conventions.add("アクションクラスはシングルトン — インスタンスフィールド禁止");

        try {
            List<String> results =
                    knowledgeBase.search("Nablarch " + type + " " + appType + " 規約", null);
            for (String result : results) {
                // 検索結果からコンテンツの先頭を抽出（規約として追加）
                if (result != null && !result.isEmpty()) {
                    String convention = result.length() > 100
                            ? result.substring(0, 100) + "..."
                            : result;
                    conventions.add(convention);
                }
            }
        } catch (Exception e) {
            log.warn("知識ベース検索でエラー、フォールバック規約を使用: {}", e.getMessage());
        }

        // タイプ別の基本規約追加
        switch (type) {
            case "action" -> {
                if ("web".equals(appType)) {
                    conventions.add("Webアクションはdoプリフィックスのメソッド名を使用");
                    conventions.add("@InjectForm + @OnError でフォーム検証");
                } else if ("rest".equals(appType)) {
                    conventions.add("JAX-RSアノテーションによるルーティング");
                    conventions.add("EntityResponseでJSON応答");
                } else if ("batch".equals(appType)) {
                    conventions.add("BatchAction<SqlRow>を継承");
                    conventions.add("DataReaderでデータ取得");
                }
            }
            case "entity" -> {
                conventions.add("Entityフィールドはpublic（getter/setter不要）");
                conventions.add("@Versionで楽観ロック");
            }
            case "handler" -> conventions.add("Handler<I, O>インターフェースを実装");
            case "interceptor" -> conventions.add("InterceptorからInvokeNode継承");
            default -> { /* 未知のタイプは無視 */ }
        }
    }

    private void lookupDependencies(String type, String appType, List<String> dependencies) {
        switch (type) {
            case "action" -> {
                switch (appType) {
                    case "web" -> {
                        dependencies.add("nablarch-fw-web");
                        dependencies.add("nablarch-common-dao");
                    }
                    case "rest" -> {
                        dependencies.add("nablarch-fw-jaxrs");
                        dependencies.add("nablarch-common-dao");
                    }
                    case "batch" -> {
                        dependencies.add("nablarch-fw-batch");
                        dependencies.add("nablarch-common-dao");
                    }
                    case "messaging" -> {
                        dependencies.add("nablarch-fw-messaging");
                    }
                    default -> { /* 未知のappTypeは無視 */ }
                }
            }
            case "form" -> dependencies.add("nablarch-core-validation-ee");
            case "entity" -> dependencies.add("nablarch-common-dao");
            case "handler", "interceptor" -> dependencies.add("nablarch-fw");
            default -> { /* 未知のタイプは無視 */ }
        }
    }

    // ====== 各タイプのコード生成メソッド ======

    private void generateAction(String name, String appType, String basePackage,
                                Map<String, Object> specs, List<GenerationResult.GeneratedFile> files,
                                List<String> warnings) {
        switch (appType) {
            case "web" -> generateWebAction(name, basePackage, specs, files);
            case "rest" -> generateRestAction(name, basePackage, specs, files);
            case "batch" -> generateBatchAction(name, basePackage, specs, files);
            case "messaging" -> generateMessagingAction(name, basePackage, specs, files);
            default -> warnings.add("未対応のapp_type: " + appType);
        }
    }

    private void generateWebAction(String name, String basePackage, Map<String, Object> specs,
                                   List<GenerationResult.GeneratedFile> files) {
        String className = NamingConventionHelper.toActionClassName(name);
        String pathName = NamingConventionHelper.toPathName(name);
        String entityName = getStringSpec(specs, "entity_name", name);
        String formName = NamingConventionHelper.toFormClassName(name);

        String code = """
                package %s.action;

                import nablarch.common.dao.UniversalDao;
                import nablarch.common.web.interceptor.InjectForm;
                import nablarch.common.web.interceptor.OnError;
                import nablarch.core.beans.BeanUtil;
                import nablarch.fw.ExecutionContext;
                import nablarch.fw.web.HttpRequest;
                import nablarch.fw.web.HttpResponse;

                import %s.entity.%s;
                import %s.form.%s;

                /**
                 * %sアクション。
                 */
                public class %s {

                    /**
                     * 一覧表示。
                     *
                     * @param request HTTPリクエスト
                     * @param context 実行コンテキスト
                     * @return HTTPレスポンス
                     */
                    public HttpResponse doList(HttpRequest request, ExecutionContext context) {
                        context.setRequestScopedVar("items", UniversalDao.findAll(%s.class));
                        return new HttpResponse("/WEB-INF/view/%s/list.jsp");
                    }

                    /**
                     * 登録画面表示。
                     *
                     * @param request HTTPリクエスト
                     * @param context 実行コンテキスト
                     * @return HTTPレスポンス
                     */
                    public HttpResponse doInput(HttpRequest request, ExecutionContext context) {
                        return new HttpResponse("/WEB-INF/view/%s/input.jsp");
                    }

                    /**
                     * 登録処理。
                     *
                     * @param request HTTPリクエスト
                     * @param context 実行コンテキスト
                     * @return HTTPレスポンス
                     */
                    @InjectForm(form = %s.class, prefix = "form")
                    @OnError(type = nablarch.core.message.ApplicationException.class,
                             path = "/WEB-INF/view/%s/input.jsp")
                    public HttpResponse doRegister(HttpRequest request, ExecutionContext context) {
                        %s form = context.getRequestScopedVar("form");
                        %s entity = BeanUtil.createAndCopy(%s.class, form);
                        UniversalDao.insert(entity);
                        return new HttpResponse("redirect:///action/%s/list");
                    }
                }
                """.formatted(
                basePackage, basePackage, entityName, basePackage, formName,
                name, className,
                entityName, pathName,
                pathName,
                formName, pathName,
                formName, entityName, entityName, pathName
        );

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "action", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateRestAction(String name, String basePackage, Map<String, Object> specs,
                                    List<GenerationResult.GeneratedFile> files) {
        String className = NamingConventionHelper.toActionClassName(name);
        String entityName = getStringSpec(specs, "entity_name", name);
        String idField = NamingConventionHelper.toIdFieldName(entityName);
        String formName = NamingConventionHelper.toFormClassName(name);

        String code = """
                package %s.action;

                import jakarta.ws.rs.Consumes;
                import jakarta.ws.rs.DELETE;
                import jakarta.ws.rs.GET;
                import jakarta.ws.rs.POST;
                import jakarta.ws.rs.PUT;
                import jakarta.ws.rs.Path;
                import jakarta.ws.rs.PathParam;
                import jakarta.ws.rs.Produces;
                import jakarta.ws.rs.core.MediaType;

                import nablarch.common.dao.UniversalDao;
                import nablarch.core.beans.BeanUtil;
                import nablarch.fw.ExecutionContext;
                import nablarch.fw.jaxrs.EntityResponse;
                import nablarch.fw.web.HttpRequest;
                import nablarch.fw.web.HttpResponse;

                import %s.entity.%s;
                import %s.form.%s;

                /**
                 * %s RESTアクション。
                 */
                @Path("/%s")
                public class %s {

                    /**
                     * 一覧取得。
                     */
                    @GET
                    @Produces(MediaType.APPLICATION_JSON)
                    public EntityResponse findAll(HttpRequest request, ExecutionContext context) {
                        return new EntityResponse(HttpResponse.Status.OK.getStatusCode(),
                                UniversalDao.findAll(%s.class));
                    }

                    /**
                     * 1件取得。
                     */
                    @GET
                    @Path("/{%s}")
                    @Produces(MediaType.APPLICATION_JSON)
                    public EntityResponse findById(HttpRequest request, ExecutionContext context,
                            @PathParam("%s") Long id) {
                        return new EntityResponse(HttpResponse.Status.OK.getStatusCode(),
                                UniversalDao.findById(%s.class, id));
                    }

                    /**
                     * 新規登録。
                     */
                    @POST
                    @Consumes(MediaType.APPLICATION_JSON)
                    @Produces(MediaType.APPLICATION_JSON)
                    public HttpResponse create(HttpRequest request, ExecutionContext context) {
                        %s form = context.getRequestScopedVar("form");
                        %s entity = BeanUtil.createAndCopy(%s.class, form);
                        UniversalDao.insert(entity);
                        return new HttpResponse(HttpResponse.Status.CREATED.getStatusCode());
                    }

                    /**
                     * 更新。
                     */
                    @PUT
                    @Path("/{%s}")
                    @Consumes(MediaType.APPLICATION_JSON)
                    @Produces(MediaType.APPLICATION_JSON)
                    public HttpResponse update(HttpRequest request, ExecutionContext context,
                            @PathParam("%s") Long id) {
                        %s form = context.getRequestScopedVar("form");
                        %s entity = UniversalDao.findById(%s.class, id);
                        BeanUtil.copy(form, entity);
                        UniversalDao.update(entity);
                        return new HttpResponse(HttpResponse.Status.OK.getStatusCode());
                    }

                    /**
                     * 削除。
                     */
                    @DELETE
                    @Path("/{%s}")
                    @Produces(MediaType.APPLICATION_JSON)
                    public HttpResponse delete(HttpRequest request, ExecutionContext context,
                            @PathParam("%s") Long id) {
                        %s entity = UniversalDao.findById(%s.class, id);
                        UniversalDao.delete(entity);
                        return new HttpResponse(HttpResponse.Status.NO_CONTENT.getStatusCode());
                    }
                }
                """.formatted(
                basePackage,
                basePackage, entityName, basePackage, formName,
                name, NamingConventionHelper.toPathName(name), className,
                entityName,
                idField, idField, entityName,
                formName, entityName, entityName,
                idField, idField, formName, entityName, entityName,
                idField, idField, entityName, entityName
        );

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "action", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateBatchAction(String name, String basePackage, Map<String, Object> specs,
                                     List<GenerationResult.GeneratedFile> files) {
        String className = NamingConventionHelper.toActionClassName(name);
        String entityName = getStringSpec(specs, "entity_name", name);

        String code = """
                package %s.action;

                import nablarch.common.dao.UniversalDao;
                import nablarch.fw.DataReader;
                import nablarch.fw.ExecutionContext;
                import nablarch.fw.Result;
                import nablarch.fw.action.BatchAction;
                import nablarch.fw.reader.DatabaseRecordReader;

                import %s.entity.%s;

                /**
                 * %sバッチアクション。
                 */
                public class %s extends BatchAction<%s> {

                    @Override
                    public DataReader<%s> createReader(ExecutionContext context) {
                        return new DatabaseRecordReader()
                                .setStatement(%s.class, "FIND_ALL");
                    }

                    @Override
                    public Result handle(%s inputData, ExecutionContext context) {
                        // TODO: バッチ処理ロジックを実装
                        return new Result.Success();
                    }
                }
                """.formatted(
                basePackage,
                basePackage, entityName,
                name, className, entityName,
                entityName, entityName,
                entityName
        );

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "action", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateMessagingAction(String name, String basePackage, Map<String, Object> specs,
                                         List<GenerationResult.GeneratedFile> files) {
        String className = NamingConventionHelper.toActionClassName(name);

        String code = """
                package %s.action;

                import nablarch.fw.ExecutionContext;
                import nablarch.fw.messaging.action.MessagingAction;
                import nablarch.fw.messaging.RequestMessage;
                import nablarch.fw.messaging.ResponseMessage;

                /**
                 * %sメッセージングアクション。
                 */
                public class %s extends MessagingAction {

                    @Override
                    protected ResponseMessage onReceive(RequestMessage request, ExecutionContext context) {
                        // TODO: メッセージ受信処理を実装
                        return request.reply();
                    }
                }
                """.formatted(basePackage, name, className);

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "action", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateForm(String name, String basePackage, Map<String, Object> specs,
                              List<GenerationResult.GeneratedFile> files, List<String> warnings) {
        String className = NamingConventionHelper.toFormClassName(name);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) specs.get("fields");

        StringBuilder fieldDefs = new StringBuilder();
        StringBuilder accessors = new StringBuilder();

        if (fields != null && !fields.isEmpty()) {
            for (Map<String, Object> field : fields) {
                String fieldName = (String) field.get("name");
                String fieldType = (String) field.getOrDefault("type", "String");
                Boolean required = (Boolean) field.getOrDefault("required", false);
                Integer maxLength = (Integer) field.get("max_length");

                // バリデーションアノテーション
                if (Boolean.TRUE.equals(required)) {
                    fieldDefs.append("    @Required\n");
                }
                if (maxLength != null) {
                    fieldDefs.append("    @Length(max = ").append(maxLength).append(")\n");
                }
                fieldDefs.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");

                // getter/setter
                String capName = NamingConventionHelper.capitalize(fieldName);
                accessors.append("    public ").append(fieldType).append(" get").append(capName).append("() {\n");
                accessors.append("        return ").append(fieldName).append(";\n");
                accessors.append("    }\n\n");
                accessors.append("    public void set").append(capName).append("(").append(fieldType).append(" ").append(fieldName).append(") {\n");
                accessors.append("        this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                accessors.append("    }\n\n");
            }
        } else {
            fieldDefs.append("    // TODO: フィールドを定義\n");
            warnings.add("フォームフィールドが未指定です。specifications.fields に定義してください。");
        }

        String code = """
                package %s.form;

                import nablarch.core.validation.ee.Length;
                import nablarch.core.validation.ee.Required;

                /**
                 * %sフォーム。
                 */
                public class %s {

                %s
                %s}
                """.formatted(basePackage, name, className, fieldDefs, accessors);

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "form", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateSql(String name, String basePackage, Map<String, Object> specs,
                             List<GenerationResult.GeneratedFile> files, List<String> warnings) {
        String tableName = NamingConventionHelper.toTableName(name);
        String idField = NamingConventionHelper.toIdFieldName(name);
        String idColumn = NamingConventionHelper.toColumnName(idField);

        String sql = """
                -- %s SQL定義ファイル

                FIND_ALL =
                SELECT
                    *
                FROM
                    %s
                ORDER BY
                    %s

                FIND_BY_ID =
                SELECT
                    *
                FROM
                    %s
                WHERE
                    %s = :%s

                """.formatted(name, tableName, idColumn, tableName, idColumn, idField);

        String filePath = NamingConventionHelper.toSqlFilePath(basePackage, name);
        files.add(new GenerationResult.GeneratedFile(filePath, name + ".sql", sql, "sql"));
    }

    private void generateEntity(String name, String basePackage, Map<String, Object> specs,
                                List<GenerationResult.GeneratedFile> files, List<String> warnings) {
        String tableName = NamingConventionHelper.toTableName(name);
        String idField = NamingConventionHelper.toIdFieldName(name);
        String idColumn = NamingConventionHelper.toColumnName(idField);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) specs.get("fields");

        StringBuilder fieldDefs = new StringBuilder();

        // ID フィールド（自動生成）
        fieldDefs.append("    @Id\n");
        fieldDefs.append("    @Column(name = \"").append(idColumn).append("\")\n");
        fieldDefs.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
        fieldDefs.append("    public Long ").append(idField).append(";\n\n");

        if (fields != null && !fields.isEmpty()) {
            for (Map<String, Object> field : fields) {
                String fieldName = (String) field.get("name");
                String fieldType = (String) field.getOrDefault("type", "String");
                String columnName = NamingConventionHelper.toColumnName(fieldName);

                fieldDefs.append("    @Column(name = \"").append(columnName).append("\")\n");
                fieldDefs.append("    public ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
            }
        }

        // バージョンフィールド（楽観ロック用）
        fieldDefs.append("    @Version\n");
        fieldDefs.append("    @Column(name = \"VERSION\")\n");
        fieldDefs.append("    public Long version;\n");

        String code = """
                package %s.entity;

                import jakarta.persistence.Column;
                import jakarta.persistence.Entity;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.GenerationType;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;
                import jakarta.persistence.Version;

                /**
                 * %sエンティティ。
                 */
                @Entity
                @Table(name = "%s")
                public class %s {

                %s}
                """.formatted(basePackage, name, tableName, name, fieldDefs);

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "entity", name);
        files.add(new GenerationResult.GeneratedFile(filePath, name + ".java", code, "java"));
    }

    private void generateHandler(String name, String basePackage, Map<String, Object> specs,
                                 List<GenerationResult.GeneratedFile> files, List<String> warnings) {
        String className = NamingConventionHelper.toHandlerClassName(name);

        String code = """
                package %s.handler;

                import nablarch.fw.ExecutionContext;
                import nablarch.fw.Handler;

                /**
                 * %sハンドラ。
                 *
                 * @param <I> 入力データ型
                 * @param <O> 出力データ型
                 */
                public class %s<I, O> implements Handler<I, O> {

                    @Override
                    @SuppressWarnings("unchecked")
                    public O handle(I data, ExecutionContext context) {
                        // 前処理
                        // TODO: ハンドラの前処理を実装

                        // 後続ハンドラを実行
                        O result = (O) context.handleNext(data);

                        // 後処理
                        // TODO: ハンドラの後処理を実装

                        return result;
                    }
                }
                """.formatted(basePackage, name, className);

        String filePath = NamingConventionHelper.toJavaFilePath(basePackage, "handler", className);
        files.add(new GenerationResult.GeneratedFile(filePath, className + ".java", code, "java"));
    }

    private void generateInterceptor(String name, String basePackage, Map<String, Object> specs,
                                     List<GenerationResult.GeneratedFile> files, List<String> warnings) {
        String className = NamingConventionHelper.toInterceptorClassName(name);
        String annotationName = name;

        // アノテーションファイル
        String annotationCode = """
                package %s.interceptor;

                import nablarch.fw.Interceptor;

                import java.lang.annotation.Documented;
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                /**
                 * %sインターセプタを適用するアノテーション。
                 */
                @Documented
                @Target(ElementType.METHOD)
                @Retention(RetentionPolicy.RUNTIME)
                @Interceptor(%s.class)
                public @interface %s {
                }
                """.formatted(basePackage, name, className, annotationName);

        String annotationPath = NamingConventionHelper.toJavaFilePath(basePackage, "interceptor", annotationName);
        files.add(new GenerationResult.GeneratedFile(annotationPath, annotationName + ".java", annotationCode, "java"));

        // インターセプタ実装ファイル
        String interceptorCode = """
                package %s.interceptor;

                import nablarch.fw.ExecutionContext;
                import nablarch.fw.Interceptor.Impl;

                /**
                 * %sインターセプタ実装。
                 */
                public class %s extends Impl<%s, Object, Object> {

                    @Override
                    public Object handle(Object data, ExecutionContext context) {
                        // 前処理
                        // TODO: インターセプタの前処理を実装

                        // 元のメソッドを実行
                        Object result = getOriginalHandler().handle(data, context);

                        // 後処理
                        // TODO: インターセプタの後処理を実装

                        return result;
                    }
                }
                """.formatted(basePackage, name, className, annotationName);

        String interceptorPath = NamingConventionHelper.toJavaFilePath(basePackage, "interceptor", className);
        files.add(new GenerationResult.GeneratedFile(interceptorPath, className + ".java", interceptorCode, "java"));
    }

    // ====== ヘルパーメソッド ======

    private String getStringSpec(Map<String, Object> specs, String key, String defaultValue) {
        if (specs == null) {
            return defaultValue;
        }
        Object value = specs.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}

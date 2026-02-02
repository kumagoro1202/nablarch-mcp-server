package com.tis.nablarch.mcp.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tis.nablarch.mcp.knowledge.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Nablarch知識ベース。
 *
 * <p>MCPツール・リソースが参照する知識データの中央リポジトリ。
 * 起動時に静的YAMLファイル（7本）をメモリにロードし、
 * キーワード検索・カテゴリフィルタ・ハンドラキュー検証機能を提供する。</p>
 *
 * <p>Phase 1では単純な文字列マッチングによる検索を実装。
 * Phase 2でRAGエンジンに内部実装を置き換える。</p>
 */
@Component
public class NablarchKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(NablarchKnowledgeBase.class);

    private final ResourceLoader resourceLoader;
    private final String basePath;
    private final ObjectMapper yamlMapper;

    /** アプリタイプ別ハンドラカタログ */
    private Map<String, HandlerCatalogEntry> handlerCatalog = Map.of();
    /** ハンドラ順序制約 */
    private List<HandlerConstraintEntry> handlerConstraints = List.of();
    /** APIパターン */
    private List<ApiPatternEntry> apiPatterns = List.of();
    /** モジュールカタログ */
    private List<ModuleEntry> modules = List.of();
    /** エラーカタログ */
    private List<ErrorEntry> errors = List.of();
    /** 設定テンプレート */
    private List<ConfigTemplateEntry> configTemplates = List.of();
    /** 設計パターン */
    private List<DesignPatternEntry> designPatterns = List.of();

    /** カテゴリ別APIパターンインデックス */
    private Map<String, List<ApiPatternEntry>> patternsByCategoryIndex = Map.of();
    /** ハンドラ名→制約のインデックス */
    private Map<String, HandlerConstraintEntry> constraintsByHandlerIndex = Map.of();
    /** FQCN→モジュールのインデックス */
    private Map<String, ModuleEntry> modulesByFqcnIndex = Map.of();

    /**
     * コンストラクタ。
     *
     * @param resourceLoader Springリソースローダー
     * @param basePath 知識ファイルのベースパス
     */
    public NablarchKnowledgeBase(
            ResourceLoader resourceLoader,
            @Value("${nablarch.mcp.knowledge.base-path:classpath:knowledge/}") String basePath) {
        this.resourceLoader = resourceLoader;
        this.basePath = basePath;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    /**
     * 知識ベースの初期化。全YAMLファイルをロードしインデックスを構築する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    public void initialize() throws IOException {
        log.info("知識ベースの初期化を開始");

        handlerCatalog = loadYaml("handler-catalog.yaml",
                new TypeReference<Map<String, HandlerCatalogEntry>>() {});
        handlerConstraints = loadListYaml("handler-constraints.yaml", "constraints",
                new TypeReference<List<HandlerConstraintEntry>>() {});
        apiPatterns = loadListYaml("api-patterns.yaml", "patterns",
                new TypeReference<List<ApiPatternEntry>>() {});
        modules = loadListYaml("module-catalog.yaml", "modules",
                new TypeReference<List<ModuleEntry>>() {});
        errors = loadListYaml("error-catalog.yaml", "errors",
                new TypeReference<List<ErrorEntry>>() {});
        configTemplates = loadListYaml("config-templates.yaml", "templates",
                new TypeReference<List<ConfigTemplateEntry>>() {});
        designPatterns = loadListYaml("design-patterns.yaml", "patterns",
                new TypeReference<List<DesignPatternEntry>>() {});

        buildIndexes();

        log.info("知識ベース初期化完了: ハンドラカタログ={}タイプ, API={}パターン, 制約={}件, "
                + "モジュール={}件, エラー={}件, テンプレート={}件, 設計パターン={}件",
                handlerCatalog.size(), apiPatterns.size(), handlerConstraints.size(),
                modules.size(), errors.size(), configTemplates.size(), designPatterns.size());
    }

    /**
     * 全カテゴリ横断キーワード検索。
     *
     * @param keyword 検索キーワード
     * @param category カテゴリフィルタ（nullで全カテゴリ検索）
     * @return マッチした結果のフォーマット済みリスト
     */
    public List<String> search(String keyword, String category) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String lowerKeyword = keyword.toLowerCase();
        List<String> results = new ArrayList<>();

        // APIパターン検索
        Stream<ApiPatternEntry> patternStream = apiPatterns.stream();
        if (category != null && !category.isBlank()) {
            patternStream = patternStream.filter(p -> category.equalsIgnoreCase(p.category));
        }
        patternStream
                .filter(p -> matchesKeyword(p, lowerKeyword))
                .forEach(p -> results.add(formatApiPattern(p)));

        // モジュール検索
        if (category == null || category.isBlank() || "library".equalsIgnoreCase(category)
                || "module".equalsIgnoreCase(category)) {
            modules.stream()
                    .filter(m -> matchesKeyword(m, lowerKeyword))
                    .forEach(m -> results.add(formatModule(m)));
        }

        // ハンドラ検索
        if (category == null || category.isBlank() || "handler".equalsIgnoreCase(category)) {
            handlerCatalog.values().stream()
                    .flatMap(c -> c.handlers != null ? c.handlers.stream() : Stream.empty())
                    .filter(h -> matchesKeyword(h, lowerKeyword))
                    .distinct()
                    .forEach(h -> results.add(formatHandler(h)));
        }

        // 設計パターン検索
        if (category == null || category.isBlank()) {
            designPatterns.stream()
                    .filter(d -> matchesKeyword(d, lowerKeyword))
                    .forEach(d -> results.add(formatDesignPattern(d)));
        }

        // エラー検索
        if (category == null || category.isBlank() || "error".equalsIgnoreCase(category)) {
            errors.stream()
                    .filter(e -> matchesKeyword(e, lowerKeyword))
                    .forEach(e -> results.add(formatError(e)));
        }

        return results;
    }

    /**
     * アプリタイプ別ハンドラカタログを取得する。
     *
     * @param applicationType アプリケーションタイプ（web, rest, batch, messaging等）
     * @return ハンドラカタログ情報。タイプが存在しない場合はnull
     */
    public Map<String, Object> getHandlerCatalog(String applicationType) {
        HandlerCatalogEntry entry = handlerCatalog.get(applicationType);
        if (entry == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("description", entry.description);
        result.put("handlers", entry.handlers);
        return result;
    }

    /**
     * API仕様をモジュール名とクラス名で取得する。
     *
     * @param module モジュール名
     * @param className クラス名（単純名またはFQCN）
     * @return API仕様のフォーマット済みテキスト
     */
    public String getApiSpec(String module, String className) {
        Optional<ModuleEntry> moduleEntry = modules.stream()
                .filter(m -> m.name.equalsIgnoreCase(module)
                        || m.artifactId.equalsIgnoreCase(module))
                .findFirst();

        if (moduleEntry.isEmpty()) {
            return "該当するモジュールが見つかりません: " + module;
        }

        ModuleEntry mod = moduleEntry.get();
        if (mod.keyClasses != null && className != null) {
            Optional<ModuleEntry.KeyClassEntry> keyClass = mod.keyClasses.stream()
                    .filter(kc -> kc.fqcn.endsWith(className) || kc.fqcn.equalsIgnoreCase(className))
                    .findFirst();
            if (keyClass.isPresent()) {
                ModuleEntry.KeyClassEntry kc = keyClass.get();
                return String.format("## %s\n- FQCN: %s\n- 説明: %s\n- モジュール: %s (%s)",
                        kc.fqcn, kc.fqcn, kc.description, mod.name, mod.description);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(mod.name).append("\n");
        sb.append("- GroupId: ").append(mod.groupId).append("\n");
        sb.append("- ArtifactId: ").append(mod.artifactId).append("\n");
        sb.append("- 説明: ").append(mod.description).append("\n");
        if (mod.keyClasses != null) {
            sb.append("- 主要クラス:\n");
            for (ModuleEntry.KeyClassEntry kc : mod.keyClasses) {
                sb.append("  - ").append(kc.fqcn).append(": ").append(kc.description).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 特定ハンドラの順序制約を取得する。
     *
     * @param handlerName ハンドラ名
     * @return 制約情報。見つからない場合はnull
     */
    public HandlerConstraintEntry getHandlerConstraints(String handlerName) {
        return constraintsByHandlerIndex.get(handlerName);
    }

    /**
     * ハンドラキューの順序制約を検証する。
     *
     * @param appType アプリケーションタイプ
     * @param handlerNames ハンドラ名のリスト（順序通り）
     * @return 検証結果のフォーマット済みテキスト
     */
    public String validateHandlerQueue(String appType, List<String> handlerNames) {
        List<String> validationErrors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        HandlerCatalogEntry catalog = handlerCatalog.get(appType);
        if (catalog == null) {
            return "エラー: 不明なアプリケーションタイプ: " + appType
                    + "\n有効なタイプ: " + String.join(", ", handlerCatalog.keySet());
        }

        // 必須ハンドラのチェック
        Set<String> providedHandlers = new LinkedHashSet<>(handlerNames);
        if (catalog.handlers != null) {
            catalog.handlers.stream()
                    .filter(h -> h.required)
                    .filter(h -> !providedHandlers.contains(h.name))
                    .forEach(h -> validationErrors.add(
                            "必須ハンドラが不足: " + h.name + " (" + h.description + ")"));
        }

        // 位置マップ構築
        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < handlerNames.size(); i++) {
            positionMap.put(handlerNames.get(i), i);
        }

        // handler-constraints.yaml の制約チェック
        for (String handlerName : handlerNames) {
            HandlerConstraintEntry constraint = constraintsByHandlerIndex.get(handlerName);
            if (constraint == null) continue;
            checkOrderConstraints(handlerName, constraint.mustBefore, constraint.mustAfter,
                    constraint.incompatibleWith, positionMap, providedHandlers,
                    validationErrors, warnings);
        }

        // handler-catalog.yaml のインラインconstraints チェック
        if (catalog.handlers != null) {
            for (HandlerEntry handler : catalog.handlers) {
                if (handler.constraints == null) continue;
                if (!positionMap.containsKey(handler.name)) continue;
                checkOrderConstraints(handler.name,
                        handler.constraints.mustBefore, handler.constraints.mustAfter,
                        null, positionMap, providedHandlers, validationErrors, warnings);
            }
        }

        // 結果の組み立て
        boolean valid = validationErrors.isEmpty();
        StringBuilder result = new StringBuilder();
        result.append("## 検証結果: ").append(valid ? "OK" : "NG").append("\n");
        result.append("アプリタイプ: ").append(appType).append("\n");
        result.append("ハンドラ数: ").append(handlerNames.size()).append("\n\n");

        if (!validationErrors.isEmpty()) {
            result.append("### エラー (").append(validationErrors.size()).append("件)\n");
            validationErrors.forEach(e -> result.append("- ").append(e).append("\n"));
            result.append("\n");
        }
        if (!warnings.isEmpty()) {
            result.append("### 警告 (").append(warnings.size()).append("件)\n");
            warnings.forEach(w -> result.append("- ").append(w).append("\n"));
        }
        if (valid && warnings.isEmpty()) {
            result.append("ハンドラキューの順序は正しいです。\n");
        }
        return result.toString();
    }

    /**
     * 設定テンプレートを名前で取得する。
     *
     * @param name テンプレート名
     * @return フォーマット済みテキスト。見つからない場合はnull
     */
    public String getConfigTemplate(String name) {
        return configTemplates.stream()
                .filter(t -> t.name.equalsIgnoreCase(name))
                .findFirst()
                .map(t -> String.format("## %s\n- カテゴリ: %s\n- 説明: %s\n\n```xml\n%s\n```",
                        t.name, t.category, t.description,
                        t.template != null ? t.template.trim() : ""))
                .orElse(null);
    }

    /**
     * 設計パターンを名前で取得する。
     *
     * @param name パターン名
     * @return フォーマット済みテキスト。見つからない場合はnull
     */
    public String getDesignPattern(String name) {
        return designPatterns.stream()
                .filter(d -> d.name.equalsIgnoreCase(name))
                .findFirst()
                .map(this::formatDesignPatternDetail)
                .orElse(null);
    }

    /**
     * 利用可能なアプリケーションタイプの一覧を返す。
     *
     * @return アプリタイプのセット
     */
    public Set<String> getAvailableAppTypes() {
        return Collections.unmodifiableSet(handlerCatalog.keySet());
    }

    // ========== 内部: YAMLロード ==========

    private <T> T loadYaml(String filename, TypeReference<T> typeRef) throws IOException {
        String resourcePath = basePath + filename;
        Resource resource = resourceLoader.getResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            T result = yamlMapper.readValue(is, typeRef);
            log.info("YAMLロード完了: {}", filename);
            return result;
        }
    }

    private <T> List<T> loadListYaml(String filename, String rootKey,
            TypeReference<List<T>> typeRef) throws IOException {
        String resourcePath = basePath + filename;
        Resource resource = resourceLoader.getResource(resourcePath);
        try (InputStream is = resource.getInputStream()) {
            Map<String, Object> root = yamlMapper.readValue(is,
                    new TypeReference<Map<String, Object>>() {});
            Object listObj = root.get(rootKey);
            if (listObj == null) {
                log.warn("YAML {} にキー '{}' なし", filename, rootKey);
                return List.of();
            }
            String json = yamlMapper.writeValueAsString(listObj);
            List<T> result = yamlMapper.readValue(json, typeRef);
            log.info("YAMLロード完了: {} ({}件)", filename, result.size());
            return result;
        }
    }

    // ========== 内部: インデックス構築 ==========

    private void buildIndexes() {
        patternsByCategoryIndex = apiPatterns.stream()
                .filter(p -> p.category != null)
                .collect(Collectors.groupingBy(p -> p.category.toLowerCase()));

        constraintsByHandlerIndex = new HashMap<>();
        for (HandlerConstraintEntry c : handlerConstraints) {
            constraintsByHandlerIndex.put(c.handler, c);
        }

        modulesByFqcnIndex = new HashMap<>();
        for (ModuleEntry m : modules) {
            if (m.keyClasses != null) {
                for (ModuleEntry.KeyClassEntry kc : m.keyClasses) {
                    modulesByFqcnIndex.put(kc.fqcn, m);
                }
            }
        }
    }

    // ========== 内部: 順序制約チェック ==========

    private void checkOrderConstraints(String handlerName,
            List<String> mustBefore, List<String> mustAfter, List<String> incompatibleWith,
            Map<String, Integer> positionMap, Set<String> providedHandlers,
            List<String> errors, List<String> warnings) {
        Integer currentPos = positionMap.get(handlerName);
        if (currentPos == null) return;

        if (mustBefore != null) {
            for (String target : mustBefore) {
                Integer targetPos = positionMap.get(target);
                if (targetPos != null && currentPos >= targetPos) {
                    String msg = String.format("順序違反: %s (位置%d) は %s (位置%d) より前に配置すべき",
                            handlerName, currentPos + 1, target, targetPos + 1);
                    if (!errors.contains(msg)) errors.add(msg);
                }
            }
        }
        if (mustAfter != null) {
            for (String target : mustAfter) {
                Integer targetPos = positionMap.get(target);
                if (targetPos != null && currentPos <= targetPos) {
                    String msg = String.format("順序違反: %s (位置%d) は %s (位置%d) より後に配置すべき",
                            handlerName, currentPos + 1, target, targetPos + 1);
                    if (!errors.contains(msg)) errors.add(msg);
                }
            }
        }
        if (incompatibleWith != null) {
            for (String incompatible : incompatibleWith) {
                if (providedHandlers.contains(incompatible)) {
                    warnings.add(String.format("互換性警告: %s と %s は同時使用非推奨",
                            handlerName, incompatible));
                }
            }
        }
    }

    // ========== 内部: キーワードマッチング ==========

    private boolean matchesKeyword(ApiPatternEntry p, String kw) {
        return ci(p.name, kw) || ci(p.description, kw) || ci(p.fqcn, kw) || ci(p.category, kw);
    }

    private boolean matchesKeyword(ModuleEntry m, String kw) {
        return ci(m.name, kw) || ci(m.description, kw) || ci(m.artifactId, kw)
                || (m.keyClasses != null && m.keyClasses.stream()
                        .anyMatch(k -> ci(k.fqcn, kw) || ci(k.description, kw)));
    }

    private boolean matchesKeyword(HandlerEntry h, String kw) {
        return ci(h.name, kw) || ci(h.description, kw) || ci(h.fqcn, kw);
    }

    private boolean matchesKeyword(DesignPatternEntry d, String kw) {
        return ci(d.name, kw) || ci(d.description, kw) || ci(d.problem, kw) || ci(d.category, kw);
    }

    private boolean matchesKeyword(ErrorEntry e, String kw) {
        return ci(e.id, kw) || ci(e.errorMessage, kw) || ci(e.cause, kw) || ci(e.category, kw);
    }

    private static boolean ci(String text, String kw) {
        return text != null && text.toLowerCase().contains(kw);
    }

    // ========== 内部: フォーマッタ ==========

    private String formatApiPattern(ApiPatternEntry p) {
        return String.format("[APIパターン] %s (%s) — %s | FQCN: %s",
                p.name, p.category, p.description, p.fqcn != null ? p.fqcn : "N/A");
    }

    private String formatModule(ModuleEntry m) {
        return String.format("[モジュール] %s (%s) — %s", m.name, m.category, m.description);
    }

    private String formatHandler(HandlerEntry h) {
        return String.format("[ハンドラ] %s — %s | FQCN: %s | 必須: %s",
                h.name, h.description, h.fqcn, h.required ? "はい" : "いいえ");
    }

    private String formatDesignPattern(DesignPatternEntry d) {
        return String.format("[設計パターン] %s (%s) — %s", d.name, d.category, d.description);
    }

    private String formatError(ErrorEntry e) {
        return String.format("[エラー] %s (%s/%s) — %s",
                e.id, e.category, e.severity, e.errorMessage);
    }

    private String formatDesignPatternDetail(DesignPatternEntry d) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(d.name).append("\n");
        sb.append("- カテゴリ: ").append(d.category).append("\n");
        sb.append("- 説明: ").append(d.description).append("\n");
        if (d.problem != null) sb.append("- 問題: ").append(d.problem).append("\n");
        if (d.solution != null) sb.append("- 解決策: ").append(d.solution).append("\n");
        if (d.codeExample != null) {
            sb.append("\n```java\n").append(d.codeExample.trim()).append("\n```\n");
        }
        return sb.toString();
    }
}

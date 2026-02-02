package com.tis.nablarch.mcp.rag.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nablarch固有の同義語マップ。
 *
 * <p>日本語→英語、英語→日本語の双方向マッピングを提供する。
 * クエリ拡張時に、ユーザーが使用した用語の同義語を検索クエリに追加することで、
 * BM25検索の再現率（Recall）を向上させる。</p>
 *
 * <p>将来的にはYAMLファイルへの外部化を想定し、
 * マップの差し替えが容易な設計としている。</p>
 */
public class NablarchSynonymMap {

    private final Map<String, List<String>> synonyms;

    /**
     * デフォルトのNablarch同義語マップを構築する。
     */
    public NablarchSynonymMap() {
        this(buildDefaultSynonyms());
    }

    /**
     * カスタム同義語マップで構築する。
     *
     * @param synonyms 同義語マップ（キー→同義語リスト）
     */
    public NablarchSynonymMap(Map<String, List<String>> synonyms) {
        this.synonyms = Collections.unmodifiableMap(new HashMap<>(synonyms));
    }

    /**
     * 指定用語の同義語リストを返す。
     *
     * <p>大文字小文字を無視して検索する。
     * 同義語が見つからない場合は空リストを返す。</p>
     *
     * @param term 検索用語
     * @return 同義語リスト（用語自体は含まない）
     */
    public List<String> getSynonyms(String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        String lowerTerm = term.toLowerCase();
        for (Map.Entry<String, List<String>> entry : synonyms.entrySet()) {
            if (entry.getKey().toLowerCase().equals(lowerTerm)) {
                return entry.getValue();
            }
            if (entry.getValue().stream().anyMatch(s -> s.toLowerCase().equals(lowerTerm))) {
                // 逆引き: 同義語リスト中に見つかった場合、キー＋他の同義語を返す
                List<String> result = new java.util.ArrayList<>();
                result.add(entry.getKey());
                entry.getValue().stream()
                        .filter(s -> !s.toLowerCase().equals(lowerTerm))
                        .forEach(result::add);
                return result;
            }
        }
        return List.of();
    }

    /**
     * 同義語マップ全体を返す。
     *
     * @return 不変の同義語マップ
     */
    public Map<String, List<String>> getAll() {
        return synonyms;
    }

    /**
     * デフォルトのNablarch固有同義語マップを構築する。
     */
    private static Map<String, List<String>> buildDefaultSynonyms() {
        Map<String, List<String>> map = new HashMap<>();

        // データベースアクセス
        map.put("DB接続", List.of("universal-dao", "UniversalDao", "データベースアクセス", "database access"));
        map.put("universal-dao", List.of("UniversalDao", "データベースアクセス", "DB接続", "nablarch-common-dao"));

        // バリデーション
        map.put("バリデーション", List.of("validation", "nablarch-core-validation", "BeanValidation", "入力チェック"));
        map.put("validation", List.of("バリデーション", "nablarch-core-validation", "入力チェック"));

        // REST API
        map.put("REST", List.of("JAX-RS", "RESTful", "nablarch-fw-jaxrs", "JaxRsResponseHandler"));
        map.put("JAX-RS", List.of("REST", "RESTful", "nablarch-fw-jaxrs"));

        // ハンドラ
        map.put("ハンドラ", List.of("Handler", "handler queue", "ハンドラキュー"));
        map.put("Handler", List.of("ハンドラ", "handler queue", "ハンドラキュー"));
        map.put("ハンドラキュー", List.of("handler queue", "Handler", "ハンドラ"));

        // リポジトリ
        map.put("システムリポジトリ", List.of("SystemRepository", "system repository", "コンポーネント定義", "DI"));
        map.put("SystemRepository", List.of("システムリポジトリ", "system repository", "コンポーネント定義"));

        // メッセージング
        map.put("メッセージング", List.of("messaging", "MOM", "nablarch-fw-messaging", "キュー"));
        map.put("messaging", List.of("メッセージング", "MOM", "nablarch-fw-messaging"));

        // バッチ
        map.put("バッチ", List.of("batch", "nablarch-fw-batch", "バッチ処理", "JSR352"));
        map.put("batch", List.of("バッチ", "nablarch-fw-batch", "バッチ処理"));

        // ログ
        map.put("ログ", List.of("log", "logging", "nablarch-core-log", "ログ出力"));
        map.put("log", List.of("ログ", "logging", "nablarch-core-log"));

        // アクション
        map.put("アクション", List.of("Action", "action class", "業務アクション"));
        map.put("Action", List.of("アクション", "action class", "業務アクション"));

        // 設定
        map.put("設定", List.of("configuration", "config", "設定ファイル", "コンポーネント定義"));
        map.put("configuration", List.of("設定", "config", "設定ファイル"));

        // テスト
        map.put("テスト", List.of("test", "testing", "nablarch-testing", "単体テスト"));
        map.put("test", List.of("テスト", "testing", "nablarch-testing"));

        // Webアプリ
        map.put("Web", List.of("web application", "nablarch-fw-web", "Webアプリケーション", "HttpRequest"));
        map.put("web application", List.of("Web", "nablarch-fw-web", "Webアプリケーション"));

        // 排他制御
        map.put("排他制御", List.of("exclusive control", "楽観ロック", "optimistic lock", "悲観ロック"));
        map.put("楽観ロック", List.of("optimistic lock", "排他制御", "exclusive control"));

        // コード管理
        map.put("コード管理", List.of("CodeManager", "code management", "コードマスタ"));
        map.put("CodeManager", List.of("コード管理", "code management", "コードマスタ"));

        return map;
    }
}

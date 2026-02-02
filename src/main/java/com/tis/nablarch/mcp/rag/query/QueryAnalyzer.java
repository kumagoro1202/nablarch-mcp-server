package com.tis.nablarch.mcp.rag.query;

import com.tis.nablarch.mcp.rag.search.SearchFilters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * クエリ解析・拡張エンジン。
 *
 * <p>ユーザークエリを解析し、以下の処理を行う:</p>
 * <ol>
 *   <li>言語検出（日本語/英語/混在）</li>
 *   <li>Nablarchドメインエンティティの抽出（FQCN、ハンドラ名、モジュール名等）</li>
 *   <li>同義語展開によるクエリ拡張</li>
 *   <li>エンティティに基づくSearchFiltersの推定</li>
 * </ol>
 *
 * <p>ハイブリッド検索の前段に位置し、検索精度の向上に寄与する。</p>
 */
@Service
public class QueryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalyzer.class);

    // --- エンティティ抽出用正規表現 ---

    /**
     * FQCNパターン: nablarch.xxx.yyy.ZzzClass
     */
    private static final Pattern FQCN_PATTERN = Pattern.compile(
            "(?:nablarch|jp\\.co\\.tis)(?:\\.[a-z][a-z0-9]*)+\\.[A-Z][a-zA-Z0-9]*");

    /**
     * ハンドラ名パターン: XxxHandler（PascalCase + Handler接尾辞）
     */
    private static final Pattern HANDLER_PATTERN = Pattern.compile(
            "\\b[A-Z][a-zA-Z0-9]*Handler\\b");

    /**
     * モジュール名パターン: nablarch-xxx-yyy
     */
    private static final Pattern MODULE_PATTERN = Pattern.compile(
            "\\bnablarch(?:-[a-z][a-z0-9]*)+\\b");

    /**
     * 設定ファイルパターン: xxx-configuration.xml, component-configuration 等
     */
    private static final Pattern CONFIG_FILE_PATTERN = Pattern.compile(
            "\\b[a-z][a-z0-9]*(?:-[a-z][a-z0-9]*)*(?:-configuration|\\.xml)\\b");

    /**
     * アプリタイプキーワードマッピング。
     */
    private static final java.util.Map<String, String> APP_TYPE_KEYWORDS = java.util.Map.ofEntries(
            java.util.Map.entry("web", "web"),
            java.util.Map.entry("rest", "rest"),
            java.util.Map.entry("jax-rs", "rest"),
            java.util.Map.entry("jaxrs", "rest"),
            java.util.Map.entry("restful", "rest"),
            java.util.Map.entry("batch", "batch"),
            java.util.Map.entry("バッチ", "batch"),
            java.util.Map.entry("messaging", "messaging"),
            java.util.Map.entry("メッセージング", "messaging"),
            java.util.Map.entry("jakarta-batch", "jakarta-batch"),
            java.util.Map.entry("http-messaging", "http-messaging")
    );

    /**
     * 日本語比率がこれ以上ならJAPANESE判定。
     */
    private static final double JAPANESE_THRESHOLD = 0.70;

    /**
     * 日本語比率がこれ未満ならENGLISH判定。
     */
    private static final double ENGLISH_THRESHOLD = 0.10;

    private final NablarchSynonymMap synonymMap;

    /**
     * コンストラクタ。デフォルトの同義語マップを使用する。
     */
    public QueryAnalyzer() {
        this(new NablarchSynonymMap());
    }

    /**
     * コンストラクタ。カスタム同義語マップを使用する。
     *
     * @param synonymMap 同義語マップ
     */
    public QueryAnalyzer(NablarchSynonymMap synonymMap) {
        this.synonymMap = synonymMap;
    }

    /**
     * クエリを解析して拡張結果を返す。
     *
     * @param query ユーザークエリ
     * @return 解析結果
     * @throws IllegalArgumentException queryがnullまたは空白の場合
     */
    public AnalyzedQuery analyze(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("クエリはnullまたは空白であってはならない");
        }

        log.debug("クエリ解析開始: '{}'", query);

        // 1. 言語検出
        QueryLanguage language = detectLanguage(query);

        // 2. エンティティ抽出
        List<String> entities = extractEntities(query);

        // 3. 同義語展開
        String expandedQuery = expandQuery(query);

        // 4. フィルタ推定
        SearchFilters suggestedFilters = suggestFilters(query, entities);

        AnalyzedQuery result = new AnalyzedQuery(
                query, expandedQuery, language, entities, suggestedFilters);

        log.debug("クエリ解析完了: language={}, entities={}, expanded='{}'",
                language, entities, expandedQuery);

        return result;
    }

    /**
     * クエリの言語を検出する。
     *
     * <p>日本語文字（ひらがな、カタカナ、漢字）の比率に基づいて判定する。</p>
     *
     * @param query クエリ文字列
     * @return 検出された言語
     */
    QueryLanguage detectLanguage(String query) {
        double ratio = calculateJapaneseRatio(query);

        if (ratio >= JAPANESE_THRESHOLD) {
            return QueryLanguage.JAPANESE;
        } else if (ratio < ENGLISH_THRESHOLD) {
            return QueryLanguage.ENGLISH;
        } else {
            return QueryLanguage.MIXED;
        }
    }

    /**
     * 日本語文字の比率を計算する。
     *
     * <p>ひらがな（\u3040-\u309F）、カタカナ（\u30A0-\u30FF）、
     * 漢字（\u4E00-\u9FFF）の文字数を全文字数で割った比率を返す。</p>
     *
     * @param text テキスト
     * @return 日本語文字比率（0.0〜1.0）
     */
    double calculateJapaneseRatio(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        long totalChars = text.codePoints()
                .filter(cp -> !Character.isWhitespace(cp))
                .count();

        if (totalChars == 0) {
            return 0.0;
        }

        long japaneseChars = text.codePoints()
                .filter(this::isJapanese)
                .count();

        return (double) japaneseChars / totalChars;
    }

    /**
     * Unicode codepoint が日本語文字かどうかを判定する。
     *
     * @param codePoint Unicode codepoint
     * @return 日本語文字の場合true
     */
    private boolean isJapanese(int codePoint) {
        return (codePoint >= 0x3040 && codePoint <= 0x309F)   // ひらがな
                || (codePoint >= 0x30A0 && codePoint <= 0x30FF) // カタカナ
                || (codePoint >= 0x4E00 && codePoint <= 0x9FFF) // CJK統合漢字
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF) // CJK統合漢字拡張A
                || (codePoint >= 0xFF66 && codePoint <= 0xFF9D); // 半角カタカナ
    }

    /**
     * Nablarchドメインエンティティを抽出する。
     *
     * <p>以下のパターンを正規表現で抽出する:</p>
     * <ul>
     *   <li>FQCN（nablarch.xxx.yyy.ZzzClass）</li>
     *   <li>ハンドラ名（XxxHandler）</li>
     *   <li>モジュール名（nablarch-xxx-yyy）</li>
     *   <li>設定ファイル名（xxx-configuration.xml等）</li>
     * </ul>
     *
     * @param query クエリ文字列
     * @return 抽出エンティティのリスト（重複なし、検出順）
     */
    List<String> extractEntities(String query) {
        Set<String> entities = new LinkedHashSet<>();

        extractByPattern(query, FQCN_PATTERN, entities);
        extractByPattern(query, HANDLER_PATTERN, entities);
        extractByPattern(query, MODULE_PATTERN, entities);
        extractByPattern(query, CONFIG_FILE_PATTERN, entities);

        return new ArrayList<>(entities);
    }

    /**
     * 正規表現パターンでマッチするエンティティを抽出し、セットに追加する。
     *
     * @param text 検索対象テキスト
     * @param pattern 正規表現パターン
     * @param results 結果セット
     */
    private void extractByPattern(String text, Pattern pattern, Set<String> results) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            results.add(matcher.group());
        }
    }

    /**
     * 同義語展開によるクエリ拡張を行う。
     *
     * <p>クエリ中の各トークンについて同義語マップを検索し、
     * 見つかった同義語をクエリに追加する。
     * 元のクエリは保持し、同義語をスペース区切りで追記する。</p>
     *
     * @param query 元のクエリ
     * @return 同義語展開後のクエリ
     */
    String expandQuery(String query) {
        Set<String> expansions = new LinkedHashSet<>();

        // スペース区切りのトークン単位で同義語を検索
        String[] tokens = query.split("\\s+");
        for (String token : tokens) {
            List<String> synonyms = synonymMap.getSynonyms(token);
            expansions.addAll(synonyms);
        }

        // 元クエリ全体でも検索（複合語対応）
        List<String> fullQuerySynonyms = synonymMap.getSynonyms(query.trim());
        expansions.addAll(fullQuerySynonyms);

        if (expansions.isEmpty()) {
            return query;
        }

        return query + " " + String.join(" ", expansions);
    }

    /**
     * 抽出エンティティとクエリ内容からSearchFiltersを推定する。
     *
     * <p>モジュール名やアプリタイプのキーワードが含まれる場合、
     * 対応するフィルタを設定する。</p>
     *
     * @param query クエリ文字列
     * @param entities 抽出エンティティ
     * @return 推定されたSearchFilters
     */
    SearchFilters suggestFilters(String query, List<String> entities) {
        String detectedModule = null;
        String detectedAppType = null;

        // エンティティからモジュール名を検出
        for (String entity : entities) {
            if (MODULE_PATTERN.matcher(entity).matches()) {
                detectedModule = entity;
                break;
            }
        }

        // クエリからアプリタイプを検出
        String lowerQuery = query.toLowerCase();
        for (var entry : APP_TYPE_KEYWORDS.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                detectedAppType = entry.getValue();
                break;
            }
        }

        if (detectedModule == null && detectedAppType == null) {
            return SearchFilters.NONE;
        }

        return new SearchFilters(detectedAppType, detectedModule, null, null, null);
    }
}

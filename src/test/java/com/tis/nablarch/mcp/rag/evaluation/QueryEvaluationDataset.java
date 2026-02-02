package com.tis.nablarch.mcp.rag.evaluation;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * YAML形式の評価データセットローダー。
 *
 * <p>evaluation-queries.yamlから評価クエリを読み込み、
 * {@link EvaluationQuery}のリストとして提供する。</p>
 */
public class QueryEvaluationDataset {

    private final List<EvaluationQuery> queries;

    /**
     * デフォルトのデータセットファイルから読み込む。
     */
    public QueryEvaluationDataset() {
        this("testdata/evaluation/evaluation-queries.yaml");
    }

    /**
     * 指定パスのYAMLファイルから読み込む。
     *
     * @param resourcePath クラスパス上のリソースパス
     */
    public QueryEvaluationDataset(String resourcePath) {
        this.queries = loadFromResource(resourcePath);
    }

    /**
     * 全評価クエリを返す。
     *
     * @return 評価クエリのリスト
     */
    public List<EvaluationQuery> getQueries() {
        return queries;
    }

    /**
     * 指定カテゴリのクエリを返す。
     *
     * @param category カテゴリ名
     * @return 該当カテゴリの評価クエリリスト
     */
    public List<EvaluationQuery> getQueriesByCategory(String category) {
        return queries.stream()
                .filter(q -> category.equals(q.category()))
                .toList();
    }

    /**
     * クエリ件数を返す。
     *
     * @return クエリ件数
     */
    public int size() {
        return queries.size();
    }

    /**
     * YAMLリソースからクエリを読み込む。
     */
    @SuppressWarnings("unchecked")
    private static List<EvaluationQuery> loadFromResource(String resourcePath) {
        Yaml yaml = new Yaml();
        InputStream inputStream = QueryEvaluationDataset.class.getClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("評価データセットが見つからない: " + resourcePath);
        }

        Map<String, Object> data = yaml.load(inputStream);
        List<Map<String, Object>> queryList = (List<Map<String, Object>>) data.get("queries");

        if (queryList == null) {
            return List.of();
        }

        List<EvaluationQuery> result = new ArrayList<>();
        for (Map<String, Object> entry : queryList) {
            String id = (String) entry.get("id");
            String query = (String) entry.get("query");
            String category = (String) entry.get("category");
            String language = (String) entry.get("language");
            List<String> relevantKeywords = (List<String>) entry.get("relevant_keywords");
            List<String> expectedTopSources = (List<String>) entry.get("expected_top_sources");

            result.add(new EvaluationQuery(
                    id, query, category, language,
                    relevantKeywords != null ? relevantKeywords : List.of(),
                    expectedTopSources != null ? expectedTopSources : List.of()
            ));
        }
        return List.copyOf(result);
    }

    /**
     * 評価クエリを表すレコード。
     *
     * @param id クエリID（Q001等）
     * @param query クエリ文字列
     * @param category カテゴリ（handler_queue, api_usage等）
     * @param language 言語（JAPANESE, ENGLISH, MIXED）
     * @param relevantKeywords 関連性判定キーワードリスト
     * @param expectedTopSources 期待されるトップソース
     */
    public record EvaluationQuery(
            String id,
            String query,
            String category,
            String language,
            List<String> relevantKeywords,
            List<String> expectedTopSources
    ) {}
}

package com.tis.nablarch.mcp.rag.evaluation;

import com.tis.nablarch.mcp.rag.search.SearchResult;

import java.util.List;
import java.util.Set;

/**
 * 検索品質評価メトリクス計算ユーティリティ。
 *
 * <p>MRR、Recall@K、NDCG@Kの3種のメトリクスを提供する。
 * 関連性の判定は、検索結果のcontentにいずれかのキーワードが
 * 含まれるかどうかで行う。</p>
 */
public final class EvaluationMetrics {

    private EvaluationMetrics() {
        // ユーティリティクラス
    }

    /**
     * MRR（Mean Reciprocal Rank）を計算する。
     *
     * <p>最初の関連ドキュメントの逆順位を返す。
     * 関連ドキュメントが見つからない場合は0.0を返す。</p>
     *
     * <p>計算式: MRR = 1 / rank（最初の関連ドキュメントの順位）</p>
     *
     * @param results 検索結果リスト（順位順）
     * @param relevantKeywords 関連性判定キーワード集合
     * @return MRR値（0.0〜1.0）
     */
    public static double calculateMRR(List<SearchResult> results, Set<String> relevantKeywords) {
        if (results == null || results.isEmpty() || relevantKeywords == null || relevantKeywords.isEmpty()) {
            return 0.0;
        }

        for (int i = 0; i < results.size(); i++) {
            if (isRelevant(results.get(i), relevantKeywords)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /**
     * Recall@Kを計算する。
     *
     * <p>上位K件中に関連キーワードを含むドキュメントが
     * 1件以上存在するかどうかを返す（バイナリRecall）。</p>
     *
     * <p>関連ドキュメントの正確な件数が不明なため、
     * 少なくとも1件の関連ドキュメントが含まれるかを評価する。</p>
     *
     * @param results 検索結果リスト（順位順）
     * @param relevantKeywords 関連性判定キーワード集合
     * @param k 上位K件
     * @return Recall値（0.0または1.0）
     */
    public static double calculateRecallAtK(List<SearchResult> results, Set<String> relevantKeywords, int k) {
        if (results == null || results.isEmpty() || relevantKeywords == null || relevantKeywords.isEmpty() || k <= 0) {
            return 0.0;
        }

        int limit = Math.min(k, results.size());
        int relevantCount = 0;

        for (int i = 0; i < limit; i++) {
            if (isRelevant(results.get(i), relevantKeywords)) {
                relevantCount++;
            }
        }

        // バイナリRecall: 関連ドキュメントが1件以上あれば1.0
        return relevantCount > 0 ? 1.0 : 0.0;
    }

    /**
     * 上位K件中の関連ドキュメント数を計算する。
     *
     * @param results 検索結果リスト（順位順）
     * @param relevantKeywords 関連性判定キーワード集合
     * @param k 上位K件
     * @return 関連ドキュメント数
     */
    public static int countRelevantAtK(List<SearchResult> results, Set<String> relevantKeywords, int k) {
        if (results == null || results.isEmpty() || relevantKeywords == null || relevantKeywords.isEmpty() || k <= 0) {
            return 0;
        }

        int limit = Math.min(k, results.size());
        int count = 0;

        for (int i = 0; i < limit; i++) {
            if (isRelevant(results.get(i), relevantKeywords)) {
                count++;
            }
        }
        return count;
    }

    /**
     * NDCG@K（Normalized Discounted Cumulative Gain）を計算する。
     *
     * <p>順位重み付き適合度を計算する。関連ドキュメントが
     * 上位に集中しているほど高いスコアとなる。</p>
     *
     * <p>計算式:</p>
     * <ul>
     *   <li>DCG@K = Σ rel_i / log2(i + 2)  (i=0始まり)</li>
     *   <li>IDCG@K = 理想的な順序でのDCG</li>
     *   <li>NDCG@K = DCG@K / IDCG@K</li>
     * </ul>
     *
     * <p>関連度はバイナリ（関連=1、非関連=0）で評価する。</p>
     *
     * @param results 検索結果リスト（順位順）
     * @param relevantKeywords 関連性判定キーワード集合
     * @param k 上位K件
     * @return NDCG値（0.0〜1.0）
     */
    public static double calculateNDCG(List<SearchResult> results, Set<String> relevantKeywords, int k) {
        if (results == null || results.isEmpty() || relevantKeywords == null || relevantKeywords.isEmpty() || k <= 0) {
            return 0.0;
        }

        int limit = Math.min(k, results.size());

        // DCG計算
        double dcg = 0.0;
        int totalRelevant = 0;

        for (int i = 0; i < limit; i++) {
            double rel = isRelevant(results.get(i), relevantKeywords) ? 1.0 : 0.0;
            if (rel > 0) {
                totalRelevant++;
            }
            dcg += rel / log2(i + 2); // i+2 because log2(1)=0 and we want log2(2)=1 for rank 1
        }

        if (totalRelevant == 0) {
            return 0.0;
        }

        // IDCG計算（理想的な順序: 全関連ドキュメントが上位に集中）
        double idcg = 0.0;
        for (int i = 0; i < Math.min(totalRelevant, limit); i++) {
            idcg += 1.0 / log2(i + 2);
        }

        if (idcg == 0.0) {
            return 0.0;
        }

        return dcg / idcg;
    }

    /**
     * 検索結果が関連キーワードを含むかを判定する。
     *
     * <p>contentの小文字化テキストに、いずれかのキーワード（小文字化）が
     * 含まれていれば関連と判定する。</p>
     *
     * @param result 検索結果
     * @param relevantKeywords 関連キーワード集合
     * @return 関連している場合true
     */
    public static boolean isRelevant(SearchResult result, Set<String> relevantKeywords) {
        if (result == null || result.content() == null || relevantKeywords == null) {
            return false;
        }

        String contentLower = result.content().toLowerCase();
        for (String keyword : relevantKeywords) {
            if (keyword != null && contentLower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * log2を計算する。
     *
     * @param x 入力値
     * @return log2(x)
     */
    static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}

package com.tis.nablarch.mcp.rag.rerank;

import com.tis.nablarch.mcp.rag.search.SearchResult;

import java.util.List;

/**
 * リランキングインターフェース。
 *
 * <p>ハイブリッド検索結果をCross-Encoderモデルで再順位付けし、
 * クエリとの関連度が高い順にソートされた結果を返す。</p>
 *
 * @see CrossEncoderReranker
 */
public interface Reranker {

    /**
     * 検索結果をリランキングする。
     *
     * <p>クエリと各候補ドキュメントのペアをCross-Encoderで評価し、
     * 関連度スコアの高い順にソートしてtopK件を返す。</p>
     *
     * @param query ユーザークエリ
     * @param candidates ハイブリッド検索結果の候補リスト
     * @param topK 返却する上位件数
     * @return リランキング済みの検索結果（スコア降順、最大topK件）
     */
    List<SearchResult> rerank(String query, List<SearchResult> candidates, int topK);
}

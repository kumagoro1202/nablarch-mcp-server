package com.tis.nablarch.mcp.rag.ingestion;

import java.time.Instant;

/**
 * ドキュメント取込インターフェース。
 *
 * <p>各データソース（Nablarch公式ドキュメント、Fintan技術記事、
 * GitHubリポジトリ等）からのドキュメント取込パイプラインが
 * 本インターフェースを実装する。</p>
 *
 * <p>パイプライン処理: 取得→パース→チャンキング→Embedding→格納</p>
 */
public interface DocumentIngester {

    /**
     * 全ドキュメントを取り込む（フル取込）。
     *
     * <p>データソースの全対象ドキュメントを取得し、
     * パース→チャンキング→Embedding→格納の全工程を実行する。</p>
     *
     * @return 取込結果
     */
    IngestionResult ingestAll();

    /**
     * 指定時刻以降に更新されたドキュメントのみを取り込む（増分取込）。
     *
     * <p>前回取込以降に追加・更新されたドキュメントのみを対象とし、
     * 既存チャンクの更新又は新規チャンクの追加を行う。</p>
     *
     * @param since この時刻以降の更新分を取り込む
     * @return 取込結果
     */
    IngestionResult ingestIncremental(Instant since);

    /**
     * データソース名を返す。
     *
     * @return データソース識別名（例: "fintan", "nablarch-document"）
     */
    String getSourceName();
}

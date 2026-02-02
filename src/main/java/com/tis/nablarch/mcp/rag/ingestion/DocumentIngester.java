package com.tis.nablarch.mcp.rag.ingestion;

import java.time.Instant;

/**
 * ドキュメント取り込みインターフェース。
 *
 * <p>各データソース（公式Docs、Fintan、GitHub等）ごとに実装し、
 * ドキュメントの取得→パース→チャンキング→Embedding→格納の
 * 一連のパイプラインを統一的に扱う。</p>
 */
public interface DocumentIngester {

    /**
     * 全件取り込み（初回）。
     *
     * <p>データソースの全ドキュメントをクロールし、
     * パイプラインを通じてベクトルDBに格納する。</p>
     *
     * @return 取り込み結果
     */
    IngestionResult ingestAll();

    /**
     * 増分取り込み。
     *
     * <p>指定時刻以降に更新されたドキュメントのみを取り込む。</p>
     *
     * @param since この時刻以降に更新されたドキュメントを対象とする
     * @return 取り込み結果
     */
    IngestionResult ingestIncremental(Instant since);

    /**
     * データソース名を返す。
     *
     * @return データソース名（例: "nablarch-official-docs"）
     */
    String getSourceName();
}

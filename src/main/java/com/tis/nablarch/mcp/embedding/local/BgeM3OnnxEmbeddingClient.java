package com.tis.nablarch.mcp.embedding.local;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * BAAI/bge-m3 ONNXモデルによるEmbeddingクライアント。
 *
 * <p>ドキュメント用Embeddingモデル。568Mパラメータ、100+言語対応、8192トークン。
 * ONNX Runtimeでローカル推論を実行し、1024次元のベクトルを生成する。</p>
 *
 * <h3>モデル仕様</h3>
 * <ul>
 *   <li>モデル名: BAAI/bge-m3</li>
 *   <li>パラメータ数: 568M</li>
 *   <li>次元数: 1024</li>
 *   <li>コンテキスト長: 8192トークン</li>
 *   <li>多言語対応: 100+言語（日本語含む）</li>
 *   <li>ライセンス: MIT</li>
 * </ul>
 *
 * <h3>有効化条件</h3>
 * <p>{@code nablarch.mcp.embedding.provider=local} の場合に有効化される。</p>
 *
 * @see <a href="https://huggingface.co/BAAI/bge-m3">BAAI/bge-m3 on HuggingFace</a>
 */
@Component
@Qualifier("document")
@ConditionalOnProperty(name = "nablarch.mcp.embedding.provider", havingValue = "local", matchIfMissing = true)
public class BgeM3OnnxEmbeddingClient extends AbstractOnnxEmbeddingClient {

    /**
     * コンストラクタ。
     *
     * @param properties Embedding設定プロパティ
     */
    public BgeM3OnnxEmbeddingClient(EmbeddingProperties properties) {
        super(properties.getLocal().getDocument());
    }
}

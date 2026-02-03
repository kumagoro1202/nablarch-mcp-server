package com.tis.nablarch.mcp.embedding.local;

import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * codesage/codesage-small-v2 ONNXモデルによるEmbeddingクライアント。
 *
 * <p>コード用Embeddingモデル。130Mパラメータ、9言語（Java含む）対応。
 * ONNX Runtimeでローカル推論を実行し、1024次元のベクトルを生成する。</p>
 *
 * <h3>モデル仕様</h3>
 * <ul>
 *   <li>モデル名: codesage/codesage-small-v2</li>
 *   <li>パラメータ数: 130M</li>
 *   <li>次元数: 1024</li>
 *   <li>対応言語: Java, Python, Go, C, C#, JavaScript, TypeScript, PHP, Ruby</li>
 *   <li>ライセンス: OSS</li>
 * </ul>
 *
 * <h3>有効化条件</h3>
 * <p>{@code nablarch.mcp.embedding.provider=local} の場合に有効化される。</p>
 *
 * @see <a href="https://huggingface.co/codesage/codesage-small-v2">codesage-small-v2 on HuggingFace</a>
 */
@Component
@Qualifier("code")
@ConditionalOnProperty(name = "nablarch.mcp.embedding.provider", havingValue = "local", matchIfMissing = true)
public class CodeSageOnnxEmbeddingClient extends AbstractOnnxEmbeddingClient {

    /**
     * コンストラクタ。
     *
     * @param properties Embedding設定プロパティ
     */
    public CodeSageOnnxEmbeddingClient(EmbeddingProperties properties) {
        super(properties.getLocal().getCode());
    }
}

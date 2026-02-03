package com.tis.nablarch.mcp.embedding.local;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import com.tis.nablarch.mcp.embedding.EmbeddingClient;
import com.tis.nablarch.mcp.embedding.EmbeddingException;
import com.tis.nablarch.mcp.embedding.config.EmbeddingProperties.OnnxModelConfig;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ONNX Runtimeを使用したローカルEmbeddingクライアントの抽象基底クラス。
 *
 * <p>BGE-M3、CodeSage等のONNX形式モデルでEmbeddingベクトルを生成する共通処理を提供する。
 * サブクラスで具体的なモデル設定を指定する。</p>
 */
public abstract class AbstractOnnxEmbeddingClient implements EmbeddingClient {

    private static final Logger logger = LoggerFactory.getLogger(AbstractOnnxEmbeddingClient.class);

    protected final OnnxModelConfig config;

    private OrtEnvironment environment;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    /**
     * コンストラクタ。
     *
     * @param config ONNXモデル設定
     */
    protected AbstractOnnxEmbeddingClient(OnnxModelConfig config) {
        this.config = config;
    }

    /**
     * モデルとトークナイザーを初期化する。
     */
    @PostConstruct
    public void init() {
        try {
            logger.info("ONNXモデルをロード中: {}", config.getModelName());

            // ONNX Runtime環境の初期化
            this.environment = OrtEnvironment.getEnvironment();

            // モデルのロード
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors());
            this.session = environment.createSession(config.getModelPath(), options);

            // トークナイザーのロード
            this.tokenizer = HuggingFaceTokenizer.newInstance(Path.of(config.getTokenizerPath()));

            logger.info("ONNXモデルのロード完了: {} ({}次元)", config.getModelName(), config.getDimensions());

        } catch (OrtException | IOException e) {
            throw new EmbeddingException("ONNXモデルの初期化に失敗: " + config.getModelName(), e);
        }
    }

    /**
     * リソースを解放する。
     */
    @PreDestroy
    public void destroy() {
        try {
            if (session != null) {
                session.close();
            }
            if (tokenizer != null) {
                tokenizer.close();
            }
            logger.info("ONNXモデルをアンロード: {}", config.getModelName());
        } catch (Exception e) {
            logger.warn("ONNXモデルのクリーンアップ中にエラー: {}", e.getMessage());
        }
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<float[]> results = new ArrayList<>();

            // バッチサイズで分割して処理
            for (int i = 0; i < texts.size(); i += config.getBatchSize()) {
                int end = Math.min(i + config.getBatchSize(), texts.size());
                List<String> batch = texts.subList(i, end);
                List<float[]> batchResults = processBatch(batch);
                results.addAll(batchResults);
            }

            logger.debug("{}: {}テキストのEmbedding生成完了", config.getModelName(), texts.size());
            return results;

        } catch (Exception e) {
            throw new EmbeddingException("Embedding生成に失敗: " + config.getModelName(), e);
        }
    }

    /**
     * バッチ単位でEmbedding推論を実行する。
     *
     * @param texts テキストのリスト
     * @return Embeddingベクトルのリスト
     */
    private List<float[]> processBatch(List<String> texts) throws OrtException {
        int batchSize = texts.size();

        // トークナイズ
        long[][] inputIds = new long[batchSize][];
        long[][] attentionMask = new long[batchSize][];

        for (int i = 0; i < batchSize; i++) {
            var encoding = tokenizer.encode(texts.get(i), true, true);
            long[] ids = encoding.getIds();
            long[] mask = encoding.getAttentionMask();

            // 最大トークン長で切り詰め
            int length = Math.min(ids.length, config.getMaxTokens());
            inputIds[i] = new long[length];
            attentionMask[i] = new long[length];
            System.arraycopy(ids, 0, inputIds[i], 0, length);
            System.arraycopy(mask, 0, attentionMask[i], 0, length);
        }

        // パディング（最大長に揃える）
        int maxLength = 0;
        for (long[] ids : inputIds) {
            maxLength = Math.max(maxLength, ids.length);
        }

        long[][] paddedInputIds = new long[batchSize][maxLength];
        long[][] paddedAttentionMask = new long[batchSize][maxLength];

        for (int i = 0; i < batchSize; i++) {
            System.arraycopy(inputIds[i], 0, paddedInputIds[i], 0, inputIds[i].length);
            System.arraycopy(attentionMask[i], 0, paddedAttentionMask[i], 0, attentionMask[i].length);
            // 残りは0（パディング）
        }

        // ONNX入力テンソルを作成
        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", OnnxTensor.createTensor(environment, paddedInputIds));
        inputs.put("attention_mask", OnnxTensor.createTensor(environment, paddedAttentionMask));

        // 推論実行
        try (OrtSession.Result result = session.run(inputs)) {
            // 出力から埋め込みベクトルを抽出
            // モデルによって出力形式が異なるため、last_hidden_stateまたはsentence_embeddingを試す
            float[][][] lastHiddenState = null;
            float[][] sentenceEmbedding = null;

            if (result.get("last_hidden_state").isPresent()) {
                lastHiddenState = (float[][][]) result.get("last_hidden_state").get().getValue();
            } else if (result.get("sentence_embedding").isPresent()) {
                sentenceEmbedding = (float[][]) result.get("sentence_embedding").get().getValue();
            } else if (result.get(0) != null) {
                // 最初の出力を使用
                Object value = result.get(0).getValue();
                if (value instanceof float[][][]) {
                    lastHiddenState = (float[][][]) value;
                } else if (value instanceof float[][]) {
                    sentenceEmbedding = (float[][]) value;
                }
            }

            List<float[]> embeddings = new ArrayList<>();

            if (sentenceEmbedding != null) {
                // 直接sentence embeddingが出力される場合
                for (int i = 0; i < batchSize; i++) {
                    embeddings.add(normalize(sentenceEmbedding[i]));
                }
            } else if (lastHiddenState != null) {
                // last_hidden_stateからmean poolingでembeddingを計算
                for (int i = 0; i < batchSize; i++) {
                    float[] embedding = meanPooling(lastHiddenState[i], paddedAttentionMask[i]);
                    embeddings.add(normalize(embedding));
                }
            } else {
                throw new EmbeddingException("モデル出力からEmbeddingを抽出できません: " + config.getModelName());
            }

            return embeddings;

        } finally {
            // 入力テンソルを解放
            for (OnnxTensor tensor : inputs.values()) {
                tensor.close();
            }
        }
    }

    /**
     * Mean Poolingでシーケンス出力から文ベクトルを計算する。
     *
     * @param hiddenState シーケンスの隠れ状態 [seqLen][hiddenSize]
     * @param attentionMask アテンションマスク [seqLen]
     * @return Mean Poolingされたベクトル
     */
    private float[] meanPooling(float[][] hiddenState, long[] attentionMask) {
        int seqLen = hiddenState.length;
        int hiddenSize = hiddenState[0].length;

        float[] sum = new float[hiddenSize];
        float count = 0;

        for (int i = 0; i < seqLen; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < hiddenSize; j++) {
                    sum[j] += hiddenState[i][j];
                }
                count++;
            }
        }

        if (count > 0) {
            for (int j = 0; j < hiddenSize; j++) {
                sum[j] /= count;
            }
        }

        return sum;
    }

    /**
     * ベクトルをL2正規化する。
     *
     * @param vector 入力ベクトル
     * @return L2正規化されたベクトル
     */
    private float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            float[] normalized = new float[vector.length];
            for (int i = 0; i < vector.length; i++) {
                normalized[i] = vector[i] / norm;
            }
            return normalized;
        }
        return vector;
    }

    @Override
    public String getModelName() {
        return config.getModelName();
    }

    @Override
    public int getDimensions() {
        return config.getDimensions();
    }
}

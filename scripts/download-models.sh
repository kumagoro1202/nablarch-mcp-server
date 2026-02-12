#!/usr/bin/env bash
# ==============================================================================
# ONNX Embeddingモデル ダウンロードスクリプト
#
# BGE-M3（ドキュメント用）とCodeSage-small-v2（コード用）のONNXモデルおよび
# トークナイザーファイルを取得する。
#
# 使用方法:
#   ./scripts/download-models.sh [OUTPUT_DIR]
#
# デフォルト出力先: ~/models
# 環境変数 EMBEDDING_BASE_PATH でも指定可能。
#
# 注意事項:
#   - BGE-M3: model.onnx + model.onnx_data で合計約2.3GB
#   - CodeSage: HuggingFaceにONNX版がないため、Docker経由でPyTorch→ONNX変換
#     (Docker不要の場合は手動でONNX変換が必要)
# ==============================================================================
set -euo pipefail

OUTPUT_DIR="${1:-${EMBEDDING_BASE_PATH:-${HOME}/models}}"

echo "=== ONNX Embeddingモデル ダウンロード ==="
echo "出力先: ${OUTPUT_DIR}"
echo ""

# ============================================================
# BGE-M3（ドキュメント用Embedding）
# ============================================================
BGE_DIR="${OUTPUT_DIR}/bge-m3"
BGE_REPO="BAAI/bge-m3"

echo "--- [1/2] BGE-M3 (ドキュメント用) ---"
mkdir -p "${BGE_DIR}"

if [ -f "${BGE_DIR}/model.onnx" ] && [ -f "${BGE_DIR}/model.onnx_data" ] && [ -f "${BGE_DIR}/tokenizer.json" ]; then
    echo "BGE-M3: 既にダウンロード済み（スキップ）"
else
    BASE_URL="https://huggingface.co/${BGE_REPO}/resolve/main"

    echo "model.onnx をダウンロード中（約700KB）..."
    curl -L -o "${BGE_DIR}/model.onnx" \
        "${BASE_URL}/onnx/model.onnx" \
        --progress-bar

    echo "model.onnx_data をダウンロード中（約2.3GB、時間がかかります）..."
    curl -L -o "${BGE_DIR}/model.onnx_data" \
        "${BASE_URL}/onnx/model.onnx_data" \
        --progress-bar

    echo "トークナイザーファイルをダウンロード中..."
    for f in tokenizer.json tokenizer_config.json special_tokens_map.json sentencepiece.bpe.model; do
        curl -sL -o "${BGE_DIR}/${f}" "${BASE_URL}/${f}" || echo "警告: ${f} のダウンロードに失敗"
    done

    if [ -f "${BGE_DIR}/model.onnx" ] && [ -f "${BGE_DIR}/model.onnx_data" ]; then
        echo "BGE-M3: ダウンロード完了"
        ls -lh "${BGE_DIR}/model.onnx" "${BGE_DIR}/model.onnx_data"
    else
        echo "BGE-M3: ダウンロード失敗"
        exit 1
    fi
fi

echo ""

# ============================================================
# CodeSage-small-v2（コード用Embedding）
# ============================================================
CODESAGE_DIR="${OUTPUT_DIR}/codesage-small-v2"

echo "--- [2/2] CodeSage-small-v2 (コード用) ---"
mkdir -p "${CODESAGE_DIR}"

if [ -f "${CODESAGE_DIR}/model.onnx" ] && [ -s "${CODESAGE_DIR}/model.onnx" ] && \
   [ "$(wc -c < "${CODESAGE_DIR}/model.onnx")" -gt 1000 ] && \
   [ -f "${CODESAGE_DIR}/tokenizer.json" ]; then
    echo "CodeSage: 既にダウンロード済み（スキップ）"
else
    echo "CodeSage: HuggingFaceにONNX版がないため、PyTorch→ONNX変換を実行します"

    if command -v docker &>/dev/null; then
        echo "Docker経由でONNX変換中（約5分）..."
        docker run --rm -v "${CODESAGE_DIR}:/output" python:3.11-slim bash -c '
pip install -q "transformers<4.40" "torch<2.4" onnx --no-cache-dir 2>&1 | tail -3
python3 << "PYEOF"
import torch
from transformers import AutoModel, AutoTokenizer
print("CodeSageモデルをロード中...")
model = AutoModel.from_pretrained("codesage/codesage-small-v2", trust_remote_code=True)
tokenizer = AutoTokenizer.from_pretrained("codesage/codesage-small-v2", trust_remote_code=True)
model.eval()
dummy = tokenizer("def hello(): pass", return_tensors="pt", padding=True, truncation=True, max_length=512)
print("ONNX変換中...")
with torch.no_grad():
    torch.onnx.export(
        model,
        (dummy["input_ids"], dummy["attention_mask"]),
        "/output/model.onnx",
        input_names=["input_ids", "attention_mask"],
        output_names=["last_hidden_state"],
        dynamic_axes={
            "input_ids": {0: "batch", 1: "seq"},
            "attention_mask": {0: "batch", 1: "seq"},
            "last_hidden_state": {0: "batch", 1: "seq"}
        },
        opset_version=14
    )
tokenizer.save_pretrained("/output")
print("ONNX変換完了!")
PYEOF
' 2>&1
    else
        echo "Dockerが見つかりません。以下の手順で手動変換してください:"
        echo ""
        echo "  pip install 'transformers<4.40' 'torch<2.4' onnx"
        echo "  python3 -c \""
        echo "  import torch"
        echo "  from transformers import AutoModel, AutoTokenizer"
        echo "  model = AutoModel.from_pretrained('codesage/codesage-small-v2', trust_remote_code=True)"
        echo "  tokenizer = AutoTokenizer.from_pretrained('codesage/codesage-small-v2', trust_remote_code=True)"
        echo "  model.eval()"
        echo "  dummy = tokenizer('test', return_tensors='pt')"
        echo "  torch.onnx.export(model, (dummy['input_ids'], dummy['attention_mask']),"
        echo "    '${CODESAGE_DIR}/model.onnx',"
        echo "    input_names=['input_ids', 'attention_mask'],"
        echo "    output_names=['last_hidden_state'],"
        echo "    dynamic_axes={'input_ids':{0:'b',1:'s'},'attention_mask':{0:'b',1:'s'}},"
        echo "    opset_version=14)"
        echo "  tokenizer.save_pretrained('${CODESAGE_DIR}')"
        echo "  \""
        exit 1
    fi

    if [ -f "${CODESAGE_DIR}/model.onnx" ] && [ "$(wc -c < "${CODESAGE_DIR}/model.onnx")" -gt 1000 ]; then
        echo "CodeSage: ONNX変換完了"
        ls -lh "${CODESAGE_DIR}/model.onnx"
    else
        echo "CodeSage: ONNX変換失敗"
        exit 1
    fi
fi

echo ""
echo "=== 完了 ==="
echo "モデルパス: ${OUTPUT_DIR}"
echo ""
echo "アプリケーション設定（環境変数）:"
echo "  EMBEDDING_DOCUMENT_MODEL_PATH=${BGE_DIR}"
echo "  EMBEDDING_CODE_MODEL_PATH=${CODESAGE_DIR}"
echo ""
echo "または application.yaml で指定:"
echo "  nablarch.mcp.embedding.local.document.model-path: ${BGE_DIR}/model.onnx"
echo "  nablarch.mcp.embedding.local.document.tokenizer-path: ${BGE_DIR}"
echo "  nablarch.mcp.embedding.local.code.model-path: ${CODESAGE_DIR}/model.onnx"
echo "  nablarch.mcp.embedding.local.code.tokenizer-path: ${CODESAGE_DIR}"

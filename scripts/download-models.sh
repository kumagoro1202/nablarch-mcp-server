#!/usr/bin/env bash
# ==============================================================================
# ONNX Embeddingモデル ダウンロードスクリプト
#
# BGE-M3（ドキュメント用）とCodeSage-small-v2（コード用）のONNXモデルおよび
# トークナイザーファイルをHuggingFace Hubからダウンロードする。
#
# 使用方法:
#   ./scripts/download-models.sh [OUTPUT_DIR]
#
# デフォルト出力先: /opt/models
# 環境変数 EMBEDDING_MODEL_PATH でも指定可能。
# ==============================================================================
set -euo pipefail

OUTPUT_DIR="${1:-${EMBEDDING_MODEL_PATH:-/opt/models}}"

echo "=== ONNX Embeddingモデル ダウンロード ==="
echo "出力先: ${OUTPUT_DIR}"
echo ""

# HuggingFace Hub CLIの存在確認
if command -v huggingface-cli &>/dev/null; then
    HF_CLI="huggingface-cli"
elif command -v hf &>/dev/null; then
    HF_CLI="hf"
else
    echo "⚠️  HuggingFace Hub CLIが見つかりません。curlでダウンロードします。"
    HF_CLI=""
fi

# ============================================================
# BGE-M3（ドキュメント用Embedding）
# ============================================================
BGE_DIR="${OUTPUT_DIR}/bge-m3"
BGE_REPO="BAAI/bge-m3"

echo "--- [1/2] BGE-M3 (ドキュメント用) ---"
mkdir -p "${BGE_DIR}"

if [ -f "${BGE_DIR}/model.onnx" ] && [ -f "${BGE_DIR}/tokenizer.json" ]; then
    echo "✅ BGE-M3: 既にダウンロード済み（スキップ）"
else
    if [ -n "${HF_CLI}" ]; then
        echo "HuggingFace CLIでダウンロード中..."
        ${HF_CLI} download "${BGE_REPO}" \
            --include "onnx/*" "tokenizer.json" "tokenizer_config.json" "special_tokens_map.json" "sentencepiece.bpe.model" \
            --local-dir "${BGE_DIR}" \
            --local-dir-use-symlinks False 2>/dev/null || true

        # onnx/ サブディレクトリ内のファイルを移動
        if [ -d "${BGE_DIR}/onnx" ] && [ -f "${BGE_DIR}/onnx/model.onnx" ]; then
            mv "${BGE_DIR}/onnx/model.onnx" "${BGE_DIR}/model.onnx"
            rm -rf "${BGE_DIR}/onnx"
        fi
    else
        echo "curlでダウンロード中..."
        BASE_URL="https://huggingface.co/${BGE_REPO}/resolve/main"

        curl -L -o "${BGE_DIR}/model.onnx" \
            "${BASE_URL}/onnx/model.onnx" \
            --progress-bar

        for f in tokenizer.json tokenizer_config.json special_tokens_map.json sentencepiece.bpe.model; do
            curl -sL -o "${BGE_DIR}/${f}" "${BASE_URL}/${f}" || echo "警告: ${f} のダウンロードに失敗"
        done
    fi

    if [ -f "${BGE_DIR}/model.onnx" ]; then
        echo "✅ BGE-M3: ダウンロード完了"
        ls -lh "${BGE_DIR}/model.onnx"
    else
        echo "❌ BGE-M3: ダウンロード失敗"
        exit 1
    fi
fi

echo ""

# ============================================================
# CodeSage-small-v2（コード用Embedding）
# ============================================================
CODESAGE_DIR="${OUTPUT_DIR}/codesage-small-v2"
CODESAGE_REPO="codesage/codesage-small-v2"

echo "--- [2/2] CodeSage-small-v2 (コード用) ---"
mkdir -p "${CODESAGE_DIR}"

if [ -f "${CODESAGE_DIR}/model.onnx" ] && [ -f "${CODESAGE_DIR}/tokenizer.json" ]; then
    echo "✅ CodeSage: 既にダウンロード済み（スキップ）"
else
    if [ -n "${HF_CLI}" ]; then
        echo "HuggingFace CLIでダウンロード中..."
        ${HF_CLI} download "${CODESAGE_REPO}" \
            --include "onnx/*" "tokenizer.json" "tokenizer_config.json" "special_tokens_map.json" \
            --local-dir "${CODESAGE_DIR}" \
            --local-dir-use-symlinks False 2>/dev/null || true

        if [ -d "${CODESAGE_DIR}/onnx" ] && [ -f "${CODESAGE_DIR}/onnx/model.onnx" ]; then
            mv "${CODESAGE_DIR}/onnx/model.onnx" "${CODESAGE_DIR}/model.onnx"
            rm -rf "${CODESAGE_DIR}/onnx"
        fi
    else
        echo "curlでダウンロード中..."
        BASE_URL="https://huggingface.co/${CODESAGE_REPO}/resolve/main"

        curl -L -o "${CODESAGE_DIR}/model.onnx" \
            "${BASE_URL}/onnx/model.onnx" \
            --progress-bar

        for f in tokenizer.json tokenizer_config.json special_tokens_map.json; do
            curl -sL -o "${CODESAGE_DIR}/${f}" "${BASE_URL}/${f}" || echo "警告: ${f} のダウンロードに失敗"
        done
    fi

    if [ -f "${CODESAGE_DIR}/model.onnx" ]; then
        echo "✅ CodeSage: ダウンロード完了"
        ls -lh "${CODESAGE_DIR}/model.onnx"
    else
        echo "❌ CodeSage: ダウンロード失敗"
        exit 1
    fi
fi

echo ""
echo "=== 完了 ==="
echo "モデルパス: ${OUTPUT_DIR}"
echo ""
echo "アプリケーション設定:"
echo "  EMBEDDING_MODEL_PATH=${OUTPUT_DIR}"
echo ""
echo "または application.yaml で指定:"
echo "  nablarch.mcp.embedding.local.document.model-path: ${BGE_DIR}/model.onnx"
echo "  nablarch.mcp.embedding.local.document.tokenizer-path: ${BGE_DIR}"
echo "  nablarch.mcp.embedding.local.code.model-path: ${CODESAGE_DIR}/model.onnx"
echo "  nablarch.mcp.embedding.local.code.tokenizer-path: ${CODESAGE_DIR}"

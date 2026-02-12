#!/bin/bash
#
# Nablarch MCP Server - ナレッジベース初期化スクリプト
#
# 機能:
#   1. pgvectorコンテナ起動確認
#   2. PostgreSQL接続待機
#   3. Nablarch公式ドキュメントのEmbedding取込実行
#   4. 取込結果サマリ表示
#
# 使用方法:
#   bash scripts/init-knowledge.sh
#
# 前提条件:
#   - Docker Composeがインストールされていること
#   - ONNXモデルが ~/models/ に配置されていること
#   - Maven 3.9以上がインストールされていること
#
# 所要時間:
#   約20-30分（467ページ分のHTML取得・チャンク分割・Embedding生成）
#

set -euo pipefail

# === 設定 ===
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="nablarch_mcp"
DB_USER="nablarch"
MAX_WAIT_SEC=30

# === カラー出力 ===
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# === Step 1: Docker Composeでpgvector起動 ===
info "Step 1: pgvectorコンテナ起動確認"

if [ ! -f "${COMPOSE_FILE}" ]; then
    error "docker-compose.ymlが見つかりません: ${COMPOSE_FILE}"
    exit 1
fi

cd "${PROJECT_ROOT}"

if docker compose ps | grep -q "nablarch-mcp-pgvector.*Up"; then
    success "pgvectorコンテナは既に起動しています"
else
    info "pgvectorコンテナを起動中..."
    docker compose up -d
    if [ $? -eq 0 ]; then
        success "pgvectorコンテナ起動完了"
    else
        error "pgvectorコンテナ起動失敗"
        exit 1
    fi
fi

# === Step 2: PostgreSQL接続待機 ===
info "Step 2: PostgreSQL起動待機（最大${MAX_WAIT_SEC}秒）"

wait_count=0
while ! docker compose exec -T pgvector pg_isready -h localhost -U "${DB_USER}" -d "${DB_NAME}" >/dev/null 2>&1; do
    if [ $wait_count -ge $MAX_WAIT_SEC ]; then
        error "PostgreSQLが${MAX_WAIT_SEC}秒以内に起動しませんでした"
        exit 1
    fi
    echo -n "."
    sleep 1
    wait_count=$((wait_count + 1))
done
echo ""
success "PostgreSQL起動完了"

# === Step 3: Embedding取込実行（Java版） ===
info "Step 3: Nablarch公式ドキュメントのEmbedding取込を開始"
info "  方式: Java IngestionRunner (Spring Boot)"
info "  Embeddingプロバイダ: ローカルONNXモデル (BGE-M3)"
info "  所要時間の目安: 20-30分"
echo ""

INGESTION_START=$(date +%s)

# Maven経由でSpring Boot実行
# CommandLineRunner (IngestionRunner) が --nablarch.mcp.ingestion.run=true で起動
mvn spring-boot:run -Dspring-boot.run.arguments="--nablarch.mcp.ingestion.enabled=true --nablarch.mcp.ingestion.run=true --nablarch.mcp.embedding.provider=local"

INGESTION_STATUS=$?
INGESTION_END=$(date +%s)
INGESTION_ELAPSED=$((INGESTION_END - INGESTION_START))

echo ""

if [ $INGESTION_STATUS -eq 0 ]; then
    success "Embedding取込完了"
else
    error "Embedding取込失敗（終了コード: ${INGESTION_STATUS}）"
    exit 1
fi

# === Step 4: 取込結果サマリ表示 ===
info "Step 4: 取込結果確認"

# PostgreSQLからレコード数を取得
CHUNK_COUNT=$(docker compose exec -T pgvector psql -U "${DB_USER}" -d "${DB_NAME}" -t -c "SELECT count(*) FROM document_chunks WHERE source = 'nablarch-official-docs';" | xargs)
EMBEDDING_COUNT=$(docker compose exec -T pgvector psql -U "${DB_USER}" -d "${DB_NAME}" -t -c "SELECT count(*) FROM document_chunks WHERE source = 'nablarch-official-docs' AND embedding IS NOT NULL;" | xargs)

echo ""
echo "============================================"
echo "  ナレッジベース初期化完了"
echo "============================================"
echo "  取込ソース: Nablarch公式ドキュメント"
echo "  格納チャンク数: ${CHUNK_COUNT}"
echo "  Embedding生成数: ${EMBEDDING_COUNT}"
echo "  所要時間: ${INGESTION_ELAPSED}秒 ($((INGESTION_ELAPSED / 60))分)"
echo "============================================"
echo ""

if [ "${CHUNK_COUNT}" -eq 0 ]; then
    warn "チャンクが1件も格納されていません。ログを確認してください。"
    exit 1
fi

if [ "${EMBEDDING_COUNT}" -lt "${CHUNK_COUNT}" ]; then
    warn "Embedding未生成のチャンクがあります（${CHUNK_COUNT} - ${EMBEDDING_COUNT} = $((CHUNK_COUNT - EMBEDDING_COUNT))件）"
fi

# === Step 5: 完了メッセージ ===
success "初期化スクリプト完了"
info "次のステップ:"
info "  1. MCP Serverを起動: mvn spring-boot:run"
info "  2. Claude Desktopから接続"
info ""
info "注意:"
info "  - init-knowledge.sh なしでもkeyword mode (BM25)で動作します"
info "  - Embedding検索を使う場合のみ初回実行が必要です"
info "  - 再実行すると既存データを上書きします（冪等性あり）"
echo ""

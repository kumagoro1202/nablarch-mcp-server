#!/usr/bin/env python3
"""
Nablarch公式ドキュメント取込スクリプト（Python版）。

BGE-M3 ONNXモデルでEmbedding生成し、pgvectorに格納する。
Java版IngestionRunnerのCPU推論が遅いため、Python版で代替。

使用方法:
    python3 scripts/ingest_docs.py

前提条件:
    - pgvectorコンテナが起動していること (docker compose up -d)
    - ~/models/bge-m3/ にONNXモデルが配置されていること
    - pip install onnxruntime psycopg2-binary tokenizers
"""

import re
import sys
import time
import urllib.request
from html.parser import HTMLParser

import numpy as np
import onnxruntime as ort
import psycopg2
from tokenizers import Tokenizer

# === 設定 ===
BASE_URL = "https://nablarch.github.io/docs/LATEST/doc/"
MODEL_DIR = "/home/kuma/models/bge-m3"
MAX_TOKENS = 128
ONNX_THREADS = 4
CHUNK_MAX_CHARS = 800
CHUNK_OVERLAP_CHARS = 100
DB_HOST = "localhost"
DB_PORT = 5432
DB_NAME = "nablarch_mcp"
DB_USER = "nablarch"
DB_PASS = "nablarch_dev"
CRAWL_DELAY_SEC = 0.5

# バッチ・REST優先URL（これらを最優先で取込）
PRIORITY_PATTERNS = [
    "batch", "nablarch_batch", "rest", "jaxrs", "action",
    "handler", "data_reader", "listener", "db_access",
    "database", "transaction", "messaging",
]


class TextExtractor(HTMLParser):
    """HTMLからテキストを抽出するパーサー。"""

    def __init__(self):
        super().__init__()
        self.texts = []
        self.skip_tags = {"script", "style", "nav", "footer", "header"}
        self._skip_depth = 0

    def handle_starttag(self, tag, attrs):
        if tag in self.skip_tags:
            self._skip_depth += 1

    def handle_endtag(self, tag):
        if tag in self.skip_tags and self._skip_depth > 0:
            self._skip_depth -= 1

    def handle_data(self, data):
        if self._skip_depth == 0:
            text = data.strip()
            if text:
                self.texts.append(text)

    def get_text(self):
        return " ".join(self.texts)


def fetch_html(url: str) -> str:
    """URLからHTMLを取得する。"""
    req = urllib.request.Request(url, headers={"User-Agent": "NablarchMCPIngester/1.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8", errors="replace")


def extract_text(html: str) -> str:
    """HTMLからテキストコンテンツを抽出する。"""
    parser = TextExtractor()
    parser.feed(html)
    return parser.get_text()


def discover_urls(index_html: str, base_url: str) -> list[str]:
    """インデックスページからドキュメントURLを抽出する。"""
    pattern = re.compile(r'href="([^"]+\.html)"')
    urls = []
    for match in pattern.finditer(index_html):
        href = match.group(1)
        if href.startswith("http"):
            full_url = href
        elif href.startswith("./"):
            full_url = base_url + href[2:]
        else:
            full_url = base_url + href
        if full_url not in urls:
            urls.append(full_url)
    return urls


def chunk_text(text: str, max_chars: int = CHUNK_MAX_CHARS, overlap: int = CHUNK_OVERLAP_CHARS) -> list[str]:
    """テキストをチャンクに分割する。"""
    if len(text) <= max_chars:
        return [text] if len(text) >= 50 else []

    chunks = []
    start = 0
    while start < len(text):
        end = start + max_chars
        if end >= len(text):
            chunk = text[start:]
        else:
            # 文の区切りで分割を試みる
            break_pos = text.rfind("。", start, end)
            if break_pos == -1:
                break_pos = text.rfind("\n", start, end)
            if break_pos == -1 or break_pos <= start:
                break_pos = end
            else:
                break_pos += 1
            chunk = text[start:break_pos]

        if len(chunk.strip()) >= 50:
            chunks.append(chunk.strip())

        start = max(start + 1, break_pos - overlap)

    return chunks


def prioritize_urls(urls: list[str]) -> list[str]:
    """バッチ・REST関連URLを先頭に並べ替える。"""
    priority = []
    others = []
    for url in urls:
        url_lower = url.lower()
        if any(p in url_lower for p in PRIORITY_PATTERNS):
            priority.append(url)
        else:
            others.append(url)
    return priority + others


class Embedder:
    """BGE-M3 ONNX Embedding生成器。"""

    def __init__(self):
        print("BGE-M3 ONNXモデルをロード中...")
        t0 = time.time()

        self.tokenizer = Tokenizer.from_file(f"{MODEL_DIR}/tokenizer.json")
        self.tokenizer.enable_truncation(max_length=MAX_TOKENS)
        self.tokenizer.enable_padding(pad_id=0, pad_token="[PAD]", length=MAX_TOKENS)

        opts = ort.SessionOptions()
        opts.intra_op_num_threads = ONNX_THREADS
        opts.inter_op_num_threads = 1
        opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        self.session = ort.InferenceSession(f"{MODEL_DIR}/model.onnx", opts)

        # ウォームアップ推論
        dummy = self.tokenizer.encode("warm up")
        dummy_ids = np.array([dummy.ids], dtype=np.int64)
        dummy_mask = np.array([dummy.attention_mask], dtype=np.int64)
        self.session.run(None, {"input_ids": dummy_ids, "attention_mask": dummy_mask})

        print(f"モデルロード完了: {time.time() - t0:.1f}s")

    def embed(self, text: str) -> list[float]:
        """テキストを1024次元のベクトルに変換する。"""
        encoding = self.tokenizer.encode(text)
        input_ids = np.array([encoding.ids], dtype=np.int64)
        attention_mask = np.array([encoding.attention_mask], dtype=np.int64)

        outputs = self.session.run(None, {"input_ids": input_ids, "attention_mask": attention_mask})

        # Mean pooling
        hidden_state = outputs[0][0]  # [seq_len, 1024]
        mask = attention_mask[0].astype(np.float32)
        embedding = (hidden_state * mask[:, np.newaxis]).sum(axis=0) / mask.sum()

        # L2正規化
        norm = np.linalg.norm(embedding)
        if norm > 0:
            embedding = embedding / norm

        return embedding.tolist()


def main():
    start_time = time.time()

    # DB接続
    print("pgvectorに接続中...")
    conn = psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME, user=DB_USER, password=DB_PASS
    )
    conn.autocommit = False
    cur = conn.cursor()

    # 既存レコードクリア（再実行時のため）
    cur.execute("DELETE FROM document_chunks WHERE source = 'nablarch-official-docs'")
    deleted = cur.rowcount
    conn.commit()
    if deleted > 0:
        print(f"既存レコード {deleted} 件削除")

    # Embeddingモデル準備
    embedder = Embedder()

    # インデックスページ取得
    print(f"\nインデックスページを取得中: {BASE_URL}")
    index_html = fetch_html(BASE_URL)
    urls = discover_urls(index_html, BASE_URL)
    urls = prioritize_urls(urls)
    print(f"検出URL数: {len(urls)} (うち優先: {sum(1 for u in urls if any(p in u.lower() for p in PRIORITY_PATTERNS))})")

    total_chunks = 0
    total_errors = 0
    processed = 0

    for i, url in enumerate(urls):
        processed += 1
        try:
            # HTML取得
            html = fetch_html(url)
            text = extract_text(html)

            if len(text) < 50:
                print(f"  [{i+1}/{len(urls)}] スキップ（テキスト不足）: {url}")
                continue

            # チャンク分割
            chunks = chunk_text(text)
            if not chunks:
                continue

            # 各チャンクをEmbedding→格納
            for chunk in chunks:
                t0 = time.time()
                embedding = embedder.embed(chunk)
                embed_time = time.time() - t0

                # pgvectorに格納
                vector_str = "[" + ",".join(str(v) for v in embedding) + "]"
                cur.execute(
                    """INSERT INTO document_chunks
                       (content, embedding, source, source_type, language, url)
                       VALUES (%s, %s::vector, %s, %s, %s, %s)""",
                    (chunk, vector_str, "nablarch-official-docs", "documentation", "ja", url),
                )
                total_chunks += 1

            # ドキュメント単位でコミット
            conn.commit()
            print(f"  [{i+1}/{len(urls)}] {len(chunks)}チャンク取込完了 ({embed_time:.1f}s/chunk): {url.split('/')[-1]}")

        except Exception as e:
            conn.rollback()
            total_errors += 1
            print(f"  [{i+1}/{len(urls)}] エラー: {url} - {e}")

        # クロールディレイ
        if i < len(urls) - 1:
            time.sleep(CRAWL_DELAY_SEC)

    # 結果サマリ
    elapsed = time.time() - start_time
    print(f"\n=== 取込完了 ===")
    print(f"処理ドキュメント数: {processed}/{len(urls)}")
    print(f"格納チャンク数: {total_chunks}")
    print(f"エラー数: {total_errors}")
    print(f"所要時間: {elapsed:.0f}秒 ({elapsed/60:.1f}分)")

    # レコード数確認
    cur.execute("SELECT count(*) FROM document_chunks WHERE source = 'nablarch-official-docs'")
    count = cur.fetchone()[0]
    cur.execute("SELECT count(*) FROM document_chunks WHERE source = 'nablarch-official-docs' AND embedding IS NOT NULL")
    with_embedding = cur.fetchone()[0]
    print(f"DB内レコード数: {count} (Embedding付き: {with_embedding})")

    cur.close()
    conn.close()


if __name__ == "__main__":
    main()

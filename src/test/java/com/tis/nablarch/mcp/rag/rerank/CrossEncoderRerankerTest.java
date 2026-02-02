package com.tis.nablarch.mcp.rag.rerank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tis.nablarch.mcp.rag.search.SearchResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CrossEncoderReranker} のユニットテスト。
 *
 * <p>MockWebServerを使用してJina Reranker APIのレスポンスをモックし、
 * リランキング動作を検証する。</p>
 */
class CrossEncoderRerankerTest {

    private MockWebServer mockServer;
    private CrossEncoderReranker reranker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        RerankProperties properties = new RerankProperties();
        RerankProperties.Jina jina = properties.getJina();
        jina.setApiKey("test-api-key");
        jina.setModel("jina-reranker-v2-base-multilingual");
        jina.setBaseUrl(mockServer.url("/v1/rerank").toString());
        jina.setTimeoutMs(5000);
        jina.setTopK(10);

        reranker = new CrossEncoderReranker(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    /**
     * テスト用の検索結果候補を生成する。
     */
    private List<SearchResult> createCandidates(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new SearchResult(
                        "doc-" + i,
                        "ドキュメント" + i + "の内容",
                        0.5 + (count - i) * 0.01,
                        Map.of("source", "test", "app_type", "web"),
                        "https://example.com/doc" + i
                ))
                .toList();
    }

    @Nested
    @DisplayName("正常系")
    class NormalTests {

        @Test
        @DisplayName("正常リランキング: スコアが置換されtopK件が返却される")
        void rerankReplacesScoresAndReturnsTopK() throws Exception {
            List<SearchResult> candidates = createCandidates(5);

            // Jina API レスポンス: index 2が最高スコア、次にindex 0
            String responseJson = """
                    {
                        "model": "jina-reranker-v2-base-multilingual",
                        "results": [
                            {"index": 2, "relevance_score": 0.95},
                            {"index": 0, "relevance_score": 0.85},
                            {"index": 4, "relevance_score": 0.70}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("テストクエリ", candidates, 3);

            assertEquals(3, results.size());
            // スコア降順
            assertEquals(0.95, results.get(0).score(), 0.001);
            assertEquals(0.85, results.get(1).score(), 0.001);
            assertEquals(0.70, results.get(2).score(), 0.001);
            // IDが正しくマッピングされている
            assertEquals("doc-3", results.get(0).id()); // index 2 → candidates[2]
            assertEquals("doc-1", results.get(1).id()); // index 0 → candidates[0]
            assertEquals("doc-5", results.get(2).id()); // index 4 → candidates[4]
        }

        @Test
        @DisplayName("APIリクエストが正しいフォーマットで送信される")
        void requestFormatIsCorrect() throws Exception {
            List<SearchResult> candidates = createCandidates(3);

            String responseJson = """
                    {
                        "results": [
                            {"index": 0, "relevance_score": 0.9},
                            {"index": 1, "relevance_score": 0.8}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            reranker.rerank("ハンドラキュー", candidates, 2);

            RecordedRequest request = mockServer.takeRequest();
            assertEquals("POST", request.getMethod());
            assertEquals("/v1/rerank", request.getPath());
            assertTrue(request.getHeader("Authorization").contains("Bearer test-api-key"));

            String body = request.getBody().readUtf8();
            assertTrue(body.contains("jina-reranker-v2-base-multilingual"));
            assertTrue(body.contains("ハンドラキュー"));
            assertTrue(body.contains("ドキュメント1の内容"));
        }

        @Test
        @DisplayName("topKが候補数より多い場合は候補数分のみ返却される")
        void topKLargerThanCandidates() throws Exception {
            List<SearchResult> candidates = createCandidates(2);

            String responseJson = """
                    {
                        "results": [
                            {"index": 1, "relevance_score": 0.9},
                            {"index": 0, "relevance_score": 0.8}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 10);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("メタデータとsourceUrlが保持される")
        void metadataAndSourceUrlPreserved() throws Exception {
            List<SearchResult> candidates = createCandidates(2);

            String responseJson = """
                    {
                        "results": [
                            {"index": 0, "relevance_score": 0.95}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 1);

            assertEquals(1, results.size());
            SearchResult result = results.get(0);
            assertEquals("doc-1", result.id());
            assertEquals("ドキュメント1の内容", result.content());
            assertEquals(0.95, result.score(), 0.001);
            assertEquals("test", result.metadata().get("source"));
            assertEquals("web", result.metadata().get("app_type"));
            assertEquals("https://example.com/doc1", result.sourceUrl());
        }
    }

    @Nested
    @DisplayName("フォールバック")
    class FallbackTests {

        @Test
        @DisplayName("API接続失敗時は元のスコア順で返却（degraded mode）")
        void apiErrorFallsBackToOriginalScoreOrder() {
            List<SearchResult> candidates = createCandidates(5);

            // サーバーエラー
            mockServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("Internal Server Error"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 3);

            assertEquals(3, results.size());
            // 元のスコア降順で返却
            assertTrue(results.get(0).score() >= results.get(1).score());
            assertTrue(results.get(1).score() >= results.get(2).score());
        }

        @Test
        @DisplayName("レスポンスが空の場合はフォールバック")
        void emptyResponseFallsBack() {
            List<SearchResult> candidates = createCandidates(3);

            mockServer.enqueue(new MockResponse()
                    .setBody("{\"results\": []}")
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 2);

            assertEquals(2, results.size());
            // 元のスコア降順
            assertTrue(results.get(0).score() >= results.get(1).score());
        }

        @Test
        @DisplayName("不正なJSONレスポンスの場合はフォールバック")
        void invalidJsonFallsBack() {
            List<SearchResult> candidates = createCandidates(3);

            mockServer.enqueue(new MockResponse()
                    .setBody("not json")
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 2);

            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("エッジケース")
    class EdgeCaseTests {

        @Test
        @DisplayName("空候補リストの場合は空リストを返す")
        void emptyCandidatesReturnsEmpty() {
            List<SearchResult> results = reranker.rerank("クエリ", Collections.emptyList(), 5);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("null候補リストの場合は空リストを返す")
        void nullCandidatesReturnsEmpty() {
            List<SearchResult> results = reranker.rerank("クエリ", null, 5);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("topKが0以下の場合はデフォルト値が使用される")
        void invalidTopKUsesDefault() throws Exception {
            List<SearchResult> candidates = createCandidates(3);

            String responseJson = """
                    {
                        "results": [
                            {"index": 0, "relevance_score": 0.9},
                            {"index": 1, "relevance_score": 0.8},
                            {"index": 2, "relevance_score": 0.7}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 0);

            // topK=0 → デフォルト(10)が使われるが候補は3件
            assertFalse(results.isEmpty());
        }

        @Test
        @DisplayName("レスポンスのindexが範囲外の場合はスキップされる")
        void outOfBoundsIndexSkipped() throws Exception {
            List<SearchResult> candidates = createCandidates(2);

            String responseJson = """
                    {
                        "results": [
                            {"index": 0, "relevance_score": 0.9},
                            {"index": 99, "relevance_score": 0.95},
                            {"index": 1, "relevance_score": 0.8}
                        ]
                    }
                    """;
            mockServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .setHeader("Content-Type", "application/json"));

            List<SearchResult> results = reranker.rerank("クエリ", candidates, 3);

            // index=99 はスキップされ、有効な2件のみ返却
            assertEquals(2, results.size());
            assertEquals("doc-1", results.get(0).id()); // index=0, score=0.9
            assertEquals("doc-2", results.get(1).id()); // index=1, score=0.8
        }
    }
}

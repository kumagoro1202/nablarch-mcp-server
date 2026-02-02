package com.tis.nablarch.mcp.rag.chunking;

import com.tis.nablarch.mcp.rag.parser.ParsedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * チャンキングサービス。
 *
 * <p>ParsedDocumentをコンテンツタイプに応じた戦略で
 * DocumentChunkDtoのリストに分割する。</p>
 *
 * <p>チャンキング戦略:</p>
 * <ul>
 *   <li>HTML: セクション単位（512トークン、128オーバーラップ）</li>
 *   <li>MARKDOWN: 見出し単位（512トークン、128オーバーラップ）</li>
 *   <li>JAVADOC: クラス/メソッド単位（256トークン、オーバーラップなし）</li>
 *   <li>JAVA: メソッド単位（512トークン、オーバーラップなし）</li>
 *   <li>XML: 要素単位（256トークン、オーバーラップなし）</li>
 *   <li>TEXT: 段落単位（512トークン、128オーバーラップ）</li>
 * </ul>
 */
@Component
public class ChunkingService {

    /** ドキュメント系の最大トークン数 */
    private static final int DOC_MAX_TOKENS = 512;

    /** コード系の最大トークン数 */
    private static final int CODE_MAX_TOKENS = 256;

    /** ドキュメント系のオーバーラップトークン数 */
    private static final int DOC_OVERLAP_TOKENS = 128;

    /** 最小チャンクサイズ（文字数）— これより短いチャンクは除外 */
    private static final int MIN_CHUNK_CHARS = 50;

    /**
     * パース済みドキュメントをチャンクに分割する。
     *
     * @param document パース済みドキュメント
     * @return チャンクのリスト
     * @throws IllegalArgumentException documentがnullの場合
     */
    public List<DocumentChunkDto> chunk(ParsedDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("documentは必須です");
        }

        List<String> rawChunks = switch (document.contentType()) {
            case HTML -> chunkByTokenLimit(document.content(), DOC_MAX_TOKENS, DOC_OVERLAP_TOKENS);
            case MARKDOWN -> chunkByTokenLimit(document.content(), DOC_MAX_TOKENS, DOC_OVERLAP_TOKENS);
            case JAVADOC -> List.of(document.content()); // 1ドキュメント=1チャンク
            case JAVA -> chunkByTokenLimit(document.content(), DOC_MAX_TOKENS, 0);
            case XML -> List.of(document.content()); // 1要素=1チャンク
            case TEXT -> chunkByTokenLimit(document.content(), DOC_MAX_TOKENS, DOC_OVERLAP_TOKENS);
        };

        // 最小サイズ未満のチャンクを除外
        List<String> filteredChunks = rawChunks.stream()
                .filter(c -> c.length() >= MIN_CHUNK_CHARS)
                .toList();

        // チャンクが全て除外された場合、元コンテンツが最小サイズ以上ならそのまま返す
        if (filteredChunks.isEmpty() && document.content().length() >= MIN_CHUNK_CHARS) {
            filteredChunks = List.of(document.content());
        }

        int totalChunks = filteredChunks.size();
        List<DocumentChunkDto> results = new ArrayList<>();

        for (int i = 0; i < filteredChunks.size(); i++) {
            Map<String, String> chunkMetadata = new HashMap<>(document.metadata());
            chunkMetadata.put("content_type", document.contentType().name());
            chunkMetadata.put("chunk_index", String.valueOf(i));
            chunkMetadata.put("total_chunks", String.valueOf(totalChunks));

            results.add(new DocumentChunkDto(
                    filteredChunks.get(i),
                    chunkMetadata,
                    i,
                    totalChunks,
                    document.contentType()
            ));
        }

        return results;
    }

    /**
     * テキストをトークン上限でチャンクに分割する。
     *
     * <p>段落（空行区切り）または文末（。）を区切り点として使用し、
     * トークン上限を超えないようにチャンクを構成する。</p>
     *
     * @param text 対象テキスト
     * @param maxTokens 最大トークン数
     * @param overlapTokens オーバーラップトークン数
     * @return チャンクのリスト
     */
    private List<String> chunkByTokenLimit(String text, int maxTokens, int overlapTokens) {
        int maxChars = tokensToChars(maxTokens, text);
        int overlapChars = tokensToChars(overlapTokens, text);

        if (text.length() <= maxChars) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        // 段落（空行区切り）で分割を試みる
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();
        String previousOverlap = "";

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 現在のチャンクにこの段落を追加するとオーバーするか？
            int projectedLength = currentChunk.length() + (currentChunk.length() > 0 ? 2 : 0) + trimmed.length();

            if (projectedLength > maxChars && currentChunk.length() > 0) {
                // 現在のチャンクを確定
                String chunkText = currentChunk.toString().trim();
                chunks.add(chunkText);

                // オーバーラップ計算
                previousOverlap = extractOverlap(chunkText, overlapChars);
                currentChunk = new StringBuilder();
                if (!previousOverlap.isEmpty()) {
                    currentChunk.append(previousOverlap).append("\n\n");
                }
            }

            // 段落自体がmaxCharsを超える場合、文単位で分割
            if (trimmed.length() > maxChars) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    previousOverlap = extractOverlap(currentChunk.toString().trim(), overlapChars);
                    currentChunk = new StringBuilder();
                }
                List<String> sentenceChunks = chunkBySentence(trimmed, maxChars, overlapChars);
                chunks.addAll(sentenceChunks);
                if (!sentenceChunks.isEmpty()) {
                    previousOverlap = extractOverlap(sentenceChunks.get(sentenceChunks.size() - 1), overlapChars);
                }
                continue;
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed);
        }

        // 残りのチャンクを確定
        if (currentChunk.length() > 0) {
            String remaining = currentChunk.toString().trim();
            if (!remaining.isEmpty()) {
                chunks.add(remaining);
            }
        }

        return chunks;
    }

    /**
     * 文単位でチャンクに分割する。段落が大きすぎる場合のフォールバック。
     */
    private List<String> chunkBySentence(String text, int maxChars, int overlapChars) {
        List<String> chunks = new ArrayList<>();
        // 日本語の句点（。）と英語のピリオド+スペースで文分割
        String[] sentences = text.split("(?<=。)|(?<=\\. )");

        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > maxChars && currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                chunks.add(chunkText);
                String overlap = extractOverlap(chunkText, overlapChars);
                currentChunk = new StringBuilder();
                if (!overlap.isEmpty()) {
                    currentChunk.append(overlap);
                }
            }
            currentChunk.append(sentence);
        }

        if (currentChunk.length() > 0) {
            String remaining = currentChunk.toString().trim();
            if (!remaining.isEmpty()) {
                chunks.add(remaining);
            }
        }

        return chunks;
    }

    /**
     * テキストの末尾からオーバーラップ部分を抽出する。
     */
    private String extractOverlap(String text, int overlapChars) {
        if (overlapChars <= 0 || text.length() <= overlapChars) {
            return "";
        }
        // 文境界で切る
        String tail = text.substring(text.length() - overlapChars);
        int sentenceStart = tail.indexOf("。");
        if (sentenceStart >= 0 && sentenceStart < tail.length() - 1) {
            return tail.substring(sentenceStart + 1).trim();
        }
        int periodStart = tail.indexOf(". ");
        if (periodStart >= 0 && periodStart < tail.length() - 2) {
            return tail.substring(periodStart + 2).trim();
        }
        // 文境界が見つからない場合、段落境界で切る
        int paraStart = tail.indexOf("\n\n");
        if (paraStart >= 0 && paraStart < tail.length() - 2) {
            return tail.substring(paraStart + 2).trim();
        }
        return tail.trim();
    }

    /**
     * トークン数から文字数に変換する（近似）。
     *
     * <p>日本語テキストは1トークン≒2文字、英語テキストは1トークン≒4文字として計算する。
     * テキスト中の日本語比率に応じて動的に調整する。</p>
     *
     * @param tokens トークン数
     * @param text 対象テキスト（日本語比率の計算に使用）
     * @return 推定文字数
     */
    static int tokensToChars(int tokens, String text) {
        if (tokens <= 0) {
            return 0;
        }
        double japaneseRatio = calculateJapaneseRatio(text);
        // 日本語100%: 1token=2chars, 英語100%: 1token=4chars
        double charsPerToken = 2.0 + (1.0 - japaneseRatio) * 2.0;
        return (int) (tokens * charsPerToken);
    }

    /**
     * テキスト中の日本語文字の比率を計算する。
     */
    static double calculateJapaneseRatio(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        long total = 0;
        long japanese = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            total++;
            if (isJapanese(c)) {
                japanese++;
            }
        }
        return total == 0 ? 0.0 : (double) japanese / total;
    }

    private static boolean isJapanese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}

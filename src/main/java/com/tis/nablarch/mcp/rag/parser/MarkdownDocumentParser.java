package com.tis.nablarch.mcp.rag.parser;

import com.tis.nablarch.mcp.rag.chunking.ContentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdownドキュメントパーサー。
 *
 * <p>Fintan記事等のMarkdownコンテンツを対象に、
 * 見出し（## / ###）単位でセクション分割を行う。
 * コードフェンス（```...```）は分割せずに保持する。</p>
 */
@Component
public class MarkdownDocumentParser implements DocumentParser {

    /** 見出しパターン（## 又は ###） */
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,3})\\s+(.+)$", Pattern.MULTILINE);

    /** コードフェンスパターン */
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^```.*$", Pattern.MULTILINE);

    @Override
    public List<ParsedDocument> parse(String content, String sourceUrl) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrlは必須です");
        }

        List<ParsedDocument> results = new ArrayList<>();
        String documentTitle = extractTitle(content);

        // 見出し位置を特定（コードフェンス内の見出しは除外）
        List<HeadingInfo> headings = findHeadings(content);

        if (headings.isEmpty()) {
            // 見出しがない場合、全体を1ドキュメントとして返す
            Map<String, String> metadata = buildMetadata(documentTitle, null, sourceUrl);
            results.add(new ParsedDocument(content.trim(), metadata, sourceUrl, ContentType.MARKDOWN));
            return results;
        }

        // 最初の見出し前のコンテンツ（イントロ部分）
        if (headings.get(0).position > 0) {
            String intro = content.substring(0, headings.get(0).position).trim();
            if (!intro.isBlank()) {
                Map<String, String> metadata = buildMetadata(documentTitle, documentTitle, sourceUrl);
                results.add(new ParsedDocument(intro, metadata, sourceUrl, ContentType.MARKDOWN));
            }
        }

        // 各セクションを処理
        for (int i = 0; i < headings.size(); i++) {
            HeadingInfo heading = headings.get(i);
            int endPos = (i < headings.size() - 1) ? headings.get(i + 1).position : content.length();
            String sectionContent = content.substring(heading.position, endPos).trim();

            if (!sectionContent.isBlank()) {
                Map<String, String> metadata = buildMetadata(documentTitle, heading.title, sourceUrl);
                metadata.put("heading_level", String.valueOf(heading.level));
                results.add(new ParsedDocument(sectionContent, metadata, sourceUrl, ContentType.MARKDOWN));
            }
        }

        return results;
    }

    /**
     * ドキュメントタイトル（# 見出し）を抽出する。
     */
    private String extractTitle(String content) {
        Matcher matcher = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE).matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * コードフェンス外の見出しを検出する。
     */
    private List<HeadingInfo> findHeadings(String content) {
        List<HeadingInfo> headings = new ArrayList<>();
        boolean inCodeFence = false;
        // 行末の\rを正規化してから分割
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        int position = 0;

        for (String line : lines) {
            String trimmedLine = line.stripTrailing();
            if (trimmedLine.startsWith("```")) {
                inCodeFence = !inCodeFence;
            } else if (!inCodeFence) {
                Matcher headingMatcher = HEADING_PATTERN.matcher(trimmedLine);
                if (headingMatcher.find()) {
                    int level = headingMatcher.group(1).length();
                    // ## (level 2) と ### (level 3) のみ分割対象。# (level 1) はタイトル扱い
                    if (level >= 2) {
                        headings.add(new HeadingInfo(position, level, headingMatcher.group(2).trim()));
                    }
                }
            }
            position += line.length() + 1; // +1 for \n
        }

        return headings;
    }

    private Map<String, String> buildMetadata(String documentTitle, String sectionTitle, String sourceUrl) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "fintan");
        metadata.put("source_type", "documentation");
        metadata.put("language", "ja");
        if (documentTitle != null && !documentTitle.isEmpty()) {
            metadata.put("title", documentTitle);
        }
        if (sectionTitle != null && !sectionTitle.isEmpty()) {
            metadata.put("section_title", sectionTitle);
        }
        metadata.put("source_url", sourceUrl);
        return metadata;
    }

    /**
     * 見出し情報。
     */
    private record HeadingInfo(int position, int level, String title) {}
}

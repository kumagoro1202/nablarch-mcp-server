package com.tis.nablarch.mcp.rag.parser;

import com.tis.nablarch.mcp.rag.chunking.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTMLドキュメントパーサー。
 *
 * <p>Nablarch公式ドキュメント（Sphinx生成HTML）を対象に、
 * h2/h3見出し単位でセクション分割を行う。
 * Jsoupを使用してHTML解析を行い、コードブロックを保持する。</p>
 */
@Component
public class HtmlDocumentParser implements DocumentParser {

    @Override
    public List<ParsedDocument> parse(String content, String sourceUrl) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrlは必須です");
        }

        Document doc = Jsoup.parse(content);
        String pageTitle = doc.title();

        List<ParsedDocument> results = new ArrayList<>();
        Elements headings = doc.select("h2, h3");

        if (headings.isEmpty()) {
            // 見出しがない場合、本文全体を1ドキュメントとして返す
            String bodyText = extractBodyText(doc);
            if (!bodyText.isBlank()) {
                Map<String, String> metadata = buildMetadata(pageTitle, null, sourceUrl);
                results.add(new ParsedDocument(bodyText, metadata, sourceUrl, ContentType.HTML));
            }
            return results;
        }

        // 見出し前のコンテンツ（イントロ部分）
        String introText = extractTextBeforeFirstHeading(doc, headings.first());
        if (introText != null && !introText.isBlank()) {
            Map<String, String> metadata = buildMetadata(pageTitle, pageTitle, sourceUrl);
            results.add(new ParsedDocument(introText, metadata, sourceUrl, ContentType.HTML));
        }

        // 各見出しセクションを処理
        for (int i = 0; i < headings.size(); i++) {
            Element heading = headings.get(i);
            String sectionTitle = heading.text();
            String sectionContent = extractSectionContent(heading, i < headings.size() - 1 ? headings.get(i + 1) : null);

            if (sectionContent.isBlank()) {
                continue;
            }

            String fullContent = "## " + sectionTitle + "\n\n" + sectionContent;
            Map<String, String> metadata = buildMetadata(pageTitle, sectionTitle, sourceUrl);
            metadata.put("heading_level", heading.tagName());
            results.add(new ParsedDocument(fullContent, metadata, sourceUrl, ContentType.HTML));
        }

        return results;
    }

    /**
     * body要素のテキストを抽出する。
     */
    private String extractBodyText(Document doc) {
        Element body = doc.body();
        if (body == null) {
            return "";
        }
        return convertElementToText(body);
    }

    /**
     * 最初の見出し前のテキストを抽出する。
     */
    private String extractTextBeforeFirstHeading(Document doc, Element firstHeading) {
        if (firstHeading == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Element body = doc.body();
        if (body == null) {
            return null;
        }
        for (Element child : body.children()) {
            if (child.equals(firstHeading)) {
                break;
            }
            if (isHeading(child)) {
                break;
            }
            String text = convertElementToText(child);
            if (!text.isBlank()) {
                sb.append(text).append("\n\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 見出し要素から次の見出し要素までのコンテンツを抽出する。
     */
    private String extractSectionContent(Element heading, Element nextHeading) {
        StringBuilder sb = new StringBuilder();
        Element sibling = heading.nextElementSibling();
        while (sibling != null) {
            if (nextHeading != null && sibling.equals(nextHeading)) {
                break;
            }
            if (isHeading(sibling)) {
                break;
            }
            String text = convertElementToText(sibling);
            if (!text.isBlank()) {
                sb.append(text).append("\n\n");
            }
            sibling = sibling.nextElementSibling();
        }
        return sb.toString().trim();
    }

    /**
     * HTML要素をテキスト形式に変換する。コードブロックはフェンスで囲む。
     */
    private String convertElementToText(Element element) {
        if ("pre".equals(element.tagName())) {
            Element code = element.selectFirst("code");
            String codeText = code != null ? code.text() : element.text();
            String lang = "";
            if (code != null && code.className() != null) {
                String className = code.className();
                if (className.startsWith("language-")) {
                    lang = className.substring("language-".length());
                } else if (!className.isEmpty()) {
                    lang = className;
                }
            }
            return "```" + lang + "\n" + codeText + "\n```";
        }
        if ("table".equals(element.tagName())) {
            return convertTableToMarkdown(element);
        }
        return element.text();
    }

    /**
     * HTMLテーブルをMarkdown形式に変換する。
     */
    private String convertTableToMarkdown(Element table) {
        StringBuilder sb = new StringBuilder();
        Elements rows = table.select("tr");
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cells = row.select("th, td");
            sb.append("| ");
            for (Element cell : cells) {
                sb.append(cell.text()).append(" | ");
            }
            sb.append("\n");
            if (i == 0) {
                sb.append("| ");
                for (int j = 0; j < cells.size(); j++) {
                    sb.append("--- | ");
                }
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }

    private boolean isHeading(Element element) {
        String tag = element.tagName();
        return "h1".equals(tag) || "h2".equals(tag) || "h3".equals(tag)
                || "h4".equals(tag) || "h5".equals(tag) || "h6".equals(tag);
    }

    private Map<String, String> buildMetadata(String pageTitle, String sectionTitle, String sourceUrl) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "nablarch-document");
        metadata.put("source_type", "documentation");
        metadata.put("language", "ja");
        if (pageTitle != null && !pageTitle.isEmpty()) {
            metadata.put("title", pageTitle);
        }
        if (sectionTitle != null && !sectionTitle.isEmpty()) {
            metadata.put("section_title", sectionTitle);
        }
        metadata.put("source_url", sourceUrl);
        return metadata;
    }
}

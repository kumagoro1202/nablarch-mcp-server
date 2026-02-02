package com.tis.nablarch.mcp.rag.parser;

import com.tis.nablarch.mcp.rag.chunking.ContentType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML設定ファイルパーサー。
 *
 * <p>Nablarchのコンポーネント定義XML、ハンドラキュー定義等を対象に、
 * トップレベル要素単位（component, handler等）でParsedDocumentを生成する。
 * javax.xml.parsers（DOM）を使用する。</p>
 */
@Component
public class XmlConfigParser implements DocumentParser {

    @Override
    public List<ParsedDocument> parse(String content, String sourceUrl) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrlは必須です");
        }

        List<ParsedDocument> results = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE対策
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            String rootTag = root.getTagName();

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element element = (Element) node;
                String elementXml = serializeElement(element);
                if (elementXml.isBlank()) {
                    continue;
                }

                // コンテキストとしてファイルパスと親要素を付与
                String fullContent = "<!-- File: " + sourceUrl + " -->\n"
                        + "<!-- Parent: <" + rootTag + "> -->\n"
                        + elementXml;

                Map<String, String> metadata = buildMetadata(element, rootTag, sourceUrl);
                results.add(new ParsedDocument(fullContent, metadata, sourceUrl, ContentType.XML));
            }

            // 子要素がない場合、ルート要素全体を返す
            if (results.isEmpty()) {
                String rootXml = serializeElement(root);
                if (!rootXml.isBlank()) {
                    Map<String, String> metadata = buildMetadata(root, null, sourceUrl);
                    results.add(new ParsedDocument(
                            "<!-- File: " + sourceUrl + " -->\n" + rootXml,
                            metadata, sourceUrl, ContentType.XML));
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            // パース不能なXMLはテキストとして処理
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", "github");
            metadata.put("source_type", "config");
            metadata.put("source_url", sourceUrl);
            metadata.put("parse_error", e.getMessage());
            results.add(new ParsedDocument(content.trim(), metadata, sourceUrl, ContentType.XML));
        }

        return results;
    }

    /**
     * DOM要素をXML文字列にシリアライズする。
     */
    private String serializeElement(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString().trim();
        } catch (TransformerException e) {
            return element.getTextContent();
        }
    }

    private Map<String, String> buildMetadata(Element element, String parentTag, String sourceUrl) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "github");
        metadata.put("source_type", "config");
        metadata.put("language", "xml");
        metadata.put("source_url", sourceUrl);
        metadata.put("element_type", element.getTagName());

        // name属性があれば取得
        String name = element.getAttribute("name");
        if (name != null && !name.isEmpty()) {
            metadata.put("element_name", name);
        }

        // class属性があればFQCNとして取得
        String classAttr = element.getAttribute("class");
        if (classAttr != null && !classAttr.isEmpty()) {
            metadata.put("fqcn", classAttr);
        }

        if (parentTag != null) {
            metadata.put("parent_element", parentTag);
        }

        return metadata;
    }
}

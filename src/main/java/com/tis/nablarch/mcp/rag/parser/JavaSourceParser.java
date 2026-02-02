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
 * Javaソースコードパーサー。
 *
 * <p>正規表現ベースでJavaソースファイルからクラス定義・メソッド・Javadocを抽出する。
 * クラスヘッダ（クラス宣言+フィールド）とメソッド単位でParsedDocumentを生成する。</p>
 */
@Component
public class JavaSourceParser implements DocumentParser {

    /** パッケージ宣言パターン */
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);

    /** クラス/インターフェース宣言パターン */
    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "((?:/\\*\\*[\\s\\S]*?\\*/\\s*)?(?:@\\w+(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)?\\s*(?:abstract\\s+)?(?:class|interface|enum)\\s+(\\w+)(?:<[^>]+>)?(?:\\s+extends\\s+[\\w.<>,\\s]+)?(?:\\s+implements\\s+[\\w.<>,\\s]+)?\\s*\\{)",
            Pattern.MULTILINE
    );

    /** メソッド宣言パターン（Javadoc含む） */
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "((?:/\\*\\*[\\s\\S]*?\\*/\\s*)?(?:@\\w+(?:\\([^)]*\\))?\\s*)*(?:public|protected|private)\\s+(?:static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?(?:<[^>]+>\\s+)?[\\w.<>,\\[\\]]+\\s+(\\w+)\\s*\\([^)]*\\)(?:\\s*throws\\s+[\\w.,\\s]+)?\\s*\\{)",
            Pattern.MULTILINE
    );

    /** フィールド宣言パターン */
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s+(?:private|protected|public)\\s+(?:static\\s+)?(?:final\\s+)?[\\w.<>,\\[\\]]+\\s+(\\w+)\\s*[;=]",
            Pattern.MULTILINE
    );

    /** Javadocコメントパターン */
    private static final Pattern JAVADOC_PATTERN = Pattern.compile("/\\*\\*([\\s\\S]*?)\\*/");

    @Override
    public List<ParsedDocument> parse(String content, String sourceUrl) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("contentは必須です");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("sourceUrlは必須です");
        }

        List<ParsedDocument> results = new ArrayList<>();
        String packageName = extractPackageName(content);
        String className = extractClassName(content);
        String fqcn = (packageName != null && className != null)
                ? packageName + "." + className : className;
        List<String> fieldNames = extractFieldNames(content);

        // クラスヘッダ（クラスJavadoc + 宣言 + フィールド一覧）
        String classHeader = extractClassHeader(content, className, fieldNames);
        if (classHeader != null && !classHeader.isBlank()) {
            Map<String, String> metadata = buildMetadata(sourceUrl, fqcn, className, packageName, "class");
            results.add(new ParsedDocument(classHeader, metadata, sourceUrl, ContentType.JAVA));
        }

        // メソッド抽出
        List<MethodInfo> methods = extractMethods(content);
        for (MethodInfo method : methods) {
            String methodContent = buildMethodChunk(method, className, fqcn, fieldNames);
            Map<String, String> metadata = buildMetadata(sourceUrl, fqcn, className, packageName, "method");
            metadata.put("method_name", method.name);
            results.add(new ParsedDocument(methodContent, metadata, sourceUrl, ContentType.JAVA));
        }

        // メソッドが見つからない場合、Javadocのみ抽出してJAVADOCタイプで返す
        if (methods.isEmpty() && classHeader == null) {
            String javadoc = extractClassJavadoc(content);
            if (javadoc != null && !javadoc.isBlank()) {
                Map<String, String> metadata = buildMetadata(sourceUrl, fqcn, className, packageName, "javadoc");
                results.add(new ParsedDocument(javadoc, metadata, sourceUrl, ContentType.JAVADOC));
            }
        }

        return results;
    }

    private String extractPackageName(String content) {
        Matcher m = PACKAGE_PATTERN.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private String extractClassName(String content) {
        Matcher m = CLASS_PATTERN.matcher(content);
        return m.find() ? m.group(2) : null;
    }

    private List<String> extractFieldNames(String content) {
        List<String> fields = new ArrayList<>();
        Matcher m = FIELD_PATTERN.matcher(content);
        while (m.find()) {
            fields.add(m.group(1));
        }
        return fields;
    }

    /**
     * クラスヘッダを抽出する（クラスJavadoc + 宣言 + フィールド一覧）。
     */
    private String extractClassHeader(String content, String className, List<String> fieldNames) {
        if (className == null) {
            return null;
        }
        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (!classMatcher.find()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(classMatcher.group(1).trim());
        sb.append("\n");

        if (!fieldNames.isEmpty()) {
            sb.append("\n    // フィールド一覧\n");
            for (String field : fieldNames) {
                sb.append("    // - ").append(field).append("\n");
            }
        }
        sb.append("}");

        return sb.toString();
    }

    /**
     * メソッド情報を抽出する。
     */
    private List<MethodInfo> extractMethods(String content) {
        List<MethodInfo> methods = new ArrayList<>();
        Matcher m = METHOD_PATTERN.matcher(content);

        while (m.find()) {
            String declaration = m.group(1).trim();
            String name = m.group(2);
            int startPos = m.start();

            // メソッド本体を取得（ブレースのバランスを追跡）
            String body = extractMethodBody(content, m.end() - 1);
            methods.add(new MethodInfo(name, declaration, body, startPos));
        }

        return methods;
    }

    /**
     * 開きブレースの位置からメソッド本体を抽出する。
     */
    private String extractMethodBody(String content, int openBracePos) {
        int depth = 0;
        int i = openBracePos;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(openBracePos, i + 1);
                }
            } else if (c == '"') {
                // 文字列リテラルをスキップ
                i++;
                while (i < content.length() && content.charAt(i) != '"') {
                    if (content.charAt(i) == '\\') {
                        i++;
                    }
                    i++;
                }
            } else if (c == '\'') {
                // 文字リテラルをスキップ
                i++;
                if (i < content.length() && content.charAt(i) == '\\') {
                    i++;
                }
                i++;
            } else if (c == '/' && i + 1 < content.length()) {
                if (content.charAt(i + 1) == '/') {
                    // 行コメントをスキップ
                    while (i < content.length() && content.charAt(i) != '\n') {
                        i++;
                    }
                } else if (content.charAt(i + 1) == '*') {
                    // ブロックコメントをスキップ
                    i += 2;
                    while (i + 1 < content.length() && !(content.charAt(i) == '*' && content.charAt(i + 1) == '/')) {
                        i++;
                    }
                    i++;
                }
            }
            i++;
        }
        // 閉じブレースが見つからない場合、最大500文字を返す
        int end = Math.min(openBracePos + 500, content.length());
        return content.substring(openBracePos, end);
    }

    private String buildMethodChunk(MethodInfo method, String className, String fqcn, List<String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("// Class: ").append(className != null ? className : "Unknown");
        if (fqcn != null) {
            sb.append(" (").append(fqcn).append(")");
        }
        sb.append("\n");

        if (!fields.isEmpty()) {
            sb.append("// Fields: ").append(String.join(", ", fields)).append("\n");
        }
        sb.append("\n");
        sb.append(method.declaration);
        if (method.body != null) {
            // 宣言は { で終わっているので、本体の { 以降を追加
            if (method.body.startsWith("{")) {
                sb.append("\n").append(method.body.substring(1));
            }
        }
        return sb.toString();
    }

    private String extractClassJavadoc(String content) {
        Matcher classMatcher = CLASS_PATTERN.matcher(content);
        if (!classMatcher.find()) {
            return null;
        }
        String beforeClass = content.substring(0, classMatcher.start());
        Matcher javadocMatcher = JAVADOC_PATTERN.matcher(beforeClass);
        String lastJavadoc = null;
        while (javadocMatcher.find()) {
            lastJavadoc = javadocMatcher.group(0);
        }
        return lastJavadoc;
    }

    private Map<String, String> buildMetadata(String sourceUrl, String fqcn, String className,
                                               String packageName, String elementType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "github");
        metadata.put("source_type", "code");
        metadata.put("language", "java");
        metadata.put("source_url", sourceUrl);
        metadata.put("element_type", elementType);
        if (fqcn != null) {
            metadata.put("fqcn", fqcn);
        }
        if (className != null) {
            metadata.put("class_name", className);
        }
        if (packageName != null) {
            metadata.put("package_name", packageName);
        }
        return metadata;
    }

    private record MethodInfo(String name, String declaration, String body, int position) {}
}

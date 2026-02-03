package com.tis.nablarch.mcp.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * pattern/{name} リソースプロバイダ。
 *
 * <p>Nablarch固有の設計パターンカタログを提供する。</p>
 */
@Component
public class PatternResourceProvider {

    private List<Map<String, Object>> patterns;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        try (InputStream is = loadResource("knowledge/design-patterns.yaml")) {
            Map<String, Object> data = mapper.readValue(is, mapType);
            patterns = (List<Map<String, Object>>) data.get("patterns");
        }
    }

    @SuppressWarnings("unchecked")
    public String getPatternList() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch デザインパターンカタログ\n\n");
        sb.append("| # | パターン名 | カテゴリ | 説明 |\n");
        sb.append("|---|-----------|---------|------|\n");
        int index = 1;
        for (Map<String, Object> pattern : patterns) {
            sb.append("| ").append(index++).append(" | ");
            sb.append(pattern.get("name")).append(" | ");
            sb.append(pattern.get("category")).append(" | ");
            String desc = (String) pattern.get("description");
            sb.append(desc != null ? desc.replace("\n", " ") : "").append(" |\n");
        }
        sb.append("\n---\n*Source: design-patterns.yaml*\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String getPatternDetail(String name) {
        Map<String, Object> pattern = findPattern(name);
        if (pattern == null) {
            return "# Unknown Pattern\n\nUnknown pattern: " + name + "\n\nValid patterns: "
                + String.join(", ", getValidPatternNames());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(pattern.get("name")).append("\n\n");
        sb.append("**カテゴリ**: ").append(pattern.get("category")).append("\n\n");
        sb.append("## 概要\n").append(pattern.get("description")).append("\n\n");
        Object problem = pattern.get("problem");
        if (problem != null) sb.append("## 問題\n").append(problem).append("\n\n");
        Object solution = pattern.get("solution");
        if (solution != null) sb.append("## 解決策\n").append(solution).append("\n\n");
        Object codeExample = pattern.get("code_example");
        if (codeExample != null) sb.append("## コード例\n```java\n").append(codeExample).append("```\n\n");
        sb.append("---\n*Source: design-patterns.yaml*\n");
        return sb.toString();
    }

    public Set<String> getValidPatternNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> pattern : patterns) {
            names.add((String) pattern.get("name"));
        }
        return names;
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private Map<String, Object> findPattern(String name) {
        for (Map<String, Object> pattern : patterns) {
            if (name.equals(pattern.get("name"))) return pattern;
        }
        return null;
    }
}

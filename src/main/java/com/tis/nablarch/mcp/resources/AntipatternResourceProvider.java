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
 * antipattern/{name} リソースプロバイダ。
 *
 * <p>Nablarch開発でよく見られるアンチパターンとその修正方法を提供する。</p>
 */
@Component
public class AntipatternResourceProvider {

    private List<Map<String, Object>> antipatterns;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        try (InputStream is = loadResource("knowledge/antipattern-catalog.yaml")) {
            Map<String, Object> data = mapper.readValue(is, mapType);
            antipatterns = (List<Map<String, Object>>) data.get("antipatterns");
        }
    }

    public String getAntipatternList() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch アンチパターンカタログ\n\n");
        sb.append("| # | 名前 | カテゴリ | 重要度 | 説明 |\n");
        sb.append("|---|------|---------|--------|------|\n");
        int index = 1;
        for (Map<String, Object> ap : antipatterns) {
            sb.append("| ").append(index++).append(" | ");
            sb.append(ap.get("name")).append(" | ");
            sb.append(ap.get("category")).append(" | ");
            sb.append(ap.get("severity")).append(" | ");
            sb.append(ap.get("title")).append(" |\n");
        }
        sb.append("\n---\n*Source: antipattern-catalog.yaml*\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String getAntipatternDetail(String name) {
        Map<String, Object> ap = findAntipattern(name);
        if (ap == null) {
            return "# Unknown Antipattern\n\nUnknown antipattern: " + name
                + "\n\nValid antipatterns: " + String.join(", ", getValidAntipatternNames());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(ap.get("title")).append("\n\n");
        sb.append("**名前**: ").append(ap.get("name")).append("\n");
        sb.append("**カテゴリ**: ").append(ap.get("category")).append("\n");
        sb.append("**重要度**: ").append(ap.get("severity")).append("\n\n");
        sb.append("## 概要\n").append(ap.get("description")).append("\n\n");
        Object problem = ap.get("problem");
        if (problem != null) sb.append("## 問題\n").append(problem).append("\n\n");
        Object badExample = ap.get("bad_example");
        if (badExample != null) sb.append("## 悪い例\n```java\n").append(badExample).append("```\n\n");
        Object goodExample = ap.get("good_example");
        if (goodExample != null) sb.append("## 良い例\n```java\n").append(goodExample).append("```\n\n");
        Object fixStrategy = ap.get("fix_strategy");
        if (fixStrategy != null) sb.append("## 修正方針\n").append(fixStrategy).append("\n\n");
        sb.append("---\n*Source: antipattern-catalog.yaml*\n");
        return sb.toString();
    }

    public Set<String> getValidAntipatternNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> ap : antipatterns) {
            names.add((String) ap.get("name"));
        }
        return names;
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private Map<String, Object> findAntipattern(String name) {
        if (name == null) return null;
        for (Map<String, Object> ap : antipatterns) {
            if (name.equals(ap.get("name"))) return ap;
        }
        return null;
    }
}

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
 * config/{name} リソースプロバイダ。
 *
 * <p>NablarchのXML設定テンプレートを提供する。</p>
 */
@Component
public class ConfigResourceProvider {

    private List<Map<String, Object>> templates;

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        try (InputStream is = loadResource("knowledge/config-templates.yaml")) {
            Map<String, Object> data = mapper.readValue(is, mapType);
            templates = (List<Map<String, Object>>) data.get("templates");
        }
    }

    public String getTemplateList() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nablarch XML設定テンプレート一覧\n\n");
        sb.append("| # | テンプレート名 | カテゴリ | 説明 |\n");
        sb.append("|---|--------------|---------|------|\n");
        int index = 1;
        for (Map<String, Object> template : templates) {
            sb.append("| ").append(index++).append(" | ");
            sb.append(template.get("name")).append(" | ");
            sb.append(template.get("category")).append(" | ");
            String desc = (String) template.get("description");
            sb.append(desc != null ? desc.replace("\n", " ") : "").append(" |\n");
        }
        sb.append("\n---\n*Source: config-templates.yaml*\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public String getTemplate(String name) {
        Map<String, Object> template = findTemplate(name);
        if (template == null) {
            return "# Unknown Config Template\n\nUnknown config template: " + name
                + "\n\nValid templates: " + String.join(", ", getValidTemplateNames());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<!--\n");
        sb.append("  Nablarch Configuration Template: ").append(template.get("name")).append("\n");
        sb.append("  Category: ").append(template.get("category")).append("\n");
        sb.append("  Description: ").append(template.get("description")).append("\n");
        sb.append("-->\n");
        Object content = template.get("template");
        if (content != null) sb.append(content);
        return sb.toString();
    }

    public Set<String> getValidTemplateNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Map<String, Object> template : templates) {
            names.add((String) template.get("name"));
        }
        return names;
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private Map<String, Object> findTemplate(String name) {
        if (name == null) return null;
        for (Map<String, Object> template : templates) {
            if (name.equals(template.get("name"))) return template;
        }
        return null;
    }
}

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
 * example/{type} リソースプロバイダ。
 *
 * <p>Nablarchのサンプルアプリケーションコードを提供する。</p>
 */
@Component
public class ExampleResourceProvider {

    private List<Map<String, Object>> examples;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        try (InputStream is = loadResource("knowledge/example-catalog.yaml")) {
            Map<String, Object> data = yamlMapper.readValue(is, mapType);
            examples = (List<Map<String, Object>>) data.get("examples");
        }
    }

    @SuppressWarnings("unchecked")
    public String getExampleList() {
        try {
            List<Map<String, Object>> exampleList = new ArrayList<>();
            for (Map<String, Object> example : examples) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("type", example.get("type"));
                entry.put("description", example.get("description"));
                entry.put("app_type", example.get("app_type"));
                List<?> files = (List<?>) example.get("files");
                entry.put("file_count", files != null ? files.size() : 0);
                entry.put("reference_repo", example.get("reference_repo"));
                exampleList.add(entry);
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "example_list");
            response.put("examples", exampleList);
            response.put("total_examples", exampleList.size());
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @SuppressWarnings("unchecked")
    public String getExampleDetail(String type) {
        try {
            Map<String, Object> example = findExample(type);
            if (example == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Unknown example type: " + type);
                error.put("valid_types", getValidExampleTypes());
                return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "example_detail");
            response.put("example_type", example.get("type"));
            response.put("description", example.get("description"));
            response.put("app_type", example.get("app_type"));
            response.put("reference_repo", example.get("reference_repo"));
            response.put("key_patterns", example.get("key_patterns"));
            response.put("files", example.get("files"));
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public Set<String> getValidExampleTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Map<String, Object> example : examples) {
            types.add((String) example.get("type"));
        }
        return types;
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private Map<String, Object> findExample(String type) {
        for (Map<String, Object> example : examples) {
            if (type.equals(example.get("type"))) return example;
        }
        return null;
    }
}

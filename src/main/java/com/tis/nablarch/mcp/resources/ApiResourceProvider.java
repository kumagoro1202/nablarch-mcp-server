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
 * api/{module}/{class} リソースプロバイダ。
 *
 * <p>NablarchのAPIリファレンス（Javadoc相当）を構造化データとして提供する。</p>
 */
@Component
public class ApiResourceProvider {

    private List<Map<String, Object>> modules;
    private List<Map<String, Object>> apiPatterns;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};

        try (InputStream is = loadResource("knowledge/module-catalog.yaml")) {
            Map<String, Object> data = yamlMapper.readValue(is, mapType);
            modules = (List<Map<String, Object>>) data.get("modules");
        }

        try (InputStream is = loadResource("knowledge/api-patterns.yaml")) {
            Map<String, Object> data = yamlMapper.readValue(is, mapType);
            apiPatterns = (List<Map<String, Object>>) data.get("patterns");
        }
    }

    public String getModuleList() {
        try {
            List<Map<String, Object>> moduleList = new ArrayList<>();
            for (Map<String, Object> module : modules) {
                Map<String, Object> entry = new LinkedHashMap<>();
                String artifactId = (String) module.get("artifact_id");
                String moduleKey = extractModuleKey(artifactId);
                entry.put("name", module.get("name"));
                entry.put("module_key", moduleKey);
                entry.put("category", module.get("category"));
                entry.put("description", module.get("description"));
                List<?> keyClasses = (List<?>) module.get("key_classes");
                entry.put("class_count", keyClasses != null ? keyClasses.size() : 0);
                entry.put("uri", "nablarch://api/" + moduleKey);
                moduleList.add(entry);
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "module_list");
            response.put("modules", moduleList);
            response.put("total_modules", moduleList.size());
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @SuppressWarnings("unchecked")
    public String getClassList(String moduleKey) {
        try {
            Map<String, Object> module = findModuleByKey(moduleKey);
            if (module == null) {
                return createModuleNotFoundError(moduleKey);
            }
            List<Map<String, Object>> classList = new ArrayList<>();
            List<Map<String, Object>> keyClasses = (List<Map<String, Object>>) module.get("key_classes");
            if (keyClasses != null) {
                for (Map<String, Object> cls : keyClasses) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    String fqcn = (String) cls.get("fqcn");
                    entry.put("simple_name", extractSimpleName(fqcn));
                    entry.put("fqcn", fqcn);
                    entry.put("description", cls.get("description"));
                    classList.add(entry);
                }
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "class_list");
            response.put("module_key", moduleKey);
            response.put("classes", classList);
            response.put("total_classes", classList.size());
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @SuppressWarnings("unchecked")
    public String getClassDetail(String moduleKey, String className) {
        try {
            Map<String, Object> module = findModuleByKey(moduleKey);
            if (module == null) {
                return createModuleNotFoundError(moduleKey);
            }
            List<Map<String, Object>> keyClasses = (List<Map<String, Object>>) module.get("key_classes");
            Map<String, Object> classInfo = null;
            if (keyClasses != null) {
                for (Map<String, Object> cls : keyClasses) {
                    String fqcn = (String) cls.get("fqcn");
                    if (extractSimpleName(fqcn).equals(className)) {
                        classInfo = cls;
                        break;
                    }
                }
            }
            if (classInfo == null) {
                return "{\"error\": \"Class not found: " + className + "\"}";
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "class_detail");
            response.put("module", module.get("name"));
            response.put("simple_name", className);
            response.put("fqcn", classInfo.get("fqcn"));
            response.put("description", classInfo.get("description"));
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    public Set<String> getValidModuleKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (Map<String, Object> module : modules) {
            keys.add(extractModuleKey((String) module.get("artifact_id")));
        }
        return keys;
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    private String extractModuleKey(String artifactId) {
        return artifactId != null && artifactId.startsWith("nablarch-")
            ? artifactId.substring("nablarch-".length()) : artifactId;
    }

    private String extractSimpleName(String fqcn) {
        if (fqcn == null) return "";
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    private Map<String, Object> findModuleByKey(String moduleKey) {
        for (Map<String, Object> module : modules) {
            if (extractModuleKey((String) module.get("artifact_id")).equals(moduleKey)) {
                return module;
            }
        }
        return null;
    }

    private String createModuleNotFoundError(String moduleKey) {
        try {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Unknown module: " + moduleKey);
            error.put("valid_modules", getValidModuleKeys());
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
        } catch (Exception e) {
            return "{\"error\": \"Unknown module\"}";
        }
    }
}

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
 * version リソースプロバイダ。
 *
 * <p>Nablarchフレームワークのバージョン情報、対応プラットフォーム、モジュール一覧を提供する。</p>
 */
@Component
public class VersionResourceProvider {

    private Map<String, Object> versionInfo;
    private List<Map<String, Object>> modules;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};

        try (InputStream is = loadResource("knowledge/version-info.yaml")) {
            Map<String, Object> data = yamlMapper.readValue(is, mapType);
            versionInfo = (Map<String, Object>) data.get("version_info");
        }

        try (InputStream is = loadResource("knowledge/module-catalog.yaml")) {
            Map<String, Object> data = yamlMapper.readValue(is, mapType);
            modules = (List<Map<String, Object>>) data.get("modules");
        }
    }

    @SuppressWarnings("unchecked")
    public String getVersionInfo() {
        try {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("type", "version_info");
            response.put("framework_name", versionInfo.get("framework_name"));
            response.put("latest_version", versionInfo.get("latest_version"));
            response.put("release_date", versionInfo.get("release_date"));
            response.put("supported_versions", versionInfo.get("supported_versions"));
            response.put("platforms", versionInfo.get("platforms"));
            response.put("bom", versionInfo.get("bom"));

            List<Map<String, Object>> moduleSummaries = new ArrayList<>();
            for (Map<String, Object> module : modules) {
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("name", module.get("name"));
                summary.put("artifact_id", module.get("artifact_id"));
                summary.put("category", module.get("category"));
                summary.put("description", module.get("description"));
                List<?> keyClasses = (List<?>) module.get("key_classes");
                summary.put("key_class_count", keyClasses != null ? keyClasses.size() : 0);
                moduleSummaries.add(summary);
            }
            response.put("modules", moduleSummaries);
            response.put("total_modules", moduleSummaries.size());
            response.put("links", versionInfo.get("links"));

            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private InputStream loadResource(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }
}

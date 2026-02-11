package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ModuleEntry {
    public String name;
    @JsonProperty("group_id")
    public String groupId;
    @JsonProperty("artifact_id")
    public String artifactId;
    public String description;
    public String category;
    @JsonProperty("key_classes")
    public List<KeyClassEntry> keyClasses;
    @JsonProperty("since_version")
    public String sinceVersion;
    public List<String> dependencies;
    @JsonProperty("source_url")
    public String sourceUrl;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KeyClassEntry {
        public String fqcn;
        public String description;
    }
}

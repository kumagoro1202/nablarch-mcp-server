package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DesignPatternEntry {
    public String name;
    public String category;
    public String description;
    public String problem;
    public String solution;
    public String structure;
    @JsonProperty("code_example")
    public String codeExample;
    @JsonProperty("related_patterns")
    public List<String> relatedPatterns;
    @JsonProperty("applicable_app_types")
    public List<String> applicableAppTypes;
    public List<String> references;
    @JsonProperty("source_url")
    public String sourceUrl;
    @JsonProperty("source_urls")
    public List<String> sourceUrls;
}

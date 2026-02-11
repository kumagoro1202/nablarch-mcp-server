package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiPatternEntry {
    public String name;
    public String category;
    public String description;
    public String fqcn;
    @JsonProperty("related_patterns")
    public List<String> relatedPatterns;
    public String example;
    @JsonProperty("source_url")
    public String sourceUrl;
    @JsonProperty("source_urls")
    public List<String> sourceUrls;
}

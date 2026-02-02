package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigTemplateEntry {
    public String name;
    public String category;
    @JsonProperty("app_type")
    public String appType;
    public String description;
    public String template;
    public List<TemplateParameter> parameters;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TemplateParameter {
        public String name;
        public String description;
        @JsonProperty("default")
        public String defaultValue;
    }
}

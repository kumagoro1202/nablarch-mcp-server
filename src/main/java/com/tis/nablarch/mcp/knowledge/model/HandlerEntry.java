package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HandlerEntry {
    public String name;
    public String fqcn;
    public String description;
    public int order;
    public boolean required;
    public String thread;
    public ConstraintRef constraints;
    @JsonProperty("source_url")
    public String sourceUrl;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConstraintRef {
        @JsonProperty("must_before")
        public List<String> mustBefore;
        @JsonProperty("must_after")
        public List<String> mustAfter;
    }
}

package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HandlerConstraintEntry {
    public String handler;
    public String fqcn;
    public String rule;
    @JsonProperty("must_before")
    public List<String> mustBefore;
    @JsonProperty("must_after")
    public List<String> mustAfter;
    @JsonProperty("incompatible_with")
    public List<String> incompatibleWith;
    @JsonProperty("required_by_app_type")
    public List<String> requiredByAppType;
    public String reason;
}

package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorEntry {
    public String id;
    public String category;
    @JsonProperty("error_message")
    public String errorMessage;
    public String cause;
    public String solution;
    @JsonProperty("example_stack_trace")
    public String exampleStackTrace;
    @JsonProperty("related_handlers")
    public List<String> relatedHandlers;
    @JsonProperty("related_modules")
    public List<String> relatedModules;
    public String severity;
}

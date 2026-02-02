package com.tis.nablarch.mcp.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HandlerCatalogEntry {
    public String description;
    public List<HandlerEntry> handlers;
}

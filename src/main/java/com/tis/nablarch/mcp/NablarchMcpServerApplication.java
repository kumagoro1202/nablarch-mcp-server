package com.tis.nablarch.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nablarch MCP Server - Spring Boot entry point.
 *
 * <p>This application provides a Model Context Protocol (MCP) server
 * that exposes Nablarch framework knowledge, tools, and resources
 * to AI coding assistants such as Claude Code and Copilot.</p>
 *
 * <p>Phase 1 supports STDIO transport for local development use.</p>
 */
@SpringBootApplication
public class NablarchMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NablarchMcpServerApplication.class, args);
    }
}

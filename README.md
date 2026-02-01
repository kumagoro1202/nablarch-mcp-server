# Nablarch MCP Server

A [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that provides
[Nablarch](https://nablarch.github.io/) framework knowledge to AI coding assistants.

## What is this?

This MCP server enables AI tools like Claude Code, Copilot, and Cursor to generate accurate
Nablarch framework code by providing:

- **Tools**: API search, handler queue validation
- **Resources**: API specifications, handler catalogs, design patterns
- **Knowledge Base**: Curated Nablarch framework knowledge (handler queue architecture, coding patterns, best practices)

## Architecture

```
AI Coding Tool ←→ MCP Client ←→ Nablarch MCP Server ←→ Knowledge Base
```

See [docs/architecture.md](docs/architecture.md) for details.

## Requirements

- Java 17 or later
- Gradle 8.x (wrapper included)

## Quick Start

### Build

```bash
./gradlew build
```

### Run (STDIO mode)

```bash
./gradlew bootRun
```

### Configure with Claude Code

Add to your MCP configuration:

```json
{
  "mcpServers": {
    "nablarch": {
      "command": "java",
      "args": ["-jar", "build/libs/nablarch-mcp-server-0.1.0-SNAPSHOT.jar"]
    }
  }
}
```

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17+ |
| Framework | Spring Boot 3.4.x |
| MCP SDK | [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) 0.17.x |
| Build | Gradle (Kotlin DSL) |
| Test | JUnit 5 + Spring Test |
| Transport | STDIO (Phase 1) |

## Project Status

**Phase 1**: Initial project skeleton with stub implementations.

- [x] Project structure
- [x] MCP SDK integration
- [x] Tool stubs (search_api, validate_handler_queue)
- [x] Resource stubs (API spec, handler spec)
- [x] Knowledge base structure
- [ ] Tool implementation
- [ ] Resource implementation
- [ ] MCP Inspector testing

## License

[Apache License 2.0](LICENSE)

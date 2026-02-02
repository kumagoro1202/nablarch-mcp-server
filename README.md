# Nablarch MCP Server

A **RAG-enhanced** [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server that provides
[Nablarch](https://nablarch.github.io/) framework knowledge to AI coding assistants.

## What is this?

Nablarch is a Java application framework for mission-critical systems, but suffers from a severe lack of community resources compared to mainstream frameworks. This MCP server bridges that gap by combining **RAG (Retrieval-Augmented Generation)** with **MCP** to give AI tools deep Nablarch knowledge.

| Technology | Role |
|---|---|
| **RAG** | Semantic search over Nablarch docs, code, and Javadoc |
| **MCP** | Standard protocol to expose knowledge as Tools, Resources, and Prompts |
| **RAG + MCP** | AI tools can "know and use" Nablarch knowledge with high precision |

### What it provides

- **Tools** (10): Semantic search, handler queue design, code generation, config validation, API search, test generation, troubleshooting, migration analysis, pattern recommendation, handler queue optimization
- **Resources** (8 URI patterns): Handler catalogs, API references, design patterns, learning guides, example code, config templates, anti-patterns, version info
- **Prompts** (6 templates): Web app creation, REST API creation, batch creation, handler queue setup, code review, troubleshooting
- **RAG Pipeline**: Hybrid search (BM25 + vector similarity) over Nablarch's official documentation, 113 GitHub repositories, Javadoc, and Fintan content

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                     AI Coding Tools                               │
│   Claude Code  |  Cursor  |  Copilot  |  VS Code                 │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │                      MCP Client                           │   │
│   └──────────────────────────┬───────────────────────────────┘   │
└──────────────────────────────┼───────────────────────────────────┘
                               │ JSON-RPC 2.0
                               │ STDIO (local) / Streamable HTTP (remote)
┌──────────────────────────────▼───────────────────────────────────┐
│              Nablarch MCP Server (Spring Boot)                    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   MCP Protocol Layer                         │ │
│  │   Tools (10)  |  Resources (8 types)  |  Prompts (6)        │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │                                    │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │                   RAG Engine (embedded)                      │ │
│  │   Semantic Search  |  Hybrid Search  |  Re-ranking          │ │
│  │   Doc Embedder (Jina v4)  |  Code Embedder (Voyage-code-3) │ │
│  └──────────────────────────┬──────────────────────────────────┘ │
│                              │                                    │
│  ┌──────────────────────────▼──────────────────────────────────┐ │
│  │              PostgreSQL + pgvector                           │ │
│  │   Docs Index  |  Code Index  |  Javadoc Index  |  Config    │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

## Requirements

- Java 17 or later
- Gradle 8.x (wrapper included)
- PostgreSQL 16+ with pgvector extension (for RAG features, Phase 2+)

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

Add to your MCP configuration (`.claude/mcp.json`):

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

### Run with Docker (Phase 4)

```bash
docker compose up
```

## Technology Stack

| Component | Technology | Rationale |
|---|---|---|
| Language | Java 17+ | Nablarch ecosystem consistency |
| Framework | Spring Boot 3.4.x | MCP Boot Starter support |
| MCP SDK | [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk) 0.17.x | Official SDK, Spring AI integration |
| Build | Gradle (Kotlin DSL) | Modern build system |
| Test | JUnit 5 + Spring Test | Standard Java testing |
| Vector DB | PostgreSQL + pgvector | SQL + vector search, cost-efficient |
| Doc Embedding | Jina embeddings-v4 | 89 languages, 32K context, OSS |
| Code Embedding | Voyage-code-3 | Best-in-class Java/XML embedding |
| Re-ranker | Cross-Encoder | Precision improvement on hybrid search |
| Transport | STDIO / Streamable HTTP | Local dev + remote team sharing |

## Knowledge Sources

The RAG pipeline indexes knowledge from:

| Source | Content | Scale |
|---|---|---|
| [Nablarch Official Docs](https://nablarch.github.io/) | Architecture, API specs, guides | Hundreds of pages |
| [GitHub nablarch org](https://github.com/nablarch) | 113 repositories (source code) | Tens of thousands of files |
| Javadoc | Full API reference | All modules |
| [Fintan](https://fintan.jp/) | Learning materials, dev standards | Dozens of articles |

## Project Status

**Current**: Planning & design phase. Project skeleton with stub implementations.

### Phase 1: MCP Foundation + Static Knowledge
- [x] Project structure
- [x] MCP SDK integration
- [x] Tool stubs (search_api, validate_handler_queue)
- [x] Resource stubs (API spec, handler spec)
- [x] Knowledge base structure
- [ ] Tool implementation
- [ ] Resource implementation
- [ ] Prompt implementation
- [ ] MCP Inspector testing

### Phase 2: RAG Engine Integration
- [ ] pgvector setup + schema
- [ ] Document ingestion pipeline
- [ ] Dual embedding (Jina v4 + Voyage-code-3)
- [ ] `semantic_search` tool
- [ ] Hybrid search (BM25 + vector)
- [ ] Re-ranking (Cross-Encoder)
- [ ] Search quality evaluation

### Phase 3: Full Tool Suite + Code Generation
- [ ] `design_handler_queue` tool (RAG-powered)
- [ ] `generate_code` tool (RAG-powered)
- [ ] `generate_test` tool
- [ ] `troubleshoot` tool
- [ ] `analyze_migration` tool
- [ ] Streamable HTTP transport
- [ ] All Prompt templates

### Phase 4: Production Deployment
- [ ] Docker Compose deployment
- [ ] OAuth 2.0 authentication
- [ ] Auto-update pipeline (GitHub webhook)
- [ ] Monitoring & logging
- [ ] IDE integration modules

## Documentation

| Document | Description |
|---|---|
| [docs/overview.md](docs/overview.md) | Project vision, target users, feature overview |
| [docs/architecture.md](docs/architecture.md) | RAG-enhanced architecture design, component diagrams, data model |
| [docs/use-cases.md](docs/use-cases.md) | 12 use cases with sequence diagrams and I/O examples |
| [docs/user-guide.md](docs/user-guide.md) | Setup, configuration, and usage guide |
| [docs/decisions/](docs/decisions/) | Architecture Decision Records (ADRs) |
| [docs/research/](docs/research/) | Research reports and analysis |

## Related Resources

- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)
- [Spring AI MCP](https://docs.spring.io/spring-ai/reference/api/mcp.html)
- [Nablarch Official Docs](https://nablarch.github.io/)
- [Nablarch GitHub](https://github.com/nablarch)
- [pgvector](https://github.com/pgvector/pgvector)

## License

[Apache License 2.0](LICENSE)

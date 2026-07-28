# Codebase Memory MCP

## Purpose

The `user-codebase-memory-mcp` server indexes this monorepo into a knowledge graph
(functions, classes, routes, CALLS/IMPORTS/USAGE edges).

**Routing:** see `AI/codebase-exploration.md` and `.cursor/rules/codebase-exploration.mdc`.
Memory is the **primary** tool for symbol/call/route exploration; Graphify covers architecture/docs; Grep is last.

Cursor rule: `.cursor/rules/codebase-memory.mdc` (alwaysApply tool map).

## Project id

| Field | Value |
|-------|--------|
| Workspace path | `D:/repos/calorieTracker` |
| Indexed project name | `D-repos-calorieTracker` |

If tools say `project not found`, use `available_projects` from the error response.

## Index status

Re-index when the graph looks stale:

```text
index_repository
  repo_path: D:/repos/calorieTracker
  mode: fast | moderate | full
  name: (optional override)
  persistence: true  # optional shared .codebase-memory/graph.db.zst
```

- `fast` — filtered files, LSP call/usage, no similarity/semantic edges
- `moderate` / `full` — needed for `semantic_query` on `search_graph`

## Workflow cheat sheet

1. **Orient** — `get_architecture` with `aspects: ["overview"]` (or `path` for one service).
2. **Find symbol** — `search_graph` with `query: "natural language"` (BM25 + label boost).
3. **Read body** — `get_code_snippet` with the returned `qualified_name`.
4. **Impact** — `trace_path` on the function (`inbound` / `outbound` / `both`).
5. **Literal string** — `search_code` (graph-enriched) before workspace Grep.
6. **Hard patterns** — `query_graph` Cypher after `get_graph_schema`.

## When Grep is still OK

- Memory returned nothing useful after a re-index attempt
- Searching for a non-code literal (env key, log line, migration SQL fragment)
- Opening a file the user already named

## Related

- Layered exploration: `AI/codebase-exploration.md`
- Graphify: `.cursor/rules/graphify.mdc`
- Context7 for *external* library docs: `.cursor/rules/context7.mdc`

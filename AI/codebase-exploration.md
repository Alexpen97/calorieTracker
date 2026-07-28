# Codebase exploration (Memory + Graphify)

## Purpose

Agents explore this monorepo with a **layered** approach so each graph is used for its strengths, and Grep/Glob stay last resort.

Cursor rules:

| Rule | Role |
|------|------|
| `.cursor/rules/codebase-exploration.mdc` | Decision tree (alwaysApply) |
| `.cursor/rules/codebase-memory.mdc` | Memory MCP tool map + project id |
| `.cursor/rules/graphify.mdc` | Graphify CLI + outputs |

User-level `~/.cursor/rules/graphify.mdc` defers to the project layering when that rule is present.

## Layering

```text
Code question (symbol / callers / HTTP)?
  → Codebase Memory MCP
  → get_code_snippet / Read
  → (optional) Graphify if docs/architecture still needed

Architecture / docs / “why” / communities?
  → Graphify query | path | explain
  → GRAPH_REPORT.md / graph.html if needed
  → verify code claims with Memory → Read

Literal string / env / error text?
  → Memory search_code → Grep only if missing

Grep / Glob / blind tree Read = last resort
```

## Codebase Memory

| Field | Value |
|-------|--------|
| Server | `user-codebase-memory-mcp` |
| Project id | `D-repos-calorieTracker` |
| Workspace | `D:/repos/calorieTracker` |

Best for: definitions, implementations, `trace_path` (calls / data_flow / cross_service), `search_code`, precise file+line.

Re-index when stale:

```text
index_repository
  repo_path: D:/repos/calorieTracker
  mode: fast | moderate | full
  persistence: true   # optional shared .codebase-memory/graph.db.zst
```

- `fast` — structural; default for refresh
- `moderate` / `full` — needed for `semantic_query` on `search_graph`

## Graphify

| Field | Value |
|-------|--------|
| Output dir | `graphify-out/` |
| Graph | `graphify-out/graph.json` |
| Report | `graphify-out/GRAPH_REPORT.md` |
| Viz | `graphify-out/graph.html` |

Best for: communities, god nodes, doc↔code links, AI notes/mockups, inferred narrative edges.

```bash
graphify query "<question>"
graphify path "A" "B"
graphify explain "<concept>"
graphify update .          # after code edits (AST-only)
```

Full rebuild (rare): `/graphify .` skill pipeline or `graphify extract`.

Treat INFERRED/AMBIGUOUS edges as hypotheses; verify in source. Health warnings about dangling endpoints mean some semantic edges did not resolve to AST ids.

## When Grep is still OK

- Both graphs failed after refresh/retry
- Non-code literal Memory cannot rank (env key, log line, migration fragment)
- User already named the file

## Related

- Context7 for *external* library docs: `.cursor/rules/context7.mdc`
- Memory = this repo’s code structure; Graphify = this repo’s code+docs map

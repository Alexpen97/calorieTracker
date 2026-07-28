# Graphify

## Purpose

[graphify](https://github.com/Graphify-Labs/graphify) builds a local knowledge graph under `graphify-out/` from code (AST) plus docs/images (semantic). Used for architecture orientation, communities, and doc↔code links.

**Routing:** see `AI/codebase-exploration.md`. Graphify is **not** first for every code lookup — Codebase Memory owns symbols/calls/routes.

Cursor rule: `.cursor/rules/graphify.mdc`.

## Outputs

| Path | Use |
|------|-----|
| `graphify-out/graph.json` | Queryable graph |
| `graphify-out/graph.html` | Interactive viz |
| `graphify-out/GRAPH_REPORT.md` | God nodes, surprises, suggested questions |
| `graphify-out/manifest.json` | Enables incremental `graphify update` |

## Common commands

```bash
graphify query "<question>" [--budget N]
graphify path "A" "B"
graphify explain "<concept>"
graphify update .                 # AST refresh after edits
graphify god-nodes [--top N]
```

Full rebuild: invoke `/graphify .` (skill pipeline) when the graph is missing or needs semantic re-extraction.

## Maintenance

- After substantial code changes: `graphify update .` (no API key).
- Re-run full `/graphify .` when docs/images changed a lot or semantic cache is wrong.
- Optional: set `GEMINI_API_KEY` for Gemini-backed semantic extraction on docs/images.

## Caveats

- INFERRED / AMBIGUOUS edges are hypotheses — verify with Memory → Read.
- Dangling-endpoint diagnostics mean some semantic node ids did not match AST ids.
- Prefer Memory when Graphify BFS returns a huge truncated neighborhood for a code symbol.

## Related

- Layered exploration: `AI/codebase-exploration.md`
- Codebase Memory: `AI/codebase-memory.md`

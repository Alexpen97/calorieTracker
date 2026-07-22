# Context7 setup

## Problem

Cursor’s default/public Context7 MCP often runs without our team key and hits `Monthly quota exceeded`.

## Solution

1. **Project MCP** — `.cursor/mcp.json` configures remote Context7 with header `CONTEXT7_API_KEY`.
2. **Always-on rule** — `.cursor/rules/context7.mdc` requires agents to fetch Context7 docs **before writing any library/framework code**, prefer the project MCP, and fall back to REST/`scripts/context7.sh` when public MCP is quota-locked.
3. **Helper** — `scripts/context7.sh` reads the key from `CONTEXT7_API_KEY` or `.cursor/mcp.json` and calls:
   - `GET /api/v2/libs/search`
   - `GET /api/v2/context`

## Agent workflow (enforced by rule)

1. `resolve-library-id` → pick official / version-specific ID
2. `query-docs` with a focused question
3. Write code from the returned docs (docs win over training memory)

Session server names may appear as `context7`, `user-context7`, or `plugin-context7-plugin-context7`. Prefer the authenticated project key path; if MCP returns quota/auth errors, use the helper/REST fallback immediately.

## Verified (2026-07-22)

- Key format `ctx7sk-…` authenticates against `https://mcp.context7.com/mcp` and the REST API.
- `user-context7` MCP: `resolve-library-id` (React → `/reactjs/react.dev`) and `query-docs` succeed.
- Unauthenticated / public MCP path returns monthly quota exceeded in this environment.
- Smoke test: `scripts/test-context7.sh`

## Ops notes

- Optional: set `CONTEXT7_API_KEY` in the Cloud Agent / local environment to override mcp.json without editing files.
- If this key was exposed outside the team, rotate it at https://context7.com/dashboard and update `.cursor/mcp.json`.
- Desktop Cursor: reload MCP after pulling so project `.cursor/mcp.json` is picked up.

## Remaining

- Confirm Cloud Agent runs pick up project MCP on next boot (rule + helper cover the gap if not).

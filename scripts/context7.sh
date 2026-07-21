#!/usr/bin/env bash
# Context7 CLI helper — uses CONTEXT7_API_KEY or the key from .cursor/mcp.json.
# Usage:
#   scripts/context7.sh search <libraryName> <query>
#   scripts/context7.sh docs <libraryId> <query>
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MCP_JSON="${ROOT}/.cursor/mcp.json"
API_BASE="https://context7.com/api/v2"

resolve_key() {
  if [[ -n "${CONTEXT7_API_KEY:-}" ]]; then
    printf '%s' "$CONTEXT7_API_KEY"
    return
  fi
  if [[ -f "$MCP_JSON" ]]; then
    # Prefer python for robust JSON; fall back to grep/sed.
    if command -v python3 >/dev/null 2>&1; then
      python3 - "$MCP_JSON" <<'PY'
import json, sys
path = sys.argv[1]
with open(path) as f:
    data = json.load(f)
servers = data.get("mcpServers") or data
ctx = servers.get("context7") or {}
headers = ctx.get("headers") or {}
key = headers.get("CONTEXT7_API_KEY") or headers.get("Authorization", "")
if key.lower().startswith("bearer "):
    key = key[7:].strip()
if not key:
    sys.exit("CONTEXT7_API_KEY not found in .cursor/mcp.json")
print(key)
PY
      return
    fi
    key="$(grep -oE 'ctx7sk-[A-Za-z0-9-]+' "$MCP_JSON" | head -n1 || true)"
    if [[ -n "$key" ]]; then
      printf '%s' "$key"
      return
    fi
  fi
  echo "CONTEXT7_API_KEY not set and no key found in .cursor/mcp.json" >&2
  exit 1
}

urlencode() {
  if command -v python3 >/dev/null 2>&1; then
    python3 -c 'import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
  else
    # Minimal fallback
    printf '%s' "$1" | sed 's|/|%2F|g; s| |%20|g'
  fi
}

KEY="$(resolve_key)"
CMD="${1:-}"
shift || true

case "$CMD" in
  search)
    lib="${1:-}"
    query="${2:-}"
    if [[ -z "$lib" || -z "$query" ]]; then
      echo "Usage: $0 search <libraryName> <query>" >&2
      exit 2
    fi
    curl -sS \
      "${API_BASE}/libs/search?libraryName=$(urlencode "$lib")&query=$(urlencode "$query")" \
      -H "Authorization: Bearer ${KEY}"
    echo
    ;;
  docs|context)
    library_id="${1:-}"
    query="${2:-}"
    if [[ -z "$library_id" || -z "$query" ]]; then
      echo "Usage: $0 docs <libraryId> <query>" >&2
      exit 2
    fi
    curl -sS \
      "${API_BASE}/context?libraryId=$(urlencode "$library_id")&query=$(urlencode "$query")&type=txt" \
      -H "Authorization: Bearer ${KEY}"
    echo
    ;;
  *)
    cat >&2 <<EOF
Usage:
  $0 search <libraryName> <query>
  $0 docs <libraryId> <query>

Examples:
  $0 search "Next.js" "app router middleware"
  $0 docs "/vercel/next.js" "app router middleware"
EOF
    exit 2
    ;;
esac

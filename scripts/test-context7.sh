#!/usr/bin/env bash
# Smoke test for Context7 project key + helper.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HELPER="${ROOT}/scripts/context7.sh"
MCP_JSON="${ROOT}/.cursor/mcp.json"
RULE="${ROOT}/.cursor/rules/context7.mdc"

fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "PASS: $*"; }

[[ -x "$HELPER" ]] || fail "helper not executable: $HELPER"
[[ -f "$MCP_JSON" ]] || fail "missing $MCP_JSON"
[[ -f "$RULE" ]] || fail "missing $RULE"
grep -q 'CONTEXT7_API_KEY' "$MCP_JSON" || fail "mcp.json missing CONTEXT7_API_KEY"
grep -q 'alwaysApply: true' "$RULE" || fail "rule should be alwaysApply"

search_out="$("$HELPER" search "Next.js" "middleware")"
echo "$search_out" | grep -q '/vercel/next.js' || fail "search did not return /vercel/next.js"
pass "search"

docs_out="$("$HELPER" docs "/vercel/next.js" "middleware")"
[[ -n "$docs_out" ]] || fail "docs returned empty"
echo "$docs_out" | grep -qiE 'middleware|proxy|NextRequest|matcher' || fail "docs missing expected content"
pass "docs"

echo "All Context7 smoke checks passed."

#!/bin/sh
set -eu
# Prefer Railway/Compose PORT, then LT_PORT, then 5000.
export LT_PORT="${PORT:-${LT_PORT:-5000}}"
export LT_HOST="${LT_HOST:-0.0.0.0}"
exec libretranslate "$@"

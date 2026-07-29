#!/bin/sh
set -eu
# Prefer Railway/Compose PORT, then LT_PORT, then 5000.
export LT_PORT="${PORT:-${LT_PORT:-5000}}"
export LT_HOST="${LT_HOST:-0.0.0.0}"

if command -v libretranslate >/dev/null 2>&1; then
  LIBRETRANSLATE_BIN="$(command -v libretranslate)"
elif [ -x /app/venv/bin/libretranslate ]; then
  LIBRETRANSLATE_BIN="/app/venv/bin/libretranslate"
else
  echo "LibreTranslate executable not found" >&2
  exit 127
fi

exec "$LIBRETRANSLATE_BIN" "$@"

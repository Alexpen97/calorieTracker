#!/bin/sh
set -eu

IMAGE_TAG="${1:-nutritrack/libretranslate:test}"

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
REPO_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"

docker build -t "$IMAGE_TAG" "$SCRIPT_DIR" >/dev/null

CONTAINER_ID="$(
  docker run -d -p 15000:5000 \
    -e PORT=5000 \
    -e LT_UPDATE=false \
    "$IMAGE_TAG"
)"
cleanup() {
  docker rm -f "$CONTAINER_ID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

i=0
while [ "$i" -lt 30 ]; do
  if curl -fsS http://127.0.0.1:15000/languages >/dev/null 2>&1; then
    echo "LibreTranslate smoke test passed"
    exit 0
  fi
  sleep 2
  i=$((i + 1))
done

docker logs "$CONTAINER_ID" >&2 || true
echo "LibreTranslate smoke test failed" >&2
exit 1

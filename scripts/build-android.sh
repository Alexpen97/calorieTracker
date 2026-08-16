#!/usr/bin/env bash
# Build the NutriTrack React SPA into a debug Android APK via Capacitor.
#
# Runs entirely inside the repo (no system-wide installs required):
#   - Downloads Node, JDK 21, Android cmdline-tools/SDK into .build-tools/
#   - npm install + builds the web bundle (Capacitor relative base)
#   - `cap sync android`
#   - Gradle `assembleDebug`
#   - Copies the APK to builds/NutriTrack-app-debug.apk
#
# The back-end URL is baked into the bundle via VITE_API_BASE_URL.
# Override it with:  BACKEND_URL=https://your-gateway.example.com ./scripts/build-android.sh
#
# Usage:
#   ./scripts/build-android.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$REPO_ROOT/.build-tools"
FRONTEND="$REPO_ROOT/frontend"
VERSION_SCRIPT="$BUILD_DIR/env.sh"

# Back-end the app talks to (single source of truth = gateway).
BACKEND_URL="${BACKEND_URL:-https://static.128.216.108.65.clients.your-server.de/calorietracker}"
GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
AUTH_MODE="${AUTH_MODE:-prod}"

echo "==> Repo:   $REPO_ROOT"
echo "==> Backend: $BACKEND_URL (VITE_API_BASE_URL)"

mkdir -p "$BUILD_DIR"

# --- Node ---
NODE_TAR="$BUILD_DIR/node.tar.xz"
if [[ ! -x "$BUILD_DIR/node/bin/node" ]]; then
  echo "==> Installing Node 22..."
  curl -fsSL -o "$NODE_TAR" https://nodejs.org/dist/v22.14.0/node-v22.14.0-linux-x64.tar.xz
  tar --no-same-owner -xf "$NODE_TAR" -C "$BUILD_DIR"
  mv "$BUILD_DIR/node-v22.14.0-linux-x64" "$BUILD_DIR/node"
  rm -f "$NODE_TAR"
fi

# --- JDK 21 ---
if [[ ! -x "$BUILD_DIR/jdk21/bin/java" ]]; then
  echo "==> Installing JDK 21 (Temurin)..."
  curl -fsSL -o "$BUILD_DIR/jdk21.tar.gz" \
    "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.6_7.tar.gz"
  tar --no-same-owner -xzf "$BUILD_DIR/jdk21.tar.gz" -C "$BUILD_DIR"
  mv "$BUILD_DIR"/jdk-21* "$BUILD_DIR/jdk21"
  rm -f "$BUILD_DIR/jdk21.tar.gz"
fi

# --- Android SDK ---
SDKMAN="$BUILD_DIR/android-sdk/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$SDKMAN" ]]; then
  echo "==> Installing Android cmdline-tools..."
  curl -fsSL -o "$BUILD_DIR/cmdtools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  rm -rf "$BUILD_DIR/android-sdk"
  mkdir -p "$BUILD_DIR/android-sdk"
  unzip -q "$BUILD_DIR/cmdtools.zip" -d "$BUILD_DIR/android-sdk"
  mkdir -p "$BUILD_DIR/android-sdk/cmdline-tools"
  mv "$BUILD_DIR/android-sdk/cmdline-tools-latest" "$BUILD_DIR/android-sdk/cmdline-tools/latest"
  rm -f "$BUILD_DIR/cmdtools.zip"
fi

# Write env script for reusable paths
cat > "$VERSION_SCRIPT" <<EOF
#!/bin/bash
export JAVA_HOME="$BUILD_DIR/jdk21"
export ANDROID_HOME="$BUILD_DIR/android-sdk"
export ANDROID_SDK_ROOT="\$ANDROID_HOME"
export PATH="$BUILD_DIR/node/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$JAVA_HOME/bin:\$PATH"
EOF

# shellcheck disable=SC1090
source "$VERSION_SCRIPT"

# Install SDK packages (idempotent, accepts licenses)
if [[ ! -d "$ANDROID_HOME/platforms/android-36" ]]; then
  echo "==> Installing Android platform-36 + build-tools (accepting licenses)..."
  yes | sdkmanager --licenses >/dev/null 2>&1 || true
  yes | sdkmanager "platforms;android-36" "build-tools;36.0.0" "platform-tools" >/dev/null
fi

# --- Frontend: npm install + Capacitor build ---
echo "==> Installing frontend dependencies..."
cd "$FRONTEND"
# Always reinstall: the git tree can move on between builds (npm ci is
# deterministic and uses package-lock.json).
npm ci --no-audit --no-fund

echo "==> Building web bundle (Capacitor) pointing at $BACKEND_URL..."
CAPACITOR_BUILD=1 \
  VITE_API_BASE_URL="$BACKEND_URL" \
  VITE_GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID" \
  VITE_AUTH_MODE="$AUTH_MODE" \
  npm run build

echo "==> Capacitor sync -> android/"
npx cap sync android

# --- Gradle assemble ---
echo "==> Gradle assembleDebug..."
cd "$FRONTEND/android"
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
# -Duser.home keeps Gradle's caches + the debug keystore inside .build-tools/:
# the registered Google OAuth SHA-1 (AI/android-build.md) lives at
# .build-tools/.android/debug.keystore — a different keystore breaks sign-in.
./gradlew --no-daemon -Duser.home="$BUILD_DIR" assembleDebug

APK="$FRONTEND/android/app/build/outputs/apk/debug/app-debug.apk"
DEST="$REPO_ROOT/builds/NutriTrack-app-debug.apk"
mkdir -p "$REPO_ROOT/builds"
cp "$APK" "$DEST"

echo ""
echo "==> DONE. APK: $DEST"
echo "    Back-end baked in: $BACKEND_URL"

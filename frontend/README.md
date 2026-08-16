# NutriTrack frontend

React 19 + TypeScript + Vite SPA. Talks only to the gateway.

## Local development

```bash
npm install
npm run dev
```

Optional env (`.env`):

- `VITE_API_BASE_URL` — gateway public URL in production (`https://…` required; host-only values are normalized). Empty in local Vite (uses proxy to gateway `:8080`)
- `VITE_GOOGLE_CLIENT_ID` — Google OAuth web client id
- `VITE_AUTH_MODE=dev` — shows Dev login (default)

Google Console must allow `https://<this-origin>/auth/callback` — see
`docs/google-oauth-setup.md`. The login page prints the exact URIs to register.

## Scripts

- `npm test` — Vitest
- `npm run build` — production bundle
- `npm run cap:sync` — Capacitor build (`CAPACITOR_BUILD=1` → relative Vite `base`)
  and sync into `android/`
- `npm run android:open` — open the Capacitor Android project in Android Studio

Railway web builds use absolute Vite `base: '/'` so deep links like `/analytics`
load `/assets/*` correctly.

## Android (Capacitor)

Phase 5 wraps this SPA in Capacitor (`com.nutritrack.app`). Native adapters:

- Barcode: ML Kit (`@capacitor-mlkit/barcode-scanning`) on device; web keeps `BarcodeDetector`
- Auth: native Google Sign-In → server auth code → same `/api/auth/google/callback`
- Tokens: Android Keystore via `@aparajita/capacitor-secure-storage`

The app talks only to the **gateway back-end**, baked in at build time via
`VITE_API_BASE_URL`. Current production API base (self-hosted, behind Caddy):

```
https://static.128.216.108.65.clients.your-server.de/calorietracker
```

(Details: `docs/compile-frontend-to-app.md`, `AI/production-debug.md`.)

### Build a debug APK (recommended)

```bash
./scripts/build-android.sh
```

This self-contained build script downloads Node, JDK 21, and the Android SDK
into the git-ignored `frontend/../.build-tools/`, then runs
`npm install` → `vite build` (Capacitor relative base) → `cap sync android` →
`gradlew assembleDebug`. Output lands at:

```
builds/NutriTrack-app-debug.apk
```

It defaults to the production API base + `VITE_AUTH_MODE=prod` (Dev login
hidden). Point at a different back-end, or provide the Google web client id:

```bash
BACKEND_URL=https://your-gateway.example.com \
  GOOGLE_CLIENT_ID=<web-client-id> \
  ./scripts/build-android.sh
```

### Manual / Android Studio workflow

```bash
cd frontend
# Build with the API base + web client id baked in:
VITE_API_BASE_URL=https://static.128.216.108.65.clients.your-server.de/calorietracker \
  VITE_GOOGLE_CLIENT_ID=<web-client-id> \
  VITE_AUTH_MODE=prod \
  npm run cap:sync
npm run android:open   # opens Android Studio
```

Dev login shows only when `VITE_AUTH_MODE=dev`.

Play Store checklist: `docs/android-play-store.md`. Notes: `AI/phase-5-android.md`.

## Container

Multi-stage `Dockerfile` (Node build → nginx). Railway root directory = `/frontend`.

Nginx listens on Railway's injected `PORT` (defaults to `80` locally). For
same-origin API proxying in Docker Compose, `GATEWAY_UPSTREAM` defaults to
`gateway:8080`. On Railway, prefer setting build arg `VITE_API_BASE_URL` to the
gateway public URL instead of relying on the nginx proxy.

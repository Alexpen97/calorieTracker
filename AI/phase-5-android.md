# Phase 5 — Android (Capacitor)

## Goal

Deliver Phase 5 from `docs/calorie-tracker-architecture.md` §14 / §9.2:

- Capacitor packaging of the React SPA → Android project
- ML Kit barcode scanning (`@capacitor-mlkit/barcode-scanning`)
- Native Google Sign-In (`@capgo/capacitor-social-login`, offline server auth code)
- Secure token storage via Android Keystore (`@aparajita/capacitor-secure-storage`)
- Gateway CORS for Capacitor `https://localhost`
- Play Store release checklist (manual publish)

## Plan

See `docs/superpowers/plans/2026-07-22-phase-5-android.md`.

## Status

Implemented on `cursor/phase-5-android-7d17`.

## Delivered

### Frontend / Capacitor

- `capacitor.config.ts` — `com.nutritrack.app`, `webDir: dist`, `androidScheme: https`
- SocialLogin providers: Google only (Facebook/Apple/Twitter disabled)
- Vite `base: './'` for WebView asset paths
- Scripts: `cap:sync`, `android:sync`, `android:open`
- Committed `frontend/android/` native project

### Platform adapters

- `src/platform/barcodeScan.ts` — ML Kit `scan()` on native; web unchanged
- `src/platform/googleNativeAuth.ts` — offline Google → `serverAuthCode`
- `src/auth/tokenStorage.ts` — memory cache + SecureStorage (native) / localStorage (web);
  `initTokenStorage()` before React mount

### Auth-service

- `redirectUri` optional on `POST /api/auth/google/callback` (native server-auth-code exchange)

### Gateway

- Default CORS origins include `https://localhost` (Capacitor Android WebView)

## Local Android workflow

```bash
cd frontend
# Build with absolute gateway URL baked in:
VITE_API_BASE_URL=https://gateway-production-777b.up.railway.app \
  VITE_GOOGLE_CLIENT_ID=<web-client-id> npm run cap:sync
npm run android:open   # opens Android Studio
```

Dev login still works in the WebView when `VITE_AUTH_MODE=dev`.

## Google Cloud (Android)

1. Keep the existing **Web application** OAuth client (used as `webClientId`).
2. Create an **Android** OAuth client with package `com.nutritrack.app` + SHA-1 of the
   debug/upload keystore (needed for Google account picker verification).
3. Auth-service still uses the Web client id + secret to exchange the server auth code.

## Verification

- Frontend Vitest (token storage hydrate, barcode/native google adapters, existing suites)
- Auth-service: callback without `redirectUri` (dev mode)

## Play Store

See `docs/android-play-store.md` for AAB signing and Console steps.

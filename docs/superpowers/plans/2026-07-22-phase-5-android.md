# Phase 5 — Android (Capacitor) Implementation Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Phase 5 from `docs/calorie-tracker-architecture.md` §14 / §9.2: Capacitor packaging of the React SPA, ML Kit barcode scanning on Android, native Google Sign-In feeding the existing auth code-exchange endpoint, Keystore-backed secure token storage, gateway CORS for Capacitor origins, and Play Store release documentation.

**Architecture:** Same React/Vite frontend; Capacitor wraps `dist/` into an Android WebView. Native concerns are behind thin platform adapters so web behavior stays unchanged. Auth still goes through `POST /api/auth/google/callback` (server auth code from native Sign-In, PKCE code from web).

**Tech Stack:** Capacitor 7+, `@capacitor-mlkit/barcode-scanning`, `@aparajita/capacitor-secure-storage`, `@capgo/capacitor-social-login`, existing Spring gateway/auth.

## Global Constraints

- Do not fork the UI; one React codebase for web + Android.
- Vite `base: './'` and `webDir: 'dist'` for Capacitor asset loading.
- Token reads stay sync via in-memory cache; hydrate from Secure Storage (native) or `localStorage` (web) at bootstrap.
- Native Google login uses SocialLogin `mode: 'offline'` → `serverAuthCode` → same auth-service exchange (redirectUri optional).
- ML Kit `BarcodeScanner.scan()` on native; keep web `BarcodeDetector` path.
- Commit `android/`; document AAB signing + Play Console steps (do not publish from CI in this phase).
- Match existing Vitest + JUnit patterns.

## File map

| Area | Create / Modify |
|------|-----------------|
| Capacitor | `capacitor.config.ts`, `android/`, npm scripts |
| Platform adapters | `src/platform/native.ts`, `barcodeScan.ts`, `googleNativeAuth.ts` |
| Auth storage | `tokenStorage.ts` (+ async persist, `initTokenStorage`) |
| Login / Lookup | `LoginPage.tsx`, `LookupPage.tsx`, `main.tsx` |
| Auth-service | optional `redirectUri` for native server-auth-code exchange |
| Gateway | default CORS includes `https://localhost` (Capacitor) |
| Docs | `AI/phase-5-android.md`, `docs/android-play-store.md`, README updates |

---

### Task 1: Capacitor project + Android platform

- [x] Install `@capacitor/core`, CLI, `@capacitor/android`, plugins
- [x] `capacitor.config.ts` (`appId: com.nutritrack.app`, `webDir: dist`, `androidScheme: https`)
- [x] Vite `base: './'`; npm scripts `cap:sync`, `android:open`, `android:build`
- [x] `npx cap add android` + sync; commit generated project

### Task 2: Secure token storage

- [x] Memory-backed getters; `initTokenStorage()` before render
- [x] Native → SecureStorage; web → localStorage
- [x] Async `saveTokens` / `clearTokens`; update callers + tests

### Task 3: ML Kit barcode scanning

- [x] Platform scanner helper; LookupPage uses native scan when available
- [x] Vitest with mocked Capacitor / scanner

### Task 4: Native Google Sign-In

- [x] SocialLogin initialize + offline login → exchange `serverAuthCode`
- [x] Auth-service: allow blank/omitted `redirectUri` when exchanging
- [x] LoginPage: native path vs web PKCE; Dev login unchanged

### Task 5: Gateway CORS + docs

- [x] Default allowed origins include Capacitor `https://localhost`
- [x] AI notes, Play Store release checklist, frontend README
- [x] Full frontend (+ auth) tests green; push PR

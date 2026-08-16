# Android build (APK) — recorded 2026-08-07

## What was done

Compiled the frontend (React SPA) into a debug Android APK via Capacitor.

- Web bundle built in Capacitor mode (relative `base: './'`)
- Back-end baked in via `VITE_API_BASE_URL` =
  `https://static.128.216.108.65.clients.your-server.de/calorietracker`
  (self-hosted gateway behind Caddy; see `docs/compile-frontend-to-app.md`)
- Google **web** OAuth client id baked in via `VITE_GOOGLE_CLIENT_ID`
- `VITE_AUTH_MODE=prod` (Dev login button hidden)
- `cap sync android` → `gradlew assembleDebug`
- Output: `builds/NutriTrack-app-debug.apk` (29 MB, `com.nutritrack.app`, minSdk 24, targetSdk 36)

## Toolchain

This host (Bazzite Fedora Atomic, running as root in a container) had **no** Node,
JDK, or Android SDK and no writable system dirs. Installed a fully self-contained
toolchain inside the git-ignored `.build-tools/` (Node 22, Temurin JDK 21,
Android cmdline-tools + platform-36/build-tools-36). `frontend/android/local.properties`
points `sdk.dir` at it (also git-ignored).

Debug signing needs a writable `user.home`; we run
`./gradlew -Duser.home=$REPO/.build-tools assembleDebug`
so Gradle creates the debug keystore under `.build-tools/.android/debug.keystore`
instead of `/root/.android` (which the sandbox can't write).

## Reproducible build

`./scripts/build-android.sh` reproduces the whole flow (downloads toolchain as
needed, npm install, Capacitor build, sync, Gradle assemble, copies APK).

```bash
./scripts/build-android.sh                      # -> builds/NutriTrack-app-debug.apk
BACKEND_URL=https://your-host/path GOOGLE_CLIENT_ID=<web-client-id> ./scripts/build-android.sh
```

`frontend/README.md` **Android (Capacitor)** section documents this + back-end URL.
Full reference: `docs/compile-frontend-to-app.md`.

## Required secrets / config (must line up or sign-in fails)

| Item | Value / where | Status |
|---|---|---|
| `VITE_GOOGLE_CLIENT_ID` (web) | `822558687260-d2t49evj30poqgbp4vc7hd27ejmls7u0.apps.googleusercontent.com` | baked into APK ✅ |
| `GOOGLE_CLIENT_SECRET` (web) | set in gitignored `.env` (`GOCSPX-…`) for the deploy stack | wired ✅ |
| `AUTH_MODE` | `prod` (deploy) | wired ✅ |
| Gateway `CORS_ALLOWED_ORIGINS` | must include `https://localhost` (Capacitor WebView) + self-hosted origin — set in `.env` and `docker-compose.deploy.yml` default | wired ✅ |
| **Android OAuth client SHA-1** | debug keystore SHA-1 must be registered on the **Android** client for package `com.nutritrack.app` | ⚠️ **ACTION REQUIRED** |

### Debug keystore SHA-1 (must register on Google Cloud Android client)

The debug keystore was generated in the sandbox:

```text
SHA1: 40:27:1E:DB:7A:83:EF:2B:13:20:DB:70:E4:4D:A6:BE:09:D4:47:E5
```

Register this on the **Android** OAuth client (package `com.nutritrack.app`) in
[Google Cloud Credentials](https://console.cloud.google.com/apis/credentials),
otherwise native Google Sign-In fails with `ApiException: 10`.

## Verification

- Frontend Vitest: **27 files / 92 tests pass**
- `aapt dump badging` confirms package/name/version
- `grep` confirms the client id + API base are present in the bundled assets inside the APK

## 2026-08-07 — white-screen root cause & fix

The Android app rendered a blank / white content area because a router `basename`
bug made **no route match** in the WebView.

- `feat(deploy)` (1473765) added router-basename handling to `main.tsx` so the SPA
  serves under a URL subpath (`VITE_BASE=/app`). It used raw
  `import.meta.env.BASE_URL`, which for Capacitor builds is the **relative** base
  `"./"` → router basename `"."` → React Router normalizes it to `"/."`.
- `stripBasename("/", "/.")` returns `null`, so `/today`, `/`, etc. match nothing
  and the logged-in screen renders empty (while the shell/background remained,
  reading as a white app).
- Fix: `frontend/src/viteBase.ts` now exports `resolveRouterBasename()` which maps
  `""`, `"/"`, `"."` and `"./"` → `''` (root) and keeps a real subpath like `/app`.
  `main.tsx` uses it. Added 5 unit tests (`viteBase.test.ts`).

Also note: this APK was built with `VITE_AUTH_MODE=prod`, so the Dev-login button is
hidden — native Google Sign-In requires the Android OAuth client + SHA-1
registration above to complete login.

## 2026-08-07 — offline Google Sign-In: MainActivity fix

`@capgo/capacitor-social-login` rejects offline-mode Google login unless the app
`MainActivity` implements `ModifiedMainActivityForSocialLoginPlugin` (it refuses with
"You CANNOT use offline mode without modifying the main activity"). Fixed
`frontend/android/app/src/main/java/com/nutritrack/app/MainActivity.java` to implement
the marker interface.

## 2026-08-07 — native login stalls before backend: gateway CORS

After the account picker, the native app's `POST {apiBase}/api/auth/google/callback`
from the Capacitor WebView origin (`https://localhost`, because `androidScheme:
'https'`) was being **rejected at the gateway CORS layer** before reaching auth-service
(hence "no logins hit the backend"). Live check:
`OPTIONS .../api/auth/google/callback -H "Origin: https://localhost"` returned 403 with
no `access-control-allow-origin`, while the self-hosted origin returned 200.

- Repo config (`.env`, `docker-compose.deploy.yml`, `SecurityConfig`) already includes
  `https://localhost` in `CORS_ALLOWED_ORIGINS`, so the running gateway had a stale
  value. Fix = **recreate/restart the gateway** so it reloads `CORS_ALLOWED_ORIGINS`
  (e.g. `docker compose -f docker-compose.deploy.yml up -d --force-recreate gateway`).
- After recreation, re-verified `Origin: https://localhost` returns 200 and a real
  `POST` (`dev:agent-debug`) returns 200 + access token, plus correct
  `access-control-allow-origin: https://localhost` headers.

If the app still does not reach the backend after CORS is confirmed working, capture
device-side evidence next: `adb logcat` for the WebView (`chromium`/`Console`) and
Capacitor (`capacitor`/`Activity`) tags.

## Notes / caveats

- Debug APK (unminified, unsigned for Play Store). For release/AAB see
  `docs/android-play-store.md`.
- `VITE_GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_ID` are the same value (the **web**
  client). The separate **Android** client is only for the native account
  picker + SHA-1 verification.
- Changing `VITE_GOOGLE_CLIENT_ID` requires a rebuild; Google Console changes
  take minutes–hours to propagate.

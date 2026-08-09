# Compiling the NutriTrack frontend into a native app

The frontend is a **Vite + React SPA** wrapped in **Capacitor** for Android (and
potentially iOS/desktop). This doc covers the exact steps to compile the web
frontend into an installable app, and every piece of configuration that has to
line up for Google Sign-In to work in that compiled app — including the gotchas
we hit on the web deploy (`redirect_uri_mismatch`, CORS 403, and the `/app`
base-path redirect).

> Source of truth for app metadata: `frontend/capacitor.config.ts`.
> App id: `com.nutritrack.app` · App name: `NutriTrack` · `webDir: dist`.
> The Android native project lives at `frontend/android/` (gitignored) and is
> synced from a fresh `dist/` build.

---

## 1. What "compile to an app" means here

Capacitor packages the built SPA (`dist/`) into a thin native shell that loads
it in a WebView. Two auth modes exist in this codebase:

| Mode | Where it runs | Sign-in flow |
|---|---|---|
| **Web SPA** | Browser / PWA | OAuth2 **Authorization Code + PKCE** redirect (`/auth/callback`) |
| **Native app** | Capacitor WebView / Android | `@capgo/capacitor-social-login` (native Google account picker, returns a **server auth code**) |

The app does not re-implement the browser PKCE redirect — it uses the native
Google Sign-In plugin (`frontend/src/platform/googleNativeAuth.ts`), which
returns a `serverAuthCode` and also stores access/refresh tokens in
`@aparajita/capacitor-secure-storage` (Android Keystore).

---

## 2. One-time prerequisites

- Node 20+ / npm (as used by the rest of the repo)
- Android Studio (Ladybug+) with SDK 35 / matching build-tools, **JDK 21**
- Capacitor dependencies already in `frontend/package.json` (cap 8)
- **Two Google Cloud OAuth clients** (see §4):
  1. **Web application** client (client id + secret) — already used by
     `auth-service` and the web SPA
  2. **Android** client for package `com.nutritrack.app` with your upload-key
     **SHA-1** (and debug SHA-1 for local testing)

---

## 3. Build the app (Android)

```bash
cd frontend

# Production API + Google web client id must be present at build time.
# The web client id is used both by the SPA build and (via the native plugin)
# to obtain the server auth code.
export VITE_API_BASE_URL=https://static.128.216.108.65.clients.your-server.de
export VITE_GOOGLE_CLIENT_ID=<web-client-id>.apps.googleusercontent.com
export VITE_AUTH_MODE=prod        # hides the Dev login button

npm run cap:sync                  # = CAPACITOR_BUILD=1 npm run build && npx cap sync android
npm run android:open              # opens the Android project in Android Studio
```

Notes:

- `cap:sync` sets `CAPACITOR_BUILD=1`, which makes `resolveViteBase()` return
  `./` (relative base) so the bundled `dist` loads correctly inside the
  WebView instead of under an absolute `/app` or `/` path.
- Do **not** set `VITE_BASE=/app` for a native build — that is only for the
  reverse-proxied web deployment.
- The first `cap sync` generates `frontend/android/` if it doesn't exist.

### 3a. In Android Studio (release AAB)

1. **Build → Generate Signed App Bundle / APK → Android App Bundle**
2. Create / select an upload keystore (store it **outside** the repo; never
   commit `.jks` / `.keystore`).
3. Build the release AAB.

CLI alternative (after configuring `frontend/android/keystore.properties`):

```bash
cd frontend/android
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```

### 3b. Local debugging / dev

Use the **debug** keystore SHA-1, register it on the Android OAuth client, then:

```bash
cd frontend
npm run cap:sync
npm run android:open
# Run from Android Studio on a device/emulator
```

For local development against the local backend, set:
`VITE_API_BASE_URL=https://static.128.216.108.65.clients.your-server.de`
(or the gateway URL appropriate to your environment). The live gateway serves
`/api/**` at that origin; the `/calorietracker` reverse-proxy base path below is
only used for the web deploy, not the app.

---

## 4. Google Cloud Console — the part that must line up

Google Sign-In fails with `redirect_uri_mismatch`, `invalid_client`, or
`ApiException: 10` when any of these are wrong. There are **two** clients.

### 4a. Web application client (already in use)

This backs the browser/PWA **and** the native token exchange. In
[Credentials → OAuth 2.0 Client ID → Web application](https://console.cloud.google.com/apis/credentials):

**Authorized JavaScript origins**

```
https://static.128.216.108.65.clients.your-server.de
https://front-end-production-4a95.up.railway.app      # if Railway SPA is live
```

**Authorized redirect URIs** (note: **no `/app` base** — the app builds
`window.location.origin + /auth/callback`)

```
https://static.128.216.108.65.clients.your-server.de/auth/callback
https://front-end-production-4a95.up.railway.app/auth/callback
```

These must match `window.location.origin + /auth/callback` character-for-character
(scheme, host, path, no trailing slash). A trailing `/` or a stray `/app` in the
redirect URI is the #1 cause of `redirect_uri_mismatch`.

> If the app is still in **Testing**, add each user's Google account as a
> **Test user** (Audience → Test users), otherwise you get `access_denied`.

### 4b. Android client (for the compiled app)

Create a second OAuth client of type **Android** for package `com.nutritrack.app`.
Add **both** SHA-1 fingerprints you sign with:

- Debug keystore SHA-1 (local builds)
- Upload-key SHA-1 (release AABs)

Get a fingerprint:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android | grep SHA1:
# or for your upload keystore:
keytool -list -v -keystore /path/to/upload.keystore -alias upload | grep SHA1:
```

Every SHA-1 you sign with must be registered, or native Google Sign-In fails
with `ApiException: 10`.

### 4c. Backend env the app talks to

- `auth-service`: `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` (the **web** client)
- **Gateway CORS**: the compiled app's WebView sends
  `Origin: https://localhost` (Capacitor `androidScheme: 'https'`). That origin
  **must** be in the gateway's `CORS_ALLOWED_ORIGINS`, e.g.:

  ```
  CORS_ALLOWED_ORIGINS=https://localhost,http://localhost:5173,http://localhost,https://static.128.216.108.65.clients.your-server.de
  ```

  Missing it causes a silent `403` on every `/api/**` call from the app (the
  exact CORS-403 we hit on the web deploy).

---

## 5. Env summary (vite build args)

| Variable | Web SPA | Native app | Purpose |
|---|---|---|---|
| `VITE_API_BASE_URL` | gateway URL with `https://` | same | all `/api/**` calls |
| `VITE_GOOGLE_CLIENT_ID` | web client id | web client id | browser PKCE **and** native plugin webClientId |
| `VITE_AUTH_MODE` | `dev` \| `prod` | `prod` recommended | shows/hides Dev login |
| `VITE_BASE` | `/app` (reverse-proxy deploy) | unset (Capacitor uses `CAPACITOR_BUILD=1` → `./`) | asset base path |
| `CAPACITOR_BUILD` | — | `1` (set by `cap:sync`) | selects relative base |

---

## 6. iOS / desktop (if enabled later)

Same concept, different shell:

- iOS: `npx cap add ios`, then `npm run cap:sync` and open in Xcode. Add a
  matching **iOS** OAuth client (bundle id + redirect URI
  `<bundle-id>:/oauthredirect`).
- The native Google Sign-In plugin already has a branch for `isNativePlatform()`
  (`frontend/src/platform/native.ts`), so the UI selects the right flow at runtime.

---

## 7. Play Store release (brief)

Full checklist: [`docs/android-play-store.md`](./android-play-store.md).
Short version:

1. `npm run cap:sync` with prod env, open in Android Studio, build signed AAB.
2. Play Console: create app `com.nutritrack.app`, complete Data safety / content
   rating, upload AAB to internal testing → promote through closed → production.
3. Smoke-test in internal testing: Google Sign-In (native picker), barcode scan
   (ML Kit Code Scanner), token persistence across process death.
4. Keep the upload keystore backed up (Play App Signing holds the signing key).

---

## 8. Troubleshooting (errors we actually hit)

| Symptom | Cause | Fix |
|---|---|---|
| Google `400 redirect_uri_mismatch` | redirect URI ≠ `origin + /auth/callback`, or stray `/app` / trailing slash | Register the exact no-`/app` URI in the **Web** client (see §4a); wait for Console propagation (minutes) |
| Blank screen after OAuth redirect | Router `basename=/app` vs URL `/auth/callback` | Proxy must **external-redirect** `/auth/callback` → `/app/auth/callback` *and preserve the query string* (Caddy: `redir` with `?{query}`) |
| `Request failed (403)` on callback | Gateway CORS rejects the browser/WebView origin | Add the real origin (and `https://localhost` for native) to `CORS_ALLOWED_ORIGINS`; recreate gateway |
| `ApiException: 10` (native) | Android client SHA-1 missing/mismatched | Register debug + upload SHA-1 on the **Android** client (§4b) |
| `invalid_client` / 400 on token exchange | Wrong/empty `GOOGLE_CLIENT_ID`/`SECRET` on auth-service | Set both from the **Web** client; recreate auth-service |
| 404 / wrong route after login | `/auth/callback` not proxied to the SPA | Add the callback rewrite before the dashboard fallback |

General note: changes to the Google Console can take **5 minutes to a few
hours** to propagate, and a redirect URI change needs **no redeploy** — but a
change to `VITE_GOOGLE_CLIENT_ID` requires **rebuilding** the frontend/app.

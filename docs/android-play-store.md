# NutriTrack Android — Play Store release

Capacitor Android app id: `com.nutritrack.app`  
Source: `frontend/android/` (synced from the Vite `dist/` build)

This checklist is the Phase 5 release path. Publishing remains a **manual** Console
action (no CI upload in this phase).

## Prerequisites

- Android Studio (Ladybug+) with SDK 35 / build-tools matching the Capacitor project
- JDK 21
- Google Play Console developer account
- Google Cloud OAuth:
  - **Web** client id/secret (already used by auth-service + `VITE_GOOGLE_CLIENT_ID`)
  - **Android** client for package `com.nutritrack.app` with the **upload key** SHA-1
    (and debug SHA-1 for local testing)

## Build a release AAB

```bash
cd frontend

# Production API + Google web client id must be present at build time
export VITE_API_BASE_URL=https://gateway-production-777b.up.railway.app
export VITE_GOOGLE_CLIENT_ID=<web-client-id.apps.googleusercontent.com>
export VITE_AUTH_MODE=prod   # hide Dev login

npm run cap:sync
npm run android:open
```

In Android Studio:

1. **Build → Generate Signed App Bundle / APK → Android App Bundle**
2. Create or select an upload keystore (store outside the repo; never commit `.jks` / `.keystore`)
3. Build the release AAB (`android/app/release/` is gitignored)

CLI alternative (after configuring `android/keystore.properties` locally):

```bash
cd frontend/android
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```

## Play Console

1. Create the app listing (name **NutriTrack**, package `com.nutritrack.app`)
2. Complete Data safety / content rating / target audience
3. Upload the AAB to an internal testing track first
4. Add testers; verify:
   - Google Sign-In (native account picker)
   - Barcode scan (ML Kit Google Code Scanner module may prompt install once)
   - Token persistence across process death (Secure Storage / Keystore)
5. Promote to closed → production when smoke checks pass

## Gateway CORS

Production `CORS_ALLOWED_ORIGINS` must include Capacitor’s WebView origin:

```text
https://localhost
```

(plus the Railway frontend origin for the web SPA). Defaults in gateway already
include `https://localhost` for local/native shells.

## Signing notes

- Keep the upload keystore backed up; Play App Signing holds the app signing key.
- Register every SHA-1 you use (debug + upload) on the Android OAuth client so
  Google Sign-In does not fail with `ApiException: 10`.

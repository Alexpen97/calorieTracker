# frontend

Frontend container — React SPA, later wrapped with Capacitor for Android. See
`docs/calorie-tracker-architecture.md` §9.

- React 19 + TypeScript, Vite, TanStack Query, React Router.
- Barcode scanning: `BarcodeDetector` Web API with ZXing fallback (web),
  ML Kit via Capacitor plugin (Android).
- Talks only to the gateway (`/api/**`).

## Container

- Build: multi-stage `Dockerfile` (Node build → nginx static runtime).
  Railway root directory = `/frontend` (can alternatively deploy as a static
  site).
- Port: `80` locally via Compose; public domain on Railway.

## Key environment variables (build-time)

| Variable | Purpose |
|---|---|
| `VITE_API_BASE_URL` | gateway public URL |
| `VITE_GOOGLE_CLIENT_ID` | Google OAuth client id (public identifier) |

The Capacitor Android shell lives in this folder too (`android/` after
`npx cap add android`, Phase 5).

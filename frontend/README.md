# NutriTrack frontend

React 19 + TypeScript + Vite SPA. Talks only to the gateway.

## Local development

```bash
npm install
npm run dev
```

Optional env (`.env`):

- `VITE_API_BASE_URL` — empty in local Vite (uses proxy to gateway `:8080`)
- `VITE_GOOGLE_CLIENT_ID` — Google OAuth web client id
- `VITE_AUTH_MODE=dev` — shows Dev login (default)

## Scripts

- `npm test` — Vitest
- `npm run build` — production bundle

## Container

Multi-stage `Dockerfile` (Node build → nginx). Railway root directory = `/frontend`.

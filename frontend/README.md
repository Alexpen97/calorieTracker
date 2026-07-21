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

## Container

Multi-stage `Dockerfile` (Node build → nginx). Railway root directory = `/frontend`.

Nginx listens on Railway's injected `PORT` (defaults to `80` locally). For
same-origin API proxying in Docker Compose, `GATEWAY_UPSTREAM` defaults to
`gateway:8080`. On Railway, prefer setting build arg `VITE_API_BASE_URL` to the
gateway public URL instead of relying on the nginx proxy.

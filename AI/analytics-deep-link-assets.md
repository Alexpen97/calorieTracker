# Fix: Analytics (and other deep links) blank on hard load (2026-07-23)

## Problem

Opening or refreshing `/analytics` (also `/today`, `/diary`, `/me`, …) on the
Railway web frontend failed to boot the SPA.

`vite.config.ts` used `base: './'` (for Capacitor). The built `index.html`
referenced `./assets/index-….js`. On a deep URL the browser resolved that to
`/analytics/assets/…`. Nginx SPA `try_files` then served **index.html** with
`Content-Type: text/html` as the “JS” module, so the page stayed blank.

Client-side navigations from `/` still worked (assets already loaded from `/`).

## Fix

- Web builds use absolute Vite `base: '/'` (default).
- Capacitor packaging sets `CAPACITOR_BUILD=1` so `cap:sync` still builds with
  relative `./`.
- Nginx `location /assets/` uses `try_files $uri =404` so a bad asset URL never
  returns HTML-as-JS.

## Verify

1. `npm test` (includes `viteBase` tests)
2. `npm run build` → `dist/index.html` has `src="/assets/…"` (not `./assets/…`)
3. After deploy: hard-refresh
   `https://front-end-production-4a95.up.railway.app/analytics` — app loads,
   then Dev login → Analytics cards from summary range / goals / weight APIs

## Files

- `frontend/src/viteBase.ts` (+ test)
- `frontend/vite.config.ts`
- `frontend/package.json` (`cap:sync`)
- `frontend/default.conf.template`

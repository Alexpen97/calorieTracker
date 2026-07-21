# Login 405 Not Allowed (nginx)

## Symptom

After Google sign-in or dev login, the browser shows nginx `405 Not Allowed` HTML
(`nginx/1.27.5` in the footer).

## Root cause

The SPA token exchange uses `fetch(\`${apiBase}/api/auth/google/callback\`, { method: 'POST' })`.

If `VITE_API_BASE_URL` is set **without** `https://` (e.g. `gateway-production.up.railway.app`),
the browser treats it as a **relative path**. From `/auth/callback` that becomes
`POST /auth/gateway-production…/api/…`, which hits nginx `location /` (static SPA).
nginx rejects POST there with 405.

A secondary case: Google Console redirect URI set to `/api/auth/google/callback` sends
`GET` to the API path. nginx now redirects that to `/auth/callback` (see
`frontend/default.conf.template`).

## Fix (code)

- `frontend/src/api/apiBase.ts` — `resolveApiBase()` prepends `https://` in production when
  the scheme is missing.
- `frontend/default.conf.template` — `GET /api/auth/google/callback` → `302 /auth/callback`.

## Fix (Railway)

- Frontend build arg: `VITE_API_BASE_URL=https://<gateway-public-domain>` (include scheme).
- Gateway: `CORS_ALLOWED_ORIGINS=https://<frontend-domain>`.
- Google Console redirect URI: `https://<frontend-domain>/auth/callback` (not `/api/…`).

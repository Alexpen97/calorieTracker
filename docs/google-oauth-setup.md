# Google OAuth setup (fix `redirect_uri_mismatch`)

NutriTrack uses the **browser Authorization Code + PKCE** flow. The SPA
redirects to Google; Google must redirect back to the **frontend**, not the
gateway.

## Exact values for the live NutriTrack frontend

Open the app and copy the URL from the address bar. As of this note the
NutriTrack SPA is:

```
https://front-end-production-4a95.up.railway.app
```

In [Google Cloud Console → APIs & Services → Credentials](https://console.cloud.google.com/apis/credentials)
open your **OAuth 2.0 Client ID** of type **Web application** and set:

**Authorized JavaScript origins**

```
https://front-end-production-4a95.up.railway.app
```

**Authorized redirect URIs**

```
https://front-end-production-4a95.up.railway.app/auth/callback
```

Save, wait about a minute, then try **Continue with Google** again.

The login page also shows these two values for the origin you are currently
on — use those if your Railway domain changes.

## What must match

| Google Console field | Value the app sends |
|---|---|
| Authorized JavaScript origins | `window.location.origin` (no path) |
| Authorized redirect URIs | `window.location.origin` + `/auth/callback` |

Character-for-character match is required (scheme, host, path, no trailing slash).

## Common mistakes that cause Error 400 `redirect_uri_mismatch`

1. **Domain only** — registering `https://….up.railway.app` without `/auth/callback`.
2. **Wrong Railway app** — e.g. `frond-end-production-7ec2` (other project) instead of
   `front-end-production-4a95` (NutriTrack). Check the browser tab title is **NutriTrack**.
3. **Gateway / API path** — do **not** register `/api/auth/google/callback`. That is the
   backend token exchange; Google never redirects there.
4. **Trailing slash** — `…/auth/callback/` ≠ `…/auth/callback`.
5. **http vs https** — production must be `https://`.
6. **Wrong client type** — must be **Web application**, not Android / iOS / Desktop.
7. **Wrong client ID** — frontend build arg `VITE_GOOGLE_CLIENT_ID` and auth-service
   `GOOGLE_CLIENT_ID` must be the same Web client.

## Env / Railway checklist

- **frontend** build args: `VITE_GOOGLE_CLIENT_ID=<web-client-id>`, `VITE_AUTH_MODE=prod`
- **auth-service**: `GOOGLE_CLIENT_ID` (same), `GOOGLE_CLIENT_SECRET`, `AUTH_MODE=prod`
- **gateway**: `CORS_ALLOWED_ORIGINS=https://front-end-production-4a95.up.railway.app`

After changing Google Console URIs, no redeploy is required. After changing
`VITE_GOOGLE_CLIENT_ID`, rebuild/redeploy the frontend.

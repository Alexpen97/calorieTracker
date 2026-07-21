# Google OAuth redirect_uri_mismatch

## Root cause (2026-07-21)

Error 400 `redirect_uri_mismatch` is **not** an app code bug. Google rejects the
auth request when the SPA's `redirect_uri` is not listed exactly on the OAuth
Web client.

NutriTrack sends:

```
{window.location.origin}/auth/callback
```

Live NutriTrack SPA (title **NutriTrack**):

```
https://front-end-production-4a95.up.railway.app
→ redirect_uri https://front-end-production-4a95.up.railway.app/auth/callback
```

A different Railway host `https://frond-end-production-7ec2.up.railway.app`
serves **DM Companion**, not NutriTrack. Registering that host (or the domain
without `/auth/callback`) causes this error.

## Fix for the operator

In Google Cloud Console → Credentials → Web application client:

1. Authorized JavaScript origins: `https://front-end-production-4a95.up.railway.app`
2. Authorized redirect URIs: `https://front-end-production-4a95.up.railway.app/auth/callback`
3. Save; wait ~1 minute; retry login on the NutriTrack URL.

Full checklist: `docs/google-oauth-setup.md`.

## Code changes on this branch

- `frontend/src/auth/oauthRedirect.ts` — shared origin / redirect URI helpers + tests
- Login page shows the exact URIs to paste into Google Console
- Docs linked from Railway phase-1 / deploy notes

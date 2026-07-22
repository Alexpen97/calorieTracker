# Diary 401 with correct JWKS_URI

## Immediate action

**Log out and log in again.** Access tokens last **15 minutes**
(`ACCESS_TOKEN_TTL=15m`). The SPA did not refresh them, so long debugging
sessions leave an expired Bearer token → 401 on `/api/diary/**` even when
`JWKS_URI` is correct.

Also note: refresh tokens are stored **in memory on auth-service**. Any
auth-service redeploy invalidates refresh tokens → must log in again.

## If 401 persists after a fresh login

1. Confirm Profile/Lookup work with the same session.
2. On **diary-service** startup logs, confirm:
   `diary-service JWKS jwk-set-uri=http://auth.railway.internal:8080/.well-known/jwks.json`
   (no trailing `=`).
3. On diary request logs: `diary-service inbound GET /api/diary/...`
   - present → diary rejected JWT (JWKS reachability / wrong value)
   - absent → gateway returned 401 (token not sent / still expired)

## Code

Frontend now refreshes via `POST /api/auth/refresh` once on 401 and retries
the API call (`authenticatedFetch` in `frontend/src/api/client.ts`).

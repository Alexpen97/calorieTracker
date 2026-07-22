# Diary 401 Unauthorized while gateway JWKS is fixed

## Symptom

```
GET /api/diary/entries|summary|water → 401
```

Profile/Lookup may still work. Gateway no longer shows `Could not obtain the keys`.

## Cause

Both **gateway** and **diary-service** validate the JWT. Gateway can succeed while
diary rejects if **diary-service** `JWKS_URI` is wrong — most often the same
trailing `=` typo:

```
http://auth.railway.internal:8080/.well-known/jwks.json=
```

Missing `JWKS_URI` on diary (defaults to localhost) also yields 401.

## Fix

On **diary-service** (and food/user if needed), set:

```
JWKS_URI=http://auth.railway.internal:8080/.well-known/jwks.json
```

No trailing `=`. Redeploy diary-service.

Then **log out and log in** again so the browser holds a fresh access token
(tokens issued while JWKS was broken can leave the SPA in a bad retry state).

## Confirm

Diary logs should show:

```
diary-service inbound GET /api/diary/entries?...
```

If you see that line and still 401, JWKS on diary is still wrong. If you never
see it, the 401 is from the gateway (token missing/expired — re-login).

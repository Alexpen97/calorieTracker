# Gateway 500 on `/api/diary/**`

## Symptom

Browser requests such as:

```
GET https://gateway-production-777b.up.railway.app/api/diary/summary?date=2026-07-21&zone=Europe%2FAmsterdam → 500
GET https://gateway-production-777b.up.railway.app/api/diary/entries?... → 500
GET https://gateway-production-777b.up.railway.app/api/diary/water?... → 500
```

Gateway logs show:

```
Caused by: java.lang.IllegalArgumentException: Host is not specified
    at reactor.netty.http.client.UriEndpointFactory.createUriEndpoint(...)
```

Downstream services (diary / user / food) show **no request logs**.

## How to tell which URL is broken

| Observation | Likely broken var |
|---|---|
| `/api-docs/diary` works, but **authenticated** `/api/diary/**` returns 500 | **`JWKS_URI`** (JWT validation WebClient) |
| Unauthenticated `/api/diary/**` → 401; authenticated → 500; Profile/Lookup also 500 | **`JWKS_URI`** |
| `/api-docs/diary` also fails | **`DIARY_SERVICE_URL`** |

Authenticated-only failures happen because Spring Security only fetches JWKS when a
Bearer token is present. `/api-docs/**` is `permitAll`, so a bad `JWKS_URI` never
runs for docs — but diary/user/food APIs all die at the gateway before proxying.

## Root causes

1. **`JWKS_URI` has no host** — e.g. blank, missing `http://`, or a Railway
   reference that resolved to `http://:8080/.well-known/jwks.json`.
2. **`DIARY_SERVICE_URL` missing / blank / bare `host:port` / broken `${{...}}`**.
3. Service name mismatch — hostname must match the **Railway service name**
   (check Networking / private domain; may be e.g. `calorietracker-8495`).

## Fix (Railway — operator)

On **gateway**, reveal values with the eye icon and set:

| Variable | Example |
|---|---|
| `JWKS_URI` | `http://auth.railway.internal:8080/.well-known/jwks.json` |
| `DIARY_SERVICE_URL` | `http://diary.railway.internal:8080` |
| `AUTH_SERVICE_URL` | `http://auth.railway.internal:8080` |
| `USER_SERVICE_URL` | `http://user.railway.internal:8080` |
| `FOOD_SERVICE_URL` | `http://food.railway.internal:8080` |

Use **your** Railway private hostnames (same pattern as the working ones). Then
**redeploy gateway**.

Quick check while logged in:

1. Profile or Lookup — if those 500 too, fix `JWKS_URI` first.
2. Gateway deploy logs should show `Gateway JWKS_URI=...` and
   `Gateway route diary-service -> http://...` after this fix lands.

## Fix (code)

- `ServiceUrlEnvironmentPostProcessor` — auto-prefix `http://` for bare
  `host:port` (including `JWKS_URI`).
- `GatewayStartupValidator` — on Railway, fail fast when JWKS or required
  routes have no host / still point at localhost; log resolved URIs.
- Drop unused `recommendation-service` route until Phase 6 (blank
  `RECO_SERVICE_URL` previously broke startup).

## Related

- Full gateway env table: `docs/railway-deploy.md`
- Phase 3 deploy checklist: `docs/railway-phase3.md`

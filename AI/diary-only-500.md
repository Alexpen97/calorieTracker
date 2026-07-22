# Diary-only 500s while Profile / Lookup work

## Symptom

Logged-in Today page:

```json
{"path":"/api/diary/summary","status":500,"error":"Internal Server Error"}
{"path":"/api/diary/entries","status":500,"error":"Internal Server Error"}
{"path":"/api/diary/water","status":500,"error":"Internal Server Error"}
```

Profile, Lookup, and other authenticated APIs work. Gateway and other services
share a working `JWKS_URI`. `/api-docs/diary` may still return 200.

## Meaning

Gateway JWT validation is fine (otherwise Profile/Lookup would fail too).
Routing to diary is fine if `/api-docs/diary` works. The failure is **inside
`diary-service`** (or only on that service's env): every authenticated diary
handler needs diary's own JWT check + Postgres; OpenAPI does not.

## Check on **diary-service** (not gateway)

| Variable | Expected |
|---|---|
| `JWKS_URI` | Same working value as food/user, e.g. `http://auth.railway.internal:8080/.well-known/jwks.json` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres.railway.internal:5432/diary` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Match postgres |
| `FOOD_SERVICE_URL` / `USER_SERVICE_URL` | Private service URLs |

Common mistakes:

1. **`JWKS_URI` set on gateway/food/user but not attached to diary-service**
   (Railway shared vars must be linked per service). Diary then defaults to
   `http://localhost:8081/...` and authenticated calls explode; docs still work.
2. **Wrong var name** — app reads `SPRING_DATASOURCE_URL`, not `DATABASE_URL`.
3. **Missing `diary` database** — Postgres init scripts run only on first volume
   boot. If Postgres predated Phase 3, create DB manually:

```sql
CREATE DATABASE diary;
```

4. Then redeploy diary-service.

## Logs

In Railway → **diary-service** → Logs, reproduce the Today page load. You should
see a stack trace (JWT/JWKS or JDBC). If there is truly nothing, the request is
still dying on the gateway — re-check `DIARY_SERVICE_URL` on gateway.

## Code

`DiaryStartupValidator` fails fast on Railway when JWKS/datasource/service URLs
still point at localhost or have no host.

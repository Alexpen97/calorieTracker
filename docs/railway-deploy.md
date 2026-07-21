# Railway deploy — full stack

One Railway **project**, one Railway **service per deployable folder**. Every
folder below has its own `Dockerfile` and is built from that root directory.

## All Railway services

| Railway service | Root directory | Watch paths | Public? | Notes |
|---|---|---|---|---|
| `postgres` | `/infra/postgres` | `/infra/postgres/**` | No | Persistent volume required |
| `redis` | `/infra/redis` | `/infra/redis/**` | No | Catalog cache |
| `gateway` | `/services/gateway` | `/services/gateway/**` | **Yes** | API + Swagger |
| `frontend` | `/frontend` | `/frontend/**` | **Yes** | React SPA |
| `auth-service` | `/services/auth-service` | `/services/auth-service/**` | No | |
| `user-profile-service` | `/services/user-profile-service` | `/services/user-profile-service/**` | No | DB: `users` |
| `food-catalog-service` | `/services/food-catalog-service` | `/services/food-catalog-service/**` | No | DB: `food_catalog` + Redis |
| `diary-service` | `/services/diary-service` | `/services/diary-service/**` | No | DB: `diary` |

`recommendation-service` is Phase 6 — no Dockerfile until implemented.

Private networking: `http://<service-name>.railway.internal:<port>` (plain HTTP).

## Infrastructure

### postgres

See `infra/postgres/README.md`.

- Set `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB=postgres`.
- Attach a **volume** on `/var/lib/postgresql/data`.
- Init script creates `users`, `food_catalog`, `diary` on first boot.

### redis

See `infra/redis/README.md`.

- No secrets required for the default config.
- `food-catalog-service` uses `REDIS_HOST=redis.railway.internal`, `REDIS_PORT=6379`.

## Application variables

### Shared

| Variable | Services |
|---|---|
| `INTERNAL_API_KEY` | `auth-service`, `user-profile-service` (same value) |
| `AUTH_MODE` | `auth-service`; frontend build arg `VITE_AUTH_MODE` |
| `PORT` | All backends: `8080` |

### auth-service

| Variable | Value |
|---|---|
| `USER_SERVICE_URL` | `http://user-profile-service.railway.internal:8080` (**required on Railway** — unset defaults to `localhost:8082` and login fails with 403/503) |
| `INTERNAL_API_KEY` | same value as on `user-profile-service` |
| `AUTH_MODE` | `prod` for live Google login |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth (when `AUTH_MODE=prod`) |
| `JWT_PRIVATE_KEY_PEM` | RS256 private key PEM |

### user-profile-service

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres.railway.internal:5432/users` |
| `SPRING_DATASOURCE_USERNAME` | same as `POSTGRES_USER` |
| `SPRING_DATASOURCE_PASSWORD` | same as `POSTGRES_PASSWORD` |
| `JWKS_URI` | `http://auth-service.railway.internal:8080/.well-known/jwks.json` |
| `INTERNAL_API_KEY` | shared secret |

### food-catalog-service

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres.railway.internal:5432/food_catalog` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | postgres credentials |
| `REDIS_HOST` | `redis.railway.internal` |
| `REDIS_PORT` | `6379` |
| `JWKS_URI` | `http://auth-service.railway.internal:8080/.well-known/jwks.json` |
| `OFF_BASE_URL` | `https://world.openfoodfacts.org` |
| `OFF_USER_AGENT` | `NutriTrack - Server - Version 0.1` |
| `FOOD_REDIS_ENABLED` | `true` |

### diary-service

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres.railway.internal:5432/diary` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | postgres credentials |
| `JWKS_URI` | `http://auth-service.railway.internal:8080/.well-known/jwks.json` |
| `FOOD_SERVICE_URL` | `http://food-catalog-service.railway.internal:8080` |
| `USER_SERVICE_URL` | `http://user-profile-service.railway.internal:8080` |

### gateway

| Variable | Value |
|---|---|
| `AUTH_SERVICE_URL` | `http://auth-service.railway.internal:8080` |
| `USER_SERVICE_URL` | `http://user-profile-service.railway.internal:8080` |
| `FOOD_SERVICE_URL` | `http://food-catalog-service.railway.internal:8080` |
| `DIARY_SERVICE_URL` | `http://diary-service.railway.internal:8080` |
| `JWKS_URI` | `http://auth-service.railway.internal:8080/.well-known/jwks.json` |
| `CORS_ALLOWED_ORIGINS` | `https://<frontend-domain>` |
| `SWAGGER_UI_ENABLED` | `true` |

### frontend

Runtime (optional — Railway injects `PORT` automatically; nginx uses it):

| Variable | Value |
|---|---|
| `GATEWAY_UPSTREAM` | `gateway.railway.internal:8080` only if using nginx `/api` proxy (usually leave unset and use `VITE_API_BASE_URL` instead) |

Build args:

| Build arg | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://<gateway-domain>` (include `https://`; host-only values are auto-fixed at build time) |
| `VITE_GOOGLE_CLIENT_ID` | Google web client id |
| `VITE_AUTH_MODE` | `dev` or `prod` |

Google OAuth (`redirect_uri_mismatch` fix): `docs/google-oauth-setup.md` —
register `https://<frontend-domain>/auth/callback` on the Web client.

## Deploy order

1. `postgres` (+ volume) and `redis`
2. `user-profile-service` → `auth-service` → `food-catalog-service` → `diary-service`
3. `gateway`
4. `frontend`

## Phase-specific notes

- Phase 1: `docs/railway-phase1.md`
- Phase 2 food catalog: `docs/railway-phase2.md`
- Phase 3 diary: `docs/railway-phase3.md`

## Local equivalent

```bash
cp .env.example .env
docker compose --profile full up --build
```

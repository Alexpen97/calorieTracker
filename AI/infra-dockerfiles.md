# Infra Dockerfiles for Railway

## Change

Added deployable Dockerfiles for infrastructure containers:

- `infra/postgres/Dockerfile` — `postgres:16-alpine` + baked `init/` scripts
- `infra/redis/Dockerfile` — `redis:7-alpine` + baked `redis.conf`

`docker-compose.yml` now builds these instead of pulling raw upstream images.

## Railway

Full deploy matrix: `docs/railway-deploy.md`

Postgres and Redis are Railway services with root directories `/infra/postgres`
and `/infra/redis`. App services connect via `postgres.railway.internal:5432`
and `redis.railway.internal:6379`.

Postgres requires a persistent volume on `/var/lib/postgresql/data`.

## App services

Already had Dockerfiles — no changes needed:

- `services/{gateway,auth-service,user-profile-service,food-catalog-service,diary-service}`
- `frontend`

`recommendation-service` remains Phase 6 stub (no Dockerfile yet).

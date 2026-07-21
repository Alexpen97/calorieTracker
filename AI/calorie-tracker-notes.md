# Calorie Tracker (NutriTrack) — AI work notes

## Completed

- 2026-07-21: Created the initial architecture/design document at
  `docs/calorie-tracker-architecture.md` (branch
  `cursor/calorie-tracker-architecture-4fd9`, PR #2).
- 2026-07-21 (v1.1): Extended the design per user follow-up: monorepo layout
  with one folder per service, Docker Compose local / Railway cloud
  deployment, nutrient education content (FR-9), moderated user product
  submissions in a separate staging table (FR-8 revised), body-weight tracking
  (FR-10), water-intake tracking (FR-11), sex/weight-based computed nutrient
  goals (FR-12), recommendation service for meal/cooking advice (FR-13,
  later phase), and Swagger aggregation on the gateway.
- 2026-07-21: Scaffolded the per-container folder structure in the repo:
  `services/{gateway,auth-service,user-profile-service,food-catalog-service,diary-service,recommendation-service}`,
  `frontend/`, and `infra/{postgres,redis}` — one folder per container, each
  with a README (purpose, ports, env vars). `infra/postgres/init/` holds the
  SQL creating the per-service databases for local Compose.
- 2026-07-21: **Phase 1 walking skeleton** implemented on
  `cursor/phase-1-walking-skeleton-58e0`:
  - Spring Boot 4.1 / Gateway 5.0.2 services: gateway, auth-service,
    user-profile-service (Flyway users schema, internal upsert, `/api/users/me`)
  - Auth: Google code exchange + `AUTH_MODE=dev` path, RS256 JWT, JWKS,
    refresh rotation (in-memory store)
  - React/Vite frontend with Google PKCE + Dev login and profile page
  - `docker-compose.yml` profiles `deps` / `full`, Dockerfiles, Railway notes
    in `docs/railway-phase1.md`
  - Tests: JUnit (auth, user, gateway) + Vitest (token storage)

## Key decisions recorded in the design doc

- Repo: single monorepo; deployables in `services/<name>/` and `frontend/`,
  each with its own Dockerfile and **standalone Maven project** (no parent POM
  / shared-lib folder) because Railway root-directory builds only pull the
  service folder (NFR-7).
- Backend: Spring Boot microservices — `gateway` (Spring Cloud Gateway +
  aggregated Swagger UI), `auth-service`, `food-catalog-service`,
  `diary-service`, `user-profile-service`, `recommendation-service` (stub
  until Phase 6); PostgreSQL per service + Redis cache.
- Deployment: Docker Compose locally (profiles: full stack vs deps-only);
  Railway in the cloud — one Railway service per folder, root directory +
  watch paths per service, private networking (`<service>.railway.internal`,
  plain http), public domains only on gateway + frontend.
- Auth Phase 1: SPA Authorization Code + PKCE; `auth-service` issues RS256
  JWTs; `AUTH_MODE=dev` accepts code `dev` without Google for local/CI.

## Remaining work / TODOs

- Roadmap phases 2–6 (design doc §14).
- Persist refresh tokens (DB/Redis) before multi-instance auth deploys.
- Restrict Swagger UI outside dev; wire Google OAuth credentials for prod.
- Resolve open questions in design doc §15 before the relevant phases.

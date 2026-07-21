# Phase 1 — Walking skeleton (Implementation 1)

## Goal

Deliver the Phase 1 roadmap item from `docs/calorie-tracker-architecture.md` §14:

- Docker Compose for local stack (postgres, redis, gateway, auth, user-profile, frontend)
- `gateway` with route forwarding + aggregated Swagger UI
- `auth-service` Google OIDC code exchange → app JWT (RS256) + refresh + JWKS
- `user-profile-service` with Flyway `users` schema and `/api/users/me` (+ internal upsert)
- React SPA with Google login (and a **dev login** path when Google creds are absent)
- Dockerfiles + Railway root-directory notes for first deploy

## Stack versions (from Context7 / Maven Central)

- Java 21
- Spring Boot **4.0.3**
- Spring Cloud Gateway **5.0.x** (`spring-cloud-starter-gateway-server-webflux`)
- springdoc-openapi **3.0.x** (Boot 4)
- React 19 + Vite + TypeScript + Vitest

## Auth flow (Phase 1)

1. SPA starts Google Authorization Code + PKCE (or Dev Login).
2. SPA `POST /api/auth/google/callback` with `{ code, codeVerifier, redirectUri }` (dev: `{ code: "dev", idToken }` stub).
3. `auth-service` verifies Google ID token (or accepts stub in `AUTH_MODE=dev`), calls
   `user-profile-service` internal upsert, issues access JWT + refresh token.
4. Other services validate JWT via `JWKS_URI` → auth-service `/.well-known/jwks.json`.

## Out of scope (later phases)

Food catalog, diary, recommendations, Capacitor, OFF import, goals engine full DRV tables.

## Test plan

- auth-service: JWT issue/validate unit tests; callback + refresh MockMvc tests (dev mode).
- user-profile-service: Flyway + H2/Testcontainers profile upsert + me endpoint tests.
- gateway: route wiring smoke test if feasible; otherwise config + compile.
- frontend: Vitest for auth token storage + login page render.

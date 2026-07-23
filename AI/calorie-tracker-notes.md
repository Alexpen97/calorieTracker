# Calorie Tracker (NutriTrack) — AI work notes

## Completed

- 2026-07-23: Web deep links (`/analytics`, etc.) blanked because Capacitor
  `base: './'` made assets resolve under the route; web builds now use `/`.
  Notes: `AI/analytics-deep-link-assets.md`.
- 2026-07-23: Dashboard card vertical spacing — `.dashboard-page` `row-gap`
  `0.27rem` → `0.9rem` (matches column gap). Notes: `AI/weight-30-day-graph.md`.
- 2026-07-22: Weight Progress chart is a compact line graph with kg/date axes,
  gridlines, and clearer markers; chart is full-width in the card. Notes:
  `AI/weight-line-graph-axes.md`.
- 2026-07-22: Dev account seeder — `scripts/seed-dev-data.ps1` seeds
  `dev` + `agent-debug` via gateway APIs; skips when data already exists
  (`-Force` to wipe). Added `DELETE /api/users/me/weight/{id}`. Notes:
  `AI/seed-dev-data.md`.
- 2026-07-22: Dashboard Weight Progress is a timed 30-day weigh-in graph;
  dashboard `row-gap` +50% (`0.18rem` → `0.27rem`). Notes:
  `AI/weight-30-day-graph.md`.
- 2026-07-22: Documented Codebase Memory MCP usage — always-apply Cursor rule
  `.cursor/rules/codebase-memory.mdc` and notes in `AI/codebase-memory.md`.
  Indexed project id: `D-repos-calorieTracker`. Prefer Memory over Grep/Glob
  for code discovery.
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
  with a README (purpose, ports, env vars) and Dockerfile for Railway deploy.
  `infra/postgres/init/` holds the SQL creating the per-service databases.
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
  Railway in the cloud — one Railway service per folder (including `infra/postgres`
  and `infra/redis`), root directory + watch paths per service, private
  networking (`<service>.railway.internal`, plain http), public domains only on
  gateway + frontend. See `docs/railway-deploy.md`.
- Auth Phase 1: SPA Authorization Code + PKCE; `auth-service` issues RS256
  JWTs; `AUTH_MODE=dev` accepts code `dev` without Google for local/CI.

## Onboarding (2026-07-22)

Implemented on `cursor/user-onboarding-flow-80e1`:

- `POST /api/users/me/onboarding` (profile + weight + apply goals)
- Frontend `/onboarding` wizard (weight, height, diet goal → nutrient goals)
- Notes: `AI/onboarding-flow.md`

## Expanded vitamins & minerals (2026-07-22)

Full micronutrient checklist on dashboard (summary → vitamins → minerals →
weight; macros card removed). Catalog education, goals DRVs, and OFF mappings
expanded — see `AI/expanded-micronutrients.md`.

2026-07-23: Diary and Analytics now show the same full vitamins/minerals grids
(and Analytics averages the range) — see `AI/fix-micros-diary-analytics.md`.

- Roadmap phase 6 (design doc §14).
- Persist refresh tokens (DB/Redis) before multi-instance auth deploys.
- Restrict Swagger UI outside dev; wire Google OAuth credentials for prod.
- Google `redirect_uri_mismatch`: register **frontend**
  `https://<frontend>/auth/callback` (not domain-only, not gateway, not another
  Railway app). Live NutriTrack example and checklist:
  `docs/google-oauth-setup.md`. Login page shows the exact URIs for the current origin.
- Resolve open questions in design doc §15 before the relevant phases.
- Assign initial `MODERATOR`/`ADMIN` users for the submission queue.
- Complete Play Store listing + signed AAB upload (`docs/android-play-store.md`);
  register Android OAuth client SHA-1 for package `com.nutritrack.app`.

## Bugfix: weight Instant from (2026-07-22)

Analytics sent LocalDate `from`/`to` to Instant weight API; shared React Query
key poisoned onboarding/dashboard. See `AI/weight-instant-from-param.md`.

## Phase 2 (2026-07-21)

Implemented on `cursor/phase-2-food-lookup-84b9`:

- `food-catalog-service`: Flyway schema + nutrient education seed, OFF live
  barcode lookup with Resilience4j, Redis/in-memory product cache, JWT APIs
- Gateway food OpenAPI aggregation + Compose service wiring
- Frontend lookup (manual + BarcodeDetector), product detail, nutrient sheets
- Tests: normalizer + MockMvc food APIs; Vitest barcode helpers
- 2026-07-21: Fixed Railway crash — `CacheConfig` now injects Spring Boot 4's
  auto-configured `JsonMapper` (Jackson 3) instead of Jackson 2 `ObjectMapper`

## Phase 3 (2026-07-21)

Implemented on `cursor/phase-3-diary-tracking-29d6` (PR #5):

- `user-profile-service`: body-weight log + goals engine (Mifflin-St Jeor,
  DRV seed, overrides / recalculate)
- `diary-service`: food entries with nutrient snapshots, water intake, daily
  summaries vs goals
- Gateway diary OpenAPI + Compose `diary-service` + `docs/railway-phase3.md`
- Frontend: Today diary, water quick-add, log-from-product, profile weight/goals
- Tests: user-profile 12, diary 11, gateway route assert, frontend Vitest 8

## Phase 4 (2026-07-22)

Implemented on `cursor/phase-4-mirror-search-submissions-f0d1`:

- food-catalog: OFF JSONL Spring Batch import, name search + OFF fallback,
  product submissions + moderation roles, PostgreSQL FTS GIN migration
- diary: `submissionId` on create entry
- frontend: search, submit product, moderation queue
- Docs: `AI/phase-4-mirror-search-submissions.md`, `docs/railway-phase4.md`

## Phase 5 (2026-07-22)

Implemented on `cursor/phase-5-android-7d17`:

- Capacitor 8 Android shell (`com.nutritrack.app`) wrapping the React SPA
- ML Kit barcode scan on native; web `BarcodeDetector` unchanged
- Native Google Sign-In (server auth code) + optional `redirectUri` on auth callback
- Keystore-backed secure token storage; gateway CORS includes `https://localhost`
- Play Store checklist: `docs/android-play-store.md`
- Notes: `AI/phase-5-android.md`

## Planned: USDA micronutrient enrichment (2026-07-23)

Separate `nutrient-enrichment-service` fills missing vitamins/minerals on OFF
products from USDA FoodData Central (GTIN → name+brand → generic
Foundation/SR proxy), stored as estimated (`source=USDA_*`) and never
overwriting OFF values. Plan:
`docs/superpowers/plans/2026-07-23-usda-micronutrient-enrichment.md`.

## Implemented: USDA micronutrient enrichment (2026-07-23)

Implemented on `cursor/usda-enrichment-plan-1504`:

- New `services/nutrient-enrichment-service` (FDC client, matching, 90-day cache,
  `POST /internal/enrich`)
- food-catalog: `V6` nutrient `source`, EnrichmentClient after OFF upsert,
  `estimated` DTO, ADMIN backfill
- Compose + `enrichment` DB; frontend ≈ marker on product detail
- Notes: `AI/usda-micronutrient-enrichment.md`, `docs/railway-enrichment.md`

## Today Summary calorie ring + macro bars (2026-07-22)

Dashboard Today Summary uses a calorie progress ring with three horizontal macro
bars. Calorie center and each macro show `amount / goal` (no outer macro ring or
percent labels). Ring stroke is tightened so center text clears the track.
Notes: `AI/today-summary-nested-donut.md`.

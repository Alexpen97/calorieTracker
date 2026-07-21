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
  plain http), public domains only on gateway + frontend. Verified via
  Context7 (`/railwayapp/docs`): root directory, watch paths, private
  domains behavior.
- Food data: Open Food Facts (ODbL). Verified via Context7
  (`/websites/openfoodfacts_github_io_openfoodfacts-server_api`): API v2,
  rate limits 15 product reads/min & 10 searches/min per IP → local mirror via
  Spring Batch bulk import + Redis caching.
- User submissions: separate `product_submission` staging table (JSONB
  nutrients), immediately usable by submitter only, moderator approve/reject
  copies into `product` with `source = USER_APPROVED`. Roles
  (USER/MODERATOR/ADMIN) carried in JWT claims.
- Nutrient education: education fields on the `nutrient` reference table
  (body effects, deficiency, excess, sources, citation), seeded via Flyway
  from NIH ODS / EFSA; served at `/api/nutrients`; "not medical advice"
  disclaimer (NFR-8).
- Goals engine (user-profile-service): Mifflin-St Jeor BMR × activity ×
  objective for energy; g/kg for protein; `nutrient_reference_intake` table
  keyed by sex + age band (FIXED or PER_KG basis) for micros; ~35 ml/kg water.
  Computed suggestions vs user overrides tracked via `origin` column.
- Weight tracking: append-only `body_weight_log` in users schema; water
  intake: `water_intake` rows in diary schema, summarized with targets.
- Recommendations (Phase 6): rule-based gap-filling scoring over frequently
  logged products + remaining daily nutrient budget; own service, no data
  ownership.
- Diary entries snapshot per-100g nutrient values at logging time (OFF data
  and submissions are mutable).
- Frontend: React + TypeScript + Capacitor for Android; barcode scanning via
  `BarcodeDetector`/ZXing on web and ML Kit on Android.
- Auth: Google OIDC Authorization Code + PKCE; `auth-service` issues RS256
  JWTs (JWKS); all services are OAuth2 resource servers.

## Remaining work / TODOs

- Roadmap phases 1–6 (design doc §14), starting with Phase 1: repo layout +
  Docker Compose, gateway with Swagger aggregation, auth-service Google login,
  user-profile-service, React app login, first Railway deploy.
- Resolve open questions in design doc §15 (offline mode, recipes, USDA
  secondary source, household units, notifications, initial moderator
  assignment) before the relevant phases.

# Phase 3 — Diary & tracking (Implementation 3)

## Goal

Deliver Phase 3 from `docs/calorie-tracker-architecture.md` §14:

- `diary-service`: food entries with nutrient snapshots, water intake, daily summaries
- `user-profile-service`: body-weight log (FR-10), goals engine (FR-12)
- Gateway OpenAPI + Compose wiring
- React: Today diary, water quick-add, log-from-product, profile weight/goals

## Plan

See `docs/superpowers/plans/2026-07-21-phase-3-diary-tracking.md`.

## Status

Completed on `cursor/phase-3-diary-tracking-29d6` (PR #5).

## Delivered

### user-profile-service

- `POST/GET /api/users/me/weight` — append-only body weight log
- Goals engine (`GoalsEngine`): Mifflin-St Jeor × activity × objective;
  protein g/kg by objective; water 35 ml/kg; micronutrient DRVs from local
  Flyway `V2__nutrient_reference_intake.sql` seed
- `GET/PUT /api/users/me/goals`, `POST /api/users/me/goals/recalculate?apply=`
- Overrides (`USER_OVERRIDE`) preserved on recalculate

### diary-service (greenfield)

- Standalone Boot 4.1.0 service, Flyway `diary` schema
- Food entries with denormalized per-100g snapshots + portion math
- Water intake CRUD
- Daily / range summaries vs user goals (RestClient → food + user services)
- Tests: PortionMath + MockMvc (11 tests)

### Gateway / Compose / Railway

- `diary-api-docs` route + Swagger UI entry
- `diary-service` container on Compose `full` profile
- `docs/railway-phase3.md`

### Frontend

- `/today` DiaryPage: energy/macros/water, meal entries, quick-add water
- ProductPage: grams + meal type → add to diary
- ProfilePage: profile edit, weight log, goals override + recalculate
- Nav: Today | Lookup | Profile

## Verification (2026-07-21)

- user-profile: 12 tests pass
- diary: 11 tests pass
- gateway: 1 test pass
- food-catalog + auth: regression green
- frontend Vitest: 8 tests pass

## Follow-ups

- Persist refresh tokens before multi-instance auth
- Harden barcode scanner fallbacks (Phase 2 leftover)
- Phase 4: OFF bulk mirror + FTS + user submissions

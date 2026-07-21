# Phase 3 — Diary & Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Phase 3 from `docs/calorie-tracker-architecture.md` §14: diary-service with portion math and daily summaries, water intake, body-weight log, and goals engine with computed suggestions and overrides.

**Architecture:** Extend `user-profile-service` for FR-10/FR-12 (weight log + goals). Greenfield `diary-service` for FR-4/FR-5/FR-11 (food entries with denormalized nutrient snapshots, water, summaries that pull targets from user-profile). Wire gateway OpenAPI + Compose. React UI for Today diary, water quick-add, log-from-product, weight/goals on Profile.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Data JPA, Flyway, RestClient, springdoc 3.0.3, H2+MockMvc tests; React 19 + Vite + TanStack Query + Vitest.

## Global Constraints

- Standalone Maven per service (no parent POM); Boot **4.1.0**, Java **21**, springdoc **3.0.3**.
- User id always from JWT `sub` (UUID); never trust body for ownership.
- Match existing patterns: `SecurityConfig` roles claim, H2 `MODE=PostgreSQL` tests, nested DTO records, `PORT` env, multi-stage Dockerfile.
- Diary stores **denormalized** per-100g nutrient snapshots; portion math server-side: `amount = per100g × weightG / 100`.
- Goals: Mifflin-St Jeor × activity × objective; protein g/kg; water ~35 ml/kg; micronutrients from seeded `nutrient_reference_intake` in **user-profile** (V2 seed copy of food-catalog DRVs for service independence). Overrides (`USER_OVERRIDE`) never silently replaced on recalculate.
- Pseudo nutrient code for water target: `water_ml`.
- Energy target code: `energy_kcal`.

## File map

| Area | Create / Modify |
|------|-----------------|
| user-profile weight/goals | domain entities, repos, `GoalsEngine`, `WeightService`, `GoalsService`, controllers, `V2__nutrient_reference_intake.sql`, tests |
| diary-service | full scaffold: pom, Dockerfile, Flyway, domain, RestClients, services, controllers, tests |
| gateway | diary-api-docs route + swagger url; test assert diary route |
| compose | `diary-service` block; gateway depends_on |
| frontend | api client, DiaryPage, ProductPage log form, Profile weight/goals, App nav, tests |
| docs | `AI/phase-3-diary-tracking.md`, `docs/railway-phase3.md`, update `AI/calorie-tracker-notes.md` |

---

### Task 1: Body weight API (user-profile-service)

**Files:**
- Create: `services/user-profile-service/src/main/java/com/nutritrack/user/domain/BodyWeightLog.java`
- Create: `services/user-profile-service/src/main/java/com/nutritrack/user/domain/BodyWeightLogRepository.java`
- Create: `services/user-profile-service/src/main/java/com/nutritrack/user/service/WeightService.java`
- Modify: `services/user-profile-service/src/main/java/com/nutritrack/user/web/UserController.java` (or new `WeightController`)
- Test: extend `UserControllerTest.java` or `WeightControllerTest.java`

**Interfaces:**
- Produces: `POST /api/users/me/weight` `{ weightKg, measuredAt? }` → weight entry
- Produces: `GET /api/users/me/weight?from=&to=` → list ordered by measuredAt desc
- Tables already exist in `V1__users_schema.sql`

- [ ] **Step 1:** Write failing MockMvc tests for POST/GET weight with JWT subject scoping
- [ ] **Step 2:** Implement entity, repo, service, endpoints
- [ ] **Step 3:** Run `mvn -q test` in user-profile-service — pass
- [ ] **Step 4:** Commit

---

### Task 2: Goals engine (user-profile-service)

**Files:**
- Create: `V2__nutrient_reference_intake.sql` (table + adult DRV seeds matching food-catalog)
- Create: `NutrientReferenceIntake`, repository, `GoalOrigin` enum, `UserGoal` entity/repo
- Create: `GoalsEngine.java` (Mifflin-St Jeor, activity multipliers, objective factors, water 35 ml/kg, DRV lookup)
- Create: `GoalsService.java` + controller endpoints
- Test: `GoalsEngineTest.java` (unit) + MockMvc for goals APIs

**Formulas (lock these values):**
- BMR male: `10*kg + 6.25*cm - 5*age + 5`; female: `10*kg + 6.25*cm - 5*age - 161`
- Activity multipliers: SEDENTARY 1.2, LIGHT 1.375, MODERATE 1.55, ACTIVE 1.725, VERY_ACTIVE 1.9 (enum already has ActivityLevel — map accordingly)
- Objective on TDEE: LOSE ×0.85, MAINTAIN ×1.0, GAIN ×1.15
- Protein: LOSE 1.6 g/kg, MAINTAIN 1.2 g/kg, GAIN 1.8 g/kg (override DRV PER_KG protein with objective factor)
- Water: `35 * weightKg` → `water_ml`
- Recalculate: replace only `COMPUTED` rows; leave `USER_OVERRIDE` untouched; return `{ goals, suggested, needsProfile: bool }`

**Endpoints:**
- `GET /api/users/me/goals`
- `PUT /api/users/me/goals` body: list of `{ nutrientCode, dailyTarget, unit }` → mark USER_OVERRIDE
- `POST /api/users/me/goals/recalculate` → recompute suggestions; accept optional `apply=true` query to write COMPUTED gaps

- [ ] **Step 1:** Unit tests for GoalsEngine golden cases
- [ ] **Step 2:** Implement engine + seed + APIs + MockMvc tests
- [ ] **Step 3:** `mvn -q test` pass; commit

---

### Task 3: diary-service scaffold + food entries

**Files:** Full greenfield under `services/diary-service/` mirroring food-catalog/user-profile.

**Schema `V1__diary_schema.sql`:** `diary_entry`, `diary_entry_nutrient`, `water_intake` per architecture §7.2.

**RestClient:** `FoodCatalogClient.getProduct(UUID id, String bearerToken)` → ProductResponse-shaped record; forward Authorization.

**Portion math:** `PortionMath.scale(per100g, weightG)` → HALF_UP 2 decimal places.

**Endpoints:**
- `POST /api/diary/entries` `{ productId, weightG, mealType, consumedAt? }`
- `GET /api/diary/entries?date=YYYY-MM-DD`
- `PUT /api/diary/entries/{id}` (weight/mealType/consumedAt; re-scale from stored snapshot)
- `DELETE /api/diary/entries/{id}`

- [ ] **Step 1:** Scaffold pom/Dockerfile/application.yml/SecurityConfig
- [ ] **Step 2:** Failing tests for create entry (mock FoodCatalogClient) + portion math unit test
- [ ] **Step 3:** Implement; `mvn -q test` pass; commit

---

### Task 4: Water + daily summary (diary-service)

**Endpoints:**
- `POST /api/diary/water` `{ amountMl, loggedAt? }`
- `GET /api/diary/water?date=`
- `DELETE /api/diary/water/{id}`
- `GET /api/diary/summary?date=` → totals per nutrient (scaled) + water vs targets from `UserGoalsClient`
- `GET /api/diary/summary/range?from=&to=` → list of daily summaries (lightweight)

**UserGoalsClient:** `GET {USER_SERVICE_URL}/api/users/me/goals` with forwarded Bearer.

- [ ] **Step 1:** Failing tests with mocked UserGoalsClient
- [ ] **Step 2:** Implement; tests pass; commit

---

### Task 5: Gateway, Compose, Railway docs

- Add `diary-api-docs` route + swagger-ui entry
- Assert diary route in gateway test
- Add `diary-service` to docker-compose `full` profile
- Write `docs/railway-phase3.md`
- [ ] Commit

---

### Task 6: Frontend — Today diary + log food + water

- Extend `api/client.ts` with diary/weight/goals types and functions
- `DiaryPage` at `/today`: summary rings/bars, entries by meal, water quick-add (250/500/custom)
- ProductPage: grams + meal type + “Add to diary”
- App nav: Today | Lookup | Profile
- Vitest for portion display helper or water total helper
- [ ] Commit

---

### Task 7: Frontend — Profile weight & goals

- ProfilePage: log weight, weight history list, goals list with override inputs, Recalculate button
- Profile edit for sex/height/birthDate/activity/objective if missing (PUT /api/users/me)
- Vitest for a small goals display helper if useful
- [ ] Commit

---

### Task 8: AI notes + verification

- Write `AI/phase-3-diary-tracking.md`; update `AI/calorie-tracker-notes.md`
- Run all service `mvn test` + `frontend npm test`
- [ ] Final commit

## Self-review

- Spec coverage: FR-4,5,7(scoping),10,11,12 + Phase 3 roadmap ✓
- Out of scope: FR-3 search, FR-8 submissions, Phase 4–6 ✓
- No placeholders in task interfaces ✓

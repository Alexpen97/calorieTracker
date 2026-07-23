# USDA Micronutrient Enrichment Service Implementation Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** New `nutrient-enrichment-service` that fills missing vitamins/minerals on OFF-sourced products using USDA FoodData Central (FDC), matched by GTIN first, then name+brand, then a generic Foundation/SR Legacy proxy — values stored and displayed as **estimated**.

**Architecture:** Separate Spring Boot microservice (own PostgreSQL DB `enrichment`, own FDC client + rate limiting + result cache). `food-catalog-service` calls it **best-effort** (internal API key, short timeout) after an OFF barcode upsert when the product has few micronutrients, and fills **only missing** micro codes tagged `USDA_BRANDED` / `USDA_PROXY`. UI shows an "estimated" marker. No gateway route — internal-only, plus an ADMIN backfill trigger on food-catalog.

**Tech Stack:** Java 21, Spring Boot **4.1.0** (standalone Maven, no parent POM), Spring Data JPA + Flyway + PostgreSQL, `RestClient` for FDC, Resilience4j 2.x (RateLimiter + CircuitBreaker + Retry), springdoc 3.0.3, H2 `MODE=PostgreSQL` + MockMvc + `MockRestServiceServer` tests; React 19 + Vitest.

## Global Constraints

- Standalone Maven per service; Boot **4.1.0**, Java **21**, springdoc **3.0.3**; multi-stage Dockerfile; `PORT` env (default 8086 locally).
- Internal endpoint auth: `X-Internal-Api-Key` == `INTERNAL_API_KEY` (same pattern as user-profile internal upsert). No JWT needed on the internal enrich route.
- FDC: base `https://api.nal.usda.gov/fdc/v1`, key via `FDC_API_KEY` (local dev may use `DEMO_KEY`, 30 req/h/IP; real key from api.data.gov is free, 1 000 req/h). Rate-limit client to ≤ 600/h, retry 5xx once, circuit-break.
- Enrichment scope = **micronutrients only** (the 13 vitamin + 13 mineral codes below). Never touch macros; never overwrite an existing `product_nutrient` row.
- Estimated values must be distinguishable end-to-end: `product_nutrient.source` (`OFF` | `USDA_BRANDED` | `USDA_PROXY` | `USER`), `estimated` flag in API DTOs, "≈" marker + footnote in UI.
- Food-catalog must keep working when enrichment is down/disabled (`ENRICHMENT_ENABLED=false` or call failure ⇒ product returned unenriched).
- Match existing patterns: nested DTO records, H2 tests, Flyway per service, `AI/` notes.

## Data flow

```
barcode scan → food-catalog: cache → DB → OFF fetch + upsert (existing)
  └─ if product.source==OFF and micro codes present < 6:
       POST enrichment /internal/enrich {barcode,name,brand,existingCodes}
         enrichment: cache table → FDC GTIN search → FDC branded name+brand
                     → FDC Foundation/SR generic proxy (unless fortified guard)
         ← {matchType, fdcId, description, confidence, nutrients[]}
       food-catalog: insert only-missing micro rows (source=USDA_*), evict cache
```

## Micronutrient code ↔ FDC nutrient mapping

Match on FDC `foodNutrients[].nutrient.number` (string). Amounts are per 100 g for Branded, Foundation and SR Legacy — verify against fixtures in Task 3.

| Our code | FDC number | FDC name (unit) |
|---|---|---|
| vitamin_a | 320 | Vitamin A, RAE (µg) |
| vitamin_b1 | 404 | Thiamin (mg) |
| vitamin_b2 | 405 | Riboflavin (mg) |
| vitamin_b3 | 406 | Niacin (mg) |
| vitamin_b5 | 410 | Pantothenic acid (mg) |
| vitamin_b6 | 415 | Vitamin B-6 (mg) |
| vitamin_b7 | 416 | Biotin (µg) |
| vitamin_b9 | 417 | Folate, total (µg) |
| vitamin_b12 | 418 | Vitamin B-12 (µg) |
| vitamin_c | 401 | Vitamin C, total ascorbic acid (mg) |
| vitamin_d | 328 | Vitamin D (D2+D3) (µg) |
| vitamin_e | 323 | Vitamin E, alpha-tocopherol (mg) |
| vitamin_k | 430 | Vitamin K, phylloquinone (µg) |
| calcium | 301 | Calcium, Ca (mg) |
| iron | 303 | Iron, Fe (mg) |
| magnesium | 304 | Magnesium, Mg (mg) |
| phosphorus | 305 | Phosphorus, P (mg) |
| potassium | 306 | Potassium, K (mg) |
| sodium | 307 | Sodium, Na (mg) |
| zinc | 309 | Zinc, Zn (mg) |
| copper | 312 | Copper, Cu (mg) |
| iodine | 314 | Iodine, I (µg) |
| manganese | 315 | Manganese, Mn (mg) |
| selenium | 317 | Selenium, Se (µg) |
| chromium | — | Chromium, Cr (µg) — rarely present; map by nutrient name |
| molybdenum | — | Molybdenum, Mo (µg) — rarely present; map by nutrient name |

If FDC unit differs from ours (e.g. IU), skip the value rather than convert (keep v1 simple; log at DEBUG).

## Matching rules (enrichment service)

1. **GTIN:** `GET /foods/search?query={barcode}&dataType=Branded`. Accept a hit whose `gtinUpc`, stripped of leading zeros, equals the barcode stripped of leading zeros. `matchType=GTIN`, confidence 1.0.
2. **Name + brand (Branded):** `query={name}`, `dataType=Branded`, `brandOwner={brand}` (retry once without `brandOwner` if 0 hits and brand was set). Score = token Jaccard between normalized names (lowercase, strip punctuation/quantities/stop-words: `with, and, the, of, original, classic, new`). Accept best hit ≥ **0.5**. `matchType=NAME_BRAND`.
3. **Generic proxy (Foundation → SR Legacy):** query = name with brand tokens and quantity words removed. Accept best hit ≥ **0.35**. `matchType=GENERIC_PROXY`. **Fortified guard:** skip this step entirely when name/OFF categories contain any of `cereal, fortified, enriched, infant, formula, supplement, protein powder, energy drink, meal replacement, soy drink, oat drink, almond drink, plant milk` (fortification levels vary too much between brands for a generic proxy).
4. Otherwise `matchType=NONE`, empty nutrients.

Results (including NONE) are cached in the `enrichment_lookup` table for 90 days keyed by barcode, so repeat scans don't burn FDC quota.

## Internal API contract

`POST /internal/enrich` (header `X-Internal-Api-Key`)

```json
// request
{ "barcode": "3017620422003", "name": "Nutella", "brand": "Ferrero",
  "existingNutrientCodes": ["vitamin_e", "calcium"] }
// response 200
{ "matchType": "GTIN", "fdcId": 2262074, "matchedDescription": "NUTELLA",
  "confidence": 1.0,
  "nutrients": [ { "code": "vitamin_b3", "amountPer100g": 1.2, "unit": "mg" } ] }
```

`nutrients` excludes any code in `existingNutrientCodes`. `matchType` ∈ `GTIN | NAME_BRAND | GENERIC_PROXY | NONE`.

## File map

| Area | Create / Modify |
|------|-----------------|
| new service | `services/nutrient-enrichment-service/` — `pom.xml`, `Dockerfile`, `README.md`, `application.yml`, pkg `com.nutritrack.enrichment` (`config/`, `fdc/`, `domain/`, `service/`, `web/`) |
| enrichment schema | `db/migration/V1__enrichment_lookup.sql` |
| food-catalog | `V6__product_nutrient_source.sql`; `ProductNutrient` (+`source`), `ProductSource`(+2 values) or new `NutrientSource` enum; `EnrichmentClient`, `EnrichmentService`; hook in `ProductLookupService.fetchPersistAndCache`; `ProductMapper`/DTO `estimated`; `AdminImportController` backfill endpoint |
| infra | `infra/postgres/init/*.sql` add `CREATE DATABASE enrichment;`; `docker-compose.yml` service + food-catalog env |
| frontend | product nutrition table "≈" + footnote; `api` types `estimated?: boolean` |
| docs | `AI/usda-micronutrient-enrichment.md`, `docs/railway-enrichment.md`, update `AI/calorie-tracker-notes.md` |

---

### Task 1: Scaffold `nutrient-enrichment-service`

- [ ] Copy food-catalog Maven/Dockerfile structure: Boot 4.1.0, web, data-jpa, flyway, postgres, h2 (test), springdoc 3.0.3, resilience4j-spring-boot3
- [ ] `application.yml`: `PORT:8086`, datasource env, `FDC_BASE_URL`, `FDC_API_KEY`, `INTERNAL_API_KEY`
- [ ] Security: permit `/actuator/health`, `/v3/api-docs/**`; internal-key filter on `/internal/**` (mirror user-profile-service internal filter)
- [ ] Flyway `V1__enrichment_lookup.sql`: `enrichment_lookup(barcode varchar(32) PK, match_type varchar(16), fdc_id bigint null, matched_description text null, confidence numeric null, nutrients_json text, created_at timestamptz)`
- [ ] Boot test: context loads, health 200; commit

### Task 2: FDC client

- [ ] `FdcClient` (`RestClient`): `searchFoods(query, dataTypes, brandOwner, pageSize=10)` → `/foods/search`; `getFood(fdcId)` → `/food/{fdcId}?format=full`
- [ ] Resilience4j: RateLimiter 10/min, Retry(1, 5xx), CircuitBreaker; empty Optional on open circuit / 4xx
- [ ] Tests with `MockRestServiceServer` + JSON fixtures (search page, food detail, 429, 500); commit

### Task 3: FDC → internal nutrient mapper

- [ ] `FdcNutrientMapper.map(foodNutrients) -> List<MappedNutrient(code, amountPer100g, unit)>` using the number table above (+ name-based fallback for chromium/molybdenum); skip unit mismatches
- [ ] Golden fixture tests: one Branded food (label micros only), one Foundation food (rich micros), IU value skipped; commit

### Task 4: Matching pipeline + `/internal/enrich`

- [ ] `NameNormalizer` (lowercase, strip punctuation/quantity tokens/stop-words) + `tokenJaccard` — unit tests first
- [ ] `EnrichmentService.enrich(request)`: lookup-table hit (≤90 d) → GTIN → NAME_BRAND (≥0.5) → fortified guard → GENERIC_PROXY (≥0.35) → NONE; persist to `enrichment_lookup`; filter `existingNutrientCodes`
- [ ] `EnrichmentController` `POST /internal/enrich` (401 without key)
- [ ] MockMvc tests: GTIN hit, name+brand hit, proxy hit, fortified name skips proxy, NONE cached, second call served from table (FDC mock verified not called); commit

### Task 5: food-catalog integration

- [ ] Flyway `V6__product_nutrient_source.sql`: `ALTER TABLE product_nutrient ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'OFF'`
- [ ] `ProductNutrient` + new enum `NutrientSource { OFF, USDA_BRANDED, USDA_PROXY, USER }`; submissions write `USER`
- [ ] `EnrichmentClient` (RestClient, `ENRICHMENT_SERVICE_URL`, `INTERNAL_API_KEY`, 3 s timeout, CircuitBreaker, no-op when `ENRICHMENT_ENABLED=false`)
- [ ] `ProductEnrichmentService.enrichIfSparse(product)`: only `source==OFF`, micro codes < 6 of the 26; append missing rows with `USDA_BRANDED`/`USDA_PROXY`; evict barcode cache entry
- [ ] Call from `ProductLookupService.fetchPersistAndCache` after upsert (failures swallowed + logged)
- [ ] DTO: `NutrientAmount` gains `estimated` (true when source is `USDA_*`); mapper sets it
- [ ] Tests: sparse OFF product gets filled (enrichment mocked), existing codes never overwritten, enrichment failure still returns product, `estimated` serialized; commit

### Task 6: Admin backfill

- [ ] `POST /api/admin/enrichment-backfill` (ADMIN, same guard as OFF import) — page through OFF products with < 6 micro codes, call enrichment, report `{scanned, enriched, failed}`
- [ ] MockMvc test: role guard 403 for USER, happy path counts; commit

### Task 7: Compose, infra, gateway

- [ ] `infra/postgres/init`: `CREATE DATABASE enrichment;`
- [ ] `docker-compose.yml`: `nutrient-enrichment-service` (profile `full`, depends_on postgres healthy) + food-catalog env `ENRICHMENT_SERVICE_URL=http://nutrient-enrichment-service:8080`, `ENRICHMENT_ENABLED=true`, `FDC_API_KEY=${FDC_API_KEY:-DEMO_KEY}`
- [ ] No gateway route (internal-only); note in gateway yml comment next to recommendation-service line
- [ ] `docker compose --profile full config` sanity; commit

### Task 8: Frontend estimated marker

- [ ] API types: nutrient rows `estimated?: boolean`
- [ ] Product detail nutrition table: prefix "≈" on estimated rows + footnote "≈ estimated from USDA FoodData Central generic data"
- [ ] Vitest: renders marker + footnote only when an estimated row exists; commit

### Task 9: Docs + verify

- [ ] `AI/usda-micronutrient-enrichment.md` (decisions, mapping table, thresholds), update `AI/calorie-tracker-notes.md`
- [ ] `docs/railway-enrichment.md`: new Railway service (root dir, `FDC_API_KEY` secret, private networking), food-catalog env additions
- [ ] Full backend + frontend test suites green; push PR

## Notes / follow-ups (out of scope)

- Diary snapshots nutrients at entry time — backfilled micros only affect **new** entries; a diary re-snapshot job is a possible follow-up.
- Unit conversion for FDC IU values (older SR records) — skipped in v1.
- Async/queued enrichment instead of best-effort sync call — revisit if FDC latency hurts barcode lookups.

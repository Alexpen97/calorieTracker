# ULT-8 Colloquial Food Search (NEVO) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Linear:** [ULT-8](https://linear.app/ultimateconcept/issue/ULT-8/fix-food-search-colloquial-names-eg-paprika-dont-return-vegetables)

**Goal:** Make Look up → Search return NEVO whole-food vegetables for colloquial NL/EN queries (e.g. `paprika`, `courgette`, `aubergine`) on the first page, without breaking packaged-product (OFF) search.

**Architecture:** Add a public NEVO term-search API that expands aliases and ranks vegetable hits above spices/snacks. Have `food-catalog-service` call that API from `ProductSearchService` and **prepend** ranked NEVO hits into the existing `GET /api/products/search` response (same shape, `source: "NEVO"`, plus `nevoCode`). Update Lookup UI to render/link NEVO hits. Diary logging of NEVO foods is **out of this card** — document a follow-up.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Data JPA, Flyway, MockMvc/H2 tests; React 19 + Vite + Vitest; gateway already routes `/api/nevo/**`.

## Global Constraints

- Standalone Maven per service (no parent POM); Boot **4.1.0**, Java **21**.
- Do **not** re-import or edit `NEVO2025_v9.0.csv`.
- Seed **high-impact aliases only** (extensible via Flyway); no broad dictionary for ~2300 foods.
- Do **not** change OFF mirror search / OFF fallback behaviour for branded packaged products (regression tests must stay green).
- Prefer gateway base `https://gateway-production-777b.up.railway.app` for manual API checks; Dev Login for JWT.
- Never invent third-party APIs — use Context7 if touching Spring/Flyway/React APIs beyond existing project patterns.
- Use Codebase Memory (`project: "D-repos-calorieTracker"`) when available; otherwise targeted Read of paths below.
- Keep commits small and push to the feature branch frequently.

## Design decision (locked)

Three approaches were considered:

| Approach | Summary | Verdict |
| --- | --- | --- |
| **A — Merge NEVO into product search** | Catalog calls NEVO search; one UI endpoint | **Chosen** — matches AC (`GET /api/products/search?q=paprika` includes NEVO vegetables) and minimizes frontend churn |
| B — Separate NEVO search only | Frontend calls `/api/nevo/foods/search` + products | Rejected as primary — split ranking / two round trips; keep endpoint but catalog owns merge |
| C — Materialize all NEVO as products | Upsert ~2300 foods into `product` | Rejected for this card — heavy, duplicates enrichment model |

**Diary logging:** Acceptance allows documenting follow-up. Lookup currently links `product/{uuid}` and diary requires `productId`. Do **not** invent synthetic catalog rows in this card unless leftover after search+UI. Ship search + detail via NEVO code; open/link a follow-up for “materialize NEVO → catalog product on Add to diary”.

## Confirmed data facts (do not rediscover)

NEVO CSV already contains:

- `Paprika groene/rode/gele rauw` ↔ `Sweet pepper … raw` (codes 31, 884, 2740, …) — NL name already includes `paprika`
- `Courgette rauw` / `Courgettes raw` (922)
- `Aubergine rauw` / `Aubergine/eggplant raw` (10)
- Noise for `paprika`: `Paprikapoeder` (1229, Herbs and spices), chips/snacks with paprika flavour

So wiring NEVO search alone is not enough — **ranking** must prefer Vegetables for plain single-word vegetable colloquialisms.

## File map

| Area | Create / Modify |
| --- | --- |
| Aliases | Create `services/nevo-service/src/main/resources/db/migration/V2__vegetable_colloquial_aliases.sql` |
| NEVO search service | Create `…/nevo/search/NevoFoodSearchService.java` (+ rank helper if needed) |
| NEVO search DTO | Create `…/web/dto/NevoFoodSearchResponse.java` (and item record) |
| NEVO controller | Modify `…/web/NevoFoodController.java` — add `GET /foods/search` |
| Alias expand | Modify `ProductNameNormalizer` **or** small `AliasExpander` used by search (token + multi-word alias) |
| Sample fixture | Extend `services/nevo-service/src/test/resources/nevo-sample.csv` with paprika/courgette/aubergine (+ one spice noise row) |
| NEVO tests | Extend `NevoServiceIntegrationTest` + unit tests for ranking/aliases |
| Catalog Nevo client | Modify `NevoClient` / `RestNevoClient` — add `searchFoods(q, limit)` |
| Catalog DTOs | Modify `ProductResponse` — optional `nevoCode`, `foodGroup`; allow non-persisted NEVO hits |
| Catalog search | Modify `ProductSearchService` — call NEVO, prepend ranked hits |
| Catalog tests | Extend `ProductControllerTest` (mock NevoClient search) |
| Frontend types | Modify `frontend/src/api/client.ts` — `nevoCode?`, `foodGroup?` |
| Lookup UI | Modify `LookupPage.tsx` — show NEVO badge/group; link NEVO hits to detail route |
| NEVO detail UI | Small route or ProductPage branch for `source === 'NEVO'` via `GET /api/nevo/foods/{code}` |
| Docs | Update `AI/nevo-micronutrient-estimates.md` briefly; keep this plan as source of truth |

---

### Task 1: Seed vegetable colloquial aliases

**Files:**
- Create: `services/nevo-service/src/main/resources/db/migration/V2__vegetable_colloquial_aliases.sql`
- Test: extend normalizer / search tests in later tasks (Flyway runs in existing H2 tests)

**Interfaces:**
- Consumes: existing `nevo_alias(alias_term, canonical_term)` unique on `alias_term`
- Produces: aliases usable by search expansion

- [ ] **Step 1: Add Flyway V2 with high-impact seeds**

```sql
-- V2__vegetable_colloquial_aliases.sql
-- NL colloquial / EN variants → terms that hit NEVO EN names / search_document.
-- Keep small and extensible; do not dump a full bilingual dictionary.

INSERT INTO nevo_alias (id, alias_term, canonical_term) VALUES
  ('22222222-2222-2222-2222-222222222201', 'paprika', 'sweet pepper'),
  ('22222222-2222-2222-2222-222222222202', 'bell pepper', 'sweet pepper'),
  ('22222222-2222-2222-2222-222222222203', 'zucchini', 'courgette'),
  ('22222222-2222-2222-2222-222222222204', 'courgettes', 'courgette'),
  ('22222222-2222-2222-2222-222222222205', 'eggplant', 'aubergine');
```

Notes:
- `paprika` → `sweet pepper` helps EN path; NL `Paprika …` already matches term `paprika` via LIKE.
- `courgette` / `aubergine` already appear in NEVO names; aliases cover EN variants.
- Do **not** alias paprika to powder/spice.

- [ ] **Step 2: Commit**

```bash
git add services/nevo-service/src/main/resources/db/migration/V2__vegetable_colloquial_aliases.sql
git commit -m "feat(nevo): seed vegetable colloquial aliases for ULT-8"
git push -u origin HEAD
```

---

### Task 2: NEVO food search + ranking (nevo-service)

**Files:**
- Create: `services/nevo-service/src/main/java/com/nutritrack/nevo/search/NevoFoodSearchService.java`
- Create: `services/nevo-service/src/main/java/com/nutritrack/nevo/web/dto/NevoFoodSearchResponse.java`
- Modify: `services/nevo-service/src/main/java/com/nutritrack/nevo/web/NevoFoodController.java`
- Modify: `services/nevo-service/src/main/java/com/nutritrack/nevo/match/ProductNameNormalizer.java` (add `expandSearchTerms(String raw)` that returns original + canonical substitutions for whole-string and token aliases)
- Modify: `services/nevo-service/src/test/resources/nevo-sample.csv` — add vegetable + spice rows
- Create/Modify tests: `…/search/NevoFoodSearchServiceTest.java`, `ProductNameNormalizerTest.java`, `NevoServiceIntegrationTest.java`

**Interfaces:**
- Consumes: `NevoFoodRepository.searchByTerm(term, limit)`, `NevoAliasRepository`, nutrient repo for detail payloads
- Produces:
  - `GET /api/nevo/foods/search?q={q}&limit={limit}` → `NevoFoodSearchResponse`
  - `NevoFoodSearchResponse(String query, List<Item> items)`
  - `Item(String nevoCode, String nameEn, String nameNl, String foodGroup, String synonym, List<NevoMatchResponse.NevoNutrientDto> nutrients)`

**Ranking rules (implement exactly):**

1. Expand `q` into search terms: trimmed original + alias expansions (`paprika` → also `sweet pepper`).
2. Union `searchByTerm` results across terms (cap candidates, e.g. 50).
3. Score each candidate:
   - +3.0 if `food_group` is `Vegetables` or `Groente`
   - +2.0 if EN/NL/synonym **starts with** query or expanded term (case-insensitive)
   - +1.0 if EN/NL/synonym contains term as a whole word
   - +0.5 if name contains `raw` / `rauw` and query has no preparation word
   - −2.5 if food_group is `Herbs and spices` / `Kruiden en specerijen` **and** query is a single token that is a known vegetable alias (`paprika`, `courgette`, …)
   - −2.0 if food_group is `Savoury snacks` / `Hartige snacks en zoutjes` under the same single-token vegetable-alias condition
4. Sort by score desc, then `food_name_en` asc; return `limit` (default 10).
5. Include nutrients (same mapping as `byCode`) so clients can show macros without a second call.

- [ ] **Step 1: Extend sample CSV** with at least:
  - Sweet pepper green raw / Paprika groene rauw (Vegetables, code 31)
  - Sweet pepper red raw / Paprika rode rauw (884)
  - Courgettes raw / Courgette rauw (922)
  - Aubergine/eggplant raw (10)
  - Paprika powder / Paprikapoeder (1229, Herbs and spices)

Copy real columns from `NEVO2025_v9.0.csv` (pipe format matching sample header) — enough nutrient columns for existing importer.

- [ ] **Step 2: Write failing tests**

```java
@Test
void searchPaprikaReturnsSweetPeppersBeforePowder() throws Exception {
  mockMvc.perform(get("/api/nevo/foods/search").param("q", "paprika").param("limit", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].foodGroup").value("Vegetables"))
      .andExpect(jsonPath("$.items[0].nameEn", containsString("Sweet pepper")))
      .andExpect(jsonPath("$.items[*].nevoCode", hasItem("31")));
}

@Test
void searchCourgetteAndAubergineReturnVegetables() throws Exception {
  mockMvc.perform(get("/api/nevo/foods/search").param("q", "courgette"))
      .andExpect(jsonPath("$.items[0].nameEn", containsStringIgnoringCase("Courgette")));
  mockMvc.perform(get("/api/nevo/foods/search").param("q", "aubergine"))
      .andExpect(jsonPath("$.items[0].nameEn", containsStringIgnoringCase("Aubergine")));
}

@Test
void sweetPepperEnglishResolvesSameFoods() throws Exception {
  mockMvc.perform(get("/api/nevo/foods/search").param("q", "sweet pepper"))
      .andExpect(jsonPath("$.items[*].nevoCode", hasItem("31")));
}
```

Also unit-test `expandSearchTerms("paprika")` contains `sweet pepper`.

- [ ] **Step 3: Run tests — expect FAIL** (endpoint missing)

```bash
cd services/nevo-service && mvn -q test -Dtest=NevoFoodSearchServiceTest,NevoServiceIntegrationTest,ProductNameNormalizerTest
```

- [ ] **Step 4: Implement search service + controller + alias expand**

Controller sketch:

```java
@GetMapping("/foods/search")
public NevoFoodSearchResponse search(
    @RequestParam("q") String q,
    @RequestParam(value = "limit", defaultValue = "10") int limit) {
  return searchService.search(q, Math.min(Math.max(limit, 1), 25));
}
```

Keep path as `/api/nevo/foods/search` (not under `/foods/{nevoCode}`) — register **before** or use a distinct mapping so `{nevoCode}` does not capture `search`. Prefer explicit `@GetMapping("/foods/search")` alongside `@GetMapping("/foods/{nevoCode}")` (Spring matches literal `search`).

- [ ] **Step 5: Run tests — expect PASS**

```bash
cd services/nevo-service && mvn -q test
```

- [ ] **Step 6: Commit + push**

```bash
git add services/nevo-service
git commit -m "feat(nevo): public food search with vegetable colloquial ranking"
git push -u origin HEAD
```

---

### Task 3: Merge NEVO hits into product search (food-catalog-service)

**Files:**
- Modify: `…/nevo/NevoClient.java`, `RestNevoClient.java`
- Create: catalog-side DTO mirrors for NEVO search response (package `com.nutritrack.food.nevo`)
- Modify: `…/web/dto/ProductResponse.java` — add `String nevoCode`, `String foodGroup` (nullable; existing constructors/call sites updated)
- Modify: `…/service/ProductMapper.java` — set `nevoCode=null`, `foodGroup=null` for OFF/submission
- Modify: `…/service/ProductSearchService.java`
- Modify: `…/config/FoodProperties.java` — optional `nevoSearchLimit` default 8
- Modify: `ProductControllerTest.java` (+ dedicated unit test if cleaner)

**Interfaces:**
- Consumes: `NevoClient.searchFoods(String q, int limit)` → list of NEVO items (empty on failure)
- Produces: `ProductSearchResponse` where NEVO items appear **first**, then submissions, then local OFF, then OFF fallback fill

**Merge rules:**

1. Call NEVO search with raw query (NEVO owns alias expansion). On `NevoUnavailableException` / any error → log and continue (degrade like OFF fallback).
2. Map each NEVO item to `ProductResponse`:
   - `id`: `UUID.nameUUIDFromBytes(("nevo:" + nevoCode).getBytes(StandardCharsets.UTF_8))` (stable, not persisted)
   - `submissionId`: null
   - `barcode`: null
   - `source`: `"NEVO"`
   - `name`: prefer `nameEn` (include NL in UI via foodGroup/brand field if useful — e.g. brand = foodGroup or `"NEVO · Vegetables"`)
   - `nevoCode`, `foodGroup` set
   - `nutrients`: map NEVO nutrient DTOs → `ProductNutrientResponse`
   - other fields null/empty
3. Insert NEVO mapped items at the **front** of the merged list (LinkedHashMap keyed by `"nevo:" + code`).
4. Then existing own-submissions → local mirror → OFF fallback.
5. Truncate to `pageSize`.
6. Do **not** change OFF upsert/enrichment paths.

- [ ] **Step 1: Write failing MockMvc test**

```java
@Test
void searchPaprikaPrependsNevoVegetables() throws Exception {
  when(nevoClient.searchFoods(eq("paprika"), anyInt()))
      .thenReturn(List.of(nevoItem("31", "Sweet pepper green raw", "Vegetables")));
  when(offClient.searchByName(anyString(), eq(1))).thenReturn(List.of());

  mockMvc.perform(get("/api/products/search").param("q", "paprika").with(asUser()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].source").value("NEVO"))
      .andExpect(jsonPath("$.items[0].nevoCode").value("31"))
      .andExpect(jsonPath("$.items[0].name").value("Sweet pepper green raw"));
}

@Test
void searchStillReturnsLocalOffProducts() throws Exception {
  // existing oat milk test remains green; nevoClient.searchFoods returns empty by default
}
```

Wire `@MockitoBean NevoClient` already present in `ProductControllerTest` — stub `searchFoods` default `List.of()` in `@BeforeEach` if needed.

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd services/food-catalog-service && mvn -q test -Dtest=ProductControllerTest
```

- [ ] **Step 3: Implement client + merge**

`RestNevoClient.searchFoods`:

```java
return restClient.get()
    .uri(uriBuilder -> uriBuilder.path("/api/nevo/foods/search")
        .queryParam("q", q).queryParam("limit", limit).build())
    .retrieve()
    .body(NevoFoodSearchResponse.class)
    .items();
```

Note: existing match uses `/internal/...` + API key. Public search uses `/api/nevo/...` (already permitAll in nevo SecurityConfig). Prefer calling public search from catalog over inventing a second internal endpoint unless auth policy changes.

- [ ] **Step 4: Run full catalog tests — expect PASS**

```bash
cd services/food-catalog-service && mvn -q test
```

- [ ] **Step 5: Commit + push**

```bash
git add services/food-catalog-service
git commit -m "feat(food-catalog): merge NEVO reference foods into product search"
git push -u origin HEAD
```

---

### Task 4: Frontend Lookup + NEVO detail

**Files:**
- Modify: `frontend/src/api/client.ts` — extend `Product` with `nevoCode?: string | null`, `foodGroup?: string | null`; add `fetchNevoFood(nevoCode)`
- Modify: `frontend/src/pages/LookupPage.tsx` — label NEVO rows (`NEVO · {foodGroup}`); link:
  - if `item.source === 'NEVO' && item.nevoCode` → `/nevo/{nevoCode}` (preserve meal query param pattern used today)
  - else existing `productPathWithMeal(item.id, mealType)`
- Create: `frontend/src/pages/NevoFoodPage.tsx` (or extend ProductPage) — load `GET /api/nevo/foods/{code}`, show name, group, nutrients, attribution “NEVO-online 2025/9.0, RIVM”
- Modify: router (`frontend/src/App.tsx` or equivalent)
- Tests: `LookupPage.test.tsx`, small Nevo page test; update `diaryRef` only if needed (NEVO should **not** pretend to be productId for diary yet — hide or disable “Add to diary” with short note, or omit button)

**Diary UX for this card:** Show nutrients and make it clear this is a reference food. If Add-to-diary button would call diary with fake UUID, **disable it** and note “Diary logging for NEVO foods tracked separately”. Do not create broken diary entries.

- [ ] **Step 1: Failing Vitest** — search results with `source: 'NEVO'` render link to `/nevo/31` and show Vegetables label

- [ ] **Step 2: Implement UI + client**

- [ ] **Step 3: Run frontend tests**

```bash
cd frontend && npm test -- --run
```

- [ ] **Step 4: Commit + push**

```bash
git add frontend
git commit -m "feat(frontend): show NEVO vegetable hits in lookup search"
git push -u origin HEAD
```

---

### Task 5: Docs, acceptance verification, follow-up card note

**Files:**
- Modify: `AI/nevo-micronutrient-estimates.md` — short “User lookup search” section pointing at this plan
- Modify: this file’s checklist status as you complete tasks
- Optional: Linear comment with verification evidence

- [ ] **Step 1: Document follow-up (diary materialization)**

Add under “Remaining / follow-up” in `AI/nevo-micronutrient-estimates.md`:

- Materialize selected NEVO food as a catalog `product` (`source=NEVO`, `sourceRef=nevoCode`) on Add to diary, **or** extend diary to accept `nevoCode` — separate Linear card.

- [ ] **Step 2: Automated verification**

```bash
cd services/nevo-service && mvn -q test
cd services/food-catalog-service && mvn -q test
cd frontend && npm test -- --run
```

- [ ] **Step 3: Manual / API verification (local compose or Railway with AUTH_MODE=dev)**

1. Dev login → JWT
2. `GET /api/products/search?q=paprika` → first items include NEVO sweet peppers (`nevoCode` 31/884/…), not only chips
3. `q=courgette`, `q=aubergine`, `q=sweet pepper`, `q=bell pepper` → vegetable hits
4. Packaged query still works (e.g. existing oat milk / brand product)
5. UI: Look up → Search → `paprika` → vegetable rows visible on first page

- [ ] **Step 4: Final commit + push**

```bash
git add AI
git commit -m "docs: ULT-8 NEVO colloquial search notes and follow-up"
git push -u origin HEAD
```

---

## Acceptance criteria mapping

| Criterion | Task |
| --- | --- |
| `paprika` returns sweet/bell pepper NEVO entries on first page | Tasks 2–4 |
| ≥2 additional colloquial veg queries (`courgette`, `aubergine`) | Tasks 1–2 |
| Not dominated by spice/snack for plain single-word query | Task 2 ranking + Task 3 prepend |
| Dutch + English resolve to same NEVO foods | Task 1 aliases + Task 2 expansion |
| OFF packaged search unchanged | Task 3 merge-only; regression tests |
| NEVO code + nutrients exposed | Tasks 2–4; diary = follow-up doc |
| Automated tests for alias + paprika | Tasks 1–3 |

## Out of scope (do not implement in this card)

- Full i18n / bilingual UI framework
- Editing/re-importing NEVO CSV
- Broad alias dictionary
- USDA FDC generic-food search
- Diary entry creation from NEVO (follow-up)

## Agent execution notes

1. Start from repo root; create/use the Linear-suggested branch or the automation branch you were given — do not push to `main`.
2. Prefer TDD order inside each task.
3. If Codebase Memory MCP is available, search before broad Grep.
4. After substantial Java edits, run the service’s `mvn test` before claiming done.
5. When complete: move Linear ULT-8 to **In Review** (or Done per team practice) and paste verification notes.

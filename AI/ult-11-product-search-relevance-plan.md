# ULT-11 — Product name search relevance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Linear:** [ULT-11](https://linear.app/ultimateconcept/issue/ULT-11/improve-product-name-search-relevance-ranking-fuzzy-matching-and-token)
>
> **Do not** implement ULT-8 (NEVO / colloquial vegetable aliases) in this card.

**Goal:** Make `GET /api/products/search` return relevance-ranked packaged-product hits for multi-token queries and single-character typos, using PostgreSQL FTS + trigram in production while keeping H2 unit tests green.

**Architecture:** Split search into (1) query normalization + tiny synonym expansion, (2) dialect-aware candidate retrieval (Postgres FTS/trgm vs H2 tokenized `LIKE`), (3) pure-Java relevance scoring used for local, OFF, and duplicate-warning ranking, (4) merge that pins the caller's own submissions then sorts catalog/OFF by score. Existing OFF fallback threshold and barcode lookup stay unchanged.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Data JPA, Flyway (`db/migration` + `db/migration-postgresql`), H2 `MODE=PostgreSQL` tests, JUnit 5 + AssertJ + MockMvc, React/Vitest unchanged.

## Global Constraints

- Standalone Maven per service; Boot **4.1.0**, Java **21**.
- Tests use H2 (`jdbc:h2:mem:…;MODE=PostgreSQL`) and Flyway **`classpath:db/migration` only** — never rely on Postgres-only SQL in default `@SpringBootTest` paths.
- Production Flyway loads `classpath:db/migration,classpath:db/migration-postgresql` (see `application.yml`).
- Existing GIN FTS index: `V4__product_fts_gin.sql` (`idx_product_fts` on `to_tsvector('english', search_document)`).
- OFF fallback gate remains `merged.size() < localMinResultsBeforeOffFallback` (default **5**) — do not change the gate semantics.
- Barcode lookup path (`ProductLookupService`) must stay behaviorally unchanged.
- Out of scope: NEVO/ULT-8, Elasticsearch, OFF re-import, Lookup UI redesign, search analytics, full i18n.
- Prefer Consistency over novelty: extend `ProductSearchService` / `ProductRepository` / `FoodProperties.Search`; do not introduce a new microservice.
- After substantial code changes, update this file's status notes and `docs/calorie-tracker-architecture.md` §5.3 if wording is still inaccurate.
- Commit and push frequently on the feature branch; run `mvn test` in `services/food-catalog-service` before claiming done. Frontend Vitest should remain green (no FE changes expected).

## Design decisions (locked)

### Chosen approach: hybrid retrieval + Java ranking

| Layer | Production (PostgreSQL) | Tests (H2) |
|-------|-------------------------|------------|
| Candidate retrieval | `websearch_to_tsquery` + existing GIN; if hit count &lt; `fuzzyMinResults`, also `pg_trgm` `%` / `similarity` | Tokenized AND of `LIKE '%token%'` on `search_document`; if thin, Java Levenshtein/trigram-ish scan over a broader candidate set |
| Ranking | `ProductRelevanceScorer` (Java) | Same scorer |
| Synonyms | Tiny in-code map before retrieval/scoring | Same |

**Why not FTS-only ranking (`ts_rank` alone)?** Acceptance needs exact-name / prefix / brand-only tiers and OFF re-rank on already-materialized `Product` rows. One Java scorer covers local + OFF + duplicate warnings and is fully unit-testable on H2.

**Why not Elasticsearch?** Explicitly out of scope.

**Why not “score everything in SQL only”?** Hard to share with OFF upserted entities and H2 tests; would also fight the unused-GIN / Phase-4 H2-safe `search_document` pattern already in the repo.

### Ranking tiers (higher score wins; ties → name ASC for stability)

1. Exact normalized name match
2. Name prefix match
3. All query tokens present in `search_document` (order-independent)
4. Partial token coverage (majority / weighted)
5. Brand-only / weak document match
6. Fuzzy-only match (trigram / edit distance), lowest among matches

Own pending/rejected submissions stay **pinned at the front** of the response (current inclusion behaviour). Catalog + OFF hits are relevance-sorted after that.

### Synonym map (product-search only — not NEVO)

Minimal curated expansions applied during normalization (bidirectional):

- `milk` ↔ `drink` (so `oat milk` can match “Oat Drink - Barista”)
- `yogurt` ↔ `yoghurt`

Do **not** add vegetable colloquialisms here (ULT-8).

### OFF merge

When OFF fallback runs: upsert as today, then **re-score the combined catalog/OFF list** and sort by relevance before truncating to `pageSize`. Silent catch on OFF failure remains.

---

## File map

| Area | Path | Responsibility |
|------|------|----------------|
| Plan / AI notes | `AI/ult-11-product-search-relevance-plan.md` | This plan + completion notes |
| Config | `…/config/FoodProperties.java` | New search knobs |
| Config test | `…/config/FoodPropertiesBindingTest.java` | Defaults for new fields |
| Normalize | **Create** `…/service/search/SearchQueryNormalizer.java` | Trim, lower, punct, synonyms, tokens |
| Score | **Create** `…/service/search/ProductRelevanceScorer.java` | Tiered relevance `double` |
| Fuzzy util | **Create** `…/service/search/EditDistance.java` | Levenshtein for H2 fuzzy + scorer |
| Dialect retrieve | **Create** `…/service/search/ProductCandidateSearcher.java` (+ Postgres/H2 impls or one class with branch) | Candidate `List<Product>` |
| Repository | `…/domain/ProductRepository.java` | FTS/trgm native queries + tokenized JPQL; update `findNameOrBrandMatches` |
| Orchestration | `…/service/ProductSearchService.java` | Wire normalize → retrieve → score → OFF → re-rank |
| Duplicate warnings | `…/service/SubmissionService.java` | Reuse scorer / improved name match |
| PG migration | **Create** `…/db/migration-postgresql/V5__product_trgm.sql` | `pg_trgm` + GIN trgm index |
| Entity (only if needed) | `…/domain/Product.java` | Keep `refreshSearchDocument()`; no schema change required for synonyms |
| Unit tests | **Create** `…/service/search/*Test.java`, extend `ProductControllerTest` / add `ProductSearchServiceTest` | Ranking, tokens, typo, alpha regression |
| Docs | `docs/calorie-tracker-architecture.md` §5.3 | State that FTS+trgm+Java rank are used |
| Frontend | none | Display-only; order comes from API |

---

### Task 1: Config + query normalizer + edit distance (TDD)

**Files:**
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/config/FoodProperties.java`
- Modify: `services/food-catalog-service/src/test/java/com/nutritrack/food/config/FoodPropertiesBindingTest.java`
- Create: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/search/SearchQueryNormalizer.java`
- Create: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/search/EditDistance.java`
- Create: `services/food-catalog-service/src/test/java/com/nutritrack/food/service/search/SearchQueryNormalizerTest.java`
- Create: `services/food-catalog-service/src/test/java/com/nutritrack/food/service/search/EditDistanceTest.java`

**Interfaces:**
- Produces:
  - `FoodProperties.Search` gains `fuzzyMinResults` (default `3`) and `similarityThreshold` (default `0.35`)
  - `record NormalizedQuery(String raw, String normalized, List<String> tokens, Set<String> expandedTokens)`
  - `SearchQueryNormalizer.normalize(String raw) → NormalizedQuery`
  - `EditDistance.levenshtein(String a, String b) → int`
  - `EditDistance.normalizedSimilarity(String a, String b) → double` in `[0,1]` (`1 - distance/maxLen`)

- [x] **Step 1: Write failing tests for normalizer + edit distance**

```java
@Test
void collapsesWhitespaceLowercasesAndStripsPunctuation() {
  var n = new SearchQueryNormalizer().normalize("  Oat,  Milk!! ");
  assertThat(n.normalized()).isEqualTo("oat milk");
  assertThat(n.tokens()).containsExactly("oat", "milk");
}

@Test
void expandsMilkDrinkSynonymsIntoExpandedTokens() {
  var n = new SearchQueryNormalizer().normalize("oat milk");
  assertThat(n.expandedTokens()).contains("oat", "milk", "drink");
}

@Test
void levenshteinNutelaNutellaIsOne() {
  assertThat(EditDistance.levenshtein("nutela", "nutella")).isEqualTo(1);
}
```

- [x] **Step 2: Run tests — expect FAIL (classes missing)**

```bash
cd services/food-catalog-service && mvn -q test -Dtest=SearchQueryNormalizerTest,EditDistanceTest
```

- [x] **Step 3: Implement minimal normalizer, edit distance, and Search config fields**

`FoodProperties.Search` should become:

```java
public record Search(
    @DefaultValue("20") int pageSize,
    @DefaultValue("5") int localMinResultsBeforeOffFallback,
    @DefaultValue("3") int fuzzyMinResults,
    @DefaultValue("0.35") double similarityThreshold) {}
```

Normalizer rules:
- null → empty; trim; lowercase `Locale.ROOT`
- replace punctuation / hyphens with space; collapse whitespace
- tokens = split on spaces, drop blanks, drop tokens length &lt; 1
- expandedTokens = tokens ∪ synonym expansions (milk↔drink, yogurt↔yoghurt)
- reject nothing here — `ProductSearchService` still enforces min length **2** on raw trim

- [x] **Step 4: Extend `FoodPropertiesBindingTest` for new defaults**

```java
assertThat(properties.search().fuzzyMinResults()).isEqualTo(3);
assertThat(properties.search().similarityThreshold()).isEqualTo(0.35);
```

- [x] **Step 5: Run tests — expect PASS; commit**

```bash
cd services/food-catalog-service && mvn -q test -Dtest=SearchQueryNormalizerTest,EditDistanceTest,FoodPropertiesBindingTest
git add services/food-catalog-service/src/main/java/com/nutritrack/food/config/FoodProperties.java \
  services/food-catalog-service/src/main/java/com/nutritrack/food/service/search \
  services/food-catalog-service/src/test/java/com/nutritrack/food/config/FoodPropertiesBindingTest.java \
  services/food-catalog-service/src/test/java/com/nutritrack/food/service/search
git commit -m "feat(food-catalog): add search query normalizer and fuzzy config (ULT-11)"
git push -u origin HEAD
```

---

### Task 2: Relevance scorer (TDD)

**Files:**
- Create: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/search/ProductRelevanceScorer.java`
- Create: `services/food-catalog-service/src/test/java/com/nutritrack/food/service/search/ProductRelevanceScorerTest.java`

**Interfaces:**
- Consumes: `NormalizedQuery`, product name/brand/`searchDocument` (or a small view interface)
- Produces: `double score(NormalizedQuery query, String name, String brand, String searchDocument)` — higher is better; non-matches return `0`

Suggested score bands (implement exactly so tests are stable):

| Band | Condition | Score |
|------|-----------|-------|
| Exact name | `normalize(name).equals(query.normalized())` | `1000` |
| Prefix name | normalized name starts with query.normalized() or first token | `800` |
| All tokens | every `query.tokens()` appears in `searchDocument` (after synonym expand of document OR query.expandedTokens used for matching) | `600 + tokenHits` |
| Partial tokens | ≥1 token hit | `300 + 50 * hitCount` |
| Brand contains | brand contains a token | `200` |
| Fuzzy | best `EditDistance.normalizedSimilarity` of any query token vs any document token ≥ threshold | `100 * similarity` |
| None | else | `0` |

Matching for “all tokens” on `oat milk` vs document `oat drink barista ferrero…`: treat a token as hit if the document contains the token **or any of its synonym siblings**.

- [x] **Step 1: Write failing ranking tests**

```java
@Test
void nutellaBeatsAlphabeticalDistractor() {
  var q = normalizer.normalize("nutella");
  double nutella = scorer.score(q, "Nutella", "Ferrero", "nutella  ferrero");
  double aaron = scorer.score(q, "Aaron's Nuts", "Acme", "aaron's nuts  acme");
  assertThat(nutella).isGreaterThan(aaron);
}

@Test
void oatMilkMatchesOatDrinkViaSynonym() {
  var q = normalizer.normalize("oat milk");
  assertThat(scorer.score(q, "Oat Drink - Barista", "Oatly", "oat drink - barista oatly"))
      .isGreaterThan(0);
}

@Test
void nutelaFuzzyScoresNutella() {
  var q = normalizer.normalize("nutela");
  assertThat(scorer.score(q, "Nutella", "Ferrero", "nutella ferrero")).isGreaterThan(0);
}
```

- [x] **Step 2: Run — expect FAIL; implement scorer; run PASS; commit**

```bash
cd services/food-catalog-service && mvn -q test -Dtest=ProductRelevanceScorerTest
git add services/food-catalog-service/src/main/java/com/nutritrack/food/service/search/ProductRelevanceScorer.java \
  services/food-catalog-service/src/test/java/com/nutritrack/food/service/search/ProductRelevanceScorerTest.java
git commit -m "feat(food-catalog): add product relevance scorer (ULT-11)"
git push -u origin HEAD
```

---

### Task 3: PostgreSQL trigram migration + repository candidate queries

**Files:**
- Create: `services/food-catalog-service/src/main/resources/db/migration-postgresql/V5__product_trgm.sql`
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/domain/ProductRepository.java`

**Interfaces:**
- Produces repository methods (names may vary slightly but keep these responsibilities):

```java
// H2-safe JPQL: every token must appear (caller passes tokens; for multi-token use 1–3 overloads or a custom Fragment / EntityManager)
Page<Product> searchByDocumentTokens(@Param("tokens") /* see below */, Pageable pageable);

@Query(value = """
    SELECT p.* FROM product p
    WHERE to_tsvector('english', coalesce(p.search_document, ''))
          @@ websearch_to_tsquery('english', :q)
    ORDER BY ts_rank(
        to_tsvector('english', coalesce(p.search_document, '')),
        websearch_to_tsquery('english', :q)
    ) DESC, p.name ASC
    """, nativeQuery = true)
List<Product> searchByFts(@Param("q") String q, Pageable pageable);

@Query(value = """
    SELECT p.* FROM product p
    WHERE coalesce(p.search_document, '') % :q
       OR similarity(coalesce(p.search_document, ''), :q) >= :threshold
    ORDER BY similarity(coalesce(p.search_document, ''), :q) DESC, p.name ASC
    """, nativeQuery = true)
List<Product> searchByTrigram(@Param("q") String q, @Param("threshold") double threshold, Pageable pageable);
```

**H2 multi-token JPQL note:** Spring Data cannot easily bind `AND` for a dynamic token list in one annotation. Prefer one of:
1. Fixed max tokens (e.g. up to 4) with optional null tokens skipped via `( :t1 IS NULL OR LOWER(doc) LIKE … )`, or
2. `EntityManager` / `JdbcTemplate` in `ProductCandidateSearcher`.

Recommended: implement retrieval in `ProductCandidateSearcher` using `EntityManager` so token count is dynamic and Postgres native SQL is explicit.

`V5__product_trgm.sql`:

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_product_trgm
    ON product
    USING GIN (search_document gin_trgm_ops);
```

Railway/Postgres: extension create may require privileges — if deploy fails, document fallback (`similarity` without index still works; ask for `CREATE EXTENSION`). Prefer `IF NOT EXISTS`.

- [x] **Step 1: Add V5 migration file**
- [x] **Step 2: Implement `ProductCandidateSearcher`**

```java
public interface ProductCandidateSearcher {
  List<Product> findCandidates(NormalizedQuery query, int limit);
}
```

Behaviour:
- If JDBC URL / dialect is PostgreSQL: call FTS with `query.normalized()` (and/or expanded websearch string). If `results.size() < fuzzyMinResults`, also run trigram with `query.normalized()` and `similarityThreshold`, union by product id.
- If H2: for each product matching tokenized AND-LIKE on `expandedTokens` **OR** (for fuzzy path when thin) LIKE on first token / longest token, load up to `limit * 5`, then filter with scorer &gt; 0 / similarity ≥ threshold in Java.
- Always return a de-duplicated list; **do not** final-sort here beyond DB order — scorer owns final order.

Detect Postgres via `EntityManager` unwrap / `DatabaseMetaData.getDatabaseProductName()` or `environment` property — avoid hard-coding profiles only.

- [x] **Step 3: Keep deprecated `searchByDocument(String, Pageable)` delegating to tokenized path for any remaining callers, or replace call sites and delete LIKE-alphabetical query.**

Replace:

```java
WHERE LOWER(COALESCE(p.searchDocument, '')) LIKE LOWER(CONCAT('%', :q, '%'))
ORDER BY p.name ASC
```

so nothing production remains alphabetically sorted by name as the primary order.

- [x] **Step 4: Unit-test candidate searcher with H2 Spring test or pure EntityManager test; commit**

```bash
cd services/food-catalog-service && mvn -q test
git add services/food-catalog-service/src/main/resources/db/migration-postgresql/V5__product_trgm.sql \
  services/food-catalog-service/src/main/java/com/nutritrack/food/domain/ProductRepository.java \
  services/food-catalog-service/src/main/java/com/nutritrack/food/service/search/ProductCandidateSearcher.java \
  services/food-catalog-service/src/test/java/com/nutritrack/food/service/search/
git commit -m "feat(food-catalog): FTS/trgm and tokenized candidate retrieval (ULT-11)"
git push -u origin HEAD
```

---

### Task 4: Wire `ProductSearchService` — score, OFF re-rank, pin submissions

**Files:**
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/ProductSearchService.java`
- Modify or create: `services/food-catalog-service/src/test/java/com/nutritrack/food/ProductControllerTest.java` and/or `ProductSearchServiceTest.java`

**Target orchestration (pseudocode):**

```java
NormalizedQuery nq = normalizer.normalize(rawQuery);
if (nq.normalized().length() < 2) throw new IllegalArgumentException(...);

Map<String, Scored> scored = new LinkedHashMap<>();

// 1) Own submissions — pin, preserve inclusion
for (submission : searchOwn(...)) {
  scored.put("sub:"+id, new Scored(mapper.toResponse(submission), Double.POSITIVE_INFINITY));
}

// 2) Local candidates
List<Product> local = candidateSearcher.findCandidates(nq, pageSize * 3);
for (Product p : local) {
  double s = scorer.score(nq, p.getName(), p.getBrand(), p.getSearchDocument());
  if (s > 0) scored.merge(... keep max score ...);
}

// 3) OFF fallback gate — count only non-submission entries
long catalogHits = scored.keySet().stream().filter(k -> k.startsWith("prod:")).count();
if (catalogHits < properties.search().localMinResultsBeforeOffFallback()) {
  try {
    for (off : offClient.searchByName(nq.normalized(), page)) {
      Product saved = upsertService.upsertFromOff(off);
      double s = scorer.score(nq, saved.getName(), saved.getBrand(), saved.getSearchDocument());
      // merge by prod:id keeping max score
    }
  } catch (RuntimeException ignored) {}
}

// 4) Sort: submissions (Infinity) first in insertion order among themselves,
//    then others by score DESC, name ASC; truncate to pageSize
```

**Pagination note:** Current code applies `PageRequest` to local SQL then merges. After relevance ranking, prefer: fetch a larger candidate window (`pageSize * 3` or page offset × pageSize), score, then apply page slice on the ranked list. For page&gt;1 without OFF, slice the ranked local list. Keep request `page` semantics (`1`-based as today).

- [x] **Step 1: Write failing integration tests in `ProductControllerTest` (H2)**

```java
@Test
void searchRanksNutellaAboveAlphabeticalDistractor() throws Exception {
  save("Aaron's Apple Sauce", "Acme", "aarons");
  save("Nutella", "Ferrero", "3017620422003");
  when(offClient.searchByName(anyString(), eq(1))).thenReturn(List.of());

  mockMvc.perform(get("/api/products/search").param("q", "nutella").with(asUser()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].name").value("Nutella"));
}

@Test
void searchOatMilkFindsOatDrink() throws Exception {
  save("Oat Drink - Barista", "Oatly", "oatly1");
  when(offClient.searchByName(anyString(), eq(1))).thenReturn(List.of());

  mockMvc.perform(get("/api/products/search").param("q", "oat milk").with(asUser()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].name").value("Oat Drink - Barista"));
}

@Test
void searchTypoNutelaFindsNutella() throws Exception {
  save("Nutella", "Ferrero", "3017620422003");
  when(offClient.searchByName(anyString(), eq(1))).thenReturn(List.of());

  mockMvc.perform(get("/api/products/search").param("q", "nutela").with(asUser()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items[0].name").value("Nutella"));
}

@Test
void searchDoesNotCallOffWhenEnoughLocalHits() throws Exception {
  // insert >= localMinResultsBeforeOffFallback products matching "milk"
  mockMvc.perform(get("/api/products/search").param("q", "milk").with(asUser()))
      .andExpect(status().isOk());
  verify(offClient, never()).searchByName(anyString(), anyInt());
}
```

- [x] **Step 2: Implement service wiring; keep existing `searchReturnsLocalProducts` green**
- [x] **Step 3: Optional unit test that OFF hits are re-scored above weak local** (mock OffClient returning a strong name; weak local distractor already in DB)
- [x] **Step 4: `mvn test`; commit; push**

```bash
cd services/food-catalog-service && mvn -q test
git commit -am "feat(food-catalog): relevance-ranked product search with OFF re-rank (ULT-11)"
git push -u origin HEAD
```

---

### Task 5: Duplicate-warning search reuse

**Files:**
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/SubmissionService.java`
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/domain/ProductRepository.java` (`findNameOrBrandMatches`)
- Modify: existing submission MockMvc tests in `ProductControllerTest` if assertions depend on warning text

**Behaviour:**
- Replace naive name/brand `LIKE` used only for warnings with: normalize needle → `candidateSearcher.findCandidates` (limit 5) **or** score `findNameOrBrandMatches` results and keep top 5 with score &gt; 0.
- Warnings text format can stay: `Similar catalog product: "…" / brand`.
- Do not change approve/reject flows.

- [x] **Step 1: Adjust `duplicateWarnings` to use normalizer + scorer**
- [x] **Step 2: Add a focused test: submitting name `nutela` warns about existing Nutella when force is false / warnings returned**
- [x] **Step 3: `mvn test`; commit; push**

```bash
git commit -am "feat(food-catalog): reuse relevance matching for submission duplicate warnings (ULT-11)"
git push -u origin HEAD
```

---

### Task 6: Docs + acceptance checklist + final verification

**Files:**
- Modify: `docs/calorie-tracker-architecture.md` (§5.3 name search bullet — state FTS GIN is used, trigram fallback, Java relevance rank)
- Modify: `AI/ult-11-product-search-relevance-plan.md` (mark tasks done; add “Completed” section)
- Modify: `AI/phase-4-mirror-search-submissions.md` (one-line pointer that ranking upgraded in ULT-11)
- Modify: `AI/food-lookup-500.md` only if search error paths change (likely no)

- [x] **Step 1: Update architecture §5.3** so it no longer implies FTS is implemented while code used LIKE
- [x] **Step 2: Full test suites**

```bash
cd services/food-catalog-service && mvn test
cd ../../frontend && npm test -- --run
```

Expected: all green.

- [x] **Step 3: Manual / API verification notes (for implementer)**

With gateway + Dev Login (`AUTH_MODE=dev`):

1. `GET /api/products/search?q=nutella` → Nutella-branded in top 3 when present in mirror
2. `q=oat milk` → oat drink/milk products on first page
3. `q=nutela` → Nutella on first page
4. `q=greek yogurt` / `whole milk` → token order does not block matches
5. Confirm OFF not called when ≥5 strong local hits (logs or mock in test already)

- [x] **Step 4: Final commit + PR**

```bash
git add docs/calorie-tracker-architecture.md AI/
git commit -m "docs: record ULT-11 product search relevance behaviour"
git push -u origin HEAD
```

Open/update PR referencing ULT-11 and this plan path.

---

## Acceptance criteria mapping

| Criterion | Task |
|-----------|------|
| `nutella` → Nutella in top 3 | Task 2 + 4 |
| `oat milk` → oat drink/milk on first page | Task 1 synonyms + Task 2/3/4 |
| `nutela` typo → Nutella on first page | Task 1/2 fuzzy + Task 3 H2/Postgres fuzzy path + Task 4 |
| Ordered by relevance not name ASC | Task 2 + 4 (regression test) |
| Multi-word token order independent | Task 3 tokenized/FTS + Task 2 |
| OFF gate unchanged at ≥5 local | Task 4 test `never()` |
| OFF merge re-ranked | Task 4 |
| Barcode + submission inclusion unchanged | Task 4 pin submissions; do not touch lookup |
| Automated tests for exact/multi/typo/alpha | Tasks 2–4 |
| `mvn test` + Vitest green | Task 6 |

## Out of scope (remind implementer)

- ULT-8 NEVO colloquial foods
- Rebuilding OFF mirror
- UI filters / redesign
- Changing Resilience4j OFF rate limits
- Storing `tsvector` column (generated expression index in V4 is enough)

## Risks / watchouts

1. **`CREATE EXTENSION pg_trgm`** may fail on managed Postgres without permission — catch in deploy notes; ranking still works via FTS + Java fuzzy if trigram migration is skipped temporarily (document in PR).
2. **H2 ≠ Postgres** — never put `websearch_to_tsquery` in the default test Flyway path; always branch in Java.
3. **Page &gt; 1** — scoring a window then slicing is approximate; acceptable for this card; do not invent cursor search.
4. **Synonym bleed** — keep the map tiny; do not expand to food generics.
5. **Performance** — rely on GIN FTS + trgm indexes; avoid `SELECT * FROM product` full scans in Postgres path.

## Completed (fill in when done)

- Date: 2026-08-09
- PR: https://github.com/Alexpen97/calorieTracker/pull/62
- Notes: Hybrid retrieval (Postgres FTS GIN + pg_trgm fuzzy fallback; H2 tokenized LIKE + Java fuzzy) with `ProductRelevanceScorer` ranking for local and OFF hits. Own submissions pinned; OFF gate unchanged at `localMinResultsBeforeOffFallback` (5); OFF merge re-ranked. `mvn test` and frontend Vitest green.

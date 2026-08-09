# ULT-7 Frequent Products + Quick-Add Card — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Linear:** [ULT-7](https://linear.app/ultimateconcept/issue/ULT-7/track-frequently-added-products-quick-add-card-in-add-food-view)  
> **Spec source of truth:** the Linear issue description (acceptance criteria AC1–AC16). This plan is the handoff for implementation; do not re-plan unless the code contradicts the ticket.

**Goal:** Expose each user’s frequently logged products (with usual portion) from diary-service, and show a one-tap **Quick add** card at the top of `/lookup`.

**Architecture:** Aggregation stays in `diary-service` over existing `diary_entry` rows (no new table). Gateway already proxies `/api/diary/**`. Frontend adds `fetchFrequentProducts` + a `FrequentFoodsCard` on `LookupPage`, reusing `createDiaryEntry` and navigating to `/today` like `ProductPage`.

**Tech Stack:** Spring Boot 4.1 / Java 21 (diary-service, H2+Flyway tests), React 19 + TanStack Query + Vitest (frontend), existing JWT auth via gateway.

## Global Constraints

- Prefer gateway base `https://gateway-production-777b.up.railway.app` for live API checks; Dev Login `code: "dev:agent-debug"` when `AUTH_MODE=dev`.
- Use Codebase Memory (`project: "D-repos-calorieTracker"`) before Grep; Graphify for docs/architecture only.
- Use Context7 before writing third-party API-facing code (Spring Data `@Query`, TanStack Query, Vitest, React Router).
- Match existing diary controller/service/test patterns; do not invent new auth or gateway routes.
- Out of scope: favorites, “recently logged today” on `/today`, recommendation service FR-13, localStorage tracking, changing ProductPage default 100 g.
- When `?meal=` is absent, use frequent item’s `lastMealType` — **not** ULT-6 time-of-day defaults (that ticket is complementary, not required here).
- Update `AI/` notes when done; commit frequently; push the working branch; leave AC checkboxes on Linear for the implementer to mark after verification.
- Write tests for every new behavior; run diary-service tests, frontend tests, and frontend build before claiming done.

---

## File map (create / modify)

| Path | Role |
| --- | --- |
| `services/diary-service/.../domain/DiaryEntryRepository.java` | Native/JPQL frequent aggregation query |
| `services/diary-service/.../service/DiaryEntryService.java` (or new `FrequentProductService.java`) | Validate params, call repo, map rows |
| `services/diary-service/.../web/DiaryController.java` | `GET /api/diary/frequent` + response record |
| `services/diary-service/.../DiaryControllerTest.java` | Integration tests for AC1–AC6 |
| `frontend/src/api/client.ts` | `FrequentProduct` + `fetchFrequentProducts` |
| `frontend/src/food/FrequentFoodsCard.tsx` | **Create** — Quick add UI |
| `frontend/src/food/FrequentFoodsCard.test.tsx` | **Create** — card unit tests (optional if covered fully in LookupPage tests; prefer both thin card + page) |
| `frontend/src/pages/LookupPage.tsx` | Mount card above method nav |
| `frontend/src/pages/LookupPage.test.tsx` | AC7–AC13 |
| `frontend/src/index.css` | Minimal list-row styles if existing diary list classes are insufficient |
| `AI/ult-7-frequent-products-quick-add.md` | **Create** after implementation — short completion notes |
| Gateway / Flyway | **No changes** expected |

---

## Decided behaviors (lock these in code comments)

1. **Query params:** Defaults `limit=8`, `weeks=8`. Cap `limit` at **20**, `weeks` at **52**. If `limit` or `weeks` is missing → use defaults. If present but `< 1` or above cap → **400** via `IllegalArgumentException` (existing `ApiExceptionHandler`). Document this in the controller/service javadoc.
2. **Empty list:** Return `[]` (200), never 404.
3. **Grouping key:** `COALESCE(product_id, submission_id)` — rows with both set still group by `product_id` (COALESCE picks product). Ticket accepts separate keys when logged under different id types.
4. **Display name / brand / lastMealType / lastConsumedAt:** Take from the entry with max `consumed_at` in the group.
5. **usualWeightG:** `ROUND(AVG(weight_g))` as integer grams.
6. **Min count:** `HAVING COUNT(*) >= 2`.
7. **Frontend empty:** **Hide** the card entirely when fetch succeeds with `[]` (no placeholder foods). Loading: short skeleton/text inside card. Error: hide card **or** one-line “Couldn’t load quick add” with retry — do not block barcode/search.
8. **Secondary action:** Product name / chevron links to `/products/:id?meal=…` when `productId` is set; if only `submissionId`, link to the same product route the app already uses for pending submissions (`/products/:submissionId` if that is how ProductPage resolves pending — verify against existing search result links before shipping; if unresolved, omit secondary link for submission-only rows in v1).
9. **Primary action:** Dedicated **+** / “Add” control (and optionally whole-row activate) calls `createDiaryEntry`, then `navigate('/today')`.

---

### Task 1: Backend — repository aggregation query

**Files:**
- Modify: `services/diary-service/src/main/java/com/nutritrack/diary/domain/DiaryEntryRepository.java`
- Modify: `services/diary-service/src/main/java/com/nutritrack/diary/service/DiaryEntryService.java` (preferred: add `listFrequent` here to avoid new DI wiring) **or** Create: `.../service/FrequentProductService.java` if you want a thinner entry service — either is fine; inject into controller consistently.
- Test: extend `DiaryControllerTest` in Task 3 (write failing controller tests first per TDD once endpoint exists; for this task write a focused repository/service test **or** go straight to MockMvc tests in Task 3 — prefer **Task 3 first as red tests**, then implement Tasks 1–2).

**Interfaces:**
- Produces: repository method returning projection rows the service can map.

**Recommended approach:** Prefer a **native SQL** `@Query` (H2 `MODE=PostgreSQL` in tests) with an interface projection or `Object[]` mapping. Example SQL shape:

```sql
SELECT
  product_id,
  submission_id,
  product_name,
  brand,
  log_count,
  usual_weight_g,
  last_meal_type,
  last_consumed_at
FROM (
  SELECT
    CASE WHEN product_id IS NOT NULL THEN product_id ELSE NULL END AS product_id,
    CASE WHEN product_id IS NULL THEN submission_id ELSE NULL END AS submission_id,
    -- snapshot from latest row: use DISTINCT ON (Postgres) OR join back to max(consumed_at)
    COUNT(*) AS log_count,
    ROUND(AVG(weight_g)) AS usual_weight_g,
    MAX(consumed_at) AS last_consumed_at
  FROM diary_entry
  WHERE user_id = :userId
    AND consumed_at >= :from
    AND COALESCE(product_id, submission_id) IS NOT NULL
  GROUP BY COALESCE(product_id, submission_id)
  HAVING COUNT(*) >= 2
) agg
-- join to pick product_name, brand, meal_type from the row matching last_consumed_at
ORDER BY log_count DESC, last_consumed_at DESC
LIMIT :limit
```

**H2-friendly alternative (recommended for this repo’s tests):** Load entries with existing:

```java
List<DiaryEntry> findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(
    UUID userId, Instant from, Instant to);
```

Then aggregate **in Java** inside the service (group by `COALESCE(productId, submissionId)`, filter count ≥ 2, sort, limit). This avoids Postgres-only SQL vs H2 mismatches and reuses the existing index filter (`user_id`, `consumed_at`). For ≤8 weeks of personal diary rows this is fine.

**Chosen default for this plan: in-memory aggregation in the service after the existing range query.** Document why in a one-line comment. If the implementing agent prefers native SQL and proves it works on H2+Postgres, that is also acceptable.

- [ ] **Step 1: Add service method signature**

```java
@Transactional(readOnly = true)
public List<FrequentProduct> listFrequent(UUID userId, int limit, int weeks) {
  // validate limit/weeks; compute Instant from = Instant.now().minus(weeks, ChronoUnit.WEEKS);
  // Instant to = Instant.now().plusSeconds(1); // or now
  // List<DiaryEntry> entries = repository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtDesc(userId, from, to);
  // group / filter / sort / map
}
```

Define a simple immutable type (record) either in the service package or as the controller response record used end-to-end:

```java
public record FrequentProduct(
    UUID productId,
    UUID submissionId,
    String productName,
    String brand,
    long logCount,
    int usualWeightG,
    MealType lastMealType,
    Instant lastConsumedAt) {}
```

- [ ] **Step 2: Implement grouping**

For each entry, key = `productId != null ? productId : submissionId` (skip if both null — should not happen). Accumulate count, sum of weights, and track the entry with latest `consumedAt` for name/brand/meal. After grouping: drop groups with `count < 2`; sort by `logCount` desc, then `lastConsumedAt` desc; `usualWeightG = (int) Math.round(sum / count)`; take first `limit` items. For identity fields: if group key came from productId, set `productId` and `submissionId=null`; else `submissionId` and `productId=null`.

- [ ] **Step 3: Commit**

```bash
git add services/diary-service/src/main/java/com/nutritrack/diary/
git commit -m "feat(diary): add frequent-product aggregation service"
```

---

### Task 2: Backend — controller endpoint

**Files:**
- Modify: `services/diary-service/src/main/java/com/nutritrack/diary/web/DiaryController.java`
- Confirm: `SecurityConfig` already authenticates `/api/diary/**` — no change.

**Interfaces:**
- Consumes: `DiaryEntryService.listFrequent(userId, limit, weeks)`
- Produces: `GET /api/diary/frequent` JSON array

- [ ] **Step 1: Wire constructor** if a new service bean was introduced; otherwise call `entryService.listFrequent`.

- [ ] **Step 2: Add handler** (place near other GETs):

```java
@GetMapping("/api/diary/frequent")
public List<FrequentProductResponse> frequentProducts(
    @AuthenticationPrincipal Jwt jwt,
    @RequestParam(required = false) Integer limit,
    @RequestParam(required = false) Integer weeks) {
  int resolvedLimit = limit == null ? 8 : limit;
  int resolvedWeeks = weeks == null ? 8 : weeks;
  return entryService.listFrequent(
          UUID.fromString(jwt.getSubject()), resolvedLimit, resolvedWeeks)
      .stream()
      .map(FrequentProductResponse::from)
      .toList();
}
```

Add nested response record matching camelCase JSON from the ticket:

```java
public record FrequentProductResponse(
    UUID productId,
    UUID submissionId,
    String productName,
    String brand,
    long logCount,
    int usualWeightG,
    MealType lastMealType,
    Instant lastConsumedAt) {
  static FrequentProductResponse from(FrequentProduct p) { ... }
}
```

Validation of limit/weeks belongs in the service (throw `IllegalArgumentException` with a clear message).

- [ ] **Step 3: Commit**

```bash
git add services/diary-service/src/main/java/com/nutritrack/diary/web/DiaryController.java
git commit -m "feat(diary): expose GET /api/diary/frequent"
```

---

### Task 3: Backend — integration tests (AC1–AC6)

**Files:**
- Modify: `services/diary-service/src/test/java/com/nutritrack/diary/DiaryControllerTest.java`

**Pattern:** Same class annotations (`@SpringBootTest`, H2, `@MockitoBean FoodCatalogClient` / `JwtDecoder`). Reuse `createEntry`, `jwtForUser`, `product(...)`, `when(foodCatalogClient.getProduct(...))`.

**TDD note:** If following strict TDD, write these tests **before** Tasks 1–2 and confirm they fail with 404, then implement.

- [ ] **Step 1: Write tests**

Cover at least:

1. **Returns ranked frequent products** — same user, product A logged 3× (weights 100, 200, 150 → usual 150), product B logged 2×; product C once → omitted. Assert order by logCount, fields `productId`, `productName`, `brand`, `usualWeightG`, `lastMealType`, `lastConsumedAt`.
2. **Empty when none qualify** — zero or single logs → `[]` and 200.
3. **User isolation** — user B’s logs never appear for user A.
4. **Submission-only rows** — create entries via submission path if the test helper supports it; otherwise insert via repository / extend helper to POST with `submissionId` and mock catalog submission fetch the same way production create does. Assert `submissionId` set, `productId` null.
5. **limit honored** — seed > limit qualifying products; `?limit=2` returns 2.
6. **Invalid params** — `?limit=0` or `?weeks=-1` → 400.
7. **Defaults** — omit params; still 200 (with or without data).

Example assertion sketch:

```java
mockMvc
    .perform(
        get("/api/diary/frequent")
            .param("limit", "8")
            .param("weeks", "8")
            .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(owner)))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.length()").value(2))
    .andExpect(jsonPath("$[0].productId").value(productA.toString()))
    .andExpect(jsonPath("$[0].usualWeightG").value(150))
    .andExpect(jsonPath("$[0].logCount").value(3));
```

Use `Instant.now()`-relative `consumedAt` values inside the 8-week window so the filter does not drop seeded rows.

- [ ] **Step 2: Run diary-service tests**

```bash
cd services/diary-service && mvn test
```

Expected: PASS (all existing + new).

- [ ] **Step 3: Commit**

```bash
git add services/diary-service/src/test/java/com/nutritrack/diary/DiaryControllerTest.java
git commit -m "test(diary): cover frequent products endpoint"
```

---

### Task 4: Frontend — API client

**Files:**
- Modify: `frontend/src/api/client.ts` (near diary helpers ~line 449+)

**Interfaces:**
- Produces:

```ts
export type FrequentProduct = {
  productId: string | null
  submissionId: string | null
  productName: string
  brand: string | null
  logCount: number
  usualWeightG: number
  lastMealType: MealType
  lastConsumedAt: string
}

export async function fetchFrequentProducts(options?: {
  limit?: number
  weeks?: number
}): Promise<FrequentProduct[]> {
  const params = new URLSearchParams()
  if (options?.limit != null) params.set('limit', String(options.limit))
  if (options?.weeks != null) params.set('weeks', String(options.weeks))
  const qs = params.toString()
  const response = await authenticatedFetch(
    `${apiBase}/api/diary/frequent${qs ? `?${qs}` : ''}`,
  )
  return parseJson<FrequentProduct[]>(response)
}
```

- [ ] **Step 1: Add type + function** matching backend JSON field names exactly.
- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/client.ts
git commit -m "feat(frontend): add fetchFrequentProducts client"
```

---

### Task 5: Frontend — `FrequentFoodsCard` + LookupPage integration (AC7–AC12)

**Files:**
- Create: `frontend/src/food/FrequentFoodsCard.tsx`
- Modify: `frontend/src/pages/LookupPage.tsx`
- Modify: `frontend/src/index.css` (only if needed)
- Optionally Create: `frontend/src/food/FrequentFoodsCard.test.tsx`

**Interfaces:**
- Consumes: `fetchFrequentProducts`, `createDiaryEntry`, `parseMealTypeParam` / `productPathWithMeal`, `MealType`, `DashboardCard`
- Props sketch:

```tsx
type FrequentFoodsCardProps = {
  mealFromUrl: MealType | null
}
```

- [ ] **Step 1: Implement card**

Use `useQuery` for fetch (`queryKey: ['diary', 'frequent']`, `retry: false` or 1). Use `useMutation` for add → `onSuccess: () => navigate('/today')`.

Resolve meal:

```ts
function resolveMeal(item: FrequentProduct, mealFromUrl: MealType | null): MealType {
  return mealFromUrl ?? item.lastMealType
}
```

UI structure:

```tsx
<DashboardCard density="list" title="Quick add">
  {isLoading ? <p>Loading…</p> : null}
  {isError ? <p>…retry…</p> : null}
  {!isLoading && !isError && items.length === 0 ? null : (
    <ul className="frequent-food-list">
      {items.map((item) => (
        <li key={item.productId ?? item.submissionId ?? item.productName}>
          {/* name link optional */}
          <button
            type="button"
            aria-label={`Add ${item.productName}, ${item.usualWeightG} grams`}
            onClick={() => add.mutate(...)}
          >
            +
          </button>
          <span>{item.productName}</span>
          {item.brand ? <span>{item.brand}</span> : null}
          <span>{item.usualWeightG} g</span>
        </li>
      ))}
    </ul>
  )}
</DashboardCard>
```

When `items.length === 0` and not loading/error: **return null** from the component (hide card).

Mutation payload:

```ts
{
  productId: item.productId ?? undefined,
  submissionId: item.submissionId ?? undefined,
  weightG: item.usualWeightG,
  mealType: resolveMeal(item, mealFromUrl),
}
```

Pass exactly one id (omit nulls). Keyboard: button is focusable by default.

- [ ] **Step 2: Mount in LookupPage**

Insert **after** the intro `<p>`, **before** `<nav className="lookup-method-nav" …>`:

```tsx
<FrequentFoodsCard mealFromUrl={mealType} />
```

Wrap LookupPage in QueryClient if tests need it — production app already has a provider at the root; verify `main.tsx` / router tree. LookupPage tests must wrap with `QueryClientProvider` once the card uses React Query.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/food/FrequentFoodsCard.tsx frontend/src/pages/LookupPage.tsx frontend/src/index.css
git commit -m "feat(frontend): Quick add card on lookup page"
```

---

### Task 6: Frontend tests (AC13) + verification (AC14–AC16)

**Files:**
- Modify: `frontend/src/pages/LookupPage.test.tsx`
- Optionally: `frontend/src/food/FrequentFoodsCard.test.tsx`

- [ ] **Step 1: Extend LookupPage tests**

Patterns from `ProductPage.test.tsx`: `vi.spyOn(client, ...)`, `QueryClientProvider`, `MemoryRouter` with `initialEntries`.

Cases:

1. Renders **Quick add** heading and product row when `fetchFrequentProducts` resolves with ≥1 item.
2. Hides card when `[]`.
3. Click add → `createDiaryEntry` called with `{ productId, weightG: usualWeightG, mealType: lastMealType }` when no `?meal=`.
4. With `initialEntries={['/lookup?meal=DINNER']}`, meal in payload is `DINNER`.
5. Fetch rejection does not remove barcode/search nav (card hidden or retry only).
6. Accessible name includes product + portion (`getByRole('button', { name: /Add Greek yogurt, 150 grams/i })`).

Mock defaults in `beforeEach` so existing barcode/search tests still pass (`fetchFrequentProducts` → `[]`).

- [ ] **Step 2: Run frontend tests + build**

```bash
cd frontend && npm test -- --run
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 3: diary-service tests again**

```bash
cd services/diary-service && mvn test
```

- [ ] **Step 4: Optional live smoke (if AUTH_MODE=dev)**

```bash
# Dev login → GET /api/diary/frequent with Bearer token via gateway
```

AC14: confirm path works without gateway YAML changes (already `Path=/api/diary/**`).

- [ ] **Step 5: Write completion notes**

Create `AI/ult-7-frequent-products-quick-add.md` summarizing what shipped, param validation choice, and any v1 limitations (e.g. submission-only secondary link).

- [ ] **Step 6: Commit + push**

```bash
git add frontend/src/pages/LookupPage.test.tsx AI/ult-7-frequent-products-quick-add.md
git commit -m "test(frontend): cover Quick add frequent foods card"
git push -u origin HEAD
```

- [ ] **Step 7: Update Linear ULT-7** — check off ACs that are done; move status to **In Progress** while implementing and **In Review** / **Done** per team process after PR.

---

## Acceptance criteria traceability

| AC | Task |
| --- | --- |
| AC1 Endpoint | 2 |
| AC2 Eligibility ≥2 | 1, 3 |
| AC3 usualWeightG mean | 1, 3 |
| AC4 Identity product vs submission | 1, 3 |
| AC5 limit/weeks | 1–3 |
| AC6 Backend tests | 3 |
| AC7 Quick add card | 5 |
| AC8 One-tap createDiaryEntry | 5–6 |
| AC9 Meal URL override | 5–6 |
| AC10 Loading/errors | 5–6 |
| AC11 Empty hidden | 5–6 |
| AC12 A11y | 5–6 |
| AC13 Frontend tests | 6 |
| AC14 Gateway | verify only (no code) |
| AC15 No regression | 3, 6 |
| AC16 Verification commands | 6 |

---

## Self-review (planner)

1. **Spec coverage:** All AC1–AC16 mapped; out-of-scope items explicitly excluded.
2. **Placeholders:** No TBD steps; SQL-vs-Java aggregation decided (Java in-service default).
3. **Type consistency:** `FrequentProduct` / `FrequentProductResponse` / TS `FrequentProduct` field names aligned with ticket JSON.
4. **Risks:** H2 vs native SQL → mitigated by Java aggregation; submission create in tests may need catalog mock for pending products — follow existing create-entry test patterns in `DiaryControllerTest`.

---

## Execution handoff

Plan saved to `AI/ult-7-frequent-products-quick-add-plan.md`.

**Next agent:** implement Tasks 1→6 in order (or TDD: Task 3 failing tests → Tasks 1–2 → green → frontend 4–6). Use `superpowers:subagent-driven-development` or `superpowers:executing-plans`. Do not expand scope into ULT-6 time-based meal defaults or favorites.

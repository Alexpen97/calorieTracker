# ULT-6 Time-Based Meal Default Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a product page is opened without a valid `?meal=` query param, initialize the "Add to today" meal select from the user's local browser time while preserving explicit meal params and user edits.

**Architecture:** Keep the inference as a pure diary helper beside existing meal URL helpers, then use it only in the `ProductPage` state initializer. This keeps the backend payload shape unchanged, preserves lookup/diary explicit meal flow, and avoids re-inferring after initial render.

**Tech Stack:** Frontend TypeScript, React 19 `useState` lazy initializer, React Router, TanStack Query, Vitest 3 fake timers, Testing Library.

## Global Constraints

- Frontend-only change; no backend API or payload changes.
- Explicit valid `?meal=BREAKFAST|LUNCH|DINNER|SNACK` always wins over time inference.
- Inference uses the local browser `Date` clock; no server timezone, profile timezone, or `consumedAt` change.
- Time windows use inclusive start and exclusive end by hour:
  - Breakfast: 05:00-10:59
  - Lunch: 11:00-15:59
  - Dinner: 16:00-21:59
  - Snack: 22:00-04:59
- The meal `<select>` remains controlled and editable; submission uses the selected value.
- Existing diary meal `+` links and lookup-to-product meal passthrough remain unchanged.
- Before implementation, try Codebase Memory with project `D-repos-calorieTracker`; if unavailable, continue with targeted reads of the files listed below.
- Context7 notes from planning: Vitest v3.2.4 documents `vi.useFakeTimers()`, `vi.setSystemTime(date)`, and `vi.useRealTimers()` for date-dependent tests; React docs confirm `useState(() => initialValue)` is called during initialization and ignored after initial render.

---

## File Structure

- Modify `frontend/src/diary/formatDay.ts`
  - Add `inferMealTypeFromLocalTime(date = new Date()): MealType`.
  - Keep it pure, synchronous, and dependent only on `date.getHours()`.
- Modify `frontend/src/diary/formatDay.test.ts`
  - Add focused boundary coverage for the helper.
  - Keep existing parse/path tests for query-param regressions.
- Modify `frontend/src/pages/ProductPage.tsx`
  - Import the new helper.
  - Replace the hardcoded `'BREAKFAST'` fallback with `inferMealTypeFromLocalTime()`.
- Modify `frontend/src/pages/ProductPage.test.tsx`
  - Add tests for time-based default, explicit param override, invalid param fallback, and user override submission.
  - Extend the test render helper to accept an initial route.
- Verify existing `frontend/src/pages/DiaryPage.test.tsx`
  - Existing assertions already cover diary meal `+` links to `/lookup?meal=...`.
- Optional docs update after implementation: append a short "ULT-6 follow-up" note to `AI/diary-meal-category-add-buttons.md` summarizing the new time-based fallback.

## Task 1: Add the Pure Meal Inference Helper

**Files:**
- Modify: `frontend/src/diary/formatDay.ts`
- Test: `frontend/src/diary/formatDay.test.ts`

**Interfaces:**
- Consumes: existing `MealType` union in `formatDay.ts`.
- Produces: `inferMealTypeFromLocalTime(date?: Date): MealType`.

- [ ] **Step 1: Write failing helper tests**

In `frontend/src/diary/formatDay.test.ts`, update the import list to include `inferMealTypeFromLocalTime`:

```ts
import {
  analyticsRangeFromEnd,
  formatAnalyticsRangeLabel,
  formatDiaryDayLabel,
  formatLocalDate,
  getMacroProgress,
  groupEntriesByMeal,
  inferMealTypeFromLocalTime,
  mealLookupPath,
  parseMealTypeParam,
  productPathWithMeal,
  shiftAnalyticsRangeEnd,
  shiftLocalDate,
  waterProgress,
  type DiaryEntryForDisplay,
  type NutrientTotalForDisplay,
} from './formatDay'
```

Add this test after the existing "parses meal query params and builds meal-aware paths" case:

```ts
  it('infers the default meal from local clock hour boundaries', () => {
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 0, 0))).toBe('SNACK')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 4, 59))).toBe('SNACK')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 5, 0))).toBe('BREAKFAST')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 10, 59))).toBe('BREAKFAST')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 11, 0))).toBe('LUNCH')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 15, 59))).toBe('LUNCH')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 16, 0))).toBe('DINNER')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 21, 59))).toBe('DINNER')
    expect(inferMealTypeFromLocalTime(new Date(2026, 6, 22, 22, 0))).toBe('SNACK')
  })
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```bash
cd frontend
npm test -- src/diary/formatDay.test.ts
```

Expected: FAIL because `inferMealTypeFromLocalTime` is not exported.

- [ ] **Step 3: Implement the helper**

In `frontend/src/diary/formatDay.ts`, add this immediately after `parseMealTypeParam`:

```ts
export function inferMealTypeFromLocalTime(date = new Date()): MealType {
  const hour = date.getHours()

  if (hour >= 5 && hour < 11) {
    return 'BREAKFAST'
  }
  if (hour >= 11 && hour < 16) {
    return 'LUNCH'
  }
  if (hour >= 16 && hour < 22) {
    return 'DINNER'
  }
  return 'SNACK'
}
```

- [ ] **Step 4: Run the focused helper test and verify it passes**

Run:

```bash
cd frontend
npm test -- src/diary/formatDay.test.ts
```

Expected: PASS for all `formatDay.test.ts` cases.

- [ ] **Step 5: Commit Task 1**

```bash
git add frontend/src/diary/formatDay.ts frontend/src/diary/formatDay.test.ts
git commit -m "feat: infer default meal from local time"
git push -u origin cursor/ult-6-ai-agent-plan-cde2
```

## Task 2: Use the Helper in ProductPage and Cover UI Behavior

**Files:**
- Modify: `frontend/src/pages/ProductPage.tsx`
- Test: `frontend/src/pages/ProductPage.test.tsx`

**Interfaces:**
- Consumes: `inferMealTypeFromLocalTime(date?: Date): MealType`.
- Produces: Product page initial `mealType` state is `selectedMeal ?? inferMealTypeFromLocalTime()`.

- [ ] **Step 1: Add ProductPage test utilities**

In `frontend/src/pages/ProductPage.test.tsx`, update imports:

```ts
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProductPage from './ProductPage'
import * as client from '../api/client'
```

Replace the current `renderProduct` helper with:

```ts
function renderProduct(initialEntry = '/products/prod-1') {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/products/:id" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
```

Add this helper below `renderProduct`:

```ts
function mockProduct(id = 'prod-1') {
  return {
    id,
    submissionId: null,
    barcode: '3017620422003',
    source: 'OFF' as const,
    name: 'Nutella',
    brand: 'Ferrero',
    quantityLabel: '400 g',
    servingSizeG: 15,
    imageUrl: null,
    nutriScore: 'E',
    ingredientsText: null,
    allergenTags: [],
    offLastSyncedAt: null,
    nutrients: [
      { code: 'energy_kcal', amountPer100g: 539, unit: 'kcal', estimated: false },
    ],
  }
}
```

Update the existing estimated nutrient tests to use `mockProduct()` where it reduces duplication, but keep their current assertions unchanged.

- [ ] **Step 2: Add failing default/override tests**

Add a new `describe` block below the existing one:

```ts
describe('ProductPage meal selection', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('defaults the meal select from local browser time when no meal param exists', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1')

    expect(await screen.findByLabelText('Meal')).toHaveValue('LUNCH')
  })

  it('uses an explicit valid meal param instead of local time inference', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1?meal=DINNER')

    expect(await screen.findByLabelText('Meal')).toHaveValue('DINNER')
  })

  it('falls back to local time when the meal param is invalid', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 23, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())

    renderProduct('/products/prod-1?meal=brunch')

    expect(await screen.findByLabelText('Meal')).toHaveValue('SNACK')
  })

  it('submits the user-selected meal override', async () => {
    vi.setSystemTime(new Date(2026, 6, 22, 12, 0))
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct())
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'entry-1',
      productId: 'prod-1',
      submissionId: null,
      productName: 'Nutella',
      brand: 'Ferrero',
      weightG: 100,
      mealType: 'SNACK',
      consumedAt: '2026-07-22T12:00:00Z',
      createdAt: '2026-07-22T12:00:00Z',
      nutrients: [],
    })

    renderProduct('/products/prod-1')

    fireEvent.change(await screen.findByLabelText('Meal'), { target: { value: 'SNACK' } })
    fireEvent.click(screen.getByRole('button', { name: 'Add to diary' }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        productId: 'prod-1',
        weightG: 100,
        mealType: 'SNACK',
      })
    })
  })
})
```

- [ ] **Step 3: Run the ProductPage tests and verify new cases fail**

Run:

```bash
cd frontend
npm test -- src/pages/ProductPage.test.tsx
```

Expected before implementation: at least the no-param and invalid-param tests FAIL because the select still defaults to `BREAKFAST`.

- [ ] **Step 4: Implement ProductPage fallback**

In `frontend/src/pages/ProductPage.tsx`, update the diary helper import:

```ts
import { inferMealTypeFromLocalTime, mealLookupPath, parseMealTypeParam } from '../diary/formatDay'
```

Replace the current meal state initializer:

```ts
const [mealType, setMealType] = useState<MealType>(() => selectedMeal ?? 'BREAKFAST')
```

with:

```ts
const [mealType, setMealType] = useState<MealType>(() => selectedMeal ?? inferMealTypeFromLocalTime())
```

- [ ] **Step 5: Run ProductPage tests and verify they pass**

Run:

```bash
cd frontend
npm test -- src/pages/ProductPage.test.tsx
```

Expected: PASS for existing nutrient tests and new meal selection tests.

- [ ] **Step 6: Commit Task 2**

```bash
git add frontend/src/pages/ProductPage.tsx frontend/src/pages/ProductPage.test.tsx
git commit -m "feat: default product meal by local time"
git push -u origin cursor/ult-6-ai-agent-plan-cde2
```

## Task 3: Regression Verification and Handoff Notes

**Files:**
- Verify: `frontend/src/pages/DiaryPage.test.tsx`
- Verify: `frontend/src/pages/LookupPage.tsx`
- Optional modify: `AI/diary-meal-category-add-buttons.md`

**Interfaces:**
- Consumes: existing `mealLookupPath` and `productPathWithMeal` helpers.
- Produces: confidence that explicit meal context remains preserved through diary `+` -> lookup -> product.

- [ ] **Step 1: Run diary regression tests**

Run:

```bash
cd frontend
npm test -- src/pages/DiaryPage.test.tsx
```

Expected: PASS, including assertions that meal add links have these hrefs:

```ts
expect(screen.getByRole('link', { name: 'Add food to Breakfast' })).toHaveAttribute(
  'href',
  '/lookup?meal=BREAKFAST',
)
expect(screen.getByRole('link', { name: 'Add food to Lunch' })).toHaveAttribute(
  'href',
  '/lookup?meal=LUNCH',
)
expect(screen.getByRole('link', { name: 'Add food to Dinner' })).toHaveAttribute(
  'href',
  '/lookup?meal=DINNER',
)
expect(screen.getByRole('link', { name: 'Add food to Snacks' })).toHaveAttribute(
  'href',
  '/lookup?meal=SNACK',
)
```

- [ ] **Step 2: Run the full frontend test suite**

Run:

```bash
cd frontend
npm test
```

Expected: PASS.

- [ ] **Step 3: Run the frontend build**

Run:

```bash
cd frontend
npm run build
```

Expected: TypeScript and Vite build complete successfully.

- [ ] **Step 4: Optional docs note**

If the implementation changed behavior exactly as planned, append this to `AI/diary-meal-category-add-buttons.md`:

```md

## ULT-6 follow-up: time-based fallback

When `ProductPage` receives a valid `?meal=BREAKFAST|LUNCH|DINNER|SNACK`, that explicit meal still initializes the "Add to today" select. When no valid meal param exists, the select now initializes from local browser time via `inferMealTypeFromLocalTime()` in `frontend/src/diary/formatDay.ts`.
```

- [ ] **Step 5: Commit verification/docs if docs were changed**

If Step 4 modified docs:

```bash
git add AI/diary-meal-category-add-buttons.md
git commit -m "docs: note time-based meal fallback"
git push -u origin cursor/ult-6-ai-agent-plan-cde2
```

- [ ] **Step 6: Final implementation handoff**

In the final implementation response, include:

```md
- Added `inferMealTypeFromLocalTime()` with boundary tests for midnight, 05:00, 11:00, 16:00, and 22:00.
- Updated ProductPage so explicit `?meal=` still wins and missing/invalid meal params use local time.
- Verified user meal overrides submit the selected value and diary `+` links still preserve explicit meal context.
- Tests run:
  - `cd frontend && npm test -- src/diary/formatDay.test.ts`
  - `cd frontend && npm test -- src/pages/ProductPage.test.tsx`
  - `cd frontend && npm test -- src/pages/DiaryPage.test.tsx`
  - `cd frontend && npm test`
  - `cd frontend && npm run build`
```

## Self-Review

- Spec coverage:
  - AC1 covered by helper tests and ProductPage no-param/invalid-param tests.
  - AC2 covered by ProductPage explicit `?meal=DINNER` test and unchanged query helpers.
  - AC3 covered by ProductPage submit-after-select-change test.
  - AC4 covered by limiting implementation to frontend state initialization and preserving `createDiaryEntry` input shape.
  - AC5 covered by `formatDay.test.ts` boundary tests including midnight and all hour transitions.
  - AC6 covered by existing `DiaryPage.test.tsx` link assertions plus unchanged `LookupPage.tsx` passthrough through `productPathWithMeal`.
- Placeholder scan: no unresolved implementation placeholders remain in this plan.
- Type consistency: `MealType`, `inferMealTypeFromLocalTime`, `parseMealTypeParam`, `mealLookupPath`, and `productPathWithMeal` names match the current source files.

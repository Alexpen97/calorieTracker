# ULT-10 — Count-based (pieces) diary entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Linear:** [ULT-10](https://linear.app/ultimateconcept/issue/ULT-10/support-count-based-entry-pieces-instead-of-grams-where-possible)
>
> **AI notes:** `AI/ult-10-count-based-entry.md`

**Goal:** On the product “Add to today” form, when `servingSizeG > 0`, let users enter amount in **pieces** and convert to `weightG` before `createDiaryEntry`, without changing the diary API.

**Architecture:** Frontend-only v1. Add a pure helper module under `frontend/src/food/` for eligibility + conversion. Extend `ProductPage` with a Grams/Pieces unit control. Diary continues to store and display grams; `PortionMath` on the server is untouched.

**Tech Stack:** React 19, TypeScript, TanStack Query, Vitest, Testing Library (`fireEvent` — same as existing page tests). No new dependencies.

## Global Constraints

- **No diary API / schema changes** — `POST /api/diary/entries` still receives `{ productId|submissionId, weightG, mealType }`.
- **Reuse `Product.servingSizeG`** as `gramsPerPiece` in the UI; do **not** add a dedicated API field in v1.
- **Pieces are positive integers only** (no half-pieces).
- **Default unit remains grams** (`"100"`) even when piece mode is available — piece mode is opt-in via toggle (OFF `serving_size` is often a spread/serving, not a countable piece).
- On unit switch, **reset amount to that mode’s default** (`grams` → `"100"`, `pieces` → `"1"`). Do not try to preserve/convert the previous value in v1.
- When `servingSizeG` is `null` or `≤ 0`, UI must match today’s grams-only form (no toggle).
- Match existing patterns: string state for inputs, `Number(...)` parse on submit, `fireEvent` in tests, error `<p className="error">`.
- Do **not** parse `quantityLabel`; do **not** change diary list display to show “pcs”.
- Document the OFF serving-size limitation in the AI note (already planned); do not hide it in UI copy beyond the helper “≈ X g” text.

## Resolved planning decisions

| # | Decision | Choice |
|---|----------|--------|
| 1 | Amount when switching Grams ↔ Pieces | **Reset** to mode default (`100` / `1`) |
| 2 | Piece precision | **Positive integers only** |
| 3 | gramsPerPiece source | **Reuse `servingSizeG`** (no API rename) |
| 4 | Initial unit when eligible | **Grams** (opt-in Pieces) — safer given OFF serving semantics |

## File map

| File | Action |
|------|--------|
| `frontend/src/food/pieceEntry.ts` | **Create** — `canLogByPieces`, `piecesToGrams`, `formatPieceGramHint` |
| `frontend/src/food/pieceEntry.test.ts` | **Create** — unit tests for helpers |
| `frontend/src/pages/ProductPage.tsx` | **Modify** — unit toggle, labels, validation, submit conversion |
| `frontend/src/pages/ProductPage.test.tsx` | **Modify** — toggle visibility, piece submit payload, grams-only product |
| `AI/ult-10-count-based-entry.md` | **Update** — mark implementation status when done |
| Backend / diary-service / OFF normalizer | **No changes** |

## Acceptance criteria → tasks

| AC | Covered by |
|----|------------|
| AC1 Piece mode when data exists | Task 2 |
| AC2 Grams-only fallback | Task 2 |
| AC3 Piece input validation | Task 2 |
| AC4 Correct conversion | Task 1 + Task 2 |
| AC5 User can override unit | Task 2 |
| AC6 Helper text | Task 1 + Task 2 |
| AC7 Nutrient totals unchanged | Implicit (same `weightG`); no diary-service change |
| AC8 No diary API change | Global constraint |
| AC9 Tests | Task 1 + Task 2 |
| AC10 Regression | Task 2 (grams-only + pending submission path unchanged) |

---

### Task 1: Pure piece-entry helpers

**Files:**
- Create: `frontend/src/food/pieceEntry.ts`
- Create: `frontend/src/food/pieceEntry.test.ts`

**Interfaces:**
- Produces:
  - `canLogByPieces(servingSizeG: number | null | undefined): boolean`
  - `piecesToGrams(pieceCount: number, gramsPerPiece: number): number`
  - `formatPieceGramHint(pieceCount: number, gramsPerPiece: number): string`

- [ ] **Step 1: Write the failing unit tests**

```ts
import { describe, expect, it } from 'vitest'
import {
  canLogByPieces,
  formatPieceGramHint,
  piecesToGrams,
} from './pieceEntry'

describe('canLogByPieces', () => {
  it('is true only for positive finite servingSizeG', () => {
    expect(canLogByPieces(15)).toBe(true)
    expect(canLogByPieces(0.5)).toBe(true)
    expect(canLogByPieces(null)).toBe(false)
    expect(canLogByPieces(undefined)).toBe(false)
    expect(canLogByPieces(0)).toBe(false)
    expect(canLogByPieces(-10)).toBe(false)
    expect(canLogByPieces(Number.NaN)).toBe(false)
  })
})

describe('piecesToGrams', () => {
  it('multiplies piece count by grams per piece', () => {
    expect(piecesToGrams(2, 50)).toBe(100)
    expect(piecesToGrams(1, 15)).toBe(15)
    expect(piecesToGrams(3, 12.5)).toBe(37.5)
  })

  it('rounds weightG to 2 decimal places via Math.round(n * 100) / 100', () => {
    expect(piecesToGrams(1, 33.333)).toBe(33.33)
    // Prefer a stable binary case; if float noise appears, use piecesToGrams(2, 12.345) → 24.69
    expect(piecesToGrams(2, 12.345)).toBe(24.69)
  })
})

describe('formatPieceGramHint', () => {
  it('formats singular and plural helper copy', () => {
    expect(formatPieceGramHint(1, 15)).toBe('1 piece ≈ 15 g')
    expect(formatPieceGramHint(2, 50)).toBe('2 pieces ≈ 100 g')
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd frontend && npm test -- src/food/pieceEntry.test.ts
```

Expected: FAIL (module / exports missing).

- [ ] **Step 3: Implement helpers**

```ts
// frontend/src/food/pieceEntry.ts

export function canLogByPieces(servingSizeG: number | null | undefined): boolean {
  return typeof servingSizeG === 'number' && Number.isFinite(servingSizeG) && servingSizeG > 0
}

/** Convert piece count to diary weightG (2 decimal places). */
export function piecesToGrams(pieceCount: number, gramsPerPiece: number): number {
  return Math.round(pieceCount * gramsPerPiece * 100) / 100
}

export function formatPieceGramHint(pieceCount: number, gramsPerPiece: number): string {
  const totalG = piecesToGrams(pieceCount, gramsPerPiece)
  const pieceWord = pieceCount === 1 ? 'piece' : 'pieces'
  return `${pieceCount} ${pieceWord} ≈ ${totalG} g`
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd frontend && npm test -- src/food/pieceEntry.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/food/pieceEntry.ts frontend/src/food/pieceEntry.test.ts
git commit -m "feat(food): add piece-to-grams helpers for ULT-10"
```

---

### Task 2: ProductPage Grams / Pieces UI + submit conversion

**Files:**
- Modify: `frontend/src/pages/ProductPage.tsx`
- Modify: `frontend/src/pages/ProductPage.test.tsx`

**Interfaces:**
- Consumes: `canLogByPieces`, `piecesToGrams`, `formatPieceGramHint` from `../food/pieceEntry`
- Consumes: existing `createDiaryEntry({ productId | submissionId, weightG, mealType })` — unchanged shape
- Produces: UI unit toggle when eligible; `weightG` on mutate always in grams

**UX lock-in (implement exactly):**

1. After product loads, if `canLogByPieces(data.servingSizeG)`, render a unit control above the amount field:
   - Accessible group label: `Unit`
   - Two options: **Grams** | **Pieces** (use `<fieldset>` + radio inputs, or two `type="button"` toggles with `aria-pressed`; prefer radios for form semantics without new CSS frameworks).
2. Default `unit` state: `'grams'`.
3. Amount state stays a string:
   - grams mode: label `Amount (g)`, `inputMode="decimal"`, default `"100"`
   - pieces mode: label `Amount (pieces)`, `inputMode="numeric"`, `step={1}`, `min={1}`, default `"1"`
4. On unit change: set unit + reset amount to `"100"` or `"1"` accordingly; clear `entryError`.
5. Pieces helper line (only in pieces mode): show `formatPieceGramHint(parsedCountOr1, data.servingSizeG)` — if current input is not a valid positive integer yet, still show hint for `1` piece or hide until valid; preferred: if parse fails, hint uses last valid positive int or `1`.
6. Submit validation:
   - grams: same as today — `Number(amount)` finite and `> 0`; error `Enter a positive gram amount.`
   - pieces: must be finite, integer, `>= 1`; error `Enter a positive whole number of pieces.`
7. Submit payload:
   - grams: `weightG = parsedAmount` (same as today; no forced 2dp change unless already doing it)
   - pieces: `weightG = piecesToGrams(parsedCount, data.servingSizeG!)`
8. Pending submission path (`submissionId` / `PENDING_SUBMISSION`) unchanged except it also uses the converted `weightG`.
9. If not eligible: no unit control; label stays `Amount (g)`; behavior identical to current file.

**State sketch:**

```tsx
type AmountUnit = 'grams' | 'pieces'

const [unit, setUnit] = useState<AmountUnit>('grams')
const [amount, setAmount] = useState('100') // replace weightG string state
```

Rename `weightG` state → `amount` to avoid confusing UI amount with API `weightG`.

- [ ] **Step 1: Write failing ProductPage tests** (append new `describe` block; keep existing estimated-nutrient tests)

```tsx
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
// ... existing imports ...

function mockProduct(overrides: Partial<client.Product> = {}): client.Product {
  return {
    id: 'prod-1',
    submissionId: null,
    barcode: '3017620422003',
    source: 'OFF',
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
    ...overrides,
  }
}

describe('ProductPage piece entry (ULT-10)', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows Grams/Pieces toggle when servingSizeG is positive', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct({ servingSizeG: 15 }))
    renderProduct()
    expect(await screen.findByLabelText(/grams/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/pieces/i)).toBeInTheDocument()
  })

  it('hides unit toggle when servingSizeG is null', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(
      mockProduct({ id: 'prod-2', name: 'Bulk oats', servingSizeG: null }),
    )
    renderProduct('prod-2')
    expect(await screen.findByLabelText(/amount \(g\)/i)).toBeInTheDocument()
    expect(screen.queryByLabelText(/^pieces$/i)).not.toBeInTheDocument()
  })

  it('submits weightG = pieces × servingSizeG in pieces mode', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct({ servingSizeG: 50 }))
    const createSpy = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({
      id: 'e1',
      productId: 'prod-1',
      submissionId: null,
      productName: 'Nutella',
      brand: 'Ferrero',
      weightG: 100,
      mealType: 'BREAKFAST',
      consumedAt: new Date().toISOString(),
      createdAt: new Date().toISOString(),
      nutrients: [],
    })

    renderProduct()
    await screen.findByLabelText(/amount \(g\)/i)

    fireEvent.click(screen.getByLabelText(/pieces/i))
    const amount = screen.getByLabelText(/amount \(pieces\)/i)
    fireEvent.change(amount, { target: { value: '2' } })
    fireEvent.click(screen.getByRole('button', { name: /add to diary/i }))

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({ productId: 'prod-1', weightG: 100, mealType: 'BREAKFAST' }),
      )
    })
  })

  it('rejects non-integer piece counts', async () => {
    vi.spyOn(client, 'fetchProductById').mockResolvedValue(mockProduct({ servingSizeG: 15 }))
    const createSpy = vi.spyOn(client, 'createDiaryEntry')

    renderProduct()
    await screen.findByLabelText(/amount \(g\)/i)
    fireEvent.click(screen.getByLabelText(/pieces/i))
    fireEvent.change(screen.getByLabelText(/amount \(pieces\)/i), {
      target: { value: '1.5' },
    })
    fireEvent.click(screen.getByRole('button', { name: /add to diary/i }))

    expect(await screen.findByText(/positive whole number of pieces/i)).toBeInTheDocument()
    expect(createSpy).not.toHaveBeenCalled()
  })
})
```

Tune accessible names to match whatever labels you use (`htmlFor` / radio `name="amount-unit"` values `grams`/`pieces` with visible text “Grams”/“Pieces”).

- [ ] **Step 2: Run ProductPage tests — expect new cases to fail**

```bash
cd frontend && npm test -- src/pages/ProductPage.test.tsx
```

- [ ] **Step 3: Implement ProductPage changes**

Key submit branch:

```tsx
if (unit === 'pieces') {
  const count = Number(amount)
  if (!Number.isInteger(count) || count < 1) {
    setEntryError('Enter a positive whole number of pieces.')
    return
  }
  weightG = piecesToGrams(count, data.servingSizeG as number)
} else {
  const parsedWeight = Number(amount)
  if (!Number.isFinite(parsedWeight) || parsedWeight <= 0) {
    setEntryError('Enter a positive gram amount.')
    return
  }
  weightG = parsedWeight
}
```

Keep meal select + pending vs product id branching identical to today.

Minimal CSS: reuse `.lookup-form` / `.cta-row`. If radios need spacing, add a small rule in `frontend/src/index.css` only if existing styles make the control unusable — prefer no new visual system.

- [ ] **Step 4: Run tests**

```bash
cd frontend && npm test -- src/pages/ProductPage.test.tsx src/food/pieceEntry.test.ts
cd frontend && npm test
```

Expected: all frontend tests PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/ProductPage.tsx frontend/src/pages/ProductPage.test.tsx frontend/src/index.css
git commit -m "feat(product): allow piece-based amount entry when servingSizeG exists"
```

---

### Task 3: Docs + Linear handoff

**Files:**
- Modify: `AI/ult-10-count-based-entry.md` — set status to Implemented; note verification commands
- Optional: one-line mention under Follow-ups in `AI/phase-3-diary-tracking.md` is **not required**

- [ ] **Step 1:** Update AI note status + “Verification” section with the npm test commands run
- [ ] **Step 2:** Commit

```bash
git add AI/ult-10-count-based-entry.md
git commit -m "docs(AI): mark ULT-10 piece entry as implemented"
```

- [ ] **Step 3:** Move Linear ULT-10 to **In Progress** while coding, then **In Review** / **Done** per team practice after PR (planner already moved card to **Todo** with this plan).

---

## Out of scope (do not implement)

- Cups / tbsp / ml / oz
- Parsing `quantityLabel` (“6 x 50 g”)
- Diary display of “2 pcs”
- Backend `entry_unit` / `piece_count`
- Hiding piece mode when serving looks like a spread
- NEvo / USDA piece weights
- Editing existing diary entries in piece mode
- Changes to `OffNutrientNormalizer`

## Manual QA checklist (implementer)

1. Product with `servingSizeG` (e.g. Nutella 15): toggle appears; Pieces → 2 → helper `2 pieces ≈ 30 g`; submit lands on `/today` with 30 g entry.
2. Product with `servingSizeG: null`: no toggle; 100 g default works.
3. Pending submission product with positive `servingSizeG`: piece mode works; mutate uses `submissionId`.
4. Invalid pieces (`0`, `-1`, `1.5`, empty): error, no API call.
5. Switch Pieces → Grams: amount resets to `100`; Grams → Pieces: resets to `1`.

## Self-review (planner)

1. **Spec coverage:** AC1–AC10 mapped to Task 1–2; open decisions resolved above.
2. **Placeholders:** none intentional — implementer should only adjust a11y label matchers to the real markup.
3. **Types:** `AmountUnit`, helpers, and `createDiaryEntry` weightG number are consistent across tasks.

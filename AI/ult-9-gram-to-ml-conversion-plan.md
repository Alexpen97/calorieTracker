# ULT-9 — Automatic gram↔ml conversion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Linear:** [ULT-9](https://linear.app/ultimateconcept/issue/ULT-9/automatic-gram-to-ml-conversion-depending-on-selected-product)
>
> **Related (out of scope):** [ULT-10](https://linear.app/ultimateconcept/issue/ULT-10/support-count-based-entry-pieces-instead-of-grams-where-possible) piece/count entry.

**Goal:** On the product **Add to today** form, let users enter **ml** for volume-capable products, convert ml → grams via product density, and submit the existing diary payload (`weightG` only).

**Architecture:** Derive `densityGPerMl` at map time in `food-catalog-service` (no new DB column). Expose it on `ProductResponse`. Frontend shows a **g | ml** unit control only when density is non-null, converts before `createDiaryEntry`. Diary-service and portion math stay grams-only.

**Tech Stack:** Java 21 / Spring Boot food-catalog-service (JUnit 5), React 19 + TypeScript + Vitest + Testing Library.

## Global Constraints

- Diary API schema unchanged: `POST /api/diary/entries` still takes `weightG` only.
- Nutrition remains **per 100 g**; conversion happens client-side before create.
- No new DB column / Flyway for density in v1 — compute in `ProductMapper`.
- Pending submissions (`quantityLabel == null`) are **never** volume-capable → `densityGPerMl == null`.
- Volume units (case-insensitive): `ml`, `cl`, `dl`, `l`, `liter`, `litre`, `liters`, `litres`.
- Convert to ml: `1 L = 1000`, `1 cl = 10`, `1 dl = 100`.
- Density order: (1) package mass+volume if both parseable from `quantityLabel`, (2) name/genericName keyword heuristics, (3) default `1.0`.
- Round converted `weightG` to **2 decimal places** (`HALF_UP`) before submit.
- Do **not** change diary list/today display units (still grams).
- Do **not** touch water quick-add, settings density prefs, or OFF full re-import.
- Preserve existing UI patterns on `ProductPage` (no redesign); match current form/error styling.
- After code changes: update this note’s “Done” section; keep Linear AC checkboxes in sync when closing.

## Design locks (do not re-litigate)

| Decision | Choice |
| --- | --- |
| Where density lives | API field `densityGPerMl: number \| null` on `GET /api/products/{id}` (and any endpoint returning `ProductResponse`) |
| Volume-capable signal | Non-null density ⇔ `quantityLabel` contains a supported volume unit |
| UI control | `<select>` with `g` / `ml` next to amount (simplest, matches meal `<select>`) |
| Default amount | Gram-only: `"100"` g (today). Volume-capable: default unit **ml**, amount `"100"` |
| Package density (v1) | Only if **both** mass (g/kg) **and** volume parse from the same `quantityLabel` (e.g. `"500 ml / 520 g"`). Single-unit labels like `"330 ml"` skip step 1 |
| OFF fields expansion | **Skip for v1** — do not add `product_quantity*` to `FoodProperties` fetch list |
| Diary client DTO | Leave `diary-service` `ProductResponse` unchanged; ignore unknown JSON property `densityGPerMl` |
| Redis cache | `ProductResponse` record change updates cache payload automatically via existing serialization |

## Heuristic density table (lock)

Match case-insensitively against concatenated `name + " " + genericName` (null-safe). First match wins:

| Keywords (any) | `densityGPerMl` |
| --- | --- |
| `oil`, `olie`, `olive oil`, `olijfolie` | `0.92` |
| `honey`, `honing`, `syrup`, `siroop` | `1.40` |
| `milk`, `melk`, `yoghurt`, `yogurt` | `1.03` |
| (fallback for volume-capable) | `1.00` |

## File map

| Area | Create / Modify |
| --- | --- |
| Volume parse + density | Create `VolumeQuantityParser.java`, `ProductDensityResolver.java` + unit tests |
| API shape | Modify `ProductResponse.java`, `ProductMapper.java`, `RedisProductCacheTest.java` |
| Frontend types/helpers | Modify `frontend/src/api/client.ts`; create `frontend/src/food/volumeConversion.ts` + tests |
| UI | Modify `ProductPage.tsx` + `ProductPage.test.tsx` |
| Docs | This file; short note in `AI/calorie-tracker-notes.md` if that file tracks feature status |
| Diary | **No changes** (verify existing diary tests still pass) |

---

### Task 1: Backend volume parsing helper

**Files:**
- Create: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/VolumeQuantityParser.java`
- Create: `services/food-catalog-service/src/test/java/com/nutritrack/food/service/VolumeQuantityParserTest.java`

**Interfaces:**
- Produces:
  - `boolean hasVolumeUnit(String quantityLabel)`
  - `Optional<BigDecimal> parseVolumeMl(String quantityLabel)` — first volume amount → ml
  - `Optional<BigDecimal> parseMassG(String quantityLabel)` — first mass amount → grams (`kg` × 1000)
  - Mass units to detect: `g`, `kg` (case-insensitive). Do not treat `mg` as package mass for density.

- [x] **Step 1: Write the failing tests**

```java
package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VolumeQuantityParserTest {

  @Test
  void detectsVolumeUnitsAndNormalizesToMl() {
    assertThat(VolumeQuantityParser.hasVolumeUnit("330 ml")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("1 L")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("25 cl")).isTrue();
    assertThat(VolumeQuantityParser.hasVolumeUnit("2 litres")).isTrue();
    assertThat(VolumeQuantityParser.parseVolumeMl("330 ml")).hasValue(new BigDecimal("330"));
    assertThat(VolumeQuantityParser.parseVolumeMl("1 L")).hasValue(new BigDecimal("1000"));
    assertThat(VolumeQuantityParser.parseVolumeMl("25 cl")).hasValue(new BigDecimal("250"));
    assertThat(VolumeQuantityParser.parseVolumeMl("1.5 dl")).hasValue(new BigDecimal("150.0"));
  }

  @Test
  void rejectsMassOnlyOrNull() {
    assertThat(VolumeQuantityParser.hasVolumeUnit("400 g")).isFalse();
    assertThat(VolumeQuantityParser.hasVolumeUnit(null)).isFalse();
    assertThat(VolumeQuantityParser.hasVolumeUnit("")).isFalse();
    assertThat(VolumeQuantityParser.parseVolumeMl("400 g")).isEmpty();
  }

  @Test
  void parsesMassAndVolumeFromCompoundLabel() {
    assertThat(VolumeQuantityParser.parseVolumeMl("500 ml / 520 g")).hasValue(new BigDecimal("500"));
    assertThat(VolumeQuantityParser.parseMassG("500 ml / 520 g")).hasValue(new BigDecimal("520"));
    assertThat(VolumeQuantityParser.parseMassG("1 kg")).hasValue(new BigDecimal("1000"));
  }
}
```

- [x] **Step 2: Run tests — expect FAIL (class missing)**

```bash
cd services/food-catalog-service && mvn -q -Dtest=VolumeQuantityParserTest test
```

Expected: compilation failure / tests not found for missing class.

- [x] **Step 3: Implement minimal parser**

```java
package com.nutritrack.food.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VolumeQuantityParser {

  private static final Pattern VOLUME =
      Pattern.compile(
          "([0-9]+(?:\\.[0-9]+)?)\\s*(ml|cl|dl|l|liters|litres|liter|litre)\\b",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern MASS =
      Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(kg|g)\\b", Pattern.CASE_INSENSITIVE);

  private VolumeQuantityParser() {}

  public static boolean hasVolumeUnit(String quantityLabel) {
    return quantityLabel != null && VOLUME.matcher(quantityLabel).find();
  }

  public static Optional<BigDecimal> parseVolumeMl(String quantityLabel) {
    if (quantityLabel == null || quantityLabel.isBlank()) {
      return Optional.empty();
    }
    Matcher m = VOLUME.matcher(quantityLabel);
    if (!m.find()) {
      return Optional.empty();
    }
    BigDecimal amount = new BigDecimal(m.group(1));
    String unit = m.group(2).toLowerCase(Locale.ROOT);
    return Optional.of(
        switch (unit) {
          case "l", "liter", "litre", "liters", "litres" -> amount.multiply(new BigDecimal("1000"));
          case "cl" -> amount.multiply(new BigDecimal("10"));
          case "dl" -> amount.multiply(new BigDecimal("100"));
          default -> amount; // ml
        });
  }

  public static Optional<BigDecimal> parseMassG(String quantityLabel) {
    if (quantityLabel == null || quantityLabel.isBlank()) {
      return Optional.empty();
    }
    Matcher m = MASS.matcher(quantityLabel);
    if (!m.find()) {
      return Optional.empty();
    }
    BigDecimal amount = new BigDecimal(m.group(1));
    String unit = m.group(2).toLowerCase(Locale.ROOT);
    return Optional.of("kg".equals(unit) ? amount.multiply(new BigDecimal("1000")) : amount);
  }
}
```

Note: put longer unit alternatives (`liters`/`litres`) before `l` in the regex alternation (already ordered above) so `l` does not steal the prefix of `liter`.

- [x] **Step 4: Re-run tests — expect PASS**

```bash
cd services/food-catalog-service && mvn -q -Dtest=VolumeQuantityParserTest test
```

- [x] **Step 5: Commit**

```bash
git add services/food-catalog-service/src/main/java/com/nutritrack/food/service/VolumeQuantityParser.java \
  services/food-catalog-service/src/test/java/com/nutritrack/food/service/VolumeQuantityParserTest.java
git commit -m "feat(food-catalog): parse volume and mass from quantity labels"
```

---

### Task 2: Backend density resolver

**Files:**
- Create: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/ProductDensityResolver.java`
- Create: `services/food-catalog-service/src/test/java/com/nutritrack/food/service/ProductDensityResolverTest.java`

**Interfaces:**
- Consumes: `VolumeQuantityParser`
- Produces: `BigDecimal resolve(String quantityLabel, String name, String genericName)` — returns `null` when not volume-capable; otherwise non-null density with scale suitable for JSON (use `BigDecimal` plain values `0.92`, `1.40`, `1.03`, `1.00`, or derived `massG/volumeMl` rounded to 4 decimal places HALF_UP)

- [x] **Step 1: Write failing tests**

```java
package com.nutritrack.food.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductDensityResolverTest {

  @Test
  void nullWhenNotVolumeCapable() {
    assertThat(ProductDensityResolver.resolve("400 g", "Nutella", null)).isNull();
    assertThat(ProductDensityResolver.resolve(null, "Water", null)).isNull();
  }

  @Test
  void defaultsToOneForBeverages() {
    assertThat(ProductDensityResolver.resolve("330 ml", "Coca-Cola", null))
        .isEqualByComparingTo("1.00");
  }

  @Test
  void oilHeuristicBelowOne() {
    assertThat(ProductDensityResolver.resolve("500 ml", "Olive oil", null))
        .isEqualByComparingTo("0.92");
    assertThat(ProductDensityResolver.resolve("1 L", "Zonnebloemolie", null))
        .isEqualByComparingTo("0.92");
  }

  @Test
  void honeyAndMilkHeuristics() {
    assertThat(ProductDensityResolver.resolve("350 g", "ignore", null)).isNull();
    assertThat(ProductDensityResolver.resolve("250 ml", "Honey", null))
        .isEqualByComparingTo("1.40");
    assertThat(ProductDensityResolver.resolve("1 L", "Semi-skimmed milk", "melk"))
        .isEqualByComparingTo("1.03");
  }

  @Test
  void derivesFromPackageWhenMassAndVolumePresent() {
    // 520 g / 500 ml = 1.04 g/ml
    assertThat(ProductDensityResolver.resolve("500 ml / 520 g", "Mystery drink", null))
        .isEqualByComparingTo("1.0400");
  }
}
```

- [x] **Step 2: Run — expect FAIL**

```bash
cd services/food-catalog-service && mvn -q -Dtest=ProductDensityResolverTest test
```

- [x] **Step 3: Implement resolver**

```java
package com.nutritrack.food.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;

public final class ProductDensityResolver {

  private ProductDensityResolver() {}

  public static BigDecimal resolve(String quantityLabel, String name, String genericName) {
    if (!VolumeQuantityParser.hasVolumeUnit(quantityLabel)) {
      return null;
    }
    Optional<BigDecimal> volumeMl = VolumeQuantityParser.parseVolumeMl(quantityLabel);
    Optional<BigDecimal> massG = VolumeQuantityParser.parseMassG(quantityLabel);
    if (volumeMl.isPresent()
        && massG.isPresent()
        && volumeMl.get().compareTo(BigDecimal.ZERO) > 0) {
      return massG.get().divide(volumeMl.get(), 4, RoundingMode.HALF_UP);
    }
    String haystack =
        ((name == null ? "" : name) + " " + (genericName == null ? "" : genericName))
            .toLowerCase(Locale.ROOT);
    if (containsAny(haystack, "olijfolie", "olive oil", "olie", "oil")) {
      return new BigDecimal("0.92");
    }
    if (containsAny(haystack, "honey", "honing", "syrup", "siroop")) {
      return new BigDecimal("1.40");
    }
    if (containsAny(haystack, "yoghurt", "yogurt", "milk", "melk")) {
      return new BigDecimal("1.03");
    }
    return new BigDecimal("1.00");
  }

  private static boolean containsAny(String haystack, String... needles) {
    for (String needle : needles) {
      if (haystack.contains(needle)) {
        return true;
      }
    }
    return false;
  }
}
```

Keyword order note: check `olijfolie` / `olive oil` before bare `olie`/`oil` is fine because all map to `0.92`; ensure `olie` does not false-positive unrelated Dutch words if observed in fixtures — acceptable for v1 per AC6.

- [x] **Step 4: Run — expect PASS; commit**

```bash
cd services/food-catalog-service && mvn -q -Dtest=ProductDensityResolverTest,VolumeQuantityParserTest test
git add services/food-catalog-service/src/main/java/com/nutritrack/food/service/ProductDensityResolver.java \
  services/food-catalog-service/src/test/java/com/nutritrack/food/service/ProductDensityResolverTest.java
git commit -m "feat(food-catalog): resolve product density for volume-capable labels"
```

---

### Task 3: Expose `densityGPerMl` on `ProductResponse`

**Files:**
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/web/dto/ProductResponse.java`
- Modify: `services/food-catalog-service/src/main/java/com/nutritrack/food/service/ProductMapper.java`
- Modify: `services/food-catalog-service/src/test/java/com/nutritrack/food/cache/RedisProductCacheTest.java` (constructor arity)
- Modify (if needed): `services/food-catalog-service/src/test/java/com/nutritrack/food/ProductControllerTest.java` — assert density for Coca-Cola fixture path if product is seeded with `"330 ml"`

**Interfaces:**
- Consumes: `ProductDensityResolver.resolve(...)`
- Produces: `ProductResponse` gains `BigDecimal densityGPerMl` (nullable), placed after `quantityLabel` for readability:

```java
public record ProductResponse(
    UUID id,
    UUID submissionId,
    String barcode,
    String source,
    String name,
    String brand,
    String quantityLabel,
    BigDecimal densityGPerMl,
    BigDecimal servingSizeG,
    String imageUrl,
    String nutriScore,
    String ingredientsText,
    List<String> allergenTags,
    Instant offLastSyncedAt,
    List<ProductNutrientResponse> nutrients) {}
```

- [x] **Step 1: Update record + mapper**

In `toResponse(Product product)`:

```java
ProductDensityResolver.resolve(
    product.getQuantityLabel(), product.getName(), product.getGenericName())
```

In `toResponse(ProductSubmission submission)`: pass `null` for density (quantityLabel is null).

- [x] **Step 2: Fix compile breaks in tests constructing `ProductResponse`**

`RedisProductCacheTest.sampleProduct()` — insert `null` density for Nutella `"400 g"` (or assert null). Prefer explicit `null`.

- [x] **Step 3: Add/extend a controller or mapper-focused assertion**

Prefer a small unit test on `ProductMapper` if one exists; otherwise extend `ProductControllerTest` after seeding an OFF product with `quantityLabel = "330 ml"` and assert:

```
jsonPath("$.densityGPerMl").value(1.0) // or 1.00
jsonPath for Nutella path → densityGPerMl null
```

If barcode lookup already imports `off-sample.jsonl` Coca-Cola (`330 ml`) / Nutella (`400 g`), assert against those responses (AC2, AC9).

- [x] **Step 4: Run food-catalog tests**

```bash
cd services/food-catalog-service && mvn -q test
```

Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add services/food-catalog-service
git commit -m "feat(food-catalog): expose densityGPerMl on ProductResponse"
```

---

### Task 4: Frontend volume conversion helper + Product type

**Files:**
- Create: `frontend/src/food/volumeConversion.ts`
- Create: `frontend/src/food/volumeConversion.test.ts`
- Modify: `frontend/src/api/client.ts` — add `densityGPerMl: number | null` to `Product`

**Interfaces:**
- Produces:

```ts
export type AmountUnit = 'g' | 'ml'

export function isVolumeCapable(densityGPerMl: number | null | undefined): boolean {
  return typeof densityGPerMl === 'number' && Number.isFinite(densityGPerMl) && densityGPerMl > 0
}

/** Round HALF_UP to 2 decimal places. */
export function convertVolumeToGrams(amountMl: number, densityGPerMl: number): number {
  const raw = amountMl * densityGPerMl
  return Math.round((raw + Number.EPSILON) * 100) / 100
}

export function resolveWeightG(amount: number, unit: AmountUnit, densityGPerMl: number | null): number {
  if (unit === 'ml') {
    if (!isVolumeCapable(densityGPerMl)) {
      throw new Error('Product does not support ml entry')
    }
    return convertVolumeToGrams(amount, densityGPerMl!)
  }
  return Math.round((amount + Number.EPSILON) * 100) / 100
}
```

Prefer a true half-up helper if the codebase already has one; otherwise the `Math.round` pattern above is acceptable for positive amounts used here.

- [x] **Step 1: Write failing Vitest cases**

```ts
import { describe, expect, it } from 'vitest'
import {
  convertVolumeToGrams,
  isVolumeCapable,
  resolveWeightG,
} from './volumeConversion'

describe('volumeConversion', () => {
  it('detects volume capability from density', () => {
    expect(isVolumeCapable(1)).toBe(true)
    expect(isVolumeCapable(null)).toBe(false)
    expect(isVolumeCapable(0)).toBe(false)
  })

  it('converts ml to grams with density', () => {
    expect(convertVolumeToGrams(250, 1)).toBe(250)
    expect(convertVolumeToGrams(100, 0.92)).toBe(92)
  })

  it('resolveWeightG passes grams through and converts ml', () => {
    expect(resolveWeightG(100, 'g', null)).toBe(100)
    expect(resolveWeightG(100, 'ml', 0.92)).toBe(92)
  })
})
```

- [x] **Step 2: Run — expect FAIL**

```bash
cd frontend && npm test -- --run src/food/volumeConversion.test.ts
```

- [x] **Step 3: Implement helper + extend `Product` type**

```ts
// in Product type:
densityGPerMl: number | null
```

Update **every** Product mock in frontend tests that constructs a full `Product` object to include `densityGPerMl: null` (or a real value). Start with `ProductPage.test.tsx`; run full frontend test suite and fix remaining mocks.

- [x] **Step 4: Run helper tests PASS; commit**

```bash
cd frontend && npm test -- --run src/food/volumeConversion.test.ts
git add frontend/src/food/volumeConversion.ts frontend/src/food/volumeConversion.test.ts frontend/src/api/client.ts
git commit -m "feat(frontend): add ml-to-gram conversion helper and Product.densityGPerMl"
```

---

### Task 5: ProductPage g | ml UI + submit conversion

**Files:**
- Modify: `frontend/src/pages/ProductPage.tsx`
- Modify: `frontend/src/pages/ProductPage.test.tsx`

**Interfaces:**
- Consumes: `isVolumeCapable`, `resolveWeightG`, `product.densityGPerMl`
- Produces: unit-aware amount form; diary mutate still `{ productId|submissionId, weightG, mealType }`

Behaviour checklist (map to AC3–AC7, AC9):

1. If `!isVolumeCapable(data.densityGPerMl)`: label `Amount (g)`, no unit select, default amount `"100"` (current).
2. If volume-capable: show amount input + `<select id="diary-unit">` options `g` / `ml`; default unit `ml`, default amount `"100"`.
3. When unit is `ml`, show helper: `≈ {grams} g at {density} g/ml` (use resolved grams + density from product).
4. Validation message: `Enter a positive amount.` (unit-agnostic).
5. On submit: `weightG = resolveWeightG(parsedAmount, unit, data.densityGPerMl)`.

Suggested state shape:

```tsx
const [amount, setAmount] = useState('100')
const [unit, setUnit] = useState<AmountUnit>('g')

// when product loads / changes, reset defaults:
// volume-capable → unit 'ml'; else 'g'; amount '100'
```

Use `useEffect` on `data?.id` + `data?.densityGPerMl` to reset defaults when navigating between products (avoid stale ml unit on a gram-only product).

- [x] **Step 1: Write failing UI tests in `ProductPage.test.tsx`**

```tsx
it('hides unit toggle for gram-only products', async () => {
  vi.spyOn(client, 'fetchProductById').mockResolvedValue({
    /* Nutella-like */ densityGPerMl: null, quantityLabel: '400 g', /* ...other required fields */
  })
  renderProduct()
  expect(await screen.findByLabelText(/amount \(g\)/i)).toBeInTheDocument()
  expect(screen.queryByLabelText(/unit/i)).not.toBeInTheDocument()
})

it('shows g|ml toggle and helper for volume-capable products', async () => {
  vi.spyOn(client, 'fetchProductById').mockResolvedValue({
    /* oil */ densityGPerMl: 0.92, quantityLabel: '500 ml', name: 'Olive oil', /* ... */
  })
  renderProduct()
  expect(await screen.findByLabelText(/unit/i)).toBeInTheDocument()
  expect(screen.getByText(/≈\s*92\s*g at 0\.92 g\/ml/i)).toBeInTheDocument()
})

it('submits converted weightG when ml selected', async () => {
  const create = vi.spyOn(client, 'createDiaryEntry').mockResolvedValue({} as never)
  // mock volume product density 1.0, user amount 250 ml
  // fill form, submit
  // expect create to have been called with expect.objectContaining({ weightG: 250 })
})
```

Fill in full `Product` mocks (copy existing mock shape + `densityGPerMl`). Use `userEvent` if already a project dependency; otherwise fire `change`/`submit` like other page tests.

- [x] **Step 2: Run ProductPage tests — expect FAIL**

```bash
cd frontend && npm test -- --run src/pages/ProductPage.test.tsx
```

- [x] **Step 3: Implement UI changes in `ProductPage.tsx`**

Keep markup minimal: reuse `lookup-form` classes; put unit `<select>` beside or under the amount field without introducing cards or new design language.

- [x] **Step 4: Run frontend tests**

```bash
cd frontend && npm test
```

Expected: PASS (fix all Product mocks for missing `densityGPerMl`).

- [x] **Step 5: Commit**

```bash
git add frontend/src/pages/ProductPage.tsx frontend/src/pages/ProductPage.test.tsx
git commit -m "feat(frontend): allow ml entry with density conversion on ProductPage"
```

---

### Task 6: Regression + AC verification

**Files:** none new (verification only); update this plan’s Done section + Linear AC checkboxes when complete.

- [x] **Step 1: Backend full test**

```bash
cd services/food-catalog-service && mvn -q test
cd services/diary-service && mvn -q test
```

Expected: PASS (diary unchanged; AC9).

- [x] **Step 2: Frontend full test**

```bash
cd frontend && npm test
```

- [x] **Step 3: Manual AC map**

| AC | Verify |
| --- | --- |
| AC1 | Parser tests: `330 ml` yes, `400 g`/null no |
| AC2 | ProductResponse / controller: Coke non-null density; Nutella null |
| AC3 | ProductPage tests: toggle visibility |
| AC4 | Submit spy sends `weightG` only |
| AC5 | `convertVolumeToGrams(250, 1) === 250` (+ optional PortionMath parity note) |
| AC6 | Oil → `0.92`; 100 ml → 92 g |
| AC7 | Helper text visible when ml selected |
| AC8 | Backend + frontend tests listed above |
| AC9 | Gram-only path + diary tests |

- [x] **Step 4: Update docs**

- Mark tasks complete in this file.
- Append a short bullet under `AI/calorie-tracker-notes.md` (if that file is the running changelog): “ULT-9: ml entry via densityGPerMl on products; diary still grams.”
- Comment on Linear ULT-9 with PR link; move status to **In Progress** when implementation starts, **In Review** when PR opens (implementing agent owns that).

- [x] **Step 5: Final commit if doc-only deltas remain; push branch; open/update PR**

---

## Out of scope (remind implementing agent)

- Storing original ml on diary rows
- ml for products without volume `quantityLabel` (see ULT-10 for pieces)
- User-editable density / settings
- Diary display in ml
- Water `POST /api/diary/water`
- Expanding OFF fetch fields / catalog re-import

## Self-review (planner)

1. **Spec coverage:** AC1–AC9 mapped to Tasks 1–6. Package derivation + heuristics + default covered in Task 2. UI + client conversion in Tasks 4–5. Diary untouched by design.
2. **Placeholders:** None intentional — regex, heuristic table, file paths, and commands are concrete.
3. **Type consistency:** Field name `densityGPerMl` used end-to-end; helpers `convertVolumeToGrams` / `resolveWeightG` / `isVolumeCapable` named consistently across Task 4–5.


## Done

Implemented on branch `cursor/ult-9-card-feature-485a`.

- Task 1–5: volume parser, density resolver, `densityGPerMl` on `ProductResponse`, frontend conversion helper, ProductPage g|ml UI.
- Task 6 verification: food-catalog `mvn test` PASS; diary-service `mvn test` PASS; frontend `npm test` 97/97 PASS.
- AC1–AC9 covered by unit/controller/UI tests; diary API unchanged.

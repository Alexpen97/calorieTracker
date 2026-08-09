# Task 5 Report: ProductPage g/ml UI + submit conversion

## Summary

- Added ProductPage diary amount unit handling for gram-only and volume-capable products.
- Volume-capable products now default to 100 ml, show a g/ml unit select, display converted grams helper text, and submit converted `weightG`.
- Gram-only products keep the `Amount (g)` label, no unit select, and default amount `100`.
- Product changes reset amount/unit defaults from `data.id` and `data.densityGPerMl`.

## TDD Evidence

### RED

Command:

```bash
cd /workspace/frontend && npm test -- --run src/pages/ProductPage.test.tsx
```

Result before implementation:

- Exit code: 1
- 7 tests run; 4 failed.
- Expected failures covered missing Unit select/helper, old gram-specific validation text, unconverted submit weight, and stale product navigation state.

### GREEN

Command:

```bash
cd /workspace/frontend && npm test -- --run src/pages/ProductPage.test.tsx
```

Result after implementation:

- Exit code: 0
- 1 test file passed.
- 7 tests passed.

### Full frontend verification

Command:

```bash
cd /workspace/frontend && npm test
```

Result:

- Exit code: 0
- 28 test files passed.
- 97 tests passed.

## Notes

- Used existing `isVolumeCapable`, `resolveWeightG`, and `AmountUnit` helpers.
- Diary mutation payload shape remains `{ productId|submissionId, weightG, mealType }`.

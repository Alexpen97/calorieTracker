# ULT-10 — Count-based entry (pieces)

## Goal

On the product “Add to today” form, allow logging in **pieces** when
`Product.servingSizeG` is a positive number. Convert to grams before
`createDiaryEntry` so the diary API stays gram-only.

## Status

Implemented on `cursor/ult-10-card-feature-e8aa`.

## Decisions (locked)

1. Unit switch resets amount to mode default (`100` g / `1` piece).
2. Pieces accept positive integers only.
3. `gramsPerPiece` reuses `Product.servingSizeG` (no new API field).
4. Initial unit remains **Grams**; Pieces is opt-in.

## Delivered

- `frontend/src/food/pieceEntry.ts` — eligibility, parse, convert, resolve helpers
- `ProductPage` — Grams/Pieces toggle, helper text, conversion on submit
- Tests: `pieceEntry.test.ts`, extended `ProductPage.test.tsx`

## Known limitation

OFF `serving_size` is not always “one countable piece” (e.g. Nutella “15 g”
spread). v1 accepts that trade-off.

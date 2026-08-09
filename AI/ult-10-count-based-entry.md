# ULT-10 — Count-based (pieces) entry

## Goal

Let users log products in **pieces** on the product page when `servingSizeG > 0`, converting to grams before `createDiaryEntry`. Diary API and storage stay grams-only.

## Plan (for implementing agent)

Full task breakdown (TDD steps, file paths, test code, resolved decisions):

→ [`docs/superpowers/plans/2026-08-09-ult-10-count-based-entry.md`](../docs/superpowers/plans/2026-08-09-ult-10-count-based-entry.md)

Linear: [ULT-10](https://linear.app/ultimateconcept/issue/ULT-10/support-count-based-entry-pieces-instead-of-grams-where-possible)

## Status

**Planned** (ready for Todo / implementation). Do not start coding from this note alone — follow the plan file.

## Resolved decisions (planning)

1. **Unit switch:** reset amount to mode default (`100` g / `1` piece).
2. **Pieces:** positive integers only.
3. **gramsPerPiece:** reuse `Product.servingSizeG` (no new API field).
4. **Initial unit:** stay on **Grams** by default; Pieces is opt-in (OFF `serving_size` is often a spread/serving, not a countable piece).

## Known limitation

OFF-derived `servingSizeG` is not always “one piece” (e.g. Nutella `15 g`). v1 still enables piece mode whenever `servingSizeG > 0`. Smarter unit detection is a follow-up.

## Out of scope (v1)

Volume units, `quantityLabel` parsing, diary “pcs” display, backend schema, edit-in-pieces, NEvo/USDA piece tables.

## Related

- Related Linear: ULT-9 (gram↔ml) — separate concern; do not couple implementations.
- Product form today: `frontend/src/pages/ProductPage.tsx` (grams-only).
- Phase 3 context: `AI/phase-3-diary-tracking.md`.

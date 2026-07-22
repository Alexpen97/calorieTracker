# Expanded vitamins & minerals (2026-07-22)

## Goal

Track and display a full adult micronutrient checklist on the dashboard,
backed by catalog education rows, goals DRVs, and Open Food Facts mappings.

## Dashboard

- Order: Today Summary → Vitamins | Minerals (side-by-side) → Weight Progress
- Removed standalone Macros card (macros remain in the nested summary ring)
- Vitamins/minerals stay side-by-side (`dashboard-micro-pair`) with compact 4-column grids and no % labels

## Codes

**Vitamins:** A, B1–B7, B9, B12, C, D, E, K  
**Minerals:** calcium, iron, magnesium, potassium, sodium, zinc, iodine,
selenium, copper, manganese, phosphorus, chromium, molybdenum

## Pipeline changes

| Layer | Change |
|---|---|
| food-catalog | Flyway `V5__expand_micronutrients.sql` (education + NRI seeds) |
| user-profile | Flyway `V3__expand_micronutrient_reference.sql` (goals DRVs) |
| OFF import | `OffNutrientNormalizer` maps OFF `_100g` tags (e.g. `vitamin-pp` → B3, `biotin` → B7) |
| frontend | `nutritionDashboard.ts` lists; always show all rows (0% when missing) |

## Follow-up

- Existing users need goals recalculate / re-apply after deploy so new DRVs
  appear in diary summary targets.
- Redeploy `food-catalog-service` and `user-profile-service` for Flyway.

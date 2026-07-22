# Empty-day summary shows calorie/macro goals

## Problem

Today Summary showed only `0` calories and macros as `0 g` (no `/ goal`) on days
with no diary entries — even after onboarding had set nutrient goals.

## Root cause

`SummaryService` only emitted `totals` for nutrients present on logged foods.
Water already always included `targetMl`; goal nutrients did not.

## Fix (2026-07-22)

- Seed each non-water goal into the daily totals at amount `0` with unit/target
  from the goals service when nothing was consumed.
- Empty and partial days now return `energy_kcal` / macros with targets so the
  dashboard can render `0 / goal` labels.

## Files

- `services/diary-service/.../SummaryService.java`
- `services/diary-service/.../DiaryControllerTest.java`
- Frontend regression: `nutritionDashboard.test.ts`, `DashboardPage.test.tsx`

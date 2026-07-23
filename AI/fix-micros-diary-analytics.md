# Fix: vitamins & minerals on Diary and Analytics (2026-07-23)

## Problem

After the expanded micronutrient checklist landed on the dashboard, Diary and
Analytics looked empty or wrong:

1. **Diary** still rendered `vitamins.slice(0, 4)` / `minerals.slice(0, 4)`.
   The expanded list starts with A, B1, B2, B3 (and calcium…), so real intake
   for C, D, B12, etc. never appeared in the checklist.
2. **Analytics** labeled vitamins/minerals as “Intake (avg)” but used only the
   latest day, and did not merge `/users/me/goals` the way Dashboard/Diary do.
   Crowded `GroupedBars` in a half-width column also made the full checklist
   hard to read.

## Fix

- Diary: full `MicroProgressGrid` for vitamins and minerals (same checklist as
  dashboard); weight card stays below.
- Analytics: `averageMicronutrientRows()` over the range, `mergeSummaryWithGoals`
  on each day, and `MicroProgressGrid` for vitamins/minerals.
- Helper + page tests cover Vitamin C / Iron visibility and range averages.

## Files

- `frontend/src/diary/nutritionDashboard.ts` (+ tests)
- `frontend/src/screens/DiaryView.tsx`
- `frontend/src/screens/AnalyticsView.tsx`
- `frontend/src/pages/AnalyticsPage.tsx` (+ tests)
- `frontend/src/pages/DiaryPage.test.tsx`

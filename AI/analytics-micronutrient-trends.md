# Analytics micronutrient 30-day trends (2026-07-25)

## Goal

Show vitamins and minerals on Analytics as line graphs over the last 30 days,
instead of averaged progress bars.

## Behavior

- Analytics fetches diary summaries with `dateDaysAgo(29)` → today (30 days).
- `buildMicronutrientTrendSeries()` builds one series per vitamin/mineral.
- Missing nutrient values for a day plot as `0`.
- Y-axis is % of daily target (0–100, capped).
- Latest amount/target label sits under each mini chart.
- Line tone: green ≥80%, amber 40–79%, red <40% (based on latest day).
- Insights still use range averages (`averageMicronutrientRows`).
- Dashboard remains today-only progress bars.

## Files

- `frontend/src/diary/nutritionDashboard.ts` (+ tests)
- `frontend/src/ui/MiniCharts.tsx` (`NutrientTrendLineChart`, `MicroTrendGrid`)
- `frontend/src/screens/AnalyticsView.tsx`
- `frontend/src/pages/AnalyticsPage.tsx` (+ tests)
- `frontend/src/pages/preview/PreviewAnalyticsPage.tsx`
- `frontend/src/index.css`

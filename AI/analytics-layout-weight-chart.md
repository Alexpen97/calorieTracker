# Analytics layout: side-by-side micros + weight chart (2026-07-25)

## Changes

- Vitamins and Minerals cards sit **side by side** on the analytics 2-column
  grid (no `dashboard-span`).
- Card order: Weight (full width) → Vitamins | Minerals → Macro | Insights.
- Weight trend uses functional `WeightTrendChart` (axes, markers, hover) instead
  of the decorative sparkline, with latest weight callout.

## Files

- `frontend/src/screens/AnalyticsView.tsx`
- `frontend/src/index.css`
- `frontend/src/pages/AnalyticsPage.test.tsx`

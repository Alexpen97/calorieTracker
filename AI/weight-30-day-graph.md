# Dashboard weight card: 30-day graph + card spacing

## Changes (2026-07-22)

1. **Vertical card gap** — `.dashboard-page` `row-gap` increased 50% from
   `0.18rem` → `0.27rem` (column gap unchanged at `0.9rem`).
2. **Weight Progress card** — shows a timed 30-day chart of logged weigh-ins
   instead of a 14-entry sequential sparkline labeled “Last 2 weeks”.
3. **Chart fills the card** — meta row (kg + “Last 30 days”) on top; chart is
   full-width underneath with height `clamp(8.5rem, 32vw, 12rem)`.

## Behavior

- `buildWeightTrendSeries(weights, { days: 30, clock })` keeps logs whose
  `measuredAt` falls in the inclusive local window
  `[start of (today − 29 days), end of today]`.
- Each point gets `t ∈ [0, 1]` so X maps to real time (gaps between weigh-ins
  are visible).
- `WeightTrendChart` draws the path plus a marker per logged input.
- Copy: “Last 30 days”. Latest kg value still shown beside the chart.
- `buildWeightTrend` now returns the same 30-day filtered values (Analytics /
  Diary sparklines stay sequential but use the new window).

## Tests

- `nutritionDashboard.test.ts` — 30-day filter + timeline `t` ordering
- `MiniCharts.test.tsx` — markers + path for `WeightTrendChart`
- `DashboardPage.test.tsx` — Weight Progress / Last 30 days / two markers

# Weight chart: proper line graph axes

## Goal

Dashboard Weight Progress looked like a bare sparkline (“just a line”).
Upgrade `WeightTrendChart` into a compact line graph with axes, gridlines,
and clearer markers — without adding a chart library.

## Delivered (2026-07-22)

- `WeightTrendChart` with viewBox `360×140`, Y kg ticks, X date ticks,
  gridlines, thin line (`stroke-width: 1.75`), markers `r=2`.
- CSS `aspect-ratio: 360 / 140` so the chart fills card width.
- Hover: larger invisible hit targets; floating label shows weight; SVG
  `<title>` (plus `aria-label`) includes the weigh-in date when `measuredAt`
  is present — not the HTML `title` attribute (invalid on React 19 SVG props).
- `buildWeightTrendAxisLabels({ days, clock })` → `[start, mid, end]`.
- Dashboard wires series + axis labels from `buildWeightTrendSeries`.
- Analytics / Diary sparklines unchanged (out of scope).

## Follow-up (2026-07-25)

- Analytics Weight trend now uses the same `WeightTrendChart` (axes, markers,
  hover) as the dashboard, fed by `buildWeightTrendSeries` over the last 30 days.

## Tests

- `MiniCharts.test.tsx` — grid, y/x ticks, marker radius, hover tooltip
- `nutritionDashboard.test.ts` — axis label window
- `DashboardPage.test.tsx` — Weight Progress + markers

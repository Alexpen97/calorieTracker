# Weight chart: proper line graph axes

## Goal

Dashboard Weight Progress looked like a bare sparkline (“just a line”).
Upgrade `WeightTrendChart` into a compact line graph with axes, gridlines,
and clearer markers — without adding a chart library.

## Delivered (2026-07-22)

- `WeightTrendChart` plot padding, Y kg ticks (min / mid / max with padding),
  X date ticks via `xLabels`, horizontal + vertical gridlines, larger markers
  (`r=2.8`), taller viewBox (`0 0 100 48`).
- `buildWeightTrendAxisLabels({ days, clock })` → `[start, mid, end]` short
  dates for the same 30-day window as `buildWeightTrendSeries`.
- Dashboard wires `xLabels={buildWeightTrendAxisLabels()}`.
- CSS: `.weight-trend-line`, gridline, axis-label styles; chart height
  `4.25rem`.
- Analytics / Diary sparklines unchanged (out of scope).

## Tests

- `MiniCharts.test.tsx` — grid, y/x ticks, marker radius
- `nutritionDashboard.test.ts` — axis label window
- `DashboardPage.test.tsx` — still finds Weight Progress + markers

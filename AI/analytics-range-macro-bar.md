# Analytics: switchable 30-day range + slim macro bar (2026-07-25)

## Changes

1. **Date range navigation** — Analytics uses the same prev/next pattern as the
   diary day nav. The window is always 30 inclusive days ending on `rangeEnd`
   (default today). Previous jumps back 30 days; next jumps forward 30 days and
   clamps to today.
2. **Slim macro bar** — Protein / Carbs / Fat use the original mixed stacked
   bar (one shared track + legend) under the range nav, without a Macro balance
   card. Values are from the latest day in the selected window.
3. **Weight chart** — Axis/filter clock follows the selected range end so past
   windows plot correctly.

## Files

- `frontend/src/diary/formatDay.ts` — `analyticsRangeFromEnd`,
  `shiftAnalyticsRangeEnd`, `formatAnalyticsRangeLabel`
- `frontend/src/pages/AnalyticsPage.tsx` — `rangeEnd` state wired to queries
- `frontend/src/screens/AnalyticsView.tsx` — range nav + slim macros
- `frontend/src/ui/MiniCharts.tsx` — `SlimMacroBar`
- `frontend/src/index.css` — range nav + slim macro styles; full-width spans
- `frontend/src/pages/preview/PreviewAnalyticsPage.tsx`
- Tests: `AnalyticsPage.test.tsx`, `formatDay.test.ts`, `MiniCharts.test.tsx`

# Mobile UI mockups

## Summary

Added generated mockups for a card-based NutriTrack redesign under
`docs/design/mockups/`.

## Direction

- Mobile is the primary platform.
- The visual direction uses rounded cards, soft light backgrounds, subtle
  shadows, and teal/emerald nutrition accents.
- Core surfaces represented: dashboard, analytics, and diary.
- Data concepts represented: weight, macros, vitamins, minerals, water, diary
  meals, and insight cards.

## Files

- `docs/design/mockups/mobile-nutrition-dashboard.png`
- `docs/design/mockups/mobile-nutrition-analytics.png`
- `docs/design/mockups/mobile-nutrition-diary.png`
- `docs/design/mockups/desktop-nutrition-dashboard.png`
- `docs/design/mockups/desktop-nutrition-analytics.png`
- `docs/design/mockups/desktop-nutrition-diary.png`
- `docs/design/mockups/README.md`

## Implementation status

Implemented on `cursor/implement-mobile-nutrition-ui-b34b`.

- Plan reference: `docs/superpowers/plans/2026-07-22-mobile-nutrition-ui.md`
- `/today` is the dashboard with calories, weight sparkline, macros, vitamins, and minerals.
- `/diary` is the food timeline with water logging, meal cards, and micronutrient checklists.
- `/analytics` shows weekly range summaries, weight trend, macro balance, micronutrients, and insights.
- Charts use lightweight CSS/SVG primitives rather than a charting library.
- Bottom navigation is the primary mobile chrome; desktop keeps a top nav.
- Profile cards reuse the shared dashboard card styling without changing form behavior.

## Mockup alignment rework

The first implementation landed with correct data and routes, but the visuals were not close enough to the approved mobile mockups. A follow-up rework is in progress on `cursor/rework-mobile-nutrition-ui-b34b` (PR #23) to better match the mockups:

- Shared primitives updated toward mockup style (SVG ring gauges, icon card headers, icon tab bar).
- Pages refactored into view components:
  - `frontend/src/screens/DashboardView.tsx`
  - `frontend/src/screens/DiaryView.tsx`
  - `frontend/src/screens/AnalyticsView.tsx`
- Dev-only preview routes added for visual QA without backend auth:
  - `/preview/dashboard`
  - `/preview/diary`
  - `/preview/analytics`


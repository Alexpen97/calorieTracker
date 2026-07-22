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


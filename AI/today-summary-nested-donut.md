# Today Summary calorie ring + macro bars

## Change

Today Summary shows a single calorie progress ring plus three horizontal macro
bars (Protein / Carbs / Fat):

- **Ring:** calorie progress toward the daily goal (track background + fill).
  Center text shows `consumed / goal` with a tighter stroke so the label sits
  inside the ring clear area.
- **Macro bars:** laid out in one horizontal row; each column shows the macro
  name above the bar and `amount / goal unit` below (no percent text).
- Side stats (Goal / Consumed / Meals) and the View Diary action remain removed —
  Today Summary is nutrients-only.
- Empty days still show all three macros at `0` progress with visible goal tracks;
  amount labels include the goal when a target is present.
- Diary summary now always includes goal nutrients at amount `0` on empty days
  (see `AI/empty-day-summary-goals.md`), so calories render as `0 / goal` and
  macros as `0 / goal g` even before any food is logged.

## Files

- `frontend/src/ui/MiniCharts.tsx` — `NestedCalorieMacroRing`
- `frontend/src/diary/nutritionDashboard.ts` — always returns Protein/Carbs/Fat
- `frontend/src/screens/DashboardView.tsx` — wires calorie + macro amount/goal labels
- `frontend/src/index.css` — calorie ring + horizontal macro bar styles
- Tests: `MiniCharts.test.tsx`, `DashboardPage.test.tsx`, `nutritionDashboard.test.ts`
- Test setup: global RTL `cleanup()` in `src/test/setup.ts`

# Today Summary nested donut

## Change

Replaced the single calorie `ProgressRing` in the dashboard **Today Summary**
card with a nested donut:

- **Inner ring:** calorie progress toward the daily goal (track background + fill).
- **Outer ring:** three equal arcs for Protein / Carbs / Fat, each with a tinted
  track background (full goal) and a fill based on that macro’s goal progress.
- **Legend:** macro name + percent with color-matched swatches and progress bars
  that also show a full-goal track background.
- Side stats (Goal / Consumed / Meals) unchanged.
- Empty days still show all three macros at `0%` with visible goal tracks.

## Files

- `frontend/src/ui/MiniCharts.tsx` — `NestedCalorieMacroRing`
- `frontend/src/diary/nutritionDashboard.ts` — always returns Protein/Carbs/Fat
- `frontend/src/screens/DashboardView.tsx` — wires macros into Today Summary
- `frontend/src/index.css` — nested ring + legend bar styles
- Tests: `MiniCharts.test.tsx`, `DashboardPage.test.tsx`, `nutritionDashboard.test.ts`
- Test setup: global RTL `cleanup()` in `src/test/setup.ts`

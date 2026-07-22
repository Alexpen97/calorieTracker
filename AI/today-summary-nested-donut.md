# Today Summary nested donut

## Change

Replaced the single calorie `ProgressRing` in the dashboard **Today Summary**
card with a nested donut:

- **Inner ring:** calorie progress toward the daily goal (track background + fill).
- **Outer ring:** three equal arcs for Protein / Carbs / Fat, each with a track
  background and a fill based on that macro’s goal progress.
- **Legend:** macro name + percent under the chart (color-matched swatches).
- Side stats (Goal / Consumed / Meals) unchanged.

## Files

- `frontend/src/ui/MiniCharts.tsx` — `NestedCalorieMacroRing`
- `frontend/src/screens/DashboardView.tsx` — wires macros into Today Summary
- `frontend/src/index.css` — nested ring + legend styles
- Tests: `MiniCharts.test.tsx`, `DashboardPage.test.tsx`
- Test setup: global RTL `cleanup()` in `src/test/setup.ts`

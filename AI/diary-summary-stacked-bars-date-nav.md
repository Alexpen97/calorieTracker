# Diary summary: stacked food bars + day navigation

## Change

Food Diary summary no longer uses donut/progress rings. It shows a vertical
stack of food bars for Calories, Protein, Carbs, and Fat with amount/goal
labels, so the focus stays on what was eaten.

Between the Food Summary card and Meals, a day navigator shows the selected
date (`Today` / `Yesterday` / `Tomorrow` / weekday) with previous/next controls
that load that day’s summary and entries (past or future).

## Files

- `frontend/src/ui/MiniCharts.tsx` — `StackedFoodBars`
- `frontend/src/screens/DiaryView.tsx` — bars summary + day nav between summary and meals
- `frontend/src/pages/DiaryPage.tsx` — `selectedDate` state wired to diary queries
- `frontend/src/diary/formatDay.ts` — `shiftLocalDate`, `formatDiaryDayLabel`
- `frontend/src/index.css` — stacked food bar + day nav styles
- Tests: `DiaryPage.test.tsx`, `formatDay.test.ts`, `MiniCharts.test.tsx`

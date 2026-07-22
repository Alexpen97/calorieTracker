# Diary summary: remove Add Food + tighter mobile bars

## Change

- Removed the **Add Food** tile from the Food Diary summary card. Food tracking
  stays available via the mobile bottom-nav Track food FAB (`/lookup`).
- Tightened stacked nutrient bar spacing on mobile (`max-width: 640px`): less
  gap between bars and a slightly shorter track height so macros read as one
  compact stack.

## Files

- `frontend/src/screens/DiaryView.tsx` — drop Add Food link / `onAddFoodHref`
- `frontend/src/index.css` — remove `.add-food-*`; mobile bar spacing after stacked bar rules
- `frontend/src/pages/DiaryPage.test.tsx` — assert Add Food link is absent

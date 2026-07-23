# Diary meal category + buttons

## Change

Each meal category in the Food Diary (Breakfast, Lunch, Dinner, Snacks) now has
a small **+** button that opens food lookup with that meal preselected.

## Behavior

- Diary meal header links go to `/lookup?meal=BREAKFAST|LUNCH|DINNER|SNACK`.
- Lookup preserves `meal` when opening a product (`/products/:id?meal=…`).
- Product “Add to today” form initializes the meal select from `?meal=`.
- Accessible names: `Add food to Breakfast`, etc.

## Files

- `frontend/src/screens/DiaryView.tsx` — meal header + button
- `frontend/src/index.css` — `.meal-header` / `.meal-add-btn`
- `frontend/src/diary/formatDay.ts` — meal query helpers
- `frontend/src/pages/LookupPage.tsx` — pass meal through to products
- `frontend/src/pages/ProductPage.tsx` — preselect meal from query
- Tests: `DiaryPage.test.tsx`, `formatDay.test.ts`

# Analytics shared micronutrient graphs (2026-07-25)

## Goal

Show vitamins and minerals on Analytics as **one shared multi-line chart each**,
instead of per-nutrient small multiples.

## Scale

Each line uses **its own RDI/target**:

| Chart position | Amount |
|---|---|
| Bottom | 50% of RDI |
| Center (dashed guide) | 100% RDI |
| Top | 150% of RDI |

Values outside that band are clipped. Different nutrients (mg vs µg) can share
one plot because each series is normalized independently.

## UI

- Vitamins card → `SharedMicronutrientTrendChart`
- Minerals card → `SharedMicronutrientTrendChart`
- Color legend under each chart
- Cards span full analytics width

## Files

- `frontend/src/diary/nutritionDashboard.ts` (`micronutrientRdiNormalized`)
- `frontend/src/ui/MiniCharts.tsx` (`SharedMicronutrientTrendChart`)
- `frontend/src/screens/AnalyticsView.tsx`
- `frontend/src/index.css`
- tests for helper, chart, and Analytics page

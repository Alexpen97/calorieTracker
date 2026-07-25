# Analytics shared micronutrient graphs (2026-07-25)

## Goal

Show vitamins and minerals on Analytics as **one shared multi-line chart each**,
instead of per-nutrient small multiples.

## Scale (fixed 2026-07-25)

Each line uses **its own RDI/target** on a **0% → 150%** vertical scale:

| Chart position | Amount |
|---|---|
| Bottom | 0% of RDI |
| Dashed guide | 100% RDI (~⅔ up) |
| Top | 150% of RDI |

Earlier [50%–150%] mapping made every low/empty day sit on the bottom edge
labeled “50%”, so limits looked wrong. Zero intake now sits at true 0%.

Values above 150% RDI clip at the top. Different nutrients (mg vs µg) share one
plot because each series is normalized independently.

## UI

- Compact chart (`360×112` viewBox, thinner strokes)
- Vitamins / Minerals cards → `SharedMicronutrientTrendChart`
- Legend shows each nutrient’s RDI amount + unit
- Cards span full analytics width

## Files

- `frontend/src/diary/nutritionDashboard.ts` (`micronutrientRdiNormalized`)
- `frontend/src/ui/MiniCharts.tsx` (`SharedMicronutrientTrendChart`)
- `frontend/src/screens/AnalyticsView.tsx`
- `frontend/src/index.css`
- tests for helper, chart, and Analytics page

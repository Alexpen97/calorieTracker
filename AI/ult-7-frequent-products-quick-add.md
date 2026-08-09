# ULT-7 — Frequent products + Quick add

Shipped on branch `cursor/ult-7-card-feature-c6fb`.

## Backend

- `GET /api/diary/frequent?limit=&weeks=` on diary-service
- In-memory aggregation over existing `diary_entry` range query (H2 + Postgres safe)
- Defaults: `limit=8`, `weeks=8`; caps 20 / 52; invalid present values → 400
- Identity: exactly one of `productId` / `submissionId`; `usualWeightG` = rounded mean
- Min eligibility: ≥ 2 logs in window

## Frontend

- `fetchFrequentProducts` + `FrequentProduct` in `client.ts`
- `FrequentFoodsCard` on `/lookup` above method nav
- One-tap `createDiaryEntry` → `/today`
- Meal: URL `?meal=` wins, else `lastMealType`
- Empty → hide card; error → retry line (barcode/search unaffected)
- Secondary link to `/products/:id?meal=…` for product or submission id

## Verification

- `cd services/diary-service && mvn test`
- `cd frontend && npm test -- --run && npm run build`

## v1 limits

- Same food logged under both product and submission ids stays separate groups
- No favorites / recently-logged-today / recommendation-service duplicate

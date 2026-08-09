# NEVO micronutrient estimates (2026-07-24)

## Goal

Fill missing micronutrients on catalog products using the Dutch Food
Composition Database (NEVO) as an **estimate** source when Open Food Facts
lacks vitamins/minerals.

## Architecture

- New microservice: `services/nevo-service`
- Dedicated DB: `nevo` (created in `infra/postgres/init/01-create-databases.sql`)
- CSV import into `nevo_food` + `nevo_nutrient_value`
- Internal match API: `POST /internal/nevo/matches/best`
- Public user lookup search: `GET /api/nevo/foods/search?q=&limit=` (alias
  expansion + vegetable-first ranking). Catalog merges these hits into
  `GET /api/products/search` as `source: "NEVO"`. See
  `AI/ult-8-colloquial-food-search-plan.md` (ULT-8).
- `food-catalog-service` after OFF upsert: USDA (`enrichIfSparse`) then NEVO
  (`enrichMissingMicros`); NEVO merges only **still-missing** micronutrient
  codes with provenance `NEVO_ESTIMATE`
- Optional LibreTranslate (`services/libretranslate`) translates generic/product
  names to English before NEVO matching (`LIBRETRANSLATE_ENABLED`)

## NEVO file format (RIVM 2025/9.0)

Files live in `services/nevo-service/src/main/resources/nevo/`:

- Delimiter: pipe `|`
- Encoding: UTF-8
- Wide matrix: `NEVO2025_v9.0.csv` (~2300 foods, nutrient codes as columns)
- Identity columns: `NEVO-code`, `Engelse naam/Food name`,
  `Voedingsmiddelnaam/Dutch food name`, `Food group`, `Synoniem`, …
- Nutrient columns use codes + unit, e.g. `ENERCC (kcal)`, `THIA (mg)`,
  `VITA_RAE (µg)`, `NA (mg)`
- Decimal separator in values: Dutch comma (`0,12`)

Mapped NEVO codes → internal codes include: ENERCC, PROT, FAT, CHO, SUGAR,
FIBT, NA, K, CA, P, MG, FE, CU, SE, ZN, ID, VITA_RAE, VITD, VITE, VITK,
THIA, RIBF, NIAEQ, VITB6, FOL, VITB12, VITC.

Not present in NEVO 2025/9.0 matrix: manganese, chromium, molybdenum,
pantothenic acid (B5), biotin (B7).

## Local setup

1. Ensure `NEVO2025_v9.0.csv` is under `services/nevo-service/src/main/resources/nevo/`
   (already the default classpath source).
2. Start `nevo-service`. On first boot (empty `nevo_food` table) it auto-imports
   the configured CSV. Restarts with data already present skip the import.
   Override with `NEVO_AUTO_IMPORT_ON_STARTUP=false` if needed; force reload via
   `POST /internal/nevo/import`.
3. Enable enrichment in food catalog:

```bash
NEVO_ESTIMATE_ENABLED=true
NEVO_SERVICE_URL=http://nevo-service:8080
```

## Matching rules

- Normalize product names: strip brand, pack size, marketing words; keep
  modifiers like skimmed/cooked/plant-based.
- Score: name similarity + category + macro similarity − modifier mismatch.
- Auto-apply `HIGH` and `MEDIUM` confidence; skip `LOW`.
- Prefer OFF values when present.

## Provenance

`product_nutrient` columns:

- `source` (`OFF` | `USDA_BRANDED` | `USDA_PROXY` | `NEVO_ESTIMATE` | `USER`)
- `source_ref` (barcode, FDC id, or NEVO code)
- `confidence` / `estimated` audit fields (`V7__product_nutrient_provenance`)
- `confidence` (`HIGH` | `MEDIUM` | `LOW`)
- `estimated` (boolean)

## Citation

> NEVO-online version 2025/9.0, RIVM, Bilthoven

## Debug: EAN 6413300019247 Melkunie Protein strawberry (2026-07-24)

Production product has macros from OFF plus many **zero** micros (`estimated=false`),
and **no** `NEVO_ESTIMATE` rows.

Root causes (layered):

1. **Match request too weak** — food-catalog sends only `name=Protein strawberry`
   (+ brand/ingredients). No OFF `generic_name` / categories (not stored on
   `Product`). NEVO then picks `Blancmange vanilla w strawberry sauce` at
   **LOW** confidence (`score≈0.42`) → auto-apply skips LOW.
2. **OFF zero micros block gaps** — OFF publishes `calcium=0`, `iron=0`, etc.
   Those codes are treated as “present”, so even a good NEVO match would not
   overwrite them. USDA `enrichIfSparse` also skips because micro count ≥ 6.
3. **Enrichment only on first OFF persist path** — existing barcode DB hits
   do not re-run NEVO/USDA; search upserts skip enrichment entirely.
4. **Deploy caveat** — if `NEVO_ESTIMATE_ENABLED` is still false in Railway,
   enrichment never runs (confirm env). Even when enabled, (1) alone fails this EAN.

Good NEVO target for this product is roughly `Quark low fat w fruit` (code 931)
/ `Quark low fat w fruit/vanilla w sweetener` (2246), not fruit blancmange.

Remaining follow-ups (match quality; not part of sparse re-enrich):

1. Pass OFF generic name + categories into NEVO match (and/or alias `kwark`→`quark`)
2. Prefer food-term NL→EN (e.g. `kwark`) over branded dessert names for Melkunie-like EANs

## Re-enrich sparse products (2026-07-24) — shipped

Fixes root causes (2) and (3) above:

- `MicroEnrichmentGate`: sparse = &lt;6 **filled** micros; OFF zeros are gaps;
  USDA/NEVO/USER rows always count as filled.
- Barcode lookup: skip cache when sparse; on DB hit run USDA then NEVO;
  NEVO replaces unfilled OFF zeros and evicts barcode cache after save.
- Admin `POST /api/admin/enrichment-backfill` runs NEVO after USDA using the
  same gate.

After deploy (with `NEVO_ESTIMATE_ENABLED=true`), re-lookup e.g.
`8718452513673` (Jumbo Franse Kwark) and optionally `6413300019247`.

## Debug: EAN 4056489801771 Milbona Choco Pudding + Today summary (2026-07-24)

Catalog (after barcode lookup) has macros + **19 estimated micros** (product id
`fff1314e-1cc1-464d-823e-263f319eaf23`). A **new** diary entry for that product
snapshots those micros and they appear in `GET /api/diary/summary`.

Why the Today summary can still show empty micros while the product page shows
them:

1. Diary **snapshots** nutrients at create time and never refreshes from catalog
   when enrichment is added later.
2. Product page / diary create use `GET /api/products/{id}`, which does **not**
   run `MicroEnrichmentGate` (only barcode lookup does). Search upsert also
   skips enrichment. So logging without a prior barcode enrich can snapshot a
   sparse product.

## Debug: EAN 8076800195057 Barilla Spaghetti + dashboard grid (2026-07-24)

Catalog has ~20 estimated micros after barcode lookup. New diary logs snapshot them
into `/api/diary/summary` correctly.

Dashboard bars are **percent of daily target** (`amount / target`), not raw amounts.
Production diary summary often returns `target: null` for every nutrient (diary →
user-profile goals call failing silently), so the SPA merges `/api/users/me/goals`
client-side. If that goals list is missing expanded micros (pre-V3 onboard) or the
profile age is outside the 19–50 DRV band, every micro bar stays at 0% and looks
empty even when pasta minerals are present.

Pasta vitamins are also mostly 0 estimates; minerals (Fe, Mg, …) carry the real
intake signal against DRVs (e.g. iron ~2 mg / 8 mg ≈ 25% for 100 g male).

Fixes: re-enrich sparse products on `GET /api/products/{id}`; backfill missing
micro DRVs on goals list; fall back to adult 19–50 references when age has no
band; harden diary goals client logging; wait for goals before rendering the
dashboard grid.

## Remaining / follow-up

### User lookup search (ULT-8) — shipped in search/UI

Colloquial vegetable queries (`paprika`, `courgette`, `aubergine`, …) return
ranked NEVO whole foods via public search and product-search merge. Diary
logging from a NEVO hit is **not** in that card.

### Diary materialization (follow-up card)

Materialize the selected NEVO food as a catalog `product` (`source=NEVO`,
`sourceRef=nevoCode`) on Add to diary, **or** extend diary to accept
`nevoCode` — track as a separate Linear card.


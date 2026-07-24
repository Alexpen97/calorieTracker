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
- `food-catalog-service` calls NEVO after OFF upsert and merges only **missing**
  micronutrient codes with provenance `NEVO_ESTIMATE`

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
2. Start `nevo-service`, then import:

```bash
curl -X POST http://localhost:8085/internal/nevo/import \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY"
```

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

- `source` (`OFF` | `NEVO_ESTIMATE`)
- `source_ref` (barcode or NEVO code)
- `confidence` (`HIGH` | `MEDIUM` | `LOW`)
- `estimated` (boolean)

## Citation

> NEVO-online version 2025/9.0, RIVM, Bilthoven

## Known limitations

- Estimates are generic-food composition, not branded lab assays.
- No live scrape of nevo-online.rivm.nl; local CSV only.
- If `nevo-service` is down, barcode lookup still succeeds without micros.
- Details / recipes / references companion CSVs are not imported (yet).

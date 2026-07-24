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

## Local setup

1. Place your NEVO-online CSV at `data/nevo.csv` (sample included for smoke tests).
2. Compose mounts it to `/data/nevo.csv` in `nevo-service`.
3. Import:

```bash
curl -X POST http://localhost:8085/internal/nevo/import \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY"
```

4. Enable enrichment in food catalog:

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

Imported data should be attributed as:

> NEVO-online version 2025/9.0, RIVM, Bilthoven

(Replace version with the actual CSV release you imported.)

## Known limitations

- Estimates are generic-food composition, not branded lab assays.
- No live scrape of nevo-online.rivm.nl; local CSV only.
- If `nevo-service` is down, barcode lookup still succeeds without micros.

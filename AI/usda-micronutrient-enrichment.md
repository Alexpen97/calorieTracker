# USDA micronutrient enrichment (2026-07-23)

## Goal

Fill missing vitamins/minerals on OFF products using USDA FoodData Central,
via a dedicated `nutrient-enrichment-service`. Estimates are fine for
generic proxies; fortified foods skip generic matching.

## Architecture

```
barcode → food-catalog (OFF upsert)
       → if sparse micros (<6): POST enrichment /internal/enrich
       → insert only-missing product_nutrient rows (source=USDA_*)
```

- Enrichment owns FDC client, rate limiting, 90-day `enrichment_lookup` cache
- food-catalog calls best-effort (`ENRICHMENT_ENABLED`, 3 s timeout, circuit breaker)
- No gateway route — internal only
- `product_nutrient.source`: `OFF` | `USDA_BRANDED` | `USDA_PROXY` | `USER`
- API DTO `estimated: true` → UI `≈` marker + footnote

## Matching

1. GTIN on FDC Branded
2. Name + brand Jaccard ≥ 0.5
3. Foundation → SR Legacy proxy ≥ 0.35 (unless fortified markers in name)

## Deploy

See `docs/railway-enrichment.md`. Local: Compose service
`nutrient-enrichment-service` + `CREATE DATABASE enrichment`.

# nutrient-enrichment-service

Fills missing vitamins/minerals on Open Food Facts products using USDA
FoodData Central (FDC). Called **internally** by `food-catalog-service`
(best-effort); not exposed via the gateway.

## Lookup ladder

1. GTIN / barcode on FDC Branded Foods
2. Name + brand (Branded), token Jaccard ≥ 0.5
3. Generic Foundation → SR Legacy proxy (unless fortified), Jaccard ≥ 0.35

Results (including `NONE`) are cached in PostgreSQL for 90 days.

## Port / env

| Variable | Purpose |
|---|---|
| `PORT` | Listen port (8080 in containers; 8086 local default) |
| `SPRING_DATASOURCE_URL` | JDBC URL for `enrichment` DB |
| `FDC_BASE_URL` | `https://api.nal.usda.gov/fdc/v1` |
| `FDC_API_KEY` | api.data.gov key (`DEMO_KEY` locally) |
| `INTERNAL_API_KEY` | Shared with food-catalog for `/internal/enrich` |

## API

`POST /internal/enrich` with header `X-Internal-Api-Key`.

# Railway — nutrient-enrichment-service

## New Railway service

| Setting | Value |
|---|---|
| Root directory | `services/nutrient-enrichment-service` |
| Dockerfile | service Dockerfile |
| Private networking | yes (no public domain) |
| Postgres | shared Postgres plugin / add database `enrichment` |

## Environment

| Variable | Notes |
|---|---|
| `PORT` | Railway sets automatically |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://…/enrichment` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | same as other services |
| `FDC_BASE_URL` | `https://api.nal.usda.gov/fdc/v1` |
| `FDC_API_KEY` | **secret** — free key from [api.data.gov](https://api.data.gov/signup/) |
| `INTERNAL_API_KEY` | must match food-catalog |

## food-catalog-service additions

| Variable | Value |
|---|---|
| `ENRICHMENT_ENABLED` | `true` |
| `ENRICHMENT_SERVICE_URL` | `http://nutrient-enrichment-service.railway.internal:8080` (or your private hostname) |
| `INTERNAL_API_KEY` | same as enrichment service |
| `ENRICHMENT_TIMEOUT` | optional, default `3s` |

## Notes

- Enrichment is **not** routed through the gateway.
- `DEMO_KEY` is OK for local Compose only (30 req/h); production needs a real key.
- After deploy, optional: `POST /api/admin/enrichment-backfill?page=0&size=50` (ADMIN JWT) to fill existing sparse products.

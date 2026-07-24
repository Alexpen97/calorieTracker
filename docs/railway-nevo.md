# Railway — nevo-service

## New Railway service

| Setting | Value |
|---|---|
| Root directory | `services/nevo-service` |
| Dockerfile | service Dockerfile |
| Private networking | yes (no public domain) |
| Postgres | shared Postgres plugin / add database `nevo` |

## Environment

| Variable | Notes |
|---|---|
| `PORT` | Railway sets automatically |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://…/nevo` |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | same as other services |
| `INTERNAL_API_KEY` | must match food-catalog |
| `NEVO_CSV_PATH` | optional; empty uses classpath `nevo/NEVO2025_v9.0.csv` |
| `NEVO_VERSION` | optional, default `2025/9.0` |
| `NEVO_AUTO_IMPORT_ON_STARTUP` | optional, default `true` — import CSV when `nevo_food` is empty |
| `LIBRETRANSLATE_ENABLED` / `LIBRETRANSLATE_URL` | see `docs/railway-libretranslate.md` |

## food-catalog-service additions

| Variable | Value |
|---|---|
| `NEVO_ESTIMATE_ENABLED` | `true` when ready (default `false`) |
| `NEVO_SERVICE_URL` | `http://nevo-service.railway.internal:8080` (or your private hostname) |
| `INTERNAL_API_KEY` | same as nevo-service |

## Post-deploy import

CSV ships in the JAR. On first boot with an empty `nevo` database the service
auto-imports it (see `NEVO_AUTO_IMPORT_ON_STARTUP`). Restarts skip when foods
already exist. Force a full reload with:

```bash
curl -X POST http://nevo-service.railway.internal:8080/internal/nevo/import \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY"
```

(From a one-off shell on the private network, or temporarily expose and revoke.)

## Notes

- NEVO is **not** routed through the gateway (internal match/import only).
- Enrichment order on barcode fetch: USDA first, then NEVO for remaining gaps.
- If `nevo-service` is down or disabled, barcode lookup still succeeds.
- Create the `nevo` database before the service’s first Flyway run.

See also: `docs/railway-deploy.md`, `AI/nevo-micronutrient-estimates.md`.

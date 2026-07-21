# Railway Phase 2 deploy notes

Extends Phase 1 (`docs/railway-phase1.md`) with `food-catalog-service`.

| Railway service | Root directory | Watch paths | Notes |
|---|---|---|---|
| `food-catalog-service` | `/services/food-catalog-service` | `/services/food-catalog-service/**` | Private + Postgres `food_catalog` + Redis |

## Additional variables

food-catalog-service:

- `SPRING_DATASOURCE_URL` / username / password (dedicated `food_catalog` DB)
- `REDIS_HOST` / `REDIS_PORT` (Railway Redis)
- `JWKS_URI=http://auth-service.railway.internal:8080/.well-known/jwks.json`
- `OFF_BASE_URL=https://world.openfoodfacts.org`
- `OFF_USER_AGENT=NutriTrack - Server - Version 0.1`
- `FOOD_REDIS_ENABLED=true`
- `PORT=8080`

gateway (add):

- `FOOD_SERVICE_URL=http://food-catalog-service.railway.internal:8080`

## Local Compose

```bash
docker compose --profile full up --build
```

Food routes via gateway: `/api/products/**`, `/api/nutrients/**`
Swagger includes food-catalog-service at `/api-docs/food`.

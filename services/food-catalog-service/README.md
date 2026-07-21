# food-catalog-service

Food catalog container — products, nutrition facts, nutrient education.
See `docs/calorie-tracker-architecture.md` §5.3 and `AI/phase-2-food-lookup.md`.

## Phase 2 scope

- Barcode lookup: Redis (TTL 24h) → PostgreSQL mirror → Open Food Facts API
  (Resilience4j rate limiter + circuit breaker + retry).
- Nutrient reference table with FR-9 education fields, seeded via Flyway.
- Read APIs: `GET /api/products/barcode/{ean}`, `GET /api/products/{id}`,
  `GET /api/nutrients`, `GET /api/nutrients/{code}`.

## Deferred

- Spring Batch OFF bulk import and PostgreSQL full-text search (Phase 4).
- User product submissions / moderation (Phase 4).

## Container

- Build: multi-stage `Dockerfile` (Maven → JRE 21). Standalone Maven project
  (Railway root directory = `/services/food-catalog-service`).
- Port: `8080` in Compose/Railway (`8083` locally by default).
- Database: PostgreSQL `food_catalog`; Redis for the hot product cache.

## Key environment variables

| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL `food_catalog` JDBC URL |
| `REDIS_HOST` / `REDIS_PORT` | Redis cache |
| `FOOD_REDIS_ENABLED` | `false` to use in-memory cache (tests) |
| `NUTRITRACK_FOOD_OFF_BASE_URL` | Open Food Facts API base (preferred Spring env name) |
| `OFF_BASE_URL` | Same value; resolved via `application.yml` placeholder |
| `NUTRITRACK_FOOD_OFF_USER_AGENT` / `OFF_USER_AGENT` | Required OFF User-Agent identifying this app |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |

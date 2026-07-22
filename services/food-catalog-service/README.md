# food-catalog-service

Food catalog container — products, nutrition facts, nutrient education,
search, submissions, and OFF bulk import.
See `docs/calorie-tracker-architecture.md` §5.3 and `AI/phase-4-mirror-search-submissions.md`.

## Scope

- Barcode lookup: Redis (TTL 24h) → PostgreSQL mirror → Open Food Facts API
  (Resilience4j rate limiter + circuit breaker + retry). Includes the caller's
  own pending/rejected submissions.
- Name search: local `search_document` (+ PostgreSQL GIN FTS) with OFF search
  fallback when local results are thin.
- User product submissions + moderation (`MODERATOR`/`ADMIN`).
- Spring Batch JSONL OFF bulk import (`POST /api/admin/off-import`, ADMIN).
- Nutrient reference table with FR-9 education fields, seeded via Flyway.

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
| `OFF_BASE_URL` | Open Food Facts API base |
| `OFF_USER_AGENT` | Required OFF User-Agent identifying this app |
| `OFF_BULK_IMPORT_ENABLED` | Enable scheduled bulk import (`false` by default) |
| `OFF_BULK_IMPORT_URL` | Default JSONL file path or HTTPS URL for import |
| `OFF_BULK_IMPORT_CRON` | Cron for scheduled import |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |

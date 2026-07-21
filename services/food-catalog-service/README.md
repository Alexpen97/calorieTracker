# food-catalog-service

Food catalog container — products, nutrition facts, nutrient education,
user submissions. See `docs/calorie-tracker-architecture.md` §5.3.

- Barcode lookup and full-text name search (Redis cache → local mirror →
  Open Food Facts API fallback with Resilience4j rate limiting).
- Spring Batch bulk import of Open Food Facts exports into the local mirror.
- Nutrient reference table incl. education content (FR-9) and DRV reference
  intakes (FR-12), seeded via Flyway.
- User product submissions in a separate `product_submission` staging table
  with moderator approval workflow (FR-8).

## Container

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project (Railway root directory = `/services/food-catalog-service`).
- Port: `8080` (private network only).
- Database: PostgreSQL `food_catalog`; Redis for the hot product cache.

## Key environment variables

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL `food_catalog` database |
| `REDIS_URL` | Redis cache |
| `OFF_BASE_URL` | Open Food Facts API base (`https://world.openfoodfacts.org`) |
| `OFF_IMPORT_CRON` | schedule for the bulk import job |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |

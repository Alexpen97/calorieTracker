# diary-service

Diary container — food log entries, water intake, daily summaries. See
`docs/calorie-tracker-architecture.md` §5.4.

- Consumption entries with denormalized per-100g nutrient snapshots (portion
  math server-side: `value_per_100g × weight_g / 100`).
- Water intake logging and daily totals vs. target (FR-11).
- Daily/weekly summaries with goal progress; internal usage-history endpoint
  for the recommendation service.

## Container

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project (Railway root directory = `/services/diary-service`).
- Port: `8080` (private network only).
- Database: PostgreSQL `diary` (Flyway migrations in this folder).

## Key environment variables

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL `diary` database |
| `FOOD_SERVICE_URL` | food-catalog-service (nutrition snapshots) |
| `USER_SERVICE_URL` | user-profile-service (targets for summaries) |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |

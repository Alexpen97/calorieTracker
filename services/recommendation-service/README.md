# recommendation-service

Recommendation container — meal and cooking advice (FR-13). See
`docs/calorie-tracker-architecture.md` §5.6.

**Status: stub — not implemented and not deployed until Phase 6.**

- Will score candidate products/recipe templates by how well they fill the
  day's remaining nutrient budget, preferring the user's frequently logged
  products. Rule-based in v1 (no ML).
- Reads over internal APIs only (diary-service usage history and daily totals,
  user-profile-service goals, food-catalog-service nutrition facts); owns no
  data initially.

## Container (planned)

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project (Railway root directory = `/services/recommendation-service`).
- Port: `8080` (private network only).

## Key environment variables (planned)

| Variable | Purpose |
|---|---|
| `DIARY_SERVICE_URL` | usage history + current-day totals |
| `USER_SERVICE_URL` | nutrient targets |
| `FOOD_SERVICE_URL` | candidate product nutrition facts |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |

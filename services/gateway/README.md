# gateway

API Gateway container — Spring Cloud Gateway. See `docs/calorie-tracker-architecture.md` §5.1.

- Single public entry point; routes `/api/**` to the downstream services.
- Validates JWTs (OAuth2 resource server against auth-service JWKS).
- Hosts the aggregated Swagger UI at `/swagger-ui.html` (proxies each
  service's `/v3/api-docs` under `/api-docs/{service}`).
- Cross-cutting: CORS, per-user rate limiting, request logging.

## Container

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project; must build from this folder alone (Railway root directory =
  `/services/gateway`).
- Port: `8080` (public on Railway; exposed via Compose locally).

## Key environment variables

| Variable | Purpose |
|---|---|
| `AUTH_SERVICE_URL` | e.g. `http://auth-service:8080` / `http://auth-service.railway.internal:8080` |
| `USER_SERVICE_URL` | user-profile-service base URL |
| `FOOD_SERVICE_URL` | food-catalog-service base URL |
| `DIARY_SERVICE_URL` | diary-service base URL (**required on Railway** — unset/empty breaks `/api/diary/**` with gateway 500) |
| `RECO_SERVICE_URL` | recommendation-service base URL (optional until Phase 6) |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |
| `CORS_ALLOWED_ORIGINS` | SPA + Capacitor origins (`https://localhost` for Android WebView) |
| `SWAGGER_UI_ENABLED` | enable/restrict Swagger UI per environment |

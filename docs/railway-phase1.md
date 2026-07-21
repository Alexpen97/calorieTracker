# Railway Phase 1 deploy notes

Phase 1 services to create in one Railway project (public domains only on
`gateway` and `frontend`):

| Railway service | Root directory | Watch paths | Notes |
|---|---|---|---|
| `gateway` | `/services/gateway` | `/services/gateway/**` | Public HTTPS |
| `auth-service` | `/services/auth-service` | `/services/auth-service/**` | Private network |
| `user-profile-service` | `/services/user-profile-service` | `/services/user-profile-service/**` | Private network + Postgres |
| `frontend` | `/frontend` | `/frontend/**` | Public HTTPS |

## Shared / per-service variables

Common:

- `INTERNAL_API_KEY` — shared between auth-service and user-profile-service
- `AUTH_MODE=dev` until Google OAuth credentials are ready; then `prod`
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` on auth-service (+ client id on frontend build args)

auth-service:

- `USER_SERVICE_URL=http://user-profile-service.railway.internal:8080`
- `PORT=8080`

user-profile-service:

- `SPRING_DATASOURCE_URL` / username / password from Railway Postgres (JDBC form)
- `JWKS_URI=http://auth-service.railway.internal:8080/.well-known/jwks.json`
- `INTERNAL_API_KEY`

gateway:

- `AUTH_SERVICE_URL=http://auth-service.railway.internal:8080`
- `USER_SERVICE_URL=http://user-profile-service.railway.internal:8080`
- `JWKS_URI=http://auth-service.railway.internal:8080/.well-known/jwks.json`
- `CORS_ALLOWED_ORIGINS=https://<frontend-domain>`
- `SWAGGER_UI_ENABLED=true` (restrict later)

frontend build args:

- `VITE_API_BASE_URL=https://<gateway-domain>` (or empty if same-origin nginx proxy is used)
- `VITE_GOOGLE_CLIENT_ID`
- `VITE_AUTH_MODE`

## Local Compose

```bash
cp .env.example .env
docker compose --profile deps up -d          # postgres + redis only
docker compose --profile full up --build     # full Phase 1 stack
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
App: `http://localhost/` (Compose) or Vite `http://localhost:5173`

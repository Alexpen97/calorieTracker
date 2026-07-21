# Railway Phase 3 deploy notes

Extends Phase 2 (`docs/railway-phase2.md`) with `diary-service` and user-profile
goals/weight APIs (same `user-profile-service` deployable).
Infrastructure remains `infra/postgres` + `infra/redis` — see `docs/railway-deploy.md`.

| Railway service | Root directory | Watch paths | Notes |
|---|---|---|---|
| `diary-service` | `/services/diary-service` | `/services/diary-service/**` | Private + Postgres `diary` |
| `user-profile-service` | `/services/user-profile-service` | `/services/user-profile-service/**` | Already deployed; ships weight + goals |

## Additional variables

diary-service:

- `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.railway.internal:5432/diary`
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` (match `postgres` service)
- `JWKS_URI=http://auth-service.railway.internal:8080/.well-known/jwks.json`
- `FOOD_SERVICE_URL=http://food-catalog-service.railway.internal:8080`
- `USER_SERVICE_URL=http://user-profile-service.railway.internal:8080`
- `PORT=8080`

gateway (add):

- `DIARY_SERVICE_URL=http://diary-service.railway.internal:8080`

## Local Compose

```bash
docker compose --profile full up --build
```

Diary routes via gateway: `/api/diary/**`
Swagger includes diary-service at `/api-docs/diary`.

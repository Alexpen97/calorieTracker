# calorieTracker (NutriTrack)

Nutrition tracker monorepo — Spring Boot microservices + React SPA.

See `docs/calorie-tracker-architecture.md` for the design and phased roadmap.

## Phase 1 (walking skeleton)

Runnable pieces:

- `services/gateway` — Spring Cloud Gateway + aggregated Swagger UI
- `services/auth-service` — Google/dev login → RS256 JWT + JWKS
- `services/user-profile-service` — profile upsert + `/api/users/me`
- `frontend` — React login + profile
- `docker-compose.yml` — local orchestration (`deps` / `full` profiles)

```bash
# Backend tests
(cd services/auth-service && mvn test)
(cd services/user-profile-service && mvn test)
(cd services/gateway && mvn test)

# Frontend
(cd frontend && npm install && npm test && npm run build)

# Full stack (requires Docker)
cp .env.example .env
docker compose --profile full up --build
```

Railway deploy notes: `docs/railway-phase1.md`.

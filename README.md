# calorieTracker (NutriTrack)

Nutrition tracker monorepo — Spring Boot microservices + React SPA.

See `docs/calorie-tracker-architecture.md` for the design and phased roadmap.

## Current phases

**Phase 1 — walking skeleton:** gateway, auth, user-profile, React login.

**Phase 2 — food lookup:** `food-catalog-service` (OFF barcode lookup + Redis +
nutrient education), product detail UI, web barcode scan. Notes:
`AI/phase-2-food-lookup.md`, Railway: `docs/railway-phase2.md`.

```bash
# Backend tests
(cd services/auth-service && mvn test)
(cd services/user-profile-service && mvn test)
(cd services/food-catalog-service && mvn test)
(cd services/gateway && mvn test)

# Frontend
(cd frontend && npm install && npm test && npm run build)

# Full stack (requires Docker)
cp .env.example .env
docker compose --profile full up --build
```

Railway: `docs/railway-phase1.md`, `docs/railway-phase2.md`.

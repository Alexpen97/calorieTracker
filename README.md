# calorieTracker (NutriTrack)

Nutrition tracker monorepo — Spring Boot microservices + React SPA (web **and**
Android via Capacitor). Deploys to Railway (Docker) or Kubernetes (k8s).

Design & phased roadmap: `docs/calorie-tracker-architecture.md`.

## Status

All planned phases are **shipped and deployed**. Linear trackers (issue keys
`ULT-6`…`ULT-13`) are closed under the `UltimateConcept` team, project
`CalorieTracker`.

- **Phase 1 — Walking skeleton:** gateway, auth (Google OIDC + PKCE), user-profile, React login.
- **Phase 2 — Food lookup:** food-catalog (OFF barcode lookup + Redis cache + nutrient education), product detail UI, web barcode scan.
- **Phase 3 — Diary & tracking:** diary-service (portion math, water intake), body-weight log, goals engine (Mifflin-St Jeor + DRV), daily summary UI.
- **Phase 4 — Mirror, search & submissions:** OFF bulk import (Spring Batch), full-text search, user product submissions + moderation.
- **Phase 5 — Android:** Capacitor shell (`com.nutritrack.app`), ML Kit barcode scan, native Google Sign-In, secure storage.
- **Enrichment:** USDA FoodData Central + local NEVO CSV fill missing micronutrients via `nutrient-enrichment-service` / `nevo-service`.
- **Translation:** optional NL→EN name translation via `libretranslate`.

Domain features shipped after the phases (tracked in Linear as ULT issues):

| Key | Feature |
|---|---|
| ULT-6 | Auto-select breakfast/lunch/dinner based on current time when adding food |
| ULT-7 | Track frequently added products + quick-add card in add-food view |
| ULT-8 | Colloquial-food search (e.g. "paprika" → vegetables) via NEVO ranking |
| ULT-9 | Automatic gram↔ml conversion from selected product density |
| ULT-10 | Count-based (pieces) entry instead of grams where possible |
| ULT-11 | Product name search: relevance ranking, fuzzy matching, token handling |
| ULT-12 | Server-driven in-app update message (opens once per user per push) |
| ULT-13 | In-app user feedback from Settings (Accepted/Pending/Completed status) |

## Architecture

```
calorieTracker/
├── docker-compose.yml          # full local stack (deps + all services + frontend)
├── docker-compose.deploy.yml   # production-style compose (for Operator/Railway-like hosts)
├── k8s/                        # Kubernetes manifests (base + environment overlays)
├── docs/                       # architecture, railway, deployment, design docs
├── AI/                         # working notes, plans, debugging write-ups
├── scripts/                    # tooling (seeds, OFF import triggers, etc.)
├── services/
│   ├── gateway/                     # Spring Cloud Gateway + aggregated Swagger UI
│   ├── auth-service/                # Google OIDC login, JWT issuing (RS256, JWKS)
│   ├── user-profile-service/        # profile, body weight, goals engine
│   ├── food-catalog-service/        # products, nutrient education, submissions, search
│   ├── diary-service/               # food entries, water intake, daily summaries
│   ├── nutrient-enrichment-service/ # USDA FoodData Central micronutrient fill
│   ├── nevo-service/                # NEVO CSV micronutrient estimates (NL)
│   ├── libretranslate/              # optional NL→EN name translation
│   └── recommendation-service/      # (stub — Phase 6 meal advice)
├── frontend/                   # React SPA (nginx) + Capacitor Android shell
└── infra/                      # local Compose infra (postgres init SQL, redis config)
```

Backend: Java 21, Spring Boot microservices; PostgreSQL (one schema per
service) + Redis cache. Frontend: React 19 + TypeScript + Vite + TanStack
Query, packaged for Android with Capacitor.

## Local development

```bash
# Backend tests
(cd services/auth-service && mvn test)
(cd services/user-profile-service && mvn test)
(cd services/food-catalog-service && mvn test)
(cd services/diary-service && mvn test)
(cd services/gateway && mvn test)

# Frontend
(cd frontend && npm install && npm test && npm run build)

# Full stack (requires Docker)
cp .env.example .env
docker compose --profile full up --build
```

## Deployment

- **Railway** (full stack): `docs/railway-deploy.md`, plus per-phase notes
  (`docs/railway-phase1.md` … `railway-phase4.md`, `railway-nevo.md`,
  `railway-enrichment.md`, `railway-libretranslate.md`).
- **Kubernetes** (Rancher/other): `docs/rancher-deployment.md`, manifests in `k8s/`.
- **Android / Play Store**: `docs/android-play-store.md`, `docs/compile-frontend-to-app.md`.
- **Google OAuth**: `docs/google-oauth-setup.md`.

## Other docs

- Architecture & design: `docs/calorie-tracker-architecture.md`
- Design mockups: `docs/design/mockups/`
- Feature plans & specs: `docs/superpowers/plans/`, `docs/superpowers/specs/`
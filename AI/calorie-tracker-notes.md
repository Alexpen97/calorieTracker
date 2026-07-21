# Calorie Tracker (NutriTrack) — AI work notes

## Completed

- 2026-07-21: Created the initial architecture/design document at
  `docs/calorie-tracker-architecture.md` (branch
  `cursor/calorie-tracker-architecture-4fd9`). No code exists yet; the repo
  currently only contains Context7 tooling.

## Key decisions recorded in the design doc

- Backend: Spring Boot microservices — `gateway` (Spring Cloud Gateway),
  `auth-service`, `food-catalog-service`, `diary-service`,
  `user-profile-service`; PostgreSQL per service + Redis cache.
- Food data: Open Food Facts (ODbL). Verified via Context7
  (`/websites/openfoodfacts_github_io_openfoodfacts-server_api`): API v2
  product-by-barcode endpoint, `nutriments` field, rate limits of 15 product
  reads/min and 10 searches/min per IP → design uses a local mirror populated
  by Spring Batch bulk imports plus Redis caching.
- Nutrition stored as key-value `product_nutrient` rows (canonical nutrient
  codes) so vitamins/minerals coverage is open-ended.
- Diary entries snapshot per-100g nutrient values at logging time (OFF data is
  mutable).
- Frontend: React + TypeScript, wrapped with Capacitor for the Android app;
  barcode scanning via `BarcodeDetector`/ZXing on web and ML Kit on Android.
- Auth: Google OIDC Authorization Code + PKCE; `auth-service` issues its own
  RS256 JWTs (JWKS endpoint); all services act as OAuth2 resource servers.

## Remaining work / TODOs

- Everything in the roadmap (design doc §13), starting with Phase 1:
  Maven multi-module skeleton, gateway, auth-service with Google login,
  user-profile-service, React app with login.
- Resolve open questions in design doc §14 (offline mode, recipes, secondary
  USDA source, household units) before the relevant phases.

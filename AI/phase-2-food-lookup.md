# Phase 2 — Food lookup (Implementation 2)

## Goal

Deliver the Phase 2 roadmap item from `docs/calorie-tracker-architecture.md` §14:

- `food-catalog-service` with Open Food Facts live barcode lookup
- Redis hot cache (TTL ~24 h) → PostgreSQL mirror → OFF API fallback
- Nutrient reference table with FR-9 education content (Flyway seed)
- Gateway routes + Compose wiring for the food service
- React UI: manual barcode entry, web camera barcode scan, product detail,
  nutrient education sheets

## Out of scope (later phases)

- Spring Batch OFF bulk import and PostgreSQL full-text search (Phase 4)
- User product submissions / moderation (Phase 4)
- Capacitor / ML Kit (Phase 5)
- Diary, water, weight, goals engine (Phase 3)

## Stack

- Java 21, Spring Boot **4.1.0** (match Phase 1 services)
- Spring Data JPA + Flyway + PostgreSQL (`food_catalog`)
- Spring Data Redis (Lettuce) for product cache
- Resilience4j 2.2 RateLimiter + CircuitBreaker + Retry around OFF client
- RestClient for OFF HTTP (User-Agent required)
- React 19 + Vite + Vitest; `BarcodeDetector` when available, else manual entry

## Lookup path

1. `GET /api/products/barcode/{ean}` (JWT required)
2. Redis key `product:barcode:{ean}`
3. Local `product` row by barcode
4. OFF `GET {base}/api/v2/product/{ean}?fields=...` (rate-limited ≤12/min)
5. Normalize nutriments → persist product + `product_nutrient` → cache → return

## Test plan

- Unit: OFF nutriment normalizer golden fixtures
- Integration (MockMvc + H2, Redis disabled / in-memory cache): barcode miss→OFF
  mock→persist; cache hit; 404 when OFF status=0; `/api/nutrients` seed content
- Frontend: Vitest for barcode sanitize helper + nutrient sheet render

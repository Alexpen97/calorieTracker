# user-profile-service

User profile container — profile, body-weight log, goals engine. See
`docs/calorie-tracker-architecture.md` §5.5.

- Owns the user record (Google sub, email, role) and physical profile (sex,
  height, age, activity level, objective).
- Body-weight tracking (append-only log with optional delete of own entries, FR-10).
- Goals engine (FR-12): computes suggested nutrient/water targets from sex,
  weight, height, age, and activity (Mifflin-St Jeor, DRV reference intakes,
  ~35 ml/kg water); preserves user overrides.
- Onboarding: `POST /api/users/me/onboarding` saves height/weight/diet goal
  (plus sex, birth date, activity) and applies computed nutrient goals.
- Weight API: `POST/GET /api/users/me/weight`, `DELETE /api/users/me/weight/{id}`.
- Feedback API: `POST/GET /api/users/me/feedback` (user submissions with
  Pending/Accepted/Completed status); moderators/admins update status via
  `PATCH /api/users/feedback/{id}/status`.

## Container

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project (Railway root directory = `/services/user-profile-service`).
- Port: `8080` (private network only).
- Database: PostgreSQL `users` (Flyway migrations in this folder).

## Key environment variables

| Variable | Purpose |
|---|---|
| `DATABASE_URL` | PostgreSQL `users` database |
| `JWKS_URI` | auth-service JWKS endpoint for JWT validation |
| `FOOD_SERVICE_URL` | food-catalog-service (nutrient reference-intake data) |

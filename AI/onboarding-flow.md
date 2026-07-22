# User onboarding flow

## Goal

First-run flow for new NutriTrack users: collect weight, height, and dieting
goal, then calculate and persist daily nutrient targets via the existing
goals engine (FR-12).

## Delivered (2026-07-22)

Branch: `cursor/user-onboarding-flow-80e1`

### Backend (`user-profile-service`)

- `POST /api/users/me/onboarding` — atomic profile update + weight log +
  `goals/recalculate?apply=true`
- Request requires: `heightCm`, `weightKg`, `objective`, plus `sex`,
  `birthDate`, `activityLevel` (needed by Mifflin-St Jeor / DRV lookup)
- Response: `{ profile, weight, needsProfile, goals }`
- Tests in `UserControllerTest` (happy path + validation)

### Frontend

- `/onboarding` multi-step wizard:
  1. Height + weight
  2. Dieting goal + activity + sex + birth date → calculate
  3. Show computed nutrient goals → dashboard
- Route guards: incomplete profiles redirect into onboarding; completed
  profiles skip it
- Auth callback / dev login land on `/onboarding` (guard sends finished
  users to `/today`)
- Helpers: `needsOnboarding`, `completeOnboarding` API client

## Notes

- Sex, birth date, and activity are collected in step 2 because the goals
  engine cannot compute energy/protein/water without them.
- Weight history cache is updated only when leaving the results step so the
  guard does not bounce the user off the goals summary.
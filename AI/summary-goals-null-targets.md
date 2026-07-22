# Summary targets missing in production

## Symptom (2026-07-22)

Today Summary showed consumed amounts (e.g. `2333` kcal, `233 g` protein) but
never `amount / goal`. Network `/api/diary/summary` returned nutrient `target: null`
(and water `targetMl: null`) even after onboarding goals existed and
`GET /api/users/me/goals` worked via the gateway.

## Root cause

Diary `SummaryService.loadTargets` calls user-profile over `USER_SERVICE_URL`.
Any failure is swallowed and summary continues with null targets. Likely causes
on Railway:

1. Service-to-service goals call fails (bad `USER_SERVICE_URL` hostname, 401, etc.)
2. Jackson 3 deserialization of the goals payload (extra `origin` / `computedAt`,
   `Instant`) failing under Boot 4 defaults (`FAIL_ON_UNKNOWN_PROPERTIES`)

## Fix

- Narrow `UserGoalResponse` to `nutrientCode` / `dailyTarget` / `unit`
- Set `spring.jackson.deserialization.fail-on-unknown-properties: false` on diary
- Log a warning when goals cannot be loaded
- Frontend merges `fetchGoals()` into day summaries on Dashboard / Diary so the
  UI still shows goals when the diary hop fails

## Verify

1. Redeploy **diary-service** + **frontend**
2. Profile → Daily goals still lists targets
3. Today Summary shows `consumed / goal`
4. If summary targets are still null, diary logs:
   `Diary summary could not load user goals`

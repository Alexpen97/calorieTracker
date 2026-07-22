# Fix: weight Instant from/to + blank calorie goals

## Problem

`user-profile-service` logged:

```
MethodArgumentTypeMismatchException: Method parameter 'from': Failed to convert
value of type 'java.lang.String' to required type 'java.time.Instant'
for value [2026-07-16]
```

Analytics passed LocalDate strings (`YYYY-MM-DD`) into `GET /api/users/me/weight`,
which expects Instant (`2026-07-16T00:00:00Z`). That request failed and shared the
React Query key `['weight-history']` with Today / Diary / Profile / onboarding
guards, so the error poisoned weight history app-wide and
`RequireOnboardingComplete` blocked the dashboard (calorie goals included).

## Fix

1. Frontend `fetchWeightHistory` converts LocalDate → Instant query params
   (`from` start-of-day UTC, `to` end-of-day UTC); Instant strings pass through.
2. Analytics uses query key `['weight-history', from, to]` so filtered fetches
   cannot overwrite the unfiltered cache.
3. Backend `WebConfig` accepts date-only Instant params as start-of-day UTC
   (defense in depth).

## Tests

- `frontend/src/api/client.weightHistory.test.ts`
- `AnalyticsPage.test.tsx` asserts LocalDate range is passed into the client
- `UserControllerTest.getWeightFiltersByLocalDateRangeParams`

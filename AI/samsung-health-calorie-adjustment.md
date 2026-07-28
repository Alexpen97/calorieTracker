# Samsung Health calorie adjustment

## Goal

Use Samsung Health calories burned on Android to dynamically adjust the daily
calorie target while showing the burn as a separate number on the existing Today
Summary donut.

## Plan

See `docs/superpowers/plans/2026-07-25-samsung-health-calorie-adjustment.md`.

## Key decisions

- Burned calories are a daily summary adjustment, not a permanent change to
  `energy_kcal` goals in `user-profile-service`.
- The effective calorie target is:
  `baseEnergyTarget + burnedCalories`.
- The existing `energy_kcal.target` remains the base plan target in API
  responses. Optional `energyAdjustment` carries provider, burned calories,
  base target, effective target, and sync timestamp.
- Dashboard calorie progress uses the effective target. The donut shows a faded
  teal adjustment arc for `burned / effective`, plus a separate `+N burned`
  label under the center amount.
- Unsupported web, denied permissions, disconnected Samsung Health, missing SDK
  AAR, and sync failures preserve the current summary behavior.

## Implementation status

Implemented on `cursor/samsung-health-calorie-adjustment-c394`:

### Backend (diary-service)

- Flyway `V2__health_activity_daily.sql` for daily activity + connection state
- `POST/GET/DELETE /api/integrations/samsung-health/*` sync/status/disconnect
- `SummaryService` attaches `energyAdjustment` when burn data exists
- Feature gate: `SAMSUNG_HEALTH_ENABLED` / `nutritrack.diary.samsung-health-enabled`
- Gateway route `diary-integrations` for `/api/integrations/**`

### Frontend

- `buildCalorieDisplayState` + `NestedCalorieMacroRing` adjustment arc/label
- Settings → Integrations section for connect/sync/disconnect
- Platform adapter `frontend/src/platform/samsungHealth.ts`
- Feature gate: `VITE_SAMSUNG_HEALTH_ENABLED` (enabled unless set to `false`)

### Android

- Capacitor plugin `SamsungHealthPlugin` reads `Exercise.CALORIE` via reflection
- Compiles without the proprietary Samsung Health SDK AAR
- Live reads require partner approval + SDK AAR on the classpath
- Manifest declares `com.samsung.health.exercise` read permission metadata

## External docs checked

- Samsung Health Android docs confirm `HealthConstants.Exercise.CALORIE` is
  burned calorie during the activity in kilocalories.
- Runtime permissions use `HealthPermissionManager.requestPermissions`.

## Remaining / ops

- Add Samsung Health partner approval and SDK AAR before production live sync
- Manual device QA on a Samsung phone with Samsung Health installed
- Optional later: recalculate macros from exercise burn
- Privacy: disconnect deletes stored daily burn rows for the user/provider

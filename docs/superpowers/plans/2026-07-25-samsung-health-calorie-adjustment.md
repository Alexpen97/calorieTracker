# Samsung Health calorie adjustment implementation plan

> For agentic workers: steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Integrate Samsung Health on Android so the app can read calories burned for the
selected day, increase the user's effective calorie target by that burn amount,
and display the burned-calorie adjustment as a separate value on the Today
Summary donut. The adjustment should share the same calorie donut as a faded
background segment in a distinct color, while keeping consumed calories as the
primary foreground progress.

## Current architecture

- The Android app is a Capacitor wrapper around the existing React/Vite SPA.
  Native concerns currently live behind platform adapters, with web behavior
  preserved.
- `user-profile-service` owns computed goals. `GoalsEngine` computes
  `energy_kcal` from Mifflin-St Jeor, activity level, and objective, and
  `GoalsService` persists computed or user-overridden goals.
- `diary-service` owns daily summaries. `SummaryService` currently aggregates
  diary nutrients and water, loads user goals through `UserGoalsClient`, and
  returns each nutrient as `{ code, amount, unit, target }`.
- The dashboard fetches `/api/diary/summary`, merges missing goals from
  `/api/users/me/goals`, and renders `energy_kcal` through
  `NestedCalorieMacroRing`.

## External API facts to verify during implementation

Context7 resolved the current Samsung Health Android docs to
`/websites/developer_samsung_health_android`. The docs show:

- Samsung Health SDK for Android requires user-controlled data permissions.
- Apps declare health data permissions in Android manifest metadata.
- Runtime permission prompts use `HealthPermissionManager.requestPermissions`.
- Exercise data access uses `HealthConstants.Exercise.HEALTH_DATA_TYPE`
  (`com.samsung.health.exercise`).

The exact burned-calorie column was not present in the returned Context7
excerpts. Treat the Samsung field mapping as the first implementation spike:
confirm whether total exercise calories, active calories, or another Samsung
field is the correct source. Do not commit a guessed constant.

## Product decisions

- Use Samsung Health burned calories as an additive daily adjustment:
  `effectiveEnergyTarget = baseEnergyTarget + burnedCalories`.
- Preserve the base goal as the user's real plan target. The burn adjustment is
  a daily summary overlay, not a permanent goal recalculation.
- Display three calorie numbers in the Today Summary area:
  - consumed calories
  - base target
  - burned adjustment, e.g. `+320 burned`
- The main foreground calorie arc remains `consumed / effectiveEnergyTarget`.
- The faded background arc shows `effectiveEnergyTarget / baseEnergyTarget`
  context using a separate color, capped visually at 100% of the donut track
  unless design explicitly chooses an overflow treatment.
- If Samsung Health is disconnected, permission-denied, unsupported, or sync
  fails, return `burnedCalories: null` and preserve the current UI.

## Data model

Create a new integration-owned daily activity table rather than modifying user
goals:

```text
health_activity_daily
- id uuid primary key
- user_id uuid not null
- provider varchar not null -- SAMSUNG_HEALTH
- local_date date not null
- zone_id varchar not null
- active_energy_kcal numeric null
- total_energy_kcal numeric null
- selected_burn_kcal numeric not null
- source_record_count int not null
- synced_at timestamptz not null
- permission_state varchar not null
- unique(user_id, provider, local_date, zone_id)
```

Keep both raw candidate energy totals when available, but expose one selected
burn value to diary summaries. This makes the Samsung field decision auditable.

## API changes

### New integration endpoints

- `GET /api/integrations/samsung-health/status`
  - Returns support, connection, permission, last sync, and latest error state.
- `POST /api/integrations/samsung-health/sync`
  - Android/native only. Accepts daily burned-calorie totals collected from
    Samsung Health for one or more local dates.
- `DELETE /api/integrations/samsung-health`
  - Disconnects the provider and removes local cached activity data.

Do not send Samsung Health credentials to the backend. The native app reads
Samsung Health locally after user consent, then posts normalized daily totals
with the existing bearer token.

### Diary summary response extension

Extend `DaySummary` with an optional energy adjustment object:

```json
{
  "energyAdjustment": {
    "provider": "SAMSUNG_HEALTH",
    "burnedCalories": 320,
    "baseTarget": 2100,
    "effectiveTarget": 2420,
    "syncedAt": "2026-07-25T16:30:00Z"
  }
}
```

For `energy_kcal`, keep `target` as the base target for backward compatibility.
Frontend helpers can use `energyAdjustment.effectiveTarget` for calorie progress
only. Macro targets remain based on the base plan unless a later nutrition
decision explicitly recalculates macros from exercise burn.

## Implementation tasks

### Task 1: Samsung Health spike and native adapter

- [x] Confirm Samsung Health SDK setup requirements, partner approval status,
      Gradle dependency, manifest metadata, and ProGuard/R8 rules.
- [x] Confirm the exact burned-calorie field mapping and units.
- [x] Add `frontend/src/platform/samsungHealth.ts` with web no-op and Android
      native implementation shape.
- [x] Add Android native plugin/bridge code under `frontend/android/app` or a
      local Capacitor plugin module, matching the current platform-adapter
      pattern.
- [x] Add Vitest coverage for unsupported web, permission-denied, and successful
      sync adapter behavior.

### Task 2: Integration service boundary

- [x] Add a small integration service or package that owns Samsung Health daily
      activity persistence.
- [x] Add Flyway migration for `health_activity_daily`.
- [x] Add controller endpoints for status, sync, and disconnect.
- [x] Validate that synced records belong to the authenticated user and dates
      are local dates with explicit `zone`.
- [x] Add JUnit/MockMvc tests for sync upsert, status, disconnect, auth, and
      invalid payloads.

### Task 3: Diary summary calorie adjustment

- [x] Add an `ActivityEnergyClient` for diary-service to load daily burn totals.
- [x] Extend `SummaryService.DailySummary` with `energyAdjustment`.
- [x] Compute `baseTarget` from `energy_kcal.target`, `burnedCalories` from the
      activity client, and `effectiveTarget = baseTarget + burnedCalories`.
- [x] On activity-client failure, log and return the current summary with no
      adjustment, mirroring current null-target behavior.
- [x] Add diary-service tests for adjusted target, no burn, no energy target,
      range summaries, and integration failure.

### Task 4: Frontend data and dashboard UI

- [x] Extend `DaySummary` in `frontend/src/api/client.ts`.
- [x] Add a helper in `nutritionDashboard.ts` to derive calorie display state:
      consumed, base target, burned adjustment, effective target, primary
      percent, adjustment percent.
- [x] Extend `NestedCalorieMacroRing` props with optional adjustment segment and
      adjustment label.
- [x] Render the adjustment as a faded distinct-color background arc on the same
      SVG donut, behind the consumed indicator.
- [x] Show the separate burn number near the donut center or immediately under
      it, e.g. `+320 burned`, without replacing `1,450 / 2,420`.
- [x] Preserve current behavior when `energyAdjustment` is absent.
- [x] Add `MiniCharts`, `nutritionDashboard`, and `DashboardPage` tests.

### Task 5: Settings and sync UX

- [x] Add a Samsung Health row under settings/profile integrations.
- [x] Show unsupported, disconnected, permission required, connected, syncing,
      and last synced states.
- [x] On Android, guide the user through Samsung permission request, then sync
      today's and recent days' activity.
- [x] On web, explain that Samsung Health sync requires the Android app.
- [x] Add tests for settings integration states.

### Task 6: Documentation and rollout

- [x] Update `AI/samsung-health-calorie-adjustment.md` as work progresses.
- [x] Update Android setup docs with Samsung Health SDK setup, partner approval,
      manifest permissions, and test-device requirements.
- [x] Add production notes for privacy, consent, data retention, and disconnect.
- [x] Gate the feature with `VITE_SAMSUNG_HEALTH_ENABLED` and backend config
      until Samsung field mapping and partner approval are verified.

## Testing plan

- Unit tests:
  - calorie adjustment helper math
  - ring adjustment rendering and accessible labels
  - Samsung Health adapter unsupported/permission/success states
  - backend daily activity normalization
- Service tests:
  - integration sync/status/disconnect endpoints
  - diary summary adjustment and fallback paths
  - gateway route validation if a new service is introduced
- Manual Android tests:
  - fresh install on Samsung device with Samsung Health installed
  - permission denied path
  - permission granted with activity for today
  - disconnect path removes adjustment from summary
- Regression tests:
  - web dashboard still renders with no `energyAdjustment`
  - empty-day summary still shows base goals
  - user goal overrides are not modified by burned calories

## Open questions

- Does Samsung require partner approval before reading exercise energy in our
  target distribution channel?
- Which Samsung metric best matches product language: active energy, total
  exercise calories, or total daily calories burned?
- Should exercise burn increase only calorie target, or eventually update macro
  targets by preserving macro ratios?
- How many previous days should the Android app backfill on first connection?
- Should users be able to disable dynamic calorie adjustment while keeping the
  Samsung Health connection active?

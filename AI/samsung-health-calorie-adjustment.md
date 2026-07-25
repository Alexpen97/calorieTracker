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
- The existing `energy_kcal.target` should remain the base plan target in API
  responses. A new optional `energyAdjustment` object should carry provider,
  burned calories, base target, effective target, and sync timestamp.
- The dashboard should use the effective target for calorie progress while
  separately displaying the burned amount, e.g. `+320 burned`.
- The donut should keep the consumed-calorie foreground arc and add a faded
  distinct-color adjustment arc behind it.
- Unsupported web, denied permissions, disconnected Samsung Health, and sync
  failures should preserve the current summary behavior.

## External docs checked

- Context7 MCP was quota-limited in this run.
- The project fallback `scripts/context7.sh` resolved
  `/websites/developer_samsung_health_android`.
- Current Samsung Health Android docs confirm manifest-declared health data
  permissions, runtime `HealthPermissionManager.requestPermissions`, and
  exercise data access through `HealthConstants.Exercise.HEALTH_DATA_TYPE`.
- The exact burned-calorie field was not present in the returned Context7
  excerpts; implementation should verify the Samsung field mapping before
  writing native code.

## Status

Planning only. No runtime behavior has been implemented yet.

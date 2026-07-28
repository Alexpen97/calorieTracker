# Dev account data seeder

Recorded 2026-07-22. Updated 2026-07-28 for vitamin/mineral demo panels.

## What it does

`scripts/seed-dev-data.ps1` seeds demo accounts via the **gateway** only:

1. Dev Login (`AUTH_MODE=dev` required)
2. **Skip** if the account already has weight history + recent diary
   entries (persistent DB). Pass `-Force` to wipe and reseed.
3. Onboarding / profile → demo persona (male, 178 cm, lose weight, moderate activity)
4. Delete existing diary entries + water for the last `max(Days, 120)` days
5. Delete all weight logs (`DELETE /api/users/me/weight/{id}`)
6. Resolve foods (OFF barcodes **only if they already have micros** + NutriTrack
   Demo submissions with full vitamin/mineral panels)
7. Write ~30 days of meals, water, and weight trend (override with `-Days`)

## Micronutrients

Each `Seed …` demo food submits the full checklist (A, B1–B7/B9/B12, C, D, E, K
+ calcium/iron/magnesium/potassium/sodium/zinc/iodine/selenium/copper/manganese/
phosphorus/chromium/molybdenum) with catalog default units (`mg` / `µg`).

- Reuses an existing submission only when it already has enough micros and
  `-Force` is not set.
- `-Force` (or a macros-only leftover submission) recreates the demo food so
  diary snapshots pick up vitamins/minerals.

## Accounts

| `-Account` | Auth code | Email |
|---|---|---|
| `dev` | `dev` | `dev-user@example.com` |
| `agent-debug` | `dev:agent-debug` | `agent-debug@example.com` |
| `all` (default) | both | both |

## Usage

```powershell
# Local compose gateway — skips accounts that already have data
./scripts/seed-dev-data.ps1

# Wipe and rewrite
./scripts/seed-dev-data.ps1 -Force

# Railway (auth-service must temporarily have AUTH_MODE=dev)
./scripts/seed-dev-data.ps1 -BaseUrl https://gateway-production-777b.up.railway.app

# One account / custom history length
./scripts/seed-dev-data.ps1 -Account agent-debug -Days 14
```

## Skip heuristic

An account is treated as already seeded when:

- weight logs ≥ `min(5, max(1, floor(Days/4)))`, and
- at least one diary entry exists in the last `min(3, Days)` local dates

## API dependency

Weight reset needs:

`DELETE /api/users/me/weight/{id}` → **204** (owner) / **404** (missing or other user)

Shipped in user-profile-service with the seeder. Redeploy that service before seeding Railway if the route is missing.

## Notes

- Default runs are safe on a persistent DB: no wipe unless `-Force`.
- Diary `zone` must be IANA (default `Europe/Berlin`), not a Windows timezone id.
- Demo foods are product submissions (`force: true`) named `Seed …` under the seeding user; reused on later runs via `/api/products/submissions/mine` when they already include vitamins/minerals.
- OFF barcode meals are skipped when the catalog product has no micronutrient panel (keeps dashboard vit/min charts populated).
- If `DELETE /api/users/me/weight/{id}` is not deployed yet, the script warns and still writes a new weight series (old points remain until the endpoint ships).
- Expect a couple of minutes for 30 days × 2 accounts when forcing a full reseed.

# Dev account data seeder

Recorded 2026-07-22.

## What it does

`scripts/seed-dev-data.ps1` resets and reseeds demo accounts via the **gateway** only:

1. Dev Login (`AUTH_MODE=dev` required)
2. Onboarding / profile → demo persona (male, 178 cm, lose weight, moderate activity)
3. Delete existing diary entries + water for the last `max(Days, 120)` days (wipes leftover longer seeds)
4. Delete all weight logs (`DELETE /api/users/me/weight/{id}`)
5. Resolve foods (OFF barcodes when available + NutriTrack Demo submissions)
6. Write ~30 days of meals, water, and weight trend (override with `-Days`)

## Accounts

| `-Account` | Auth code | Email |
|---|---|---|
| `dev` | `dev` | `dev-user@example.com` |
| `agent-debug` | `dev:agent-debug` | `agent-debug@example.com` |
| `all` (default) | both | both |

## Usage

```powershell
# Local compose gateway
./scripts/seed-dev-data.ps1

# Railway (auth-service must temporarily have AUTH_MODE=dev)
./scripts/seed-dev-data.ps1 -BaseUrl https://gateway-production-777b.up.railway.app

# One account / custom history length
./scripts/seed-dev-data.ps1 -Account agent-debug -Days 14
```

## API dependency

Weight reset needs:

`DELETE /api/users/me/weight/{id}` → **204** (owner) / **404** (missing or other user)

Shipped in user-profile-service with the seeder. Redeploy that service before seeding Railway if the route is missing.

## Notes

- Re-runs are idempotent in spirit: wipe then rewrite.
- Diary `zone` must be IANA (default `Europe/Berlin`), not a Windows timezone id.
- Demo foods are product submissions (`force: true`) named `Seed …` under the seeding user; reused on later runs via `/api/products/submissions/mine`.
- If `DELETE /api/users/me/weight/{id}` is not deployed yet, the script warns and still writes a new weight series (old points remain until the endpoint ships).
- Expect a couple of minutes for 30 days × 2 accounts (many HTTP round-trips).

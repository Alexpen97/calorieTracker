# Railway — Phase 4 (mirror, search & submissions)

Phase 4 changes live mainly in `food-catalog-service`. No new Railway services.

## Deploy notes

1. Redeploy **food-catalog-service** after merge (Flyway V3 + optional V4 GIN).
2. Redeploy **diary-service** (optional `submissionId` on diary create).
3. Redeploy **frontend** (search, submit product, moderation UI).

## Food catalog env (optional)

| Variable | Notes |
|---|---|
| `OFF_BULK_IMPORT_ENABLED` | Set `true` only if you want the nightly cron |
| `OFF_BULK_IMPORT_URL` | HTTPS URL or mounted path to OFF JSONL export |
| `OFF_SEARCH_RATE_LIMIT_PER_MINUTE` | Default `8` (below OFF's 10 searches/min) |

## Bulk import

- Manual: `POST /api/admin/off-import?input=<file-or-url>` with an `ADMIN` JWT
  (via gateway).
- Scheduled: enable `OFF_BULK_IMPORT_ENABLED=true` and set `OFF_BULK_IMPORT_URL`.
- Streams JSONL (does not require persisting the dump on Railway disk).

## Roles

Assign `MODERATOR` or `ADMIN` on `app_user.role` in user-profile DB so
moderation endpoints and the Moderation nav link work.

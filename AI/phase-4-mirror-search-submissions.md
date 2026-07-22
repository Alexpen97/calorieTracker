# Phase 4 — Mirror, search & submissions

## Goal

Deliver Phase 4 from `docs/calorie-tracker-architecture.md` §14:

- Spring Batch OFF JSONL bulk import into the local product mirror
- Product name search (local FTS / `search_document` + OFF fallback)
- User product submissions + moderator approve/reject (FR-8)
- Diary logging via `submissionId`; React search / submit / moderation UI

## Plan

See `docs/superpowers/plans/2026-07-22-phase-4-mirror-search-submissions.md`.

## Status

Implemented on `cursor/phase-4-mirror-search-submissions-f0d1`.

## Delivered

### food-catalog-service

- Flyway `V3__search_and_submissions.sql` (`search_document`, `product_submission`)
- PostgreSQL-only `V4__product_fts_gin.sql` (extra Flyway location)
- `GET /api/products/search?q=&page=`
- Submissions: create, mine, moderator queue/approve/reject
- Barcode/id lookup includes caller's own pending submissions
- Spring Batch `offBulkImportJob` + `POST /api/admin/off-import`
- Optional scheduled import (`OFF_BULK_IMPORT_ENABLED`)

### diary-service

- `POST /api/diary/entries` accepts `productId` **xor** `submissionId`

### Frontend

- Lookup search tab + add-your-own flow
- Submit product form; moderation queue (role-gated nav)
- Diary add uses `submissionId` for pending products

## Verification

- food-catalog: `mvn test` green (incl. batch JSONL fixture + moderation 403/approve)
- diary: `mvn test` green
- frontend: Vitest (incl. diaryRef helper)

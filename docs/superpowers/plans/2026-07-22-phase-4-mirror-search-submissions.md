# Phase 4 — Mirror, Search & Submissions Implementation Plan

> **For agentic workers:** Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Phase 4 from `docs/calorie-tracker-architecture.md` §14: Spring Batch OFF bulk import, PostgreSQL full-text search, user product submissions + moderation queue/roles.

**Architecture:** Extend `food-catalog-service` for FR-3/FR-8 (search + submissions/moderation) and OFF mirror bulk import. Extend diary to accept `submissionId`. React UI: name search, submit-own-product, moderator queue.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Batch 6 (via `spring-boot-starter-batch`), Spring Data JPA, Flyway, Resilience4j, springdoc 3.0.3, H2+MockMvc tests; React 19 + Vite + Vitest.

## Global Constraints

- Standalone Maven per service (no parent POM); Boot **4.1.0**, Java **21**, springdoc **3.0.3**.
- User id always from JWT `sub` (UUID); never trust body for ownership.
- JWT `roles` claim → `ROLE_*` authorities (existing converter). Enable `@EnableMethodSecurity`; moderation uses `@PreAuthorize("hasAnyRole('MODERATOR','ADMIN')")`.
- `product_submission` is a **separate** staging table; approve **copies** into `product` with `source=USER_APPROVED`.
- Pending submissions are visible to **submitter only** in barcode/search/id lookup.
- FTS: maintain `search_document` (H2-safe); PostgreSQL-only GIN on `to_tsvector('english', search_document)`.
- Bulk import: stream JSONL (file path or HTTP URL); `spring.batch.job.enabled=false`; launch via admin endpoint or scheduler.
- Match existing patterns: H2 `MODE=PostgreSQL` tests, nested DTO records, `PORT` env, multi-stage Dockerfile.

## File map

| Area | Create / Modify |
|------|-----------------|
| food-catalog schema | `V3__search_and_submissions.sql`, `db/migration-postgresql/V4__product_fts_gin.sql` |
| food-catalog search | `ProductSearchService`, OFF search client, controller endpoints |
| food-catalog submissions | domain + `SubmissionService` + `SubmissionController` |
| food-catalog batch | `OffBulkImportJobConfig`, JSONL reader/processor/writer, trigger API |
| food-catalog security | `@EnableMethodSecurity` |
| diary | optional `submissionId` on create; `getSubmission` / unified resolve |
| frontend | Lookup search tab, SubmitProductPage, ModerationPage, API client |
| docs | `AI/phase-4-*.md`, `docs/railway-phase4.md`, update notes |

---

### Task 1: Schema + search_document maintenance

- [ ] Flyway V3: `search_document` on product; `product_submission` table + indexes
- [ ] PostgreSQL-only V4 GIN index (extra Flyway location in prod yml)
- [ ] Update `Product` entity; refresh `search_document` on save
- [ ] Commit

### Task 2: Name search API

- [ ] Local search via `search_document` ILIKE / FTS
- [ ] OFF search fallback when local page is thin (rate-limited)
- [ ] Include caller's own pending submissions in results
- [ ] MockMvc tests; commit

### Task 3: Submissions + moderation

- [ ] POST/GET mine / GET queue / approve / reject
- [ ] Duplicate barcode + fuzzy name warnings on submit
- [ ] Approve copies to product; ROLE guard 403 for USER
- [ ] Barcode/id resolve own pending submissions
- [ ] Tests; commit

### Task 4: Spring Batch OFF JSONL import

- [ ] Add `spring-boot-starter-batch`; jdbc schema always
- [ ] Job: read JSONL → normalize → upsert by barcode
- [ ] `POST /api/admin/off-import` (ADMIN) with `filePath` or `url` param
- [ ] Optional scheduled cron (disabled by default)
- [ ] Unit/integration test with fixture JSONL; commit

### Task 5: Diary submissionId

- [ ] Create entry with `productId` XOR `submissionId`
- [ ] Food catalog: `GET /api/products/submissions/{id}` for owner (or product-shaped resolve)
- [ ] Tests; commit

### Task 6: Frontend

- [ ] Search on LookupPage; Submit product form; Moderation queue (role-gated)
- [ ] Diary create with submissionId; Vitest helpers
- [ ] Commit

### Task 7: Docs + verify

- [ ] AI notes, railway-phase4, README updates
- [ ] Full test suite green; push PR

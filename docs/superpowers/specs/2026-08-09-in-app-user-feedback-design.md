# In-app user feedback (ULT-13) — Design

## Goal

Let any signed-in user submit product feedback from Settings, persist it
server-side with enough context to act on later, and show that user the
lifecycle status of their own submissions.

## Decisions (open questions resolved)

| Question | Decision |
|---|---|
| Settings entry point | New hub row **Feedback** → `/settings/feedback` (between Account and Sign out) |
| Submit surface | Settings section with a short form (message only) plus a list of prior submissions |
| Message limits | Min 10, max 2000 characters |
| Storage | `user_feedback` table in **user-profile-service** (Flyway `V4`) |
| Status vocabulary | `PENDING` → `ACCEPTED` → `COMPLETED` |
| Default status | New submissions start as `PENDING` |
| Status changes | Moderators/admins via `PATCH /api/users/feedback/{id}/status` (no user-facing triage UI yet) |
| User refresh | React Query refetch on Feedback page visit; no push/email notifications in this slice |
| Context captured | `message`, `user_id`, `created_at`, `updated_at`, optional `app_version` |

## Approaches considered

1. **Extend user-profile-service** (chosen) — feedback is user-scoped; gateway already proxies `/api/users/**`; matches weight/goals patterns.
2. **New microservice** — unnecessary operational cost for store + list.
3. **Client-only storage** — fails the “persisted/server-side” requirement.

## Architecture

### Backend (`user-profile-service`)

- Entity `UserFeedback` + enum `FeedbackStatus` (`PENDING`, `ACCEPTED`, `COMPLETED`)
- `FeedbackService` creates (always `PENDING`) and lists by authenticated user
- Status updates restricted to `ROLE_MODERATOR` or `ROLE_ADMIN`
- Flyway migration creates table + index on `(user_id, created_at DESC)`

### API

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/api/users/me/feedback` | JWT user | Create feedback |
| `GET` | `/api/users/me/feedback` | JWT user | List own feedback (newest first) |
| `PATCH` | `/api/users/feedback/{id}/status` | JWT moderator/admin | Update status |

Create body:

```json
{ "message": "…", "appVersion": "0.1.0" }
```

Response item:

```json
{
  "id": "uuid",
  "message": "…",
  "status": "PENDING",
  "appVersion": "0.1.0",
  "createdAt": "…",
  "updatedAt": "…"
}
```

Gateway: no route changes (`/api/users/**` already covers these paths).

### Frontend

- Settings hub row summarizing open feedback count when available
- `SettingsFeedbackSection` with:
  - textarea + Submit
  - list of own items showing message snippet, status label, created date
- API helpers `submitFeedback` / `fetchMyFeedback` in `api/client.ts`
- Route `/settings/feedback` wired like other settings sections
- Preserve existing settings visual language (hub rows, section shell, dashboard cards)

## Error handling

- Validation failures → `400`
- Unknown feedback id on status patch → `404`
- Non-moderator status patch → `403`
- Users never see others’ feedback

## Testing

- Backend MockMvc: create defaults to `PENDING`, list is user-scoped, status patch role-gated, message length validation
- Frontend: hub link; submit + list status visibility

## Out of scope

- Admin triage UI, email/push notifications, attachments, categories, editing/deleting feedback after submit

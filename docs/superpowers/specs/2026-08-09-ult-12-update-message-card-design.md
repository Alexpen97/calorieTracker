# ULT-12 — In-app update message card

## Goal

Server-driven update card that opens **once per user per push**, set and published via API.

## Placement

Owned by **user-profile-service** (user identity + per-user state). Gateway already routes `/api/users/**`.

## Data model

`update_message`

- `id` UUID PK
- `title`, `body` (required)
- `image_url`, `action_label`, `action_url` (optional)
- `pushed_at`, `created_at`

`update_message_ack`

- PK `(user_id, message_id)`
- `acknowledged_at`
- FK to `app_user` and `update_message`

Each push inserts a new `update_message`. Ack is per user + message. A new push is a new row, so every user sees it once even if they dismissed an earlier message.

## API

### Push (operator / backend)

`POST /api/users/internal/update-messages`

- Auth: `X-Internal-Api-Key` (same pattern as user upsert)
- Body: `{ title, body, imageUrl?, actionLabel?, actionUrl? }`
- Effect: create message with `pushed_at = now` (live immediately)
- Response: message DTO

Also: `POST /api/users/admin/update-messages` with JWT `ROLE_ADMIN` for gateway-friendly admin push (same body/effect).

### Client pending

`GET /api/users/me/update-messages/pending`

- Auth: user JWT
- Returns oldest live message the user has **not** acknowledged, or **204** if none

### Client acknowledge

`POST /api/users/me/update-messages/{id}/acknowledge`

- Auth: user JWT
- Idempotent insert into `update_message_ack`
- 404 if message id unknown

## Client UX

- Mount an overlay in the authenticated, onboarding-complete app shell (`App.tsx`).
- Reuse existing sheet overlay pattern (`sheet-backdrop` / `sheet`).
- Show title, body, optional image, optional action link/button, and a primary dismiss (“Got it”).
- On dismiss (or backdrop close): call acknowledge; clear local pending so it does not reappear.
- If multiple unacked pushes exist, show FIFO (oldest first); after ack, next pending loads on subsequent visit/query.

## Out of scope

- Draft/edit/unpublish workflow
- Push notifications / email
- In-app admin UI to compose messages

# In-app user feedback (ULT-13)

## Summary

Users can submit product feedback from Settings and see Pending / Accepted /
Completed status on their own submissions.

## Surface

- Settings hub row: **Feedback** → `/settings/feedback`
- Form: message (10–2000 chars) + list of prior submissions with status

## Backend

- `user-profile-service` table `user_feedback` (Flyway `V4`)
- `POST/GET /api/users/me/feedback`
- `PATCH /api/users/feedback/{id}/status` (MODERATOR/ADMIN)

## Spec / plan

- `docs/superpowers/specs/2026-08-09-in-app-user-feedback-design.md`
- `docs/superpowers/plans/2026-08-09-in-app-user-feedback.md`

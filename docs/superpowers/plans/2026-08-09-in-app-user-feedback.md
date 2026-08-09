# In-app User Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (or subagent-driven-development). Steps use checkbox (`- [ ]`) syntax.

**Goal:** Settings → Feedback submit + list with Pending/Accepted/Completed, persisted in user-profile-service.

**Architecture:** Flyway table + REST under `/api/users/**` in user-profile-service; frontend settings section + client helpers. Gateway unchanged.

**Tech Stack:** Spring Boot / JPA / Flyway / MockMvc; React + React Query + Vitest; existing Settings patterns.

---

### Task 1: Backend persistence + create/list API

**Files:**
- Create: `services/user-profile-service/src/main/resources/db/migration/V4__user_feedback.sql`
- Create: domain `FeedbackStatus`, `UserFeedback`, `UserFeedbackRepository`
- Create: `FeedbackService`, `FeedbackController`
- Modify: `SecurityConfig.java` (moderator status path)
- Modify: `UserControllerTest.java` (or dedicated `FeedbackControllerTest.java`)

- [ ] Write failing MockMvc tests for POST/GET `/api/users/me/feedback`
- [ ] Add migration + entity/repo/service/controller
- [ ] Run user-profile-service tests; commit

### Task 2: Moderator status update API

**Files:**
- Modify: `FeedbackService`, `FeedbackController`, `SecurityConfig`
- Modify: tests

- [ ] Write failing test for PATCH status (moderator ok, user forbidden)
- [ ] Implement PATCH `/api/users/feedback/{id}/status`
- [ ] Run tests; commit

### Task 3: Frontend API + Settings UI

**Files:**
- Modify: `frontend/src/api/client.ts`
- Create: `frontend/src/pages/settings/SettingsFeedbackSection.tsx`
- Modify: `SettingsHomePage.tsx`, `App.tsx`, `SettingsHomePage.test.tsx`, `index.css` (minimal if needed)

- [ ] Write failing hub + feedback section tests
- [ ] Implement client helpers + UI + route
- [ ] Run frontend tests; commit

### Task 4: Docs + verify

- [ ] Short `AI/` note if repo pattern expects it
- [ ] Full frontend + user-profile-service test suites
- [ ] Push branch; open PR

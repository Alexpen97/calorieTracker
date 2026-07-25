# Settings/Profile Navigation Design

## Goal

Turn the current `Profile` navigation destination into a real `Settings` area that
is easier to understand and use on mobile. `Profile` becomes one section inside
settings instead of the primary account-facing nav concept.

## Problem statement

The current information architecture exposes `Profile` as a top-level desktop and
mobile destination while the `/me` screen is actually a mixed account-management
page. That page combines profile editing, weight logging, goal management, and
sign-out into one long stacked experience. The result is functional, but it is
not user-friendly or mobile-friendly because it is dense, scroll-heavy, and does
not clearly separate tasks by intent.

## Decision summary

- Replace the `Profile` nav label with `Settings` on desktop and mobile.
- Replace route `/me` with `/settings`.
- Keep the fifth mobile bottom-nav destination as a direct `Settings` tab.
- Preserve the centered `Track food` FAB and the rest of the bottom-nav layout.
- Introduce a settings home screen that links to focused settings sections.
- Move the current profile form under `Settings > Profile`.
- Split weight and goals into their own focused settings sections.
- Keep moderation separate and role-gated; do not move it under settings in this
  change.

## Why this approach

This design keeps the existing navigation model stable while fixing the actual UX
problem. Users already have a bottom navigation with a stable fifth destination.
Keeping that destination visible as `Settings` is simpler than introducing a
`More` overflow step. The current app also does not appear crowded enough to
justify hiding account actions behind an additional sheet or menu.

## Selected UX model

### Navigation

- Desktop top navigation shows `Settings` instead of `Profile`.
- Mobile bottom navigation shows `Settings` instead of `Profile`.
- The `Settings` destination remains a first-class route, not a popover or sheet.
- The `Track food` FAB remains centered between `Diary` and `Analytics`.

### Settings landing page

The landing page for `/settings` becomes a lightweight settings hub instead of a
large form. It presents a short list of tappable rows or cards:

- `Profile`
- `Goals`
- `Weight`
- `Account`
- `Sign out`

Each row includes a short summary so the page is useful before the user opens a
subsection. Examples:

- `Profile`: display name and objective
- `Goals`: whether goals are custom or calculated
- `Weight`: latest recorded value or empty-state copy
- `Account`: email and role

`Sign out` is visually separated from edit flows so it reads as an account action
rather than part of profile editing.

### Section behavior

#### Profile

Contains the existing personal-details form:

- display name
- sex
- birth date
- height
- activity level
- objective

This section keeps the current save behavior and validation rules.

#### Goals

Contains:

- daily goals list
- per-goal override save actions
- goals recalculation action

This section keeps the current data model and mutation behavior, but it is no
longer mixed into the default first view of the account area.

#### Weight

Contains:

- weight logging input
- recent weight history

This section keeps the current behavior while becoming easier to find and scan on
mobile.

#### Account

Contains:

- email
- role
- sign-out action if not already surfaced on the landing page

The account section is read-only except for sign-out in this iteration.

## Mobile UX rules

- The first `/settings` screen must be scannable without forcing users through a
  long form.
- Primary actions must be grouped by task instead of mixed in one vertical stack.
- Tappable rows and buttons must be large enough for comfortable thumb use.
- Important actions must be reachable within one or two taps from the settings
  landing page.
- The fixed bottom navigation must not overlap settings content or action buttons.
- Save actions stay near the form they affect rather than being pushed to the
  bottom of a long page.

## Desktop UX rules

- Desktop uses the same information architecture as mobile.
- Desktop may display sections in wider cards or panels, but the route names,
  labels, and grouping stay consistent with mobile.
- Do not create a separate desktop-only account model for this change.

## Routing and component shape

### Route changes

- Replace `/me` with `/settings`.
- Update navigation links to point to `/settings`.

### Suggested component split

- `frontend/src/navigation/AppNavigation.tsx`
  - rename nav destination and keep bottom-nav structure intact
- `frontend/src/App.tsx`
  - register `/settings`
- `frontend/src/pages/SettingsPage.tsx`
  - new landing page and subsection shell
- `frontend/src/pages/ProfilePage.tsx`
  - either replace with settings-specific composition or shrink into a section
    component used by settings
- new focused section components if needed:
  - `frontend/src/pages/settings/SettingsProfileSection.tsx`
  - `frontend/src/pages/settings/SettingsGoalsSection.tsx`
  - `frontend/src/pages/settings/SettingsWeightSection.tsx`
  - `frontend/src/pages/settings/SettingsAccountSection.tsx`
- `frontend/src/index.css`
  - settings landing rows, subsection spacing, mobile-safe layout polish

Exact file names for extracted section components can vary as long as
responsibilities stay small and clear.

## Data and behavior constraints

- Reuse the existing React Query data sources and mutations.
- Do not change backend APIs for this work.
- Preserve current validation and success/error messaging unless copy cleanup is
  part of the settings polish.
- Preserve moderation visibility rules.
- Preserve auth/logout behavior.
- Preserve existing mobile-first navigation and FAB behavior.

## Error handling

- Loading, save, and validation states must remain visible inside each focused
  settings section.
- A failure in one section must not block rendering of unrelated settings entry
  points when avoidable.
- Empty states should remain explicit, especially for weight history and goals.

## Testing requirements

### Automated

- Update `AppNavigation.test.tsx` to assert the `Settings` label and `/settings`
  href.
- Add focused tests for the new settings landing page.
- Add or update tests proving profile save, weight logging, goal save, and goal
  recalculation still work after the IA change.
- Keep moderator-only behavior covered where settings and moderation visibility
  intersect.

### Manual

- Verify the mobile bottom nav still feels balanced after renaming the fifth tab.
- Verify `/settings` is readable and easy to scan on a narrow viewport.
- Verify each subsection can be opened and used without excessive scrolling.
- Verify sign-out is easy to find but visually separated from edit actions.

## Out of scope

- Adding new preference domains that do not exist yet
- Creating a `More` overflow pattern
- Moving moderation under settings
- Backend or API contract changes
- New account security features beyond the current sign-out flow

## Default implementation order

1. Rename nav label and route from `Profile`/`/me` to `Settings`/`/settings`.
2. Introduce the settings landing page.
3. Move profile editing into a dedicated profile section.
4. Move goals into a dedicated goals section.
5. Move weight logging/history into a dedicated weight section.
6. Separate account metadata and sign-out visually.
7. Add mobile-specific layout polish and update tests.

## Follow-up option

If future secondary destinations accumulate, the fifth mobile destination can be
revisited as a `More` entry later. That is not the default design for this
iteration.

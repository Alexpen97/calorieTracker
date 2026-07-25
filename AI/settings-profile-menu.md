# Settings/Profile menu redesign

## Summary

Replaced the top-level `Profile` destination with a mobile-friendly `Settings`
area. Profile remains available as one settings section.

## Decision

- Nav label is `Settings` on desktop and mobile.
- Route is `/settings` (with `/me` redirecting to `/settings`).
- Fifth mobile bottom-nav destination stays a direct `Settings` tab.
- Settings home is a lightweight hub with rows for Profile, Goals, Weight, and
  Account, plus a separated Sign out action.
- Focused subsections live at:
  - `/settings/profile`
  - `/settings/goals`
  - `/settings/weight`
  - `/settings/account`

## Spec

- `docs/superpowers/specs/2026-07-25-settings-profile-menu-design.md`

## Implementation

- `frontend/src/navigation/AppNavigation.tsx` — Settings label/route + gear icon
- `frontend/src/App.tsx` — settings routes + `/me` redirect
- `frontend/src/pages/SettingsHomePage.tsx` — settings hub
- `frontend/src/pages/settings/*` — focused Profile/Goals/Weight/Account sections
- `frontend/src/index.css` — settings hub/row mobile-friendly styles
- Removed monolithic `frontend/src/pages/ProfilePage.tsx`

## Tests

- Updated `AppNavigation.test.tsx`
- Added `SettingsHomePage.test.tsx` covering hub, profile save, weight log,
  goals save/recalculate, and account sign-out
- Full frontend suite: 83 passing

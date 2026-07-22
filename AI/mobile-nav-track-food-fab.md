# Mobile bottom-nav track-food FAB

## Summary

Added a centered round `+` button to the mobile bottom navigation that opens
food tracking (`/lookup`).

## Changes

- `AppNavigation` inserts a raised FAB between Diary and Analytics on mobile.
- Link target is `/lookup` (primary food-tracking entry on mobile; diary summary no longer has Add Food).
- Accessible name: `Track food`.
- Desktop top nav is unchanged (still includes Lookup as a text link).
- New `IconPlus` icon used by the FAB.

## Layout

Bottom nav grid is now `2 tabs | FAB | 2 tabs` so the action sits in the middle
of the bar and slightly elevates above it.

## Tests

- `AppNavigation.test.tsx` asserts the FAB link href and accessible name.

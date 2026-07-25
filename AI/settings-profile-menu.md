# Settings/Profile menu redesign

## Summary

Wrote a design spec for replacing the current `Profile` destination with a real
`Settings` area that is easier to use on mobile.

## Decision

- Default nav label becomes `Settings`, not `Profile`.
- Default route becomes `/settings`, not `/me`.
- `Profile` becomes one settings section instead of the top-level account concept.
- The fifth mobile bottom-nav destination stays a direct `Settings` tab.
- The mobile flow should use a lightweight settings hub plus focused subsections
  for profile, goals, weight, and account.

## Spec

- `docs/superpowers/specs/2026-07-25-settings-profile-menu-design.md`

## Notes

- This is a design/spec artifact only; implementation has not started yet.
- If future secondary destinations grow, a later follow-up can evaluate a
  `More` overflow destination, but that is not the default direction.

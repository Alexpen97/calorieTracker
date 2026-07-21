# Task 1 Report: Body weight API (user-profile-service)

## Status

DONE

## Summary

- Added `BodyWeightLog` JPA entity mapped to the existing `body_weight_log` Flyway table.
- Added `BodyWeightLogRepository` with user-scoped newest-first queries and optional `from`/`to` measured-at filtering.
- Added `WeightService` to create logs, default missing `measuredAt` to `Instant.now()`, require the JWT user to exist, and list logs for that user only.
- Added `WeightController` endpoints:
  - `POST /api/users/me/weight`
  - `GET /api/users/me/weight?from=&to=`
- Added MockMvc coverage for explicit and default `measuredAt`, response shape, JWT user scoping, newest-first order, range filtering, and `weightKg > 0` validation.

## TDD Evidence

### RED

Command:

`mvn -q -Dtest=UserControllerTest test`

Result before implementation:

- Exit code: `1`
- `Tests run: 6, Failures: 4, Errors: 0, Skipped: 0`
- New weight tests failed because `/api/users/me/weight` was not implemented:
  - `Status expected:<200> but was:<404>`
  - `Status expected:<400> but was:<404>`
  - MockMvc resolved the request to `ResourceHttpRequestHandler` with `NoResourceFoundException`.

### GREEN

Targeted command:

`mvn -q -Dtest=UserControllerTest test`

Result after implementation:

- Exit code: `0`

Full required command:

`mvn -q test`

Result:

- Exit code: `0`
- Surefire report: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

## Self-Review

- Verified response shape is exactly `{ id, weightKg, measuredAt }` for POST and GET entries.
- Verified GET queries are scoped to the authenticated JWT `sub` UUID and do not return another user's entries.
- Verified ordering is `measuredAt DESC`.
- Verified `from` and `to` Instant query params filter by `measuredAt` range.
- Verified missing `measuredAt` is persisted with the current server time.
- Verified non-positive `weightKg` returns HTTP 400 through bean validation.
- No schema migration was added because `body_weight_log` already exists in `V1__users_schema.sql`.

## Concerns

- Test output includes existing environment warnings from SpringDoc defaults, Flyway's newer H2 version notice, and Mockito dynamic agent loading. They did not fail the build.

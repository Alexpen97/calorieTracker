# Login 403 on `/api/auth/google/callback`

## Symptom

After Google sign-in, the browser network tab shows:

```
POST https://gateway-production-777b.up.railway.app/api/auth/google/callback → 403 (Forbidden)
```

The response body is empty.

## Root cause

The gateway **does** forward the request to `auth-service`. Security on `/api/auth/**`
is `permitAll`. The callback handler runs, then calls `user-profile-service` to upsert
the user (`USER_SERVICE_URL` + `/api/users/internal/upsert`).

When that downstream call fails (most often **connection refused** because
`USER_SERVICE_URL` was never set on Railway and defaults to
`http://localhost:8082`), Spring/Tomcat surfaced the failure as a misleading **403**
with an empty body.

## Fix (Railway — operator)

On **auth-service**, set:

| Variable | Value |
|---|---|
| `USER_SERVICE_URL` | `http://user-profile-service.railway.internal:8080` |
| `INTERNAL_API_KEY` | same secret as on `user-profile-service` |
| `AUTH_MODE` | `prod` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Web OAuth client credentials |

Ensure **user-profile-service** is running and healthy (Postgres `users` DB reachable).

After redeploying auth-service with `USER_SERVICE_URL`, retry Google login.

## Fix (code)

- `auth-service` `ApiExceptionHandler` — map `WebClientRequestException` to **503**
  with a message mentioning `USER_SERVICE_URL` (no more empty 403).
- `AuthStartupValidator` — fail fast on startup when `AUTH_MODE=prod` but
  `USER_SERVICE_URL` still points at localhost or Google / internal API env is missing.
- Tests: `AuthControllerDownstreamFailureTest`, `AuthStartupValidatorTest`.

## Related

- Token exchange URL must be the **gateway** (`VITE_API_BASE_URL`), not registered in
  Google Console — see `docs/google-oauth-setup.md`.
- nginx 405 after login: `AI/login-405-nginx.md`.

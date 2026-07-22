# Production debugging (Railway)

Recorded 2026-07-22.

## Public URLs

| Service | URL |
|---|---|
| Gateway (prefer for API calls) | `https://gateway-production-777b.up.railway.app` |
| Frontend | `https://front-end-production-4a95.up.railway.app` |
| Auth | `https://auth-production-6f20.up.railway.app` |
| User profile | `https://user-production-e77e.up.railway.app` |
| Diary | `https://diary-production-0f2f.up.railway.app` |
| Food catalog | `https://food-catalog-production.up.railway.app` |

Prefer **gateway** for app API traffic (`/api/**`). Hit service public URLs only when isolating a single service (e.g. JWKS on auth).

## Agent debug user

Auth supports a deterministic **dev login** when `AUTH_MODE=dev` on **auth-service**:

```http
POST {{gateway}}/api/auth/google/callback
Content-Type: application/json

{
  "code": "dev:agent-debug",
  "redirectUri": "https://front-end-production-4a95.up.railway.app/auth/callback"
}
```

Identity minted by `GoogleTokenClient.parseDevIdentity`:

| Field | Value for `dev:agent-debug` |
|---|---|
| Google sub | `dev-sub-agent-debug` |
| Email | `agent-debug@example.com` |
| Display name | `Dev User` |

Response includes `accessToken` + `refreshToken`. Use `Authorization: Bearer <accessToken>` on subsequent calls.

Suffix rules:

- `code: "dev"` → email `dev-user@example.com`, sub `dev-sub-dev-user`
- `code: "dev:<suffix>"` → email `<suffix>@example.com` (or raw if suffix contains `@`), sub `dev-sub-<suffix>`

Frontend “Dev login” button appears only when `VITE_AUTH_MODE=dev` and sends `code: "dev"`.

### Production status (verified 2026-07-22)

Railway auth is currently **`AUTH_MODE=prod`**. Calling the callback with `code: "dev:…"` returns:

```json
{"error":"Downstream request failed: Bad Request"}
```

(Google token exchange is attempted; fake codes fail.)

**To enable agent API debugging against Railway**, temporarily set on **auth-service**:

| Variable | Value |
|---|---|
| `AUTH_MODE` | `dev` |

Then re-run Dev Login. Flip back to `prod` when done. Do not leave `AUTH_MODE=dev` on a public production deployment longer than needed.

JWKS is healthy: `GET https://auth-production-6f20.up.railway.app/.well-known/jwks.json`.

## Postman

Workspace: **NutriTrack** (`78e98043-292a-4c10-bed9-f570a3d0c0f5`)

| Element | Id |
|---|---|
| Collection `NutriTrack Debug` | `a3bff6c0-32f3-492f-ae75-0a68fcce9690` |
| Environment `Railway Production` | `5ef257ec-0c87-4e9b-9690-bb30e916caf8` |

Collection folders: Auth (Dev Login, Refresh, JWKS), User (me / onboarding / goals), Food & Diary.

Dev Login test script writes `access_token` and `refresh_token` into the active environment.

**Note:** Postman MCP manages collections/envs; it does **not** execute HTTP. Agents run requests via Shell (`Invoke-RestMethod` / `curl.exe`) using the same shapes, or the human runs them in the Postman app.

## Typical agent API flow

1. Ensure `AUTH_MODE=dev` on auth-service (or use local docker-compose).
2. Dev Login → capture tokens.
3. Optional: `POST /api/users/me/onboarding` if profile incomplete.
4. Call gateway routes under test with Bearer token.
5. On 401, Refresh then retry once.

## Seed realistic demo data

```powershell
./scripts/seed-dev-data.ps1 -BaseUrl https://gateway-production-777b.up.railway.app
```

Requires `AUTH_MODE=dev` and a deployed `DELETE /api/users/me/weight/{id}`.
Details: `AI/seed-dev-data.md`.

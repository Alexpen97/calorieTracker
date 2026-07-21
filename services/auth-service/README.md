# auth-service

Authentication container — Google OIDC login and app token issuing. See
`docs/calorie-tracker-architecture.md` §5.2.

- Exchanges the Google authorization code (Authorization Code + PKCE),
  verifies the Google ID token, upserts the user via user-profile-service.
- Issues short-lived RS256 access JWTs + rotating refresh tokens; exposes a
  JWKS endpoint for the other services.

## Container

- Build: multi-stage `Dockerfile` (Maven build → JRE 21 runtime). Standalone
  Maven project (Railway root directory = `/services/auth-service`).
- Port: `8080` (private network only; reached through the gateway).

## Key environment variables

| Variable | Purpose |
|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth client |
| `JWT_SIGNING_KEY` | RS256 private key (secret) |
| `USER_SERVICE_URL` | user-profile-service base URL for login upsert |
| `REFRESH_TOKEN_TTL` / `ACCESS_TOKEN_TTL` | token lifetimes |

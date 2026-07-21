# Gateway 500 on `/api/diary/**`

## Symptom

Browser requests such as:

```
GET https://gateway-production-777b.up.railway.app/api/diary/summary?date=2026-07-21&zone=Europe%2FAmsterdam → 500
GET https://gateway-production-777b.up.railway.app/api/diary/entries?... → 500
GET https://gateway-production-777b.up.railway.app/api/diary/water?... → 500
```

Gateway logs show:

```
Caused by: java.lang.IllegalArgumentException: Host is not specified
    at reactor.netty.http.client.UriEndpointFactory.createUriEndpoint(...)
```

## Root cause

The gateway route for `diary-service` has no upstream host. Common causes on
Railway:

1. **`DIARY_SERVICE_URL` is missing or an empty string** on the **gateway**
   service. Spring resolves `${DIARY_SERVICE_URL:http://localhost:8084}`; a
   blank variable overrides the default.
2. **The URL omits the `http://` scheme**, e.g. `diary-service.railway.internal:8080`
   instead of `http://diary-service.railway.internal:8080`. Java URI parsing
   treats the hostname as the scheme, so the gateway sees no host and throws
   `Host is not specified`.
3. **A Railway reference resolved to an empty host**, e.g.
   `http://${{diary-service.RAILWAY_PRIVATE_DOMAIN}}:8080` becomes `http://:8080`
   when `diary-service` is not deployed yet or the service name in the reference
   does not match the Railway service name.

Phase 3 added diary routes; older gateway deployments may not have the new env
var yet, or it may have been added in the bare-host form.

## Fix (Railway — operator)

1. Deploy **`diary-service`** (see `docs/railway-phase3.md`).
2. On **gateway**, set:

| Variable | Value |
|---|---|
| `DIARY_SERVICE_URL` | `http://diary-service.railway.internal:8080` |

3. Redeploy gateway and confirm `diary-service` is healthy.

## Fix (code)

- `ServiceUrlEnvironmentPostProcessor` — auto-prefix `http://` when operators set
  bare `host:port` private URLs.
- `GatewayStartupValidator` — fail fast at startup when a required route has no
  host or still points at localhost while `RAILWAY_ENVIRONMENT` is set.
- Tests: `ServiceUrlNormalizerTest`, `GatewayStartupValidatorTest`,
  `GatewayApplicationTest`.

## Related

- Full gateway env table: `docs/railway-deploy.md`
- Phase 3 deploy checklist: `docs/railway-phase3.md`

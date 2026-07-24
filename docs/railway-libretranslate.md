# Railway — libretranslate

Self-hosted LibreTranslate for NL→EN name translation before NEVO matching.
See also `services/libretranslate/README.md`.

## New Railway service

| Setting | Value |
|---|---|
| Root directory | `services/libretranslate` |
| Dockerfile | service Dockerfile |
| Private networking | yes (no public domain) |
| Memory | ≥2 GB recommended (Argos models) |

## Environment

| Variable | Notes |
|---|---|
| `PORT` | Railway sets automatically (mapped to `LT_PORT`) |
| `LT_LOAD_ONLY` | optional, image default `en,nl` |
| `LT_UPDATE` | `true` |

## nevo-service additions

| Variable | Value |
|---|---|
| `LIBRETRANSLATE_ENABLED` | `true` |
| `LIBRETRANSLATE_URL` | `http://libretranslate.railway.internal:8080` (match Railway `PORT`) |
| `LIBRETRANSLATE_SOURCE` | `auto` (default) |
| `LIBRETRANSLATE_TARGET` | `en` (default) |
| `LIBRETRANSLATE_TIMEOUT` | optional, default `3s` |

## Notes

- Not routed through the gateway.
- First boot downloads language models; allow a long start period.
- If LibreTranslate is down, NEVO matching continues with the untranslated name.

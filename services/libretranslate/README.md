# libretranslate

Self-hosted [LibreTranslate](https://github.com/LibreTranslate/LibreTranslate) for
translating OFF product / generic names to English before NEVO matching.

## Local Compose

```bash
docker compose --profile full up libretranslate nevo-service
```

Listens on `5000` locally (`http://localhost:5000`). Inside Compose:
`http://libretranslate:5000`.

## Railway

| Setting | Value |
|---|---|
| Service name | `libretranslate` |
| Root directory | `/services/libretranslate` |
| Watch paths | `/services/libretranslate/**` |
| Networking | **Private only** (no public domain) |
| Resources | Prefer ≥2 GB RAM (Argos models) |

### Variables (this service)

| Variable | Notes |
|---|---|
| `PORT` | Railway sets automatically (entrypoint maps to `LT_PORT`) |
| `LT_LOAD_ONLY` | default in image: `en,nl` |
| `LT_UPDATE` | `true` recommended behind private network |

### Variables (nevo-service)

| Variable | Value |
|---|---|
| `LIBRETRANSLATE_ENABLED` | `true` |
| `LIBRETRANSLATE_URL` | `http://libretranslate.railway.internal:${PORT}` (or Compose `http://libretranslate:5000`) |
| `LIBRETRANSLATE_SOURCE` | `auto` (default) |
| `LIBRETRANSLATE_TARGET` | `en` (default) |

## Smoke test

```bash
curl -s -X POST http://localhost:5000/translate \
  -H "Content-Type: application/json" \
  -d '{"q":"magere kwark met aardbei","source":"nl","target":"en"}'
```

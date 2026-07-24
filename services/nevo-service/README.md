# nevo-service

Local Dutch Food Composition Database (NEVO) import and micronutrient estimate matching.

## Role

- Imports NEVO-online CSV into a dedicated `nevo` Postgres database
- Matches barcode/product queries to the best NEVO food
- Returns estimated micronutrients for foods where Open Food Facts lacks micros

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/internal/nevo/import` | `X-Internal-Api-Key` | Import CSV from configured path or request body path |
| `POST` | `/internal/nevo/matches/best` | `X-Internal-Api-Key` | Best NEVO match + mapped nutrients |
| `GET` | `/api/nevo/foods/{nevoCode}` | JWT | Debug/detail lookup (optional gateway) |

## Import

Place the NEVO CSV on disk and set:

```bash
NEVO_CSV_PATH=/data/nevo.csv
NEVO_VERSION=2025/9.0
```

Then call:

```bash
curl -X POST http://localhost:8085/internal/nevo/import \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY"
```

## Attribution

Data: NEVO-online (RIVM). Cite the imported version (for example `NEVO-online version 2025/9.0, RIVM, Bilthoven`).

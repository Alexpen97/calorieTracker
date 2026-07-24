# nevo-service

Local Dutch Food Composition Database (NEVO) import and micronutrient estimate matching.

## Data files

Shipped under `src/main/resources/nevo/`:

| File | Role |
|---|---|
| `NEVO2025_v9.0.csv` | Wide food × nutrient matrix (pipe `\|` delimited) — **imported** |
| `NEVO2025_v9.0_Nutrienten_Nutrients.csv` | Nutrient code dictionary (reference) |
| `NEVO2025_v9.0_Details.csv` | Long-form per-value provenance (not imported; excluded from JAR) |
| `NEVO2025_v9.0_Recepten_Recipes.csv` | Recipe ingredient breakdown (not imported) |
| `NEVO2025_v9.0_Referenties_References.csv` | Source references (not imported) |

Default import uses the classpath matrix (`classpath:nevo/NEVO2025_v9.0.csv`).

## Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `POST` | `/internal/nevo/import` | `X-Internal-Api-Key` | Import configured/default CSV |
| `POST` | `/internal/nevo/matches/best` | `X-Internal-Api-Key` | Best NEVO match + mapped nutrients |
| `GET` | `/api/nevo/foods/{nevoCode}` | open (debug) | Direct food lookup |

## Import

```bash
curl -X POST http://localhost:8085/internal/nevo/import \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY"
```

Optional override:

```bash
NEVO_CSV_PATH=classpath:nevo/NEVO2025_v9.0.csv
# or a filesystem path:
NEVO_CSV_PATH=D:/data/NEVO2025_v9.0.csv
```

## Attribution

> NEVO-online version 2025/9.0, RIVM, Bilthoven

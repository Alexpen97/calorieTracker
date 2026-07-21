# infra/postgres

PostgreSQL container for NutriTrack — local Compose and Railway.

- **Dockerfile**: `postgres:16-alpine` + `init/` scripts baked in.
- `init/01-create-databases.sql` creates per-service databases on first start:
  `users`, `food_catalog`, `diary`.
- Schema migrations run inside each Spring Boot service (Flyway), not here.

## Local Compose

Started via `docker compose --profile deps up` or `--profile full up`.
Credentials default to `nutritrack` / `nutritrack` (see `docker-compose.yml`).

## Railway

| Setting | Value |
|---|---|
| Service name | `postgres` (other services reference this hostname) |
| Root directory | `/infra/postgres` |
| Watch paths | `/infra/postgres/**` |
| Networking | Private only |
| Volume | **Required** — mount persistent storage on `/var/lib/postgresql/data` |

### Variables (postgres service)

Defaults are baked into the image (`nutritrack` / `nutritrack` / `postgres`).
Override at runtime only if you need different credentials:

| Variable | Default |
|---|---|
| `POSTGRES_USER` | `nutritrack` |
| `POSTGRES_PASSWORD` | `nutritrack` |
| `POSTGRES_DB` | `postgres` (app DBs created by init script) |

### JDBC URLs (app services)

Point each service at the same host, different database name:

| App service | `SPRING_DATASOURCE_URL` |
|---|---|
| `user-profile-service` | `jdbc:postgresql://postgres.railway.internal:5432/users` |
| `food-catalog-service` | `jdbc:postgresql://postgres.railway.internal:5432/food_catalog` |
| `diary-service` | `jdbc:postgresql://postgres.railway.internal:5432/diary` |

Also set `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` to match
`POSTGRES_USER` / `POSTGRES_PASSWORD`.

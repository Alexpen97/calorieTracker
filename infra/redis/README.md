# infra/redis

Redis container for NutriTrack — local Compose and Railway.

- **Dockerfile**: `redis:7-alpine` + `redis.conf` baked in.
- Used by `food-catalog-service` for hot product cache (24h TTL).

## Local Compose

Started via `docker compose --profile deps up` or `--profile full up`.
Listens on port `6379`.

## Railway

| Setting | Value |
|---|---|
| Service name | `redis` |
| Root directory | `/infra/redis` |
| Watch paths | `/infra/redis/**` |
| Networking | Private only |

### Variables (food-catalog-service)

| Variable | Value |
|---|---|
| `REDIS_HOST` | `redis.railway.internal` |
| `REDIS_PORT` | `6379` |
| `FOOD_REDIS_ENABLED` | `true` |

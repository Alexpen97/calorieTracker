# infra/redis

Redis container configuration for the **local** Docker Compose setup.

- Uses the official `redis` image (no custom Dockerfile needed).
- `redis.conf` overrides live here if defaults ever need changing (memory
  limit, eviction policy for the product cache — `allkeys-lru` recommended).
- On Railway, a managed Redis instance replaces this container.

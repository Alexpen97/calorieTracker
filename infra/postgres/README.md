# infra/postgres

PostgreSQL container configuration for the **local** Docker Compose setup.

- Uses the official `postgres` image (no custom Dockerfile needed).
- `init/` holds `*.sql` scripts mounted into
  `/docker-entrypoint-initdb.d/` to create the per-service databases
  (`food_catalog`, `diary`, `users`) on first start.
- On Railway, managed PostgreSQL instances replace this container; schema
  creation there is handled by each service's Flyway migrations against its
  provisioned database.

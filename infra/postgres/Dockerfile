# syntax=docker/dockerfile:1
# PostgreSQL for NutriTrack — one instance, schema-per-service databases.
# Default credentials for Compose / Railway; override at runtime if needed.
FROM postgres:16-alpine

ENV POSTGRES_USER=nutritrack
ENV POSTGRES_PASSWORD=nutritrack
ENV POSTGRES_DB=postgres

COPY init/ /docker-entrypoint-initdb.d/

EXPOSE 5432

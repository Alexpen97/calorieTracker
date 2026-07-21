# syntax=docker/dockerfile:1
# PostgreSQL for NutriTrack — one instance, schema-per-service databases.
# Credentials and POSTGRES_DB come from runtime env (Compose / Railway variables).
FROM postgres:16-alpine

COPY init/ /docker-entrypoint-initdb.d/

EXPOSE 5432

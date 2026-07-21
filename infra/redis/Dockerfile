# syntax=docker/dockerfile:1
# Redis cache for food-catalog-service (product lookup TTL cache).
FROM redis:7-alpine

COPY redis.conf /usr/local/etc/redis/redis.conf

EXPOSE 6379

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]

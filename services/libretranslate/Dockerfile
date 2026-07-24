# syntax=docker/dockerfile:1
# Self-hosted LibreTranslate for NL→EN food-name translation before NEVO match.
# Official image: https://hub.docker.com/r/libretranslate/libretranslate
FROM libretranslate/libretranslate:latest

ENV LT_HOST=0.0.0.0
# Restrict models to keep image/memory smaller (Dutch ↔ English for NL products).
ENV LT_LOAD_ONLY=en,nl
ENV LT_UPDATE=true

# Image runs as non-root; set mode via COPY --chmod (no RUN chmod).
COPY --chmod=755 docker-entrypoint.sh /usr/local/bin/nutritrack-lt-entrypoint.sh

# Railway injects PORT; local Compose defaults to 5000 via entrypoint.
EXPOSE 5000

ENTRYPOINT ["/usr/local/bin/nutritrack-lt-entrypoint.sh"]

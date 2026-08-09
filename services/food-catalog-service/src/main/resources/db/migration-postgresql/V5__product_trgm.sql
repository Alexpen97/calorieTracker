CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_product_trgm
    ON product
    USING GIN (search_document gin_trgm_ops);

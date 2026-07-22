-- PostgreSQL-only full-text index on product.search_document
CREATE INDEX IF NOT EXISTS idx_product_fts
    ON product
    USING GIN (to_tsvector('english', coalesce(search_document, '')));

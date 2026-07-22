-- Phase 4: searchable document (H2 + PostgreSQL) and submission staging table

ALTER TABLE product ADD COLUMN search_document TEXT;

UPDATE product
SET search_document = LOWER(
    TRIM(
        COALESCE(name, '') || ' ' || COALESCE(brand, '')
    )
);

CREATE INDEX idx_product_search_document ON product (search_document);

CREATE TABLE product_submission (
    id                     UUID PRIMARY KEY,
    submitter_user_id      UUID NOT NULL,
    status                 VARCHAR(32) NOT NULL,
    barcode                VARCHAR(32),
    name                   TEXT NOT NULL,
    brand                  TEXT,
    serving_size_g         NUMERIC,
    nutrients              TEXT NOT NULL,
    submitted_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_by            UUID,
    reviewed_at            TIMESTAMP WITH TIME ZONE,
    review_note            TEXT,
    published_product_id   UUID REFERENCES product(id)
);

CREATE INDEX idx_product_submission_submitter ON product_submission (submitter_user_id);
CREATE INDEX idx_product_submission_status ON product_submission (status);
CREATE INDEX idx_product_submission_barcode ON product_submission (barcode);

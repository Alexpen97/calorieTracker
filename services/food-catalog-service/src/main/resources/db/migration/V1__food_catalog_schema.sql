-- Phase 2 food catalog schema (mirror + nutrient education; submissions/FTS in Phase 4)
CREATE TABLE product (
    id                 UUID PRIMARY KEY,
    barcode            VARCHAR(32) UNIQUE,
    source             VARCHAR(32) NOT NULL,
    name               TEXT NOT NULL,
    brand              TEXT,
    quantity_label     TEXT,
    serving_size_g     NUMERIC,
    image_url          TEXT,
    nutri_score        VARCHAR(1),
    ingredients_text   TEXT,
    allergen_tags      TEXT,
    off_last_synced_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE product_nutrient (
    product_id         UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    nutrient_code      VARCHAR(64) NOT NULL,
    amount_per_100g    NUMERIC NOT NULL,
    unit               VARCHAR(16) NOT NULL,
    PRIMARY KEY (product_id, nutrient_code)
);

CREATE TABLE nutrient (
    code                 VARCHAR(64) PRIMARY KEY,
    display_name         TEXT NOT NULL,
    category             VARCHAR(32) NOT NULL,
    default_unit         VARCHAR(16) NOT NULL,
    description          TEXT,
    body_effects         TEXT,
    deficiency_effects   TEXT,
    excess_effects       TEXT,
    common_sources       TEXT,
    content_source       TEXT
);

CREATE TABLE nutrient_reference_intake (
    nutrient_code   VARCHAR(64) NOT NULL REFERENCES nutrient(code),
    sex             VARCHAR(16) NOT NULL,
    age_min         SMALLINT NOT NULL,
    age_max         SMALLINT NOT NULL,
    daily_amount    NUMERIC NOT NULL,
    unit            VARCHAR(16) NOT NULL,
    basis           VARCHAR(16) NOT NULL,
    PRIMARY KEY (nutrient_code, sex, age_min)
);

CREATE INDEX idx_product_barcode ON product(barcode);

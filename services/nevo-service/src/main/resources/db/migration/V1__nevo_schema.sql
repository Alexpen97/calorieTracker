CREATE TABLE nevo_import_run (
    id              UUID PRIMARY KEY,
    csv_filename    TEXT NOT NULL,
    nevo_version    TEXT NOT NULL,
    food_count      INTEGER NOT NULL DEFAULT 0,
    nutrient_count  INTEGER NOT NULL DEFAULT 0,
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at     TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(32) NOT NULL,
    error_message   TEXT
);

CREATE TABLE nevo_food (
    nevo_code           VARCHAR(32) PRIMARY KEY,
    food_name_en        TEXT NOT NULL,
    food_name_nl        TEXT,
    food_group          TEXT,
    synonym             TEXT,
    quantity_label      TEXT,
    remark              TEXT,
    nevo_version        TEXT NOT NULL,
    search_document     TEXT NOT NULL,
    energy_kcal         NUMERIC,
    protein_g           NUMERIC,
    fat_g               NUMERIC,
    carbohydrate_g      NUMERIC,
    sugars_g            NUMERIC,
    fiber_g             NUMERIC,
    sodium_mg           NUMERIC
);

CREATE TABLE nevo_nutrient_value (
    id                  UUID PRIMARY KEY,
    nevo_code           VARCHAR(32) NOT NULL REFERENCES nevo_food(nevo_code) ON DELETE CASCADE,
    nutrient_code       VARCHAR(64),
    nevo_column         TEXT NOT NULL,
    amount_per_100g     NUMERIC,
    unit                VARCHAR(16),
    raw_value           TEXT
);

CREATE TABLE nevo_alias (
    id              UUID PRIMARY KEY,
    alias_term      TEXT NOT NULL,
    canonical_term  TEXT NOT NULL
);

CREATE UNIQUE INDEX uq_nevo_alias_term ON nevo_alias(alias_term);
CREATE INDEX idx_nevo_food_search ON nevo_food(search_document);
CREATE INDEX idx_nevo_food_group ON nevo_food(food_group);
CREATE INDEX idx_nevo_nutrient_code ON nevo_nutrient_value(nevo_code, nutrient_code);

INSERT INTO nevo_alias (id, alias_term, canonical_term) VALUES
  ('11111111-1111-1111-1111-111111111101', 'yoghurt', 'yogurt'),
  ('11111111-1111-1111-1111-111111111103', 'cornflakes', 'corn flakes'),
  ('11111111-1111-1111-1111-111111111105', 'soya', 'soy'),
  ('11111111-1111-1111-1111-111111111106', 'soymilk', 'soy drink'),
  ('11111111-1111-1111-1111-111111111107', 'plant based', 'plant-based'),
  ('11111111-1111-1111-1111-111111111108', 'semi skimmed', 'semi-skimmed'),
  ('11111111-1111-1111-1111-111111111109', 'wholemeal', 'whole wheat');

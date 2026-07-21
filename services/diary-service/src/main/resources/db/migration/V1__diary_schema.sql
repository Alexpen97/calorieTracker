CREATE TABLE diary_entry (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL,
    product_id     UUID NULL,
    submission_id  UUID NULL,
    product_name   TEXT NOT NULL,
    brand          TEXT NULL,
    weight_g       NUMERIC NOT NULL,
    meal_type      VARCHAR(16) NOT NULL,
    consumed_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_diary_entry_user_consumed ON diary_entry(user_id, consumed_at DESC);

CREATE TABLE diary_entry_nutrient (
    entry_id         UUID NOT NULL REFERENCES diary_entry(id) ON DELETE CASCADE,
    nutrient_code    VARCHAR(64) NOT NULL,
    amount_per_100g  NUMERIC NOT NULL,
    unit             VARCHAR(16) NOT NULL,
    PRIMARY KEY (entry_id, nutrient_code)
);

CREATE TABLE water_intake (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    amount_ml  NUMERIC NOT NULL,
    logged_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_water_intake_user_logged ON water_intake(user_id, logged_at DESC);

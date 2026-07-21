-- Phase 1 user schema
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    google_sub    VARCHAR(64) NOT NULL UNIQUE,
    email         TEXT NOT NULL,
    display_name  TEXT NOT NULL,
    avatar_url    TEXT NULL,
    role          VARCHAR(32) NOT NULL DEFAULT 'USER',
    sex           VARCHAR(16) NULL,
    birth_date    DATE NULL,
    height_cm     NUMERIC NULL,
    activity_level VARCHAR(32) NULL,
    objective     VARCHAR(16) NOT NULL DEFAULT 'MAINTAIN',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE body_weight_log (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES app_user(id),
    weight_kg    NUMERIC NOT NULL,
    measured_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_body_weight_user_measured ON body_weight_log(user_id, measured_at DESC);

CREATE TABLE user_goal (
    user_id        UUID NOT NULL REFERENCES app_user(id),
    nutrient_code  VARCHAR(64) NOT NULL,
    daily_target   NUMERIC NOT NULL,
    unit           VARCHAR(16) NOT NULL,
    origin         VARCHAR(32) NOT NULL,
    computed_at    TIMESTAMP WITH TIME ZONE NULL,
    PRIMARY KEY (user_id, nutrient_code)
);

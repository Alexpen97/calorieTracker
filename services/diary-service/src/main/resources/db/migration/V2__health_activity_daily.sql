CREATE TABLE health_activity_daily (
    id                   UUID PRIMARY KEY,
    user_id              UUID NOT NULL,
    provider             VARCHAR(32) NOT NULL,
    local_date           DATE NOT NULL,
    zone_id              VARCHAR(64) NOT NULL,
    active_energy_kcal   NUMERIC NULL,
    total_energy_kcal    NUMERIC NULL,
    selected_burn_kcal   NUMERIC NOT NULL,
    source_record_count  INTEGER NOT NULL,
    synced_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    permission_state     VARCHAR(32) NOT NULL
);

CREATE UNIQUE INDEX uq_health_activity_user_provider_date_zone
    ON health_activity_daily(user_id, provider, local_date, zone_id);

CREATE INDEX idx_health_activity_user_date
    ON health_activity_daily(user_id, local_date);

CREATE TABLE health_integration_connection (
    user_id          UUID NOT NULL,
    provider         VARCHAR(32) NOT NULL,
    connected        BOOLEAN NOT NULL,
    permission_state VARCHAR(32) NOT NULL,
    last_synced_at   TIMESTAMP WITH TIME ZONE NULL,
    last_error       TEXT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, provider)
);

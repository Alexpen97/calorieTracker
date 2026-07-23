CREATE TABLE enrichment_lookup (
  barcode              VARCHAR(32) PRIMARY KEY,
  match_type           VARCHAR(16) NOT NULL,
  fdc_id               BIGINT NULL,
  matched_description  TEXT NULL,
  confidence           NUMERIC NULL,
  nutrients_json       TEXT NOT NULL,
  created_at           TIMESTAMP WITH TIME ZONE NOT NULL
);

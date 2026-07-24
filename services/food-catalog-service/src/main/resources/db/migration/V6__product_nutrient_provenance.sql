-- Provenance for nutrient amounts (OFF vs NEVO estimates).
ALTER TABLE product_nutrient ADD COLUMN source VARCHAR(32);
ALTER TABLE product_nutrient ADD COLUMN source_ref VARCHAR(64);
ALTER TABLE product_nutrient ADD COLUMN confidence VARCHAR(16);
ALTER TABLE product_nutrient ADD COLUMN estimated BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE product_nutrient SET source = 'OFF' WHERE source IS NULL;

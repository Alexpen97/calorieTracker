-- Extra provenance for estimated micros (NEVO / USDA audit fields).
ALTER TABLE product_nutrient ADD COLUMN source_ref VARCHAR(64);
ALTER TABLE product_nutrient ADD COLUMN confidence VARCHAR(16);
ALTER TABLE product_nutrient ADD COLUMN estimated BOOLEAN NOT NULL DEFAULT FALSE;

-- Widen source for NEVO_ESTIMATE and future values.
ALTER TABLE product_nutrient ALTER COLUMN source TYPE VARCHAR(32);

UPDATE product_nutrient
SET estimated = TRUE
WHERE source IN ('USDA_BRANDED', 'USDA_PROXY', 'NEVO_ESTIMATE');

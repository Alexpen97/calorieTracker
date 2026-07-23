-- Track provenance of each nutrient amount (OFF measured vs USDA estimated vs user).
ALTER TABLE product_nutrient
  ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'OFF';

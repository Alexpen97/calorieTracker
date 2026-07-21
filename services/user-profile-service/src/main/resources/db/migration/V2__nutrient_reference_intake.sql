CREATE TABLE nutrient_reference_intake (
    nutrient_code   VARCHAR(64) NOT NULL,
    sex             VARCHAR(16) NOT NULL,
    age_min         SMALLINT NOT NULL,
    age_max         SMALLINT NOT NULL,
    daily_amount    NUMERIC NOT NULL,
    unit            VARCHAR(16) NOT NULL,
    basis           VARCHAR(16) NOT NULL,
    PRIMARY KEY (nutrient_code, sex, age_min)
);

INSERT INTO nutrient_reference_intake (nutrient_code, sex, age_min, age_max, daily_amount, unit, basis) VALUES
('protein', 'MALE', 19, 50, 0.83, 'g', 'PER_KG'),
('protein', 'FEMALE', 19, 50, 0.83, 'g', 'PER_KG'),
('fiber', 'MALE', 19, 50, 35, 'g', 'FIXED'),
('fiber', 'FEMALE', 19, 50, 25, 'g', 'FIXED'),
('vitamin_c', 'MALE', 19, 50, 90, 'mg', 'FIXED'),
('vitamin_c', 'FEMALE', 19, 50, 75, 'mg', 'FIXED'),
('vitamin_d', 'MALE', 19, 50, 15, 'µg', 'FIXED'),
('vitamin_d', 'FEMALE', 19, 50, 15, 'µg', 'FIXED'),
('vitamin_b12', 'MALE', 19, 50, 2.4, 'µg', 'FIXED'),
('vitamin_b12', 'FEMALE', 19, 50, 2.4, 'µg', 'FIXED'),
('calcium', 'MALE', 19, 50, 1000, 'mg', 'FIXED'),
('calcium', 'FEMALE', 19, 50, 1000, 'mg', 'FIXED'),
('iron', 'MALE', 19, 50, 8, 'mg', 'FIXED'),
('iron', 'FEMALE', 19, 50, 18, 'mg', 'FIXED'),
('magnesium', 'MALE', 19, 50, 400, 'mg', 'FIXED'),
('magnesium', 'FEMALE', 19, 50, 310, 'mg', 'FIXED'),
('zinc', 'MALE', 19, 50, 11, 'mg', 'FIXED'),
('zinc', 'FEMALE', 19, 50, 8, 'mg', 'FIXED'),
('potassium', 'MALE', 19, 50, 3400, 'mg', 'FIXED'),
('potassium', 'FEMALE', 19, 50, 2600, 'mg', 'FIXED'),
('sodium', 'MALE', 19, 50, 2300, 'mg', 'FIXED'),
('sodium', 'FEMALE', 19, 50, 2300, 'mg', 'FIXED');

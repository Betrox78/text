-- 1546282268 DOWN add-penalty-days

ALTER TABLE rental
DROP COLUMN penalty_days;

ALTER TABLE rental
DROP COLUMN penalty_amount;
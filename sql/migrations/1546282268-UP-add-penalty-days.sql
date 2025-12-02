-- 1546282268 UP add-penalty-days

ALTER TABLE rental
ADD COLUMN penalty_days int(11) DEFAULT NULL AFTER extra_charges;

ALTER TABLE rental
ADD COLUMN penalty_amount decimal(12, 2) DEFAULT '0.00' AFTER penalty_days;
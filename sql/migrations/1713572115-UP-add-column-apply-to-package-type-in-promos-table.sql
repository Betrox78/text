-- 1713572115 UP add column apply to package type in promos table
ALTER TABLE promos
ADD COLUMN apply_to_package_type VARCHAR(50) DEFAULT NULL AFTER apply_to_package_price_distance;
-- 1571351451 UP add apply_to_package_price_distance column in promos table
ALTER TABLE promos
ADD COLUMN apply_to_package_price_distance VARCHAR(255) DEFAULT NULL AFTER apply_to_package_price;
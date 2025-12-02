-- 1564185498 DOWN drop column discount code id of parcels packages table
ALTER TABLE parcels_packages
ADD COLUMN discount_code_id int(11) DEFAULT NULL AFTER discount;
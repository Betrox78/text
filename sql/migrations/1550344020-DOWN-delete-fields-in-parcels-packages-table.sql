-- 1550344020 DOWN delete-fields-in-parcels-packages-table
ALTER TABLE parcels_packages
ADD COLUMN has_insurance tinyint(1) NOT NULL DEFAULT 0 AFTER discount_code_id,
ADD COLUMN insurance_value decimal(12,2) NOT NULL DEFAULT 0 AFTER has_insurance,
ADD COLUMN insurance_amount decimal(12,2) NOT NULL DEFAULT 0 AFTER insurance_value;
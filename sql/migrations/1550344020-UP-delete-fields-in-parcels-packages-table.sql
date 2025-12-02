-- 1550344020 UP delete-fields-in-parcels-packages-table
ALTER TABLE parcels_packages
DROP COLUMN has_insurance,
DROP COLUMN insurance_value,
DROP COLUMN insurance_amount;
-- 1550344963 UP add-insurance-value-column-in-parcels-table
ALTER TABLE parcels
ADD COLUMN insurance_value decimal(12,2) NOT NULL DEFAULT 0 AFTER has_insurance;
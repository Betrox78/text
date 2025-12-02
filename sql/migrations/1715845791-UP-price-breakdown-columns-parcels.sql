-- 1715845791 UP price-breakdown-columns-parcels
ALTER TABLE parcels
ADD COLUMN services_amount DECIMAL(12,2) DEFAULT 0.00 NOT NULL AFTER discount;
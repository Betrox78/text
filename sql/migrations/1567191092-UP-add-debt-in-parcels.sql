-- 1567191092 UP add-debt-in-parcels
ALTER TABLE parcels
ADD COLUMN debt FLOAT(12,2) DEFAULT 0 AFTER payment_condition;
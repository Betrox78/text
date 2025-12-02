-- 1570124463 DOWN promise delivery date column in parcels table was added
ALTER TABLE parcels
DROP COLUMN promise_delivery_date,
ADD COLUMN delivery_time INT(11) NOT NULL DEFAULT 24 AFTER total_packages;
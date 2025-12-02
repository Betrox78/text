-- 1570124463 UP promise delivery date column in parcels table was added
ALTER TABLE parcels
DROP COLUMN delivery_time,
ADD COLUMN promise_delivery_date DATETIME DEFAULT NULL AFTER total_packages;
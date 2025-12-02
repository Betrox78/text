-- 1570145622 UP delivered at column in parcels table
ALTER TABLE parcels
ADD COLUMN delivered_at DATETIME DEFAULT NULL AFTER status;
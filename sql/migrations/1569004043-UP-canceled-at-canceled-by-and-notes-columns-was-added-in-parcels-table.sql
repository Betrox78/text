-- 1569004043 UP canceled at canceled by and notes columns was added in parcels table
ALTER TABLE parcels
ADD COLUMN notes TEXT DEFAULT NULL AFTER parcel_status,
ADD COLUMN canceled_at DATETIME DEFAULT NULL AFTER updated_by,
ADD COLUMN canceled_by INT(11) DEFAULT NULL AFTER canceled_at;
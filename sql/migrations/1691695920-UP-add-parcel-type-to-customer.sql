-- 1691695920 UP add-parcel-type-to-customer
ALTER TABLE customer
ADD COLUMN parcel_type ENUM('guiapp', 'agreement') DEFAULT NULL;
-- 1545163085 UP change-default-parcel-status
ALTER TABLE parcels
MODIFY COLUMN parcel_status int(11) NOT NULL DEFAULT 0;
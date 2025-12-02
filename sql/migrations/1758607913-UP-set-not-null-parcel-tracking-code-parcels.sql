-- 1758607913 UP set not null parcel tracking code parcels
ALTER TABLE parcels
CHANGE COLUMN parcel_tracking_code VARCHAR(60) NOT NULL ;
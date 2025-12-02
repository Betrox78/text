-- 1758607913 DOWN set not null parcel tracking code parcels
ALTER TABLE parcels
CHANGE COLUMN parcel_tracking_code VARCHAR(60) DEFAULT NULL ;
-- 1730869957 DOWN add unique index parcel tracking code
ALTER TABLE parcels
DROP INDEX parcels_parcel_tracking_code_idx,
ADD INDEX parcels_parcel_tracking_code_idx(parcel_tracking_code);
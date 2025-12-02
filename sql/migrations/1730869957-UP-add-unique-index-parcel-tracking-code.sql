-- 1730869957 UP add unique index parcel tracking code
ALTER TABLE parcels
DROP INDEX parcels_parcel_tracking_code_idx,
ADD UNIQUE INDEX parcels_parcel_tracking_code_idx(parcel_tracking_code);
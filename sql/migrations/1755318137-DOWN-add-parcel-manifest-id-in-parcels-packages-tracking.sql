-- 1755318137 DOWN add parcel manifest id in parcels packages tracking
ALTER TABLE parcels_packages_tracking
DROP FOREIGN KEY fk_parcels_packages_tracking_parcel_manifest_id;

ALTER TABLE parcels_packages_tracking
DROP COLUMN parcel_manifest_id;
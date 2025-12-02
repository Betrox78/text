-- 1755318137 UP add parcel manifest id in parcels packages tracking
ALTER TABLE parcels_packages_tracking
ADD COLUMN parcel_manifest_id INT DEFAULT NULL AFTER parcel_package_id,
ADD CONSTRAINT fk_parcels_packages_tracking_parcel_manifest_id
	FOREIGN KEY (parcel_manifest_id) REFERENCES parcels_manifest(id) ON DELETE SET NULL ON UPDATE CASCADE;
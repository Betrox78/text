-- 1544042826 DOWN modify-parcel-package-id
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN parcel_package_id int(11) NOT NULL;
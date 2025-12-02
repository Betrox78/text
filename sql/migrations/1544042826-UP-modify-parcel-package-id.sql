-- 1544042826 UP modify-parcel-package-id
ALTER TABLE parcels_packages_tracking
MODIFY COLUMN parcel_package_id int(11) DEFAULT NULL;
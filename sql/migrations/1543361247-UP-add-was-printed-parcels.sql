-- 1543361247 UP add-was-printed-parcels

ALTER TABLE parcels
ADD COLUMN was_printed tinyint(1) DEFAULT 0 AFTER parcel_tracking_code;

ALTER TABLE parcels_packages
ADD COLUMN was_printed tinyint(1) DEFAULT 0 AFTER package_code;

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed', 'printed') DEFAULT NULL;
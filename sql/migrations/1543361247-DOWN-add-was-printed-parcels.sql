-- 1543361247 DOWN add-was-printed-parcels
ALTER TABLE parcels
DROP COLUMN was_printed;

ALTER TABLE parcels_packages
DROP COLUMN was_printed;

ALTER TABLE parcels_packages_tracking
MODIFY COLUMN action enum('register','paid','move','intransit','loaded','downloaded','incidence','canceled','closed') DEFAULT NULL;
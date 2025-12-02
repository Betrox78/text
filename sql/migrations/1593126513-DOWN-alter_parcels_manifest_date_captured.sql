-- 1593126513 DOWN alter_parcels_manifest_date_captured
ALTER TABLE `parcels_manifest`
DROP COLUMN `date_captured`;
ALTER TABLE `parcels_rad_ead`
DROP COLUMN `collection_attempts`;
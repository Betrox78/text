-- 1593126513 UP alter_parcels_manifest_date_captured
ALTER TABLE `parcels_manifest`
ADD COLUMN `date_captured` DATETIME NULL AFTER `updated_by`;
ALTER TABLE `parcels_rad_ead`
ADD COLUMN `collection_attempts` INT NOT NULL DEFAULT 0 AFTER `confirme_rad`;
ALTER TABLE `parcels_manifest`
CHANGE COLUMN `printing_date_updated_at` `printing_date_updated_at` DATETIME NULL DEFAULT NULL ;

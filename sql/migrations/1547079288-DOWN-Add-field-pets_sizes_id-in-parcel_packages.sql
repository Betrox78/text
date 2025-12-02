-- 1547079288 DOWN Add field pets_sizes_id in parcel_packages
ALTER TABLE parcels_packages
DROP FOREIGN KEY `parcels_packages_pets_sizes_id_fk`;
ALTER TABLE parcels_packages
DROP COLUMN `pets_sizes_id`,
DROP INDEX `parcels_packages_pets_sizes_id_fk_idx`;
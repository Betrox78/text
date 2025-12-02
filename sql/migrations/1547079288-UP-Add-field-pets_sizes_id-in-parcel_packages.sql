-- 1547079288 UP Add field pets_sizes_id in parcel_packages
ALTER TABLE `parcels_packages` 
ADD COLUMN `pets_sizes_id` INT NULL DEFAULT NULL AFTER `excess_cost`,
ADD INDEX `parcels_packages_pets_sizes_id_fk_idx` (`pets_sizes_id` ASC);
;
ALTER TABLE `parcels_packages` 
ADD CONSTRAINT `parcels_packages_pets_sizes_id_fk`
  FOREIGN KEY (`pets_sizes_id`)
  REFERENCES `pets_sizes` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
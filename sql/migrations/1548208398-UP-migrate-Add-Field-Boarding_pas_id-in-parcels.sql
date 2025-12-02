-- 1548208398 UP migrate Add Field Boarding_pas_id in parcels
ALTER TABLE `parcels` 
ADD COLUMN `boarding_pass_id` INT NULL AFTER `customer_id`,
ADD INDEX `parcels_boarding_pass_id_fk_idx` (`boarding_pass_id` ASC);
;
ALTER TABLE `parcels` 
ADD CONSTRAINT `parcels_boarding_pass_id_fk`
  FOREIGN KEY (`boarding_pass_id`)
  REFERENCES `boarding_pass` (`id`)
  ON DELETE RESTRICT
  ON UPDATE RESTRICT;
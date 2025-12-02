-- 1548208440 UP migrate Add Field Parcel_id In Boarding_pass_ticket
ALTER TABLE `boarding_pass_ticket` 
ADD COLUMN `parcel_id` INT NULL AFTER `config_ticket_price_id`,
ADD INDEX `fk_boarding_pass_ticket_parcel_id_idx` (`parcel_id` ASC);

ALTER TABLE `boarding_pass_ticket` 
ADD CONSTRAINT `fk_boarding_pass_ticket_parcel_id`
  FOREIGN KEY (`parcel_id`)
  REFERENCES `parcels` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
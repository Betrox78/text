-- 1548208440 DOWN migrate Add Field Parcel_id In Boarding_pass_ticket
ALTER TABLE `boarding_pass_ticket` 
DROP FOREIGN KEY `fk_boarding_pass_ticket_parcel_id`;
ALTER TABLE `boarding_pass_ticket` 
DROP COLUMN `parcel_id`,
DROP INDEX `fk_boarding_pass_ticket_parcel_id_idx` ;
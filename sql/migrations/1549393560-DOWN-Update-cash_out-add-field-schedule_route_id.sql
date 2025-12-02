-- 1549393560 DOWN Update cash_out add field schedule_route_id
ALTER TABLE `cash_out` 
DROP FOREIGN KEY `fk_cash_out_branchoffice_id`,
DROP FOREIGN KEY `fk_cash_out_schedule_route_id`;
ALTER TABLE `cash_out` 
DROP COLUMN `cash_out_origin`,
DROP COLUMN `schedule_route_id`,
CHANGE COLUMN `branchoffice_id` `branchoffice_id` INT(11) NOT NULL ,
DROP INDEX `fk_cash_out_schedule_route_id_idx` ;

ALTER TABLE `cash_out` 
ADD CONSTRAINT `fk_cash_out_branchoffice_id`
  FOREIGN KEY (`branchoffice_id`)
  REFERENCES `branchoffice` (`id`);
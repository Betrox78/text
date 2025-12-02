-- 1549393560 UP Update cash_out add field schedule_route_id
ALTER TABLE `cash_out` 
DROP FOREIGN KEY `fk_cash_out_branchoffice_id`;
ALTER TABLE `cash_out` 
ADD COLUMN `schedule_route_id` INT(11) NULL DEFAULT NULL AFTER `branchoffice_id`,
ADD COLUMN `cash_out_origin` ENUM('branchoffice', 'driver') NOT NULL DEFAULT 'branchoffice' AFTER `employee_id`,
CHANGE COLUMN `branchoffice_id` `branchoffice_id` INT(11) NULL DEFAULT NULL ,
ADD INDEX `fk_cash_out_schedule_route_id_idx` (`schedule_route_id` ASC);

ALTER TABLE `cash_out` 
ADD CONSTRAINT `fk_cash_out_branchoffice_id`
  FOREIGN KEY (`branchoffice_id`)
  REFERENCES `branchoffice` (`id`),
ADD CONSTRAINT `fk_cash_out_schedule_route_id`
  FOREIGN KEY (`schedule_route_id`)
  REFERENCES `schedule_route` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
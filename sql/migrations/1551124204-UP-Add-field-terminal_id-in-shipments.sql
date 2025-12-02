-- 1551124204 UP Add field terminal_id in shipments
ALTER TABLE `shipments` 
ADD COLUMN `terminal_id` INT(11) NOT NULL AFTER `schedule_route_id`,
ADD INDEX `fk_shipments_branchoffice_id_idx` (`terminal_id` ASC);
ALTER TABLE `shipments` 
ADD CONSTRAINT `fk_shipments_branchoffice_id`
  FOREIGN KEY (`terminal_id`)
  REFERENCES `branchoffice` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
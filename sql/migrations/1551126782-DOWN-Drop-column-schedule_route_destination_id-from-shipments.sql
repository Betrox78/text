-- 1551126782 DOWN Drop column schedule_route_destination_id from shipments
ALTER TABLE `shipments` 
ADD COLUMN `schedule_route_destination_id` INT(11) NOT NULL AFTER `schedule_route_id`,
ADD INDEX `fk_shimpments_schedule_route_destination_id_idx` (`schedule_route_destination_id` ASC);
ALTER TABLE `shipments` 
ADD CONSTRAINT `fk_shipments_schedule_route_destination_id`
  FOREIGN KEY (`schedule_route_destination_id`)
  REFERENCES `schedule_route_destination_id` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
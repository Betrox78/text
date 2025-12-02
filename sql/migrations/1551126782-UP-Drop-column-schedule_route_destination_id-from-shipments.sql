-- 1551126782 UP Drop column schedule_route_destination_id from shipments
ALTER TABLE `shipments` 
DROP FOREIGN KEY `fk_shipments_schedule_route_destination_id`;
ALTER TABLE `shipments` 
DROP COLUMN `schedule_route_destination_id`,
DROP INDEX `fk_shimpments_schedule_route_destination_id_idx` ;
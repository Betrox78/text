-- 1548983399 DOWN Update travel status from int to enum in schedule_route_destination
ALTER TABLE `schedule_route_destination` 
CHANGE COLUMN `destination_status` `destination_status` INT(11) NULL DEFAULT NULL ;

-- 1548896564 DOWN Update travel status from in to enum in schedule_route
ALTER TABLE `schedule_route` 
CHANGE COLUMN `schedule_status` `schedule_status` INT(11) NULL DEFAULT NULL ;
-- 1548983399 UP Update travel status from int to enum in schedule_route_destination
ALTER TABLE `schedule_route_destination` 
CHANGE COLUMN `destination_status` `destination_status` ENUM('canceled', 'scheduled', 'loading', 'ready-to-go', 'in-transit', 'stopped', 'downloading', 'ready-to-load', 'paused', 'finished-ok') NULL ;

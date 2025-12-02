-- 1548896564 UP Update travel status from in to enum in schedule_route
ALTER TABLE `schedule_route` 
CHANGE COLUMN `schedule_status` `schedule_status` ENUM('canceled', 'scheduled', 'loading', 'ready-to-go', 'in-transit', 'stopped', 'downloading', 'ready-to-load', 'paused', 'finished-ok') NULL DEFAULT 'scheduled' ;

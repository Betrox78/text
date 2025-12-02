-- 1550511431 DOWN update-destination-status
ALTER TABLE `schedule_route_destination`
CHANGE COLUMN `destination_status` `destination_status` ENUM('canceled', 'scheduled', 'loading', 'ready-to-go', 'in-transit', 'stopped', 'downloading', 'ready-to-load', 'paused', 'finished-ok') NULL ;
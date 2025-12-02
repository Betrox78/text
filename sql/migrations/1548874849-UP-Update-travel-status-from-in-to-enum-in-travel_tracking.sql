-- 1548874849 UP Update travel status from in to enum in travel_tracking
ALTER TABLE `travel_tracking` 
CHANGE COLUMN `status` `status` ENUM('canceled','scheduled','loading','ready-to-go','in-transit','stopped','downloading','ready-to-load','paused','finished-ok') NULL DEFAULT 'scheduled' ;
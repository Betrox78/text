-- 1548874849 DOWN Update travel status from in to enum in travel_tracking
ALTER TABLE `travel_tracking` 
CHANGE COLUMN `status` `status` INT(11) NULL DEFAULT NULL ;
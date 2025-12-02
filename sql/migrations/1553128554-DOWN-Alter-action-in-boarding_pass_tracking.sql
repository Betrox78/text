-- 1553128554 DOWN Alter action in boarding_pass_tracking
ALTER TABLE `abordo_dev`.`boarding_pass_tracking` 
CHANGE COLUMN `action` `action` ENUM('created', 'changed-passenger', 'changed-date', 'changed-route', 'checkin', 'loaded', 'downloaded', 'canceled', 'printed', 'init-route', 'finished') NOT NULL ;
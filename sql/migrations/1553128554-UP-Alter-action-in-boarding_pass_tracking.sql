-- 1553128554 UP Alter action in boarding_pass_tracking
ALTER TABLE `boarding_pass_tracking` 
CHANGE COLUMN `action` `action` ENUM('created', 'changed-passenger', 'changed-date', 'changed-route', 'checkin', 'loaded', 'downloaded', 'canceled', 'printed', 'intransit', 'finished') NOT NULL ;
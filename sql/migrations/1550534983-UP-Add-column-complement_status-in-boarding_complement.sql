-- 1550534983 UP Add column complement_status in boarding_complement
ALTER TABLE `boarding_pass_complement` 
ADD COLUMN `complement_status` INT(11) NOT NULL DEFAULT 1 AFTER `tracking_code`;
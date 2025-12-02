-- 1546994866 UP ADD allow_frozen field to config_vehicle
ALTER TABLE `config_vehicle` 
ADD COLUMN `allow_frozen` TINYINT(1) NULL DEFAULT 0 AFTER `is_base`;
-- 1546993819 UP ADD allow_pets field to config_vehicle
ALTER TABLE `config_vehicle` 
ADD COLUMN `allow_pets` TINYINT(1) NULL DEFAULT 0 AFTER `is_base`;
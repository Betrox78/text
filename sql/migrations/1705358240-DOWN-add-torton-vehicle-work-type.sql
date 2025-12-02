-- 1705358240 DOWN add-torton-vehicle-work-type
ALTER TABLE vehicle
MODIFY COLUMN work_type ENUM('0', '1', '2', '3', '4') NOT NULL;
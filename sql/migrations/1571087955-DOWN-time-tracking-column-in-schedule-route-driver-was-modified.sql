-- 1571087955 DOWN time tracking column in schedule route driver was modified
ALTER TABLE schedule_route_driver
MODIFY COLUMN time_tracking VARCHAR(5) DEFAULT NULL AFTER employee_id;
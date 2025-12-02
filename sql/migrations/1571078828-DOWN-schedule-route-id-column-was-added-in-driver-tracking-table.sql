-- 1571078828 DOWN schedule route id column was added in driver tracking table
ALTER TABLE driver_tracking
DROP FOREIGN KEY driver_tracking_schedule_route_id_fk,
DROP COLUMN schedule_route_id;
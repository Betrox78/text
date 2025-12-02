-- 1571078828 UP schedule route id column was added in driver tracking table
ALTER TABLE driver_tracking
ADD COLUMN schedule_route_id INT(11) DEFAULT NULL AFTER rental_id,
ADD CONSTRAINT driver_tracking_schedule_route_id_fk FOREIGN KEY (schedule_route_id) REFERENCES schedule_route(id);
-- 1581639199 UP create table schedule route vehicle tracking
CREATE TABLE IF NOT EXISTS schedule_route_vehicle_tracking(
 id INT(11) AUTO_INCREMENT NOT NULL,
 schedule_route_id INT(11) NOT NULL,
 vehicle_id INT(11) NOT NULL,
 action ENUM('assigned','change-vehicle') NOT NULL,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_by int(11) NOT NULL,
 PRIMARY KEY (id),
 CONSTRAINT fk_schedule_route_vehicle_tracking_schedule_route_id FOREIGN KEY (schedule_route_id) REFERENCES schedule_route(id) ON DELETE CASCADE,
 CONSTRAINT fk_schedule_route_vehicle_tracking_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON DELETE CASCADE)
ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- 1702496207 UP second employee schedule route driver
ALTER TABLE schedule_route_driver
ADD COLUMN second_employee_id INT(11) DEFAULT NULL AFTER employee_id,
ADD CONSTRAINT fk_schedule_route_driver_second_employee_id
FOREIGN KEY (second_employee_id) REFERENCES employee(id) ON DELETE NO ACTION ON UPDATE NO ACTION;
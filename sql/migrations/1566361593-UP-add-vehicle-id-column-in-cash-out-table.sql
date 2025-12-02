-- 1566361593 UP add-vehicle id column in cash out table
ALTER TABLE cash_out
ADD COLUMN vehicle_id INT(11) DEFAULT NULL AFTER employee_id,
ADD CONSTRAINT fk_cash_out_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicle(id);
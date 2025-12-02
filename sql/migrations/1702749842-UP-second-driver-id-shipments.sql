-- 1702749842 UP second driver id shipments
ALTER TABLE shipments
ADD COLUMN second_driver_id INT(11) DEFAULT NULL AFTER driver_id,
ADD CONSTRAINT fk_shipments_second_driver_id
FOREIGN KEY (second_driver_id) REFERENCES employee(id) ON DELETE NO ACTION ON UPDATE NO ACTION;
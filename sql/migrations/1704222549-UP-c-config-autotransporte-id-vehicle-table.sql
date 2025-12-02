-- 1704222549 UP c config autotransporte id vehicle table
ALTER TABLE vehicle
ADD COLUMN c_ConfigAutotransporte_id INT(11) DEFAULT NULL AFTER can_use_trailer,
ADD CONSTRAINT fk_vehicle_c_ConfigAutotransporte_id
FOREIGN KEY (c_ConfigAutotransporte_id) REFERENCES c_ConfigAutotransporte(id) ON DELETE NO ACTION ON UPDATE NO ACTION;
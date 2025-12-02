-- 1552435056 UP Add fields yo driver_tracking
ALTER TABLE driver_tracking 
ADD COLUMN type_tracking ENUM('bus', 'van') NOT NULL DEFAULT 'bus' AFTER id,
ADD COLUMN vehicle_id INT(11) NOT NULL AFTER type_tracking,
ADD COLUMN rental_id INT(11) NULL AFTER vehicle_id,
ADD COLUMN terminal_origin_id INT(11) NOT NULL AFTER rental_id,
ADD COLUMN terminal_destiny_id INT(11) NULL AFTER terminal_origin_id,
ADD COLUMN action ENUM('driving', 'waiting') NOT NULL DEFAULT 'driving' AFTER status,
CHANGE COLUMN time_tracking time_tracking TIME NULL DEFAULT NULL ,
CHANGE COLUMN status status ENUM('in-transit', 'finished') NOT NULL DEFAULT 'in-transit' ,
ADD INDEX fk_driver_tracking_vehicle_id_idx (vehicle_id ASC),
ADD INDEX fk_driver_tracking_branchoffice_origin_id_idx (terminal_origin_id ASC),
ADD INDEX fk_driver_tracking_branchofficer_destiny_id_idx (terminal_destiny_id ASC);

ALTER TABLE driver_tracking 
ADD CONSTRAINT fk_driver_tracking_vehicle_id
  FOREIGN KEY (vehicle_id)
  REFERENCES vehicle (id)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT fk_driver_tracking_branchoffice_origin_id
  FOREIGN KEY (terminal_origin_id)
  REFERENCES branchoffice (id)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
ADD CONSTRAINT fk_driver_tracking_branchofficer_destiny_id
  FOREIGN KEY (terminal_destiny_id)
  REFERENCES branchoffice (id)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
-- 1550170895 UP Create table shipments
CREATE TABLE shipments (
  id INT NOT NULL AUTO_INCREMENT,
  schedule_route_id INT(11) NOT NULL,
  schedule_route_destination_id INT(11) NOT NULL,
  shipment_type ENUM('load', 'download') NOT NULL,
  shipment_status INT(11) NOT NULL DEFAULT 1,
  driver_id INT(11) NULL,
  left_stamp VARCHAR(100) NOT NULL,
  right_stamp VARCHAR(100) NOT NULL,
  total_tickets INT(11) NOT NULL,
  total_complements INT(11) NOT NULL,
  total_packages INT(11) NOT NULL,
  status INT(11) NOT NULL DEFAULT 1,
  created_by INT(11) NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP(),
  updated_by INT(11) NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (id),
  INDEX fk_shipments_schedule_route_id_idx (schedule_route_id ASC),
  INDEX fk_shimpments_schedule_route_destination_id_idx (schedule_route_destination_id ASC),
  INDEX fk_shipments_driver_id_idx (driver_id ASC),
  CONSTRAINT fk_shipments_schedule_route_id
    FOREIGN KEY (schedule_route_id)
    REFERENCES schedule_route (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_shipments_schedule_route_destination_id
    FOREIGN KEY (schedule_route_destination_id)
    REFERENCES schedule_route_destination (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_shipments_driver_id
    FOREIGN KEY (driver_id)
    REFERENCES employee (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
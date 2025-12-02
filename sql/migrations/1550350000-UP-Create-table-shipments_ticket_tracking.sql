-- 1550350000 UP Create table shipments_ticket_tracking
CREATE TABLE shipments_ticket_tracking (
  id INT NOT NULL AUTO_INCREMENT,
  boarding_pass_ticket_id INT(11) NOT NULL,
  status ENUM('loaded', 'ready-to-go', 'in-transit', 'arrived-to-terminal', 'downloaded') NOT NULL,
  shipment_id INT(11) NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP(),
  created_by INT(11) NULL,
  updated_at DATETIME NULL,
  updated_by INT(11) NULL,
  PRIMARY KEY (id),
  INDEX fk_shipment_ticket_boarding_pass_ticket_id_idx (boarding_pass_ticket_id ASC),
  INDEX fk_shipment_ticket_shipment_idx (shipment_id ASC),
  CONSTRAINT fk_shipment_ticket_boarding_pass_ticket_id
    FOREIGN KEY (boarding_pass_ticket_id)
    REFERENCES boarding_pass_ticket (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_shipment_ticket_shipment
    FOREIGN KEY (shipment_id)
    REFERENCES shipments (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
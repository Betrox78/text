-- 1550347119 UP Create table shipments_parcel_packages_tracking
CREATE TABLE shipments_parcel_package_tracking (
  id INT NOT NULL AUTO_INCREMENT,
  parcel_id INT(11) NOT NULL,
  parcel_package_id INT(11) NOT NULL,
  status ENUM('loaded', 'ready-to-go', 'in-transit', 'arrived-to-terminal', 'downloaded') NOT NULL,
  shipment_id INT(11) NOT NULL,
  created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP(),
  created_by INT(11) NULL,
  updated_at DATETIME NULL,
  updated_by INT(11) NULL,
  PRIMARY KEY (id),
  INDEX fk_shipment_parcel_packages_parcel_id_idx (parcel_id ASC),
  INDEX fk_shipment_parcel_packages_parcel_packages_id_idx (parcel_package_id ASC),
  INDEX fk_shipment_parcel_shipment_idx (shipment_id ASC),
  CONSTRAINT fk_shipment_parcel_packages_parcel_id
    FOREIGN KEY (parcel_id)
    REFERENCES parcels (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_shipment_parcel_packages_parcel_packages_id
    FOREIGN KEY (parcel_package_id)
    REFERENCES parcels_packages (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT fk_shipment_parcels_packages_shipment
    FOREIGN KEY (shipment_id)
    REFERENCES shipments (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
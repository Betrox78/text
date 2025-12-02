-- 1594169474 UP shipments-parcels
CREATE TABLE `shipments_parcels` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `shipment_id` INT NOT NULL,
  `parcel_id` INT NOT NULL,
  `status` TINYINT(4) NOT NULL DEFAULT '1',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` INT(11) NULL DEFAULT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `updated_by` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`));

 ALTER TABLE `shipments_parcels`
 ADD UNIQUE INDEX `UQ_SHIPMENT_PARCELS` (`parcel_id` ASC, `shipment_id` ASC);
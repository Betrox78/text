-- 1554168693 UP Add shipment_id in parcel_incidences.
ALTER TABLE `parcels_incidences` 
ADD COLUMN `shipment_id` INT(11) NULL AFTER `incidence_id`,
ADD INDEX `fk_parcels_incidences_shipments_id_idx` (`shipment_id` ASC);

ALTER TABLE `parcels_incidences` 
ADD CONSTRAINT `fk_parcels_incidences_shipments_id`
  FOREIGN KEY (`shipment_id`)
  REFERENCES `shipments` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;
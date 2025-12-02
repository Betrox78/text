-- 1554168693 DOWN Add shipment_id in parcel_incidences.
ALTER TABLE `parcels_incidences` 
DROP FOREIGN KEY `fk_parcels_incidences_shipments_id`;
ALTER TABLE `parcels_incidences` 
DROP COLUMN `shipment_id`,
DROP INDEX `fk_parcels_incidences_shipments_id_idx` ;
-- 1749340567 DOWN shipments parcels index unique shipment_id trailer_id parcel_id
ALTER TABLE `shipments_parcels`
DROP INDEX `UQ_SHIPMENT_PARCELS`;
ALTER TABLE `shipments_parcels`
ADD UNIQUE INDEX `UQ_SHIPMENT_PARCELS` (`parcel_id` ASC, `shipment_id` ASC);
-- 1722453432 UP alter table parcels packages scanner tracking add column shipment id
ALTER TABLE parcels_packages_scanner_tracking
ADD COLUMN shipment_id INT DEFAULT NULL AFTER parcel_package_id,
ADD CONSTRAINT parcels_packages_scanner_tracking_pshipment_id_tk FOREIGN KEY (shipment_id)
	REFERENCES shipments(id) ON UPDATE NO ACTION ON DELETE NO ACTION;
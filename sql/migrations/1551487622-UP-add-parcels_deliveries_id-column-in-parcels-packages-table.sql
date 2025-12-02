-- 1551487622 UP add-parcels_deliveries_id-column-in-parcels-packages-table
ALTER TABLE parcels_packages ADD COLUMN parcels_deliveries_id int(11) DEFAULT NULL AFTER prints_counter,
ADD CONSTRAINT fk_parcels_deliveries_id FOREIGN KEY (parcels_deliveries_id) REFERENCES parcels_deliveries(id) ON DELETE NO ACTION ON UPDATE NO ACTION;
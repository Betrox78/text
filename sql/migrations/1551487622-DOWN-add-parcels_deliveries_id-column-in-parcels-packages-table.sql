-- 1551487622 DOWN add-parcels_deliveries_id-column-in-parcels-packages-table
ALTER TABLE parcels_packages
DROP FOREIGN KEY fk_parcels_deliveries_id,
DROP COLUMN parcels_deliveries_id;
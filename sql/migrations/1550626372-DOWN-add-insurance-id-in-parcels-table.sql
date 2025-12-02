-- 1550626372 DOWN add-insurance-id-in-parcels-table
ALTER TABLE parcels
DROP FOREIGN KEY fk_parcels_insurances_id,
DROP COLUMN insurance_id;

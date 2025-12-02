-- 1564505369 DOWN add cash_register_id column in parcel table
ALTER TABLE parcels
DROP FOREIGN KEY parcels_cash_register_id_fk,
DROP COLUMN cash_register_id;
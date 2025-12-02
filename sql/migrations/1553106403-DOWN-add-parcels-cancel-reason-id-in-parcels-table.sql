-- 1553106403 DOWN add parcels cancel reason id in parcels table
ALTER TABLE parcels
DROP FOREIGN KEY parcels_parcels_cancel_reason_id_fk,
DROP COLUMN parcels_cancel_reason_id;
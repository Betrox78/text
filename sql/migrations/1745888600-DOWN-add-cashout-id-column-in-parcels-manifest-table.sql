-- 1745888600 DOWN add cashout id column in parcels manifest table
ALTER TABLE parcels_manifest
DROP CONSTRAINT fk_parcels_manifest_cash_out_id,
DROP COLUMN cash_out_id;
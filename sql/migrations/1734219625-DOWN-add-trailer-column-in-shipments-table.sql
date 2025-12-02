-- 1734219625 DOWN add trailer column in shipments table
ALTER TABLE shipments
DROP CONSTRAINT fk_shipments_trailer_id,
DROP COLUMN trailer_id;
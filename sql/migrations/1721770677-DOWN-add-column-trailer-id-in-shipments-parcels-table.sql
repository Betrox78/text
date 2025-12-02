-- 1721770677 DOWN add column trailer id in shipments parcels table
ALTER TABLE shipments_parcels
DROP CONSTRAINT fk_shipments_parcels_trailer_id,
DROP COLUMN trailer_id;
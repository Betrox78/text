-- 1721770677 UP add column trailer id in shipments parcels table
ALTER TABLE shipments_parcels
ADD COLUMN trailer_id INT DEFAULT NULL AFTER shipment_id,
ADD CONSTRAINT fk_shipments_parcels_trailer_id FOREIGN KEY (trailer_id) REFERENCES trailers(id);
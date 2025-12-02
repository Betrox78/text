-- 1734219625 UP add trailer column in shipments table
ALTER TABLE shipments
ADD COLUMN trailer_id INTEGER DEFAULT NULL AFTER second_driver_id,
ADD CONSTRAINT fk_shipments_trailer_id FOREIGN KEY (trailer_id) REFERENCES trailers(id);
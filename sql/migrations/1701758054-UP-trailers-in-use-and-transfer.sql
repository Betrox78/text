-- 1701758054 UP trailers in use and transfer
ALTER TABLE trailers
ADD COLUMN in_use BOOLEAN DEFAULT FALSE AFTER plate;
CREATE INDEX in_use_trailers_idx ON trailers(in_use);

ALTER TABLE shipments_trailers
ADD COLUMN is_transfer BOOLEAN DEFAULT false AFTER trailer_id,
ADD COLUMN transfer_trailer_id INT(11) DEFAULT NULL AFTER is_transfer,
ADD CONSTRAINT fk_transfer_trailer_id_idx FOREIGN KEY (transfer_trailer_id) REFERENCES trailers(id) ON DELETE NO ACTION ON UPDATE NO ACTION;
CREATE INDEX transfer_shipments_trailers_idx ON shipments_trailers(is_transfer, transfer_trailer_id);
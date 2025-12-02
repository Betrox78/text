-- 1701758054 DOWN trailers in use and transfer
ALTER TABLE trailers
DROP COLUMN in_use;

ALTER TABLE shipments_trailers
DROP COLUMN is_transfer,
DROP COLUMN transfer_trailer_id;
-- 1703817712 UP shipments trailers latest movement
ALTER TABLE shipments_trailers
ADD COLUMN latest_movement BOOLEAN DEFAULT FALSE AFTER transfer_trailer_id;
-- 1701905720 UP action column shipments trailers
ALTER TABLE shipments_trailers
ADD COLUMN action ENUM('hitch', 'release', 'transfer') NOT NULL DEFAULT 'hitch' AFTER transfer_trailer_id;